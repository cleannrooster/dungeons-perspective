package com.cleannrooster.dungeons_iso.api.cullers.room;

import com.cleannrooster.dungeons_iso.config.Config;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ByteOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * Decides which connected terrain shapes between the camera and the player are actually hiding
 * the player, and which merely happen to fall inside the cylinder.
 *
 * <p>Three steps, all on a worker thread over a {@link ChunkView} captured on the client thread:
 * <ol>
 *   <li><b>Cast.</b> A bundle of rays from the camera to sample points across the player's hitbox,
 *       walked with a 3D DDA. Each ray records the solid voxels it enters and whether it arrived.</li>
 *   <li><b>Segment.</b> Every voxel any ray hit seeds a connected-component flood, giving the
 *       discrete terrain shapes involved. Floods are capped in size and confined to the
 *       neighbourhood of the camera-to-player segment, because terrain is otherwise all one
 *       component stretching to the horizon.</li>
 *   <li><b>Judge.</b> Each shape is scored by the fraction of sample rays it is responsible for
 *       blocking. Below the threshold the entire shape is vetoed — kept, not partly cut away.</li>
 * </ol>
 *
 * <p>This is what handles the archway case: rays through the opening arrive, so the arch scores
 * low and survives intact, while a solid wall scores high and is left to the cylinder to remove.
 */
public final class SightlineScanner implements ScanWorker.Job {

    public static final SightlineScanner INSTANCE = new SightlineScanner();

    /** Sample points per axis across the player's hitbox; 3 gives a 3x3x3 bundle of 27 rays. */
    private static final int SAMPLES_PER_AXIS = 3;
    /** Pulled in from the hitbox faces so rays do not graze the blocks beside the player. */
    private static final double HITBOX_INSET = 0.05;
    /** Rays stop short of the player so the player's own voxels never count as occluders. */
    private static final double TARGET_BACKOFF = 0.3;
    /** Voxels a single ray may traverse before it is abandoned. */
    private static final int MAX_RAY_STEPS = 256;
    /** Idle nap between budget slices, so a scan can never saturate a core. */
    private static final long SLICE_GAP_NANOS = 500_000L;
    /** Cached for the memoisation table, which stores ordinals rather than references. */
    private static final OccluderClass[] OCCLUDER_CLASSES = OccluderClass.values();
    /**
     * Marks voxels of an object that failed to resolve. Distinct from unclaimed (-1) so the hits
     * inside it are handed to the silhouette instead of re-flooding into the same failure, and
     * negative so the silhouette is still free to claim them.
     */
    private static final int DROPPED = -2;
    /**
     * Content hash for a suppressing mask. Distinct from the hash of an ordinary empty result,
     * which culls nothing for a different reason and must not be mistaken for it.
     */
    private static final long SUPPRESSED_HASH = 0x5501FEEDL;

    private final AtomicReference<CastRequest> pending = new AtomicReference<>();
    private final AtomicReference<SightlineMask> completed = new AtomicReference<>();

    /** Read by Sodium worker threads during meshing. Written only by the client thread. */
    private volatile SightlineMask published;
    /** Whether the published mask should be honoured at all. */
    private volatile boolean active;


    /**
     * Occluder class per block state, memoised.
     *
     * <p>TREE floods 26-connected, so a thousand-block canopy asks the question ~26,000 times, and
     * each answer walks up to eleven tag lists. Block states are interned, so identity lookup is
     * exact and this turns the hot inner test into a single hash probe. Only the scanner's own
     * worker thread touches it, so it needs no synchronisation.
     */
    private final Reference2ByteOpenHashMap<BlockState> classCache = new Reference2ByteOpenHashMap<>();
    /** Trunk-or-not per block state, memoised for the same reason. */
    private final Reference2ByteOpenHashMap<BlockState> logCache = new Reference2ByteOpenHashMap<>();

    private long sliceStart;
    private int sliceWork;

    // Diagnostics — see the matching block in RoomScanner.
    public volatile String lastResult = "never run";
    public volatile int lastRays;
    public volatile int lastClearRays;
    public volatile int lastShapesFound;
    public volatile int lastShapesIncomplete;
    public volatile int lastShapesBelowThreshold;
    public volatile int lastShapesCulled;
    public volatile String lastShapeClasses = "";
    public volatile int lastUnresolvedByCap;
    public volatile int lastUnresolvedBySpan;
    public volatile int lastUnresolvedByHeight;
    /** Objects that failed to resolve and were handed to the silhouette instead of dropped. */
    public volatile int lastShapesFellBack;
    public volatile int castCount;

    private SightlineScanner() {
        this.classCache.defaultReturnValue((byte) -1);
        this.logCache.defaultReturnValue((byte) -1);
    }

    // ------------------------------------------------------------------ read side (any thread)

    public SightlineMask mask() {
        return this.published;
    }

    public boolean isActive() {
        return this.active;
    }

    /**
     * The sole authority on whether a block between the camera and the player is removed. Returns
     * false whenever there is no mask yet — a scan still in flight culls nothing rather than
     * handing the decision back to a cylinder.
     */
    public boolean shouldCull(int x, int y, int z) {
        if (!this.active) {
            return false;
        }
        SightlineMask m = this.published;
        return m != null && m.isOccluding(x, y, z);
    }

    /**
     * Turns shape culling on or off, returning the sections needing a rebuild when the state
     * actually changed. Culling is baked into chunk meshes, so flipping this without rebuilding
     * would leave sections frozen in their previous state.
     */
    public LongOpenHashSet setActive(boolean value) {
        if (this.active == value) {
            return null;
        }
        this.active = value;

        SightlineMask m = this.published;
        if (m == null || m.sections().isEmpty()) {
            return null;
        }
        return new LongOpenHashSet(m.sections());
    }

    // ------------------------------------------------------------------ client thread

    /**
     * Captures chunk references, camera position and player hitbox samples, and queues a cast.
     * Must be called on the client thread.
     */
    public void requestScan(World world, Vec3d cameraPos, Box playerBox) {
        double dist = cameraPos.distanceTo(playerBox.getCenter());
        int radius = (int) Math.ceil(dist) + 8;

        Vec3d center = playerBox.getCenter();
        ChunkView view = ChunkView.capture(world, (int) Math.floor(center.x), (int) Math.floor(center.z), radius);
        if (view == null) {
            this.lastResult = "chunk capture failed (centre chunk not loaded)";
            return;
        }

        // The lowest block that may be removed is the one *above* whatever is being stood on.
        //
        // Taking floor(minY) assumed feet land on a block boundary, which only holds for full
        // blocks. On a path, a slab or a carpet the feet sit partway up, floor() returns that same
        // block, and the thing underfoot ends up inside the cull range and disappears. Rounding up
        // instead names the first block genuinely above the surface, whatever its height:
        // standing on a path at y=64 the feet are at 64.9375, so 65 is the first cullable block.
        int minCullY = (int) Math.ceil(playerBox.minY);

        this.pending.set(new CastRequest(view, cameraPos, sampleHitbox(playerBox),
                world.getRegistryKey(), minCullY));
        ensureWorker();
    }

    /** Installs a finished mask and returns the sections needing a rebuild, or null. */
    public LongOpenHashSet pollCompleted() {
        SightlineMask next = this.completed.getAndSet(null);
        if (next == null) {
            return null;
        }

        SightlineMask old = this.published;

        // Discard a cast that produced the same answer, keeping the old object. The camera
        // retriggers this several times a second, and republishing an identical mask queued every
        // section it touched for a rebuild into the mesh it already had — and, because the ghost
        // cache keys on mask identity, threw that cache away just as often.
        if (old != null && old.contentHash() == next.contentHash()) {
            return null;
        }

        this.published = next;
        if (old == null) {
            return next.sections().isEmpty() ? null : new LongOpenHashSet(next.sections());
        }

        LongOpenHashSet dirty = SectionRebuildQueue.diff(old.sectionHashes(), next.sectionHashes());
        return dirty.isEmpty() ? null : dirty;
    }

    /** Drops all state, e.g. on world change. Returns sections needing a rebuild, or null. */
    public LongOpenHashSet reset() {
        this.pending.set(null);
        this.completed.set(null);

        SightlineMask old = this.published;
        this.published = null;
        if (old == null || old.sections().isEmpty()) {
            return null;
        }
        return new LongOpenHashSet(old.sections());
    }

    /** A grid of target points spanning the player's hitbox, flattened as x,y,z triples. */
    private static double[] sampleHitbox(Box box) {
        int n = SAMPLES_PER_AXIS;
        double[] out = new double[n * n * n * 3];

        double minX = box.minX + HITBOX_INSET, maxX = box.maxX - HITBOX_INSET;
        double minY = box.minY + HITBOX_INSET, maxY = box.maxY - HITBOX_INSET;
        double minZ = box.minZ + HITBOX_INSET, maxZ = box.maxZ - HITBOX_INSET;

        int i = 0;
        for (int ix = 0; ix < n; ix++) {
            for (int iy = 0; iy < n; iy++) {
                for (int iz = 0; iz < n; iz++) {
                    out[i++] = lerp(minX, maxX, ix, n);
                    out[i++] = lerp(minY, maxY, iy, n);
                    out[i++] = lerp(minZ, maxZ, iz, n);
                }
            }
        }
        return out;
    }

    private static double lerp(double min, double max, int index, int count) {
        return count == 1 ? (min + max) * 0.5 : min + (max - min) * index / (count - 1.0);
    }

    // ------------------------------------------------------------------ worker thread

    private void ensureWorker() {
        ScanWorker.INSTANCE.submit(this);
    }

    /**
     * One pass for {@link ScanWorker}: run a queued cast if there is one. The shared thread owns
     * the loop and the idle parking, so this only has to do the work in front of it.
     */
    @Override
    public boolean runOnce() {
        CastRequest request = this.pending.getAndSet(null);
        if (request == null) {
            return false;
        }
        try {
            SightlineMask result = cast(request);
            if (result != null) {
                this.completed.set(result);
            }
        } catch (Preempted ignored) {
            // A newer request arrived; it is still queued and will be picked up next pass.
        } catch (Throwable ignored) {
            // Never let a failed cast kill the worker.
        }
        return true;
    }

    private SightlineMask cast(CastRequest req) {
        beginSlice();
        this.castCount++;

        int rayCount = req.targets.length / 3;
        if (rayCount == 0) {
            this.lastResult = "no sample rays";
            return null;
        }
        this.lastRays = rayCount;

        // Step 1 — cast. rayHits[i] is the solid voxels ray i entered, in order.
        List<LongArrayList> rayHits = new ArrayList<>(rayCount);
        int clearRays = 0;
        for (int i = 0; i < rayCount; i++) {
            tickBudget();
            LongArrayList hits = new LongArrayList();
            traverse(req, req.targets[i * 3], req.targets[i * 3 + 1], req.targets[i * 3 + 2], hits);
            rayHits.add(hits);
            if (hits.isEmpty()) {
                clearRays++;
            }
        }

        float visibleFraction = clearRays / (float) rayCount;
        this.lastClearRays = clearRays;

        float suppressAt = clamp01(Config.GSON.instance().sightlineSuppressThreshold);
        if (visibleFraction >= suppressAt) {
            // Essentially nothing is in the way, so nothing is removed.
            this.lastResult = "suppressed: " + clearRays + "/" + rayCount + " rays reached the player";
            this.lastShapesFound = 0;
            this.lastShapesIncomplete = 0;
            this.lastShapesBelowThreshold = 0;
            this.lastShapesCulled = 0;
            this.lastUnresolvedByCap = 0;
            this.lastUnresolvedBySpan = 0;
            this.lastUnresolvedByHeight = 0;
            this.lastShapesFellBack = 0;
            this.lastShapeClasses = "(tree=0 built=0 terrain=0)";
            return new SightlineMask(new LongOpenHashSet(), new Long2LongOpenHashMap(),
                    SUPPRESSED_HASH, visibleFraction, true, 0, 0, 0);
        }

        // Step 2 — segment every hit voxel into a shape, bounded by occluder class.
        Long2IntOpenHashMap voxelToShape = new Long2IntOpenHashMap();
        voxelToShape.defaultReturnValue(-1);
        List<Shape> shapes = new ArrayList<>();
        LongArrayList terrainHits = new LongArrayList();
        // Hits belonging to objects that failed to resolve. They fall through to the silhouette
        // rather than being dropped, so a tree the flood could not bound still gets an opening.
        LongArrayList fallbackHits = new LongArrayList();
        int fellBack = 0;

        // Segmenting occluders into whole objects is optional, and off by default. Bounding a
        // tree turned out to be a running battle — canopies touch, so a flood walks the forest;
        // trunk-relative leash, size caps and extent bounds each fixed one case and exposed the
        // next. The silhouette path has none of that: it is bounded by construction, always
        // resolves, and never has an opinion about where one object ends.
        //
        // What made whole-object removal worth the trouble was avoiding a bore through solid
        // geometry. The ghost pass now draws removed blocks back translucently, so a silhouette
        // opening reads as the tree going see-through where you are behind it, and the argument
        // for segmenting mostly evaporates.
        boolean unified = Config.GSON.instance().unifiedSilhouette;
        boolean groundAllowed = Config.GSON.instance().terrainSilhouetteCulling;
        int shapeCap = Math.max(64, Config.GSON.instance().terrainShapeCap);

        for (LongArrayList hits : rayHits) {
            for (LongIterator it = hits.iterator(); it.hasNext(); ) {
                long packed = it.nextLong();
                int claim = voxelToShape.get(packed);
                if (claim >= 0) {
                    continue;
                }
                if (claim == DROPPED) {
                    // Inside an object that already failed to resolve; hand it to the silhouette
                    // instead of flooding it again to fail the same way.
                    fallbackHits.add(packed);
                    continue;
                }
                if (unified) {
                    // Honour the natural-ground toggle here too, or it would silently do nothing
                    // in the mode that is now the default. The classification only runs when the
                    // toggle is off, so the common path pays nothing for it.
                    if (!groundAllowed && classOf(req.view.getBlockState(
                            BlockPos.unpackLongX(packed),
                            BlockPos.unpackLongY(packed),
                            BlockPos.unpackLongZ(packed))) == OccluderClass.TERRAIN) {
                        continue;
                    }
                    terrainHits.add(packed);
                    continue;
                }

                OccluderClass cls = classOf(req.view.getBlockState(
                        BlockPos.unpackLongX(packed),
                        BlockPos.unpackLongY(packed),
                        BlockPos.unpackLongZ(packed)));

                if (cls == OccluderClass.TERRAIN) {
                    // Deferred: natural ground has no component boundary, so every terrain hit is
                    // pooled into one silhouette shape below instead of flooded individually.
                    terrainHits.add(packed);
                    continue;
                }

                Shape shape = new Shape(cls);
                // Trees need far more headroom than built structures: a dark oak or jungle canopy
                // runs to several thousand blocks, and interlocking canopies merge into one shape.
                int cap = cls == OccluderClass.TREE
                        ? Math.max(256, Config.GSON.instance().treeShapeCap)
                        : shapeCap;
                floodShape(req, packed, shapes.size(), voxelToShape, shape, cap);

                if (shape.complete) {
                    shapes.add(shape);
                } else {
                    // Could not be bounded — a canopy that ran into its neighbours, a structure
                    // past its extent limit. Rather than discard it and leave the player hidden,
                    // release the claim and let the silhouette cut through it. Object detection
                    // still gets first refusal on everything; this is only what happens when it
                    // cannot answer.
                    for (LongIterator bit = shape.blocks.iterator(); bit.hasNext(); ) {
                        voxelToShape.put(bit.nextLong(), DROPPED);
                    }
                    fallbackHits.add(packed);
                    fellBack++;
                }
            }
        }

        // Natural ground is opt-in; objects that failed to resolve always fall through, since the
        // alternative there is leaving the player behind something with nothing removed at all.
        LongArrayList silhouetteSeeds = new LongArrayList();
        if (unified || groundAllowed) {
            silhouetteSeeds.addAll(terrainHits);
        }
        silhouetteSeeds.addAll(fallbackHits);

        if (!silhouetteSeeds.isEmpty()) {
            Shape shape = new Shape(OccluderClass.TERRAIN);
            buildSilhouette(req, silhouetteSeeds, shapes.size(), voxelToShape, shape,
                    Math.max(0, Config.GSON.instance().terrainSilhouetteDilation));
            if (!shape.blocks.isEmpty()) {
                shapes.add(shape);
            }
        }
        this.lastShapesFellBack = fellBack;

        // Step 3 — score each shape by how many sample rays it is responsible for blocking.
        IntOpenHashSet seen = new IntOpenHashSet();
        for (LongArrayList hits : rayHits) {
            tickBudget();
            if (hits.isEmpty()) {
                continue;
            }
            seen.clear();
            for (LongIterator it = hits.iterator(); it.hasNext(); ) {
                int id = voxelToShape.get(it.nextLong());
                if (id >= 0 && seen.add(id)) {
                    shapes.get(id).blockedRays++;
                }
            }
        }

        // Step 4 — pick which whole shapes to remove.
        float occludeAt = clamp01(Config.GSON.instance().terrainOccludeThreshold);
        int maxShapes = Math.max(1, Config.GSON.instance().sightlineMaxShapes);
        int maxBlocks = Math.max(256, Config.GSON.instance().sightlineMaxCulledBlocks);

        List<Shape> candidates = new ArrayList<>();
        int incomplete = 0;
        int belowThreshold = 0;
        int byCap = 0, bySpan = 0, byHeight = 0;

        for (Shape shape : shapes) {
            tickBudget();
            if (!shape.complete) {
                // A fragment of something larger. Culling it would leave a ragged bite out of the
                // world — precisely the artifact the shape approach exists to avoid — so it is
                // skipped whole rather than partly removed. The reason is recorded because "cap"
                // and "reach" call for different config changes.
                incomplete++;
                if ("cap".equals(shape.incompleteReason)) {
                    byCap++;
                } else if ("span".equals(shape.incompleteReason)) {
                    bySpan++;
                } else if ("height".equals(shape.incompleteReason)) {
                    byHeight++;
                }
                continue;
            }
            if (shape.blockedRays / (float) rayCount < occludeAt) {
                // Not meaningfully hiding the player: the legs of an archway seen through, a spur
                // of hillside off to one side. Left standing, whole.
                belowThreshold++;
                continue;
            }
            if (shape.maxY < req.minCullY) {
                // Entirely below the block being stood on — the ground, not an occluder.
                continue;
            }
            candidates.add(shape);
        }

        // Nearest the player first, so a budget shortfall drops the shapes furthest away — the ones
        // least likely to be hiding anything the player cares about.
        candidates.sort(Comparator.comparingDouble(s -> s.minDistSq));

        LongOpenHashSet occluding = new LongOpenHashSet();
        int taken = 0;
        int tree = 0, built = 0, terrain = 0;
        for (Shape shape : candidates) {
            tickBudget();
            if (taken >= maxShapes || occluding.size() + shape.blocks.size() > maxBlocks) {
                // Out of budget. Stopping here leaves fewer complete shapes removed, which is the
                // intended degradation — never a partial shape, and never a cylinder.
                break;
            }
            occluding.addAll(shape.blocks);
            taken++;
            switch (shape.cls) {
                case TREE -> tree++;
                case BUILT -> built++;
                case TERRAIN -> terrain++;
            }
        }
        this.lastShapeClasses = "(tree=" + tree + " built=" + built + " terrain=" + terrain + ")";

        this.lastShapesFound = shapes.size();
        this.lastShapesIncomplete = incomplete;
        this.lastShapesBelowThreshold = belowThreshold;
        this.lastShapesCulled = taken;
        this.lastUnresolvedByCap = byCap;
        this.lastUnresolvedBySpan = bySpan;
        this.lastUnresolvedByHeight = byHeight;
        this.lastResult = taken > 0
                ? "ok"
                : "no shape qualified (" + incomplete + " unresolved [cap=" + byCap + " span="
                        + bySpan + " height=" + byHeight + "], " + belowThreshold
                        + " below occlusion threshold, of " + shapes.size() + " found)";

        return new SightlineMask(occluding, buildSections(occluding), contentHash(occluding),
                visibleFraction, false, shapes.size(), taken, incomplete);
    }

    /**
     * Amanatides-Woo voxel traversal from the camera toward one sample point, collecting the solid
     * voxels entered. Stops short of the target so the player's own voxels never count.
     */
    private void traverse(CastRequest req, double tx, double ty, double tz, LongArrayList hits) {
        double ox = req.cameraX, oy = req.cameraY, oz = req.cameraZ;
        double dx = tx - ox, dy = ty - oy, dz = tz - oz;

        double length = Math.sqrt(dx * dx + dy * dy + dz * dz) - TARGET_BACKOFF;
        if (length <= 0) {
            return;
        }
        double inv = 1.0 / (length + TARGET_BACKOFF);
        double ux = dx * inv, uy = dy * inv, uz = dz * inv;

        int x = (int) Math.floor(ox), y = (int) Math.floor(oy), z = (int) Math.floor(oz);
        int stepX = signum(ux), stepY = signum(uy), stepZ = signum(uz);

        double tDeltaX = ux == 0 ? Double.MAX_VALUE : Math.abs(1.0 / ux);
        double tDeltaY = uy == 0 ? Double.MAX_VALUE : Math.abs(1.0 / uy);
        double tDeltaZ = uz == 0 ? Double.MAX_VALUE : Math.abs(1.0 / uz);

        double tMaxX = boundary(ox, ux, x);
        double tMaxY = boundary(oy, uy, y);
        double tMaxZ = boundary(oz, uz, z);

        double travelled = 0;
        // The camera's own voxel is skipped: the first iteration advances before classifying.
        for (int step = 0; step < MAX_RAY_STEPS && travelled < length; step++) {
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                x += stepX; travelled = tMaxX; tMaxX += tDeltaX;
            } else if (tMaxY < tMaxZ) {
                y += stepY; travelled = tMaxY; tMaxY += tDeltaY;
            } else {
                z += stepZ; travelled = tMaxZ; tMaxZ += tDeltaZ;
            }
            if (travelled >= length) {
                break;
            }
            if (req.view.classify(x, y, z) == ChunkView.SOLID) {
                hits.add(BlockPos.asLong(x, y, z));
            }
        }
    }

    private static double boundary(double origin, double dir, int voxel) {
        if (dir == 0) {
            return Double.MAX_VALUE;
        }
        return dir > 0 ? (voxel + 1 - origin) / dir : (voxel - origin) / dir;
    }

    private static int signum(double v) {
        return v > 0 ? 1 : (v < 0 ? -1 : 0);
    }

    /**
     * Connected-component flood over solid blocks, confined to a corridor around the
     * camera-to-player segment and capped in size. Terrain is one component all the way to the
     * horizon, so both limits are load-bearing: they are what turn "the ground" into "this outcrop".
     */
    private void floodShape(CastRequest req, long seed, int id,
                            Long2IntOpenHashMap voxelToShape, Shape out, int cap) {
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        queue.enqueue(seed);
        voxelToShape.put(seed, id);
        out.record(seed, req);

        int seedX = BlockPos.unpackLongX(seed);
        int seedY = BlockPos.unpackLongY(seed);
        int seedZ = BlockPos.unpackLongZ(seed);

        // Canopies join diagonally at the corners, so face-only connectivity splits one tree into
        // several shapes — each too small to clear the occlusion threshold, so none get culled.
        boolean tree = out.cls == OccluderClass.TREE;
        boolean diagonal = tree;

        Long2IntOpenHashMap leafDepth = null;
        if (tree) {
            leafDepth = new Long2IntOpenHashMap();
            leafDepth.defaultReturnValue(0);
            leafDepth.put(seed, isLog(req.view.getBlockState(seedX, seedY, seedZ)) ? 0 : 1);
        }

        while (!queue.isEmpty()) {
            tickBudget();
            long packed = queue.dequeueLong();
            int x = BlockPos.unpackLongX(packed);
            int y = BlockPos.unpackLongY(packed);
            int z = BlockPos.unpackLongZ(packed);
            int depth = tree ? leafDepth.get(packed) : 0;

            for (int n = 0; n < 26; n++) {
                int dx = (n % 3) - 1;
                int dy = ((n / 3) % 3) - 1;
                int dz = (n / 9) - 1;
                if (dx == 0 && dy == 0 && dz == 0) {
                    continue;
                }
                if (!diagonal && (Math.abs(dx) + Math.abs(dy) + Math.abs(dz)) != 1) {
                    continue;
                }

                int nx = x + dx, ny = y + dy, nz = z + dz;
                long neighbour = BlockPos.asLong(nx, ny, nz);
                if (voxelToShape.get(neighbour) >= 0) {
                    continue;
                }
                if (req.view.classify(nx, ny, nz) != ChunkView.SOLID) {
                    continue;
                }
                // Logs and leaves stop at the ground, so a tree resolves as a complete object
                // instead of dragging the whole world in through the dirt its trunk stands on.
                BlockState neighbourState = req.view.getBlockState(nx, ny, nz);
                if (classOf(neighbourState) != out.cls) {
                    continue;
                }

                if (tree) {
                    // Leaves count how far they are from the nearest log; a log resets the count.
                    // Without this the flood walks a forest canopy from tree to tree - leaves touch
                    // diagonally and 26-connectivity happily follows them - until it hits the leash
                    // 32 blocks out and the whole merged blob is discarded as unresolved. That was
                    // every single tree failure in the log: cap=0, reach=N, every time.
                    //
                    // This is a real boundary rather than a truncation, since a canopy genuinely
                    // ends a few blocks from its trunk, so shapes stopped here stay complete.
                    int childDepth = isLog(neighbourState) ? 0 : depth + 1;
                    if (childDepth > req.leafSpread) {
                        continue;
                    }
                    leafDepth.put(neighbour, childDepth);
                }

                // Bound the shape by its own extent rather than by a radius from the seed. A
                // radius depends on where the ray happened to hit: a large jungle tree is thirty
                // blocks tall, so a seed at the trunk base put the far canopy right on the leash
                // while a seed in the canopy passed comfortably. Extent is seed-independent.
                //
                // Width and height are limited separately because that is what actually separates
                // a tree from a forest. Trees are tall and narrow; a canopy sprawling from tree to
                // tree is wide and flat, so it trips the width limit long before the height one.
                if (Math.max(out.maxX, nx) - Math.min(out.minX, nx) > req.maxSpan
                        || Math.max(out.maxZ, nz) - Math.min(out.minZ, nz) > req.maxSpan) {
                    out.complete = false;
                    out.incompleteReason = "span";
                    continue;
                }
                if (Math.max(out.maxY, ny) - Math.min(out.minY, ny) > req.maxHeight) {
                    out.complete = false;
                    out.incompleteReason = "height";
                    continue;
                }
                if (out.blocks.size() >= cap) {
                    out.complete = false;
                    out.incompleteReason = "cap";
                    return;
                }
                voxelToShape.put(neighbour, id);
                out.record(neighbour, req);
                queue.enqueue(neighbour);
            }
        }
    }

    /**
     * Builds the terrain shape as the player's silhouette rather than as a component.
     *
     * <p>Natural ground has no boundary to find — it is one mass to the horizon at every scale — so
     * there is no "whole object" to remove and no size cap that makes one appear. What does have a
     * boundary is the material actually standing in the way: the voxels the rays hit, grown a few
     * blocks outward so the opening has thickness rather than being a per-ray speckle. The result
     * is bounded by construction and always complete, and the hole it leaves is the shape of you as
     * seen from the camera, not a cylinder.
     */
    private void buildSilhouette(CastRequest req, LongArrayList seeds, int id,
                                 Long2IntOpenHashMap voxelToShape, Shape out, int dilation) {
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        Long2IntOpenHashMap depth = new Long2IntOpenHashMap();
        depth.defaultReturnValue(-1);

        for (LongIterator it = seeds.iterator(); it.hasNext(); ) {
            long packed = it.nextLong();
            if (voxelToShape.get(packed) >= 0) {
                continue;
            }
            // Never take ground from under the player. The shape-level "entirely below the feet"
            // guard cannot help here: the silhouette is one pooled shape, so a wall in front and
            // the floor underfoot share a maxY and stand or fall together. Terrain below the feet
            // cannot be hiding the player from a camera that sits above them anyway.
            if (BlockPos.unpackLongY(packed) < req.minCullY) {
                continue;
            }
            voxelToShape.put(packed, id);
            depth.put(packed, 0);
            out.record(packed, req);
            queue.enqueue(packed);
        }

        while (!queue.isEmpty()) {
            tickBudget();
            long packed = queue.dequeueLong();
            int d = depth.get(packed);
            if (d >= dilation) {
                continue;
            }

            int x = BlockPos.unpackLongX(packed);
            int y = BlockPos.unpackLongY(packed);
            int z = BlockPos.unpackLongZ(packed);

            for (int face = 0; face < 6; face++) {
                int nx = x + (face == 0 ? 1 : face == 1 ? -1 : 0);
                int ny = y + (face == 2 ? 1 : face == 3 ? -1 : 0);
                int nz = z + (face == 4 ? 1 : face == 5 ? -1 : 0);

                if (ny < req.minCullY) {
                    continue;
                }
                long neighbour = BlockPos.asLong(nx, ny, nz);
                if (voxelToShape.get(neighbour) >= 0) {
                    continue;
                }
                if (req.view.classify(nx, ny, nz) != ChunkView.SOLID) {
                    continue;
                }
                // The opening grows through any block, not just natural ground. Refusing by
                // class left leaves, planks and walls standing in the middle of a hole that had
                // already taken the ground out from around them.
                //
                // Nothing is needed to protect objects the component path resolved: it runs first
                // and claims their voxels, and claimed voxels are skipped above. Only material no
                // culler has spoken for is reachable here.
                voxelToShape.put(neighbour, id);
                depth.put(neighbour, d + 1);
                queue.enqueue(neighbour);

                out.record(neighbour, req);
            }
        }

        // Bounded by the dilation radius, so it is finished by definition.
        out.complete = true;
    }


    /** Squared distance from a point to the camera-to-player segment. */
    private static double distToSegmentSq(CastRequest req, double px, double py, double pz) {
        double ax = req.cameraX, ay = req.cameraY, az = req.cameraZ;
        double bx = req.targetX, by = req.targetY, bz = req.targetZ;

        double abx = bx - ax, aby = by - ay, abz = bz - az;
        double lenSq = abx * abx + aby * aby + abz * abz;
        if (lenSq <= 1.0E-6) {
            double dx = px - ax, dy = py - ay, dz = pz - az;
            return dx * dx + dy * dy + dz * dz;
        }

        double t = ((px - ax) * abx + (py - ay) * aby + (pz - az) * abz) / lenSq;
        t = Math.max(0, Math.min(1, t));

        double cx = ax + abx * t, cy = ay + aby * t, cz = az + abz * t;
        double dx = px - cx, dy = py - cy, dz = pz - cz;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Sections touched by the cull set, plus one block of slack in every direction so that faces
     * exposed by the removal are re-meshed even when the neighbour lives in the next section.
     */
    /**
     * Sections touched by the cull set, plus one block of slack in every direction so faces exposed
     * by the removal are re-meshed even when the neighbour lives in the next section.
     *
     * <p>Only blocks sitting on a section boundary can reach into a neighbouring section, so the
     * slack is applied to those alone. Expanding all three axes for every block did the same job
     * with twenty-seven inserts each instead of one.
     */
    private static Long2LongOpenHashMap buildSections(LongOpenHashSet blocks) {
        Long2LongOpenHashMap sections = new Long2LongOpenHashMap();
        for (LongIterator it = blocks.iterator(); it.hasNext(); ) {
            long packed = it.nextLong();
            int x = BlockPos.unpackLongX(packed);
            int y = BlockPos.unpackLongY(packed);
            int z = BlockPos.unpackLongZ(packed);
            int sx = x >> 4;
            int sy = y >> 4;
            int sz = z >> 4;

            // Folded into every section this block affects, including the neighbours it only
            // reaches through the one-block slack, so a section's hash covers everything that can
            // change its mesh.
            long contribution = SectionRebuildQueue.mix(packed);

            addSection(sections, sx, sy, sz, contribution);
            if ((x & 15) == 0) addSection(sections, sx - 1, sy, sz, contribution);
            if ((x & 15) == 15) addSection(sections, sx + 1, sy, sz, contribution);
            if ((y & 15) == 0) addSection(sections, sx, sy - 1, sz, contribution);
            if ((y & 15) == 15) addSection(sections, sx, sy + 1, sz, contribution);
            if ((z & 15) == 0) addSection(sections, sx, sy, sz - 1, contribution);
            if ((z & 15) == 15) addSection(sections, sx, sy, sz + 1, contribution);
        }
        return sections;
    }

    private static void addSection(Long2LongOpenHashMap sections, int sx, int sy, int sz, long contribution) {
        long key = ChunkSectionPos.asLong(sx, sy, sz);
        sections.put(key, sections.get(key) + contribution);
    }

    /** Order-independent hash of the cull set, for recognising an unchanged cast. */
    private static long contentHash(LongOpenHashSet blocks) {
        long h = blocks.size() * 0x9E3779B97F4A7C15L;
        for (LongIterator it = blocks.iterator(); it.hasNext(); ) {
            h += SectionRebuildQueue.mix(it.nextLong());
        }
        return h;
    }

    private static float clamp01(float v) {
        return Math.max(0F, Math.min(1F, v));
    }

    // ------------------------------------------------------------------ budget

    /** Memoised trunk test, for the canopy leash. Worker thread only. */
    private boolean isLog(BlockState state) {
        byte cached = this.logCache.getByte(state);
        if (cached >= 0) {
            return cached != 0;
        }
        boolean computed = OccluderClass.isTrunk(state);
        this.logCache.put(state, (byte) (computed ? 1 : 0));
        return computed;
    }

    /** Memoised {@link OccluderClass#of}. Worker thread only. */
    private OccluderClass classOf(BlockState state) {
        byte cached = this.classCache.getByte(state);
        if (cached >= 0) {
            return OCCLUDER_CLASSES[cached];
        }
        OccluderClass computed = OccluderClass.of(state);
        this.classCache.put(state, (byte) computed.ordinal());
        return computed;
    }

    private void beginSlice() {
        this.sliceStart = System.nanoTime();
        this.sliceWork = 0;
    }

    private void tickBudget() {
        this.sliceWork++;
        if (this.sliceWork < 2048 && System.nanoTime() - this.sliceStart < 400_000L) {
            return;
        }
        if (this.pending.get() != null) {
            throw Preempted.INSTANCE;
        }
        LockSupport.parkNanos(SLICE_GAP_NANOS);
        beginSlice();
    }

    // ------------------------------------------------------------------ types

    /**
     * One connected run of solid blocks, treated as an all-or-nothing unit.
     *
     * <p>{@code complete} is the important field. A shape whose flood stopped because it ran out of
     * solid neighbours is a real object with real edges and can be removed wholesale. A shape that
     * stopped because it hit the size cap or walked out of the search corridor is a fragment of
     * something larger — a hillside, a cliff — and removing it would take a ragged bite out of the
     * world. Those are skipped rather than partly culled.
     */
    private static final class Shape {
        final LongArrayList blocks = new LongArrayList();
        final OccluderClass cls;
        boolean complete = true;
        double minDistSq = Double.MAX_VALUE;
        int blockedRays;

        // Bounding box, kept live so the flood can be bounded by extent.
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        String incompleteReason = "";

        Shape(OccluderClass cls) {
            this.cls = cls;
        }

        void record(long packed, CastRequest req) {
            this.blocks.add(packed);

            int x = BlockPos.unpackLongX(packed);
            int y = BlockPos.unpackLongY(packed);
            int z = BlockPos.unpackLongZ(packed);

            if (x < this.minX) this.minX = x;
            if (x > this.maxX) this.maxX = x;
            if (y < this.minY) this.minY = y;
            if (y > this.maxY) this.maxY = y;
            if (z < this.minZ) this.minZ = z;
            if (z > this.maxZ) this.maxZ = z;
            double dx = x + 0.5 - req.targetX;
            double dy = y + 0.5 - req.targetY;
            double dz = z + 0.5 - req.targetZ;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < this.minDistSq) {
                this.minDistSq = distSq;
            }
        }
    }

    private static final class CastRequest {
        final ChunkView view;
        final double cameraX, cameraY, cameraZ;
        final double targetX, targetY, targetZ;
        final double[] targets;
        final RegistryKey<World> dimension;
        final int maxSpan;
        final int maxHeight;
        final int leafSpread;
        /** Lowest Y the silhouette may remove. Everything below is the ground being stood on. */
        final int minCullY;

        CastRequest(ChunkView view, Vec3d camera, double[] targets, RegistryKey<World> dimension,
                    int minCullY) {
            this.view = view;
            this.cameraX = camera.x;
            this.cameraY = camera.y;
            this.cameraZ = camera.z;
            this.targets = targets;
            this.dimension = dimension;
            this.maxSpan = Math.max(4, Math.min(96, Config.GSON.instance().shapeMaxSpan));
            this.maxHeight = Math.max(4, Math.min(160, Config.GSON.instance().shapeMaxHeight));
            this.leafSpread = Math.max(1, Math.min(16, Config.GSON.instance().treeLeafSpread));
            this.minCullY = minCullY;
            // Segment endpoint: the centre sample, used for the corridor test and for ordering
            // shapes by how close they are to the player.
            int mid = (targets.length / 3 / 2) * 3;
            this.targetX = targets[mid];
            this.targetY = targets[mid + 1];
            this.targetZ = targets[mid + 2];
        }
    }

    /** Control-flow signal, not an error — no stack trace, allocated once. */
    private static final class Preempted extends RuntimeException {
        static final Preempted INSTANCE = new Preempted();

        private Preempted() {
            super(null, null, false, false);
        }
    }
}
