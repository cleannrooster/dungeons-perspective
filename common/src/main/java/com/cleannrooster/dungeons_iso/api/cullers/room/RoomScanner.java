package com.cleannrooster.dungeons_iso.api.cullers.room;

import com.cleannrooster.dungeons_iso.config.Config;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * Works out which blocks form the roof of the room the player is standing in, off the client
 * thread and under a budget.
 *
 * <p>The pipeline has four stages:
 * <ol>
 *   <li><b>Client thread, microseconds.</b> {@link #requestScan} captures chunk references for the
 *       search box and hands them to the worker. This is the only stage that touches the world.</li>
 *   <li><b>Worker thread, budgeted.</b> Breadth-first flood through passable cells, then a per-column
 *       roof solve, then a 2D pass that closes the footprint so walls and pillars are included.</li>
 *   <li><b>Client thread.</b> {@link #pollCompleted} swaps the finished snapshot in and reports which
 *       sections need re-meshing, for the caller to throttle.</li>
 *   <li><b>Sodium workers.</b> {@link #snapshot()} is read lock-free during chunk meshing.</li>
 * </ol>
 *
 * <p>The flood is breadth-first rather than depth-first on purpose: a budget-truncated BFS is a
 * clean disc centred on the player that grows outward, whereas a truncated DFS is a tendril
 * snaking off through the dungeon. It makes partial results look deliberate.
 */
public final class RoomScanner implements ScanWorker.Job {

    public static final RoomScanner INSTANCE = new RoomScanner();

    /** How far above the player the flood may travel, and how far a roof may be searched for. */
    private static final int MAX_VERTICAL_UP = 24;
    /** How far below the player the flood may travel, for sunken floors and stairs. */
    private static final int MAX_VERTICAL_DOWN = 8;
    /** Extra height above the flood ceiling to keep looking for a solid block. */
    private static final int ROOF_SEARCH_MARGIN = 4;
    /** Fallback for {@link Config#roomCoverHeight} when it is configured to nonsense. */
    private static final int DEFAULT_COVER_HEIGHT = 24;
    /** Above this fraction of open-sky columns the space is not a room; stand down entirely. */
    private static final double OPEN_SKY_BAIL_RATIO = 0.5;
    /** Hard ceiling on the 2D closing grid, so a huge room cannot allocate without bound. */
    private static final int MAX_GRID_CELLS = 512 * 512;
    /** Passes allowed when filling enclosed holes in the footprint. */
    private static final int MAX_HOLE_FILL_PASSES = 32;
    /** Idle nap between budget slices, so a scan can never saturate a core. */
    private static final long SLICE_GAP_NANOS = 1_000_000L;
    /** Returned by {@link #ceilingOf} for a column open to the sky. */
    private static final int NO_CEILING = Integer.MAX_VALUE;

    // Grid cell states used by the footprint closing pass.
    private static final byte CELL_EMPTY = 0;
    private static final byte CELL_ROOM = 1;
    private static final byte CELL_CLAIMED = 2;
    private static final byte CELL_OPEN_SKY = 3;

    private final AtomicReference<ScanRequest> pending = new AtomicReference<>();
    private final AtomicReference<RoomSnapshot> completed = new AtomicReference<>();

    /** Read by Sodium worker threads during meshing. Written only by the client thread. */
    private volatile RoomSnapshot published;
    /** Whether the published snapshot should be honoured at all. */
    private volatile boolean active;


    // Diagnostics. Every path that ends a scan without publishing records why, because those are
    // exactly the outcomes that are invisible from the game and unprovable by reading the code.
    public volatile String lastResult = "never run";
    public volatile int lastAirColumns;
    public volatile int lastOpenSkyColumns;
    public volatile int lastFinalColumns;
    public volatile int lastSectionCount;
    public volatile int lastStartCeiling;
    public volatile int scanCount;

    // Slice bookkeeping, worker thread only.
    private long sliceStart;
    private int sliceNodes;

    private RoomScanner() {
    }

    // ------------------------------------------------------------------ read side (any thread)

    /** The current snapshot, or null if none has been built yet. Safe from any thread. */
    public RoomSnapshot snapshot() {
        return this.published;
    }

    /** Whether room culling should currently be applied. Safe from any thread. */
    public boolean isActive() {
        return this.active;
    }

    /**
     * Convenience for the render path: {@link RoomSnapshot#NO_OPINION} when inactive or unscanned.
     */
    public int test(int x, int y, int z) {
        if (!this.active) {
            return RoomSnapshot.NO_OPINION;
        }
        RoomSnapshot snap = this.published;
        return snap == null ? RoomSnapshot.NO_OPINION : snap.test(x, y, z);
    }

    // ------------------------------------------------------------------ client thread

    /**
     * Captures chunk references and queues a scan. Cheap enough to call every tick; the caller is
     * responsible for deciding when a rescan is actually warranted.
     */
    public void requestScan(World world, BlockPos playerPos) {
        int radius = Math.max(8, Math.min(96, Config.GSON.instance().roomRadius));
        ChunkView view = ChunkView.capture(world, playerPos.getX(), playerPos.getZ(), radius + 2);
        if (view == null) {
            this.lastResult = "chunk capture failed (centre chunk not loaded)";
            return;
        }

        ScanRequest request = new ScanRequest(
                view,
                playerPos.up(),
                world.getRegistryKey(),
                radius,
                Math.max(0, Math.min(8, Config.GSON.instance().roomWallThickness)),
                Math.max(4096, Config.GSON.instance().roomMaxVolume));

        // A newer request replaces any queued one, and preempts a scan already in flight.
        this.pending.set(request);
        ensureWorker();
    }

    /**
     * Installs a finished snapshot if one is ready, and returns the sections whose meshes are now
     * stale — the union of what the old snapshot touched and what the new one touches. Returns null
     * when nothing changed. Client thread only.
     */
    public LongOpenHashSet pollCompleted() {
        RoomSnapshot next = this.completed.getAndSet(null);
        if (next == null) {
            return null;
        }
        return install(next);
    }

    /**
     * Turns room culling on or off. Returns the sections needing a rebuild when the state actually
     * changed, else null. Because culling is baked into chunk meshes, flipping this without
     * rebuilding would leave sections stuck in their previous state. Client thread only.
     */
    public LongOpenHashSet setActive(boolean value) {
        if (this.active == value) {
            return null;
        }
        this.active = value;

        RoomSnapshot snap = this.published;
        if (snap == null || snap.sections().isEmpty()) {
            return null;
        }
        return new LongOpenHashSet(snap.sections());
    }

    /** Drops all state, e.g. on world change. Returns sections needing a rebuild, or null. */
    public LongOpenHashSet reset() {
        this.pending.set(null);
        this.completed.set(null);

        RoomSnapshot old = this.published;
        this.published = null;
        if (old == null || old.sections().isEmpty()) {
            return null;
        }
        return new LongOpenHashSet(old.sections());
    }

    private LongOpenHashSet install(RoomSnapshot next) {
        RoomSnapshot old = this.published;

        // Discard a rescan that produced the same answer, keeping the old object. Standing still
        // rescans on a timer, and republishing an identical result queued every section it touched
        // for a rebuild into exactly the mesh it already had — and, because the ghost cache keys on
        // snapshot identity, threw that cache away several times a second too.
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

    // ------------------------------------------------------------------ worker thread

    private void ensureWorker() {
        ScanWorker.INSTANCE.submit(this);
    }

    /**
     * One pass for {@link ScanWorker}: run a queued scan if there is one. The shared thread owns
     * the loop and the idle parking, so this only has to do the work in front of it.
     */
    @Override
    public boolean runOnce() {
        ScanRequest request = this.pending.getAndSet(null);
        if (request == null) {
            return false;
        }
        try {
            RoomSnapshot result = scan(request);
            if (result != null) {
                this.completed.set(result);
            }
        } catch (Preempted ignored) {
            // A newer request arrived; it is still queued and will be picked up next pass.
        } catch (Throwable ignored) {
            // Never let a scan failure kill the worker.
        }
        return true;
    }

    private RoomSnapshot scan(ScanRequest req) {
        beginSlice();
        this.scanCount++;

        BlockPos start = findStart(req);
        if (start == null) {
            this.lastResult = "no passable cell at head or feet height";
            return empty(req);
        }

        Long2IntOpenHashMap topAir = new Long2IntOpenHashMap();
        topAir.defaultReturnValue(Integer.MIN_VALUE);
        LongArrayList airColumns = new LongArrayList();

        flood(req, start, topAir, airColumns);
        this.lastAirColumns = airColumns.size();
        if (airColumns.isEmpty()) {
            this.lastResult = this.lastStartCeiling == NO_CEILING
                    ? "player is under open sky — not in a room"
                    : "flood reached no air";
            return empty(req);
        }

        Long2LongOpenHashMap roofs = solveRoofs(req, start, topAir, airColumns);
        if (roofs == null) {
            this.lastResult = "open-sky bail: " + this.lastOpenSkyColumns + "/" + airColumns.size()
                    + " columns had no ceiling — not treated as a room";
            return empty(req);
        }

        Long2LongOpenHashMap closed = closeFootprint(req, roofs);
        if (closed == null || closed.isEmpty()) {
            this.lastResult = "footprint close produced no columns";
            return empty(req);
        }

        Long2LongOpenHashMap sectionHashes = buildSections(closed);
        this.lastFinalColumns = closed.size();
        this.lastSectionCount = sectionHashes.size();
        this.lastResult = "ok";
        return new RoomSnapshot(closed, sectionHashes, contentHash(closed), req.dimension, start);
    }

    /**
     * A published snapshot that culls nothing.
     *
     * <p>Returning null here would leave the previous snapshot in place, so walking out of a room
     * and into the open kept culling the roof of the room you had left — the scan concluded "not a
     * room" and then had no way to say so. Publishing an empty snapshot both clears the verdict and
     * puts the old sections through the rebuild queue so the roof comes back.
     */
    private static RoomSnapshot empty(ScanRequest req) {
        return new RoomSnapshot(new Long2LongOpenHashMap(), new Long2LongOpenHashMap(),
                0L, req.dimension, req.origin);
    }

    /** Order-independent hash of the whole column set, for recognising an unchanged rescan. */
    private static long contentHash(Long2LongOpenHashMap columns) {
        long h = columns.size() * 0x9E3779B97F4A7C15L;
        for (Long2LongMap.Entry e : columns.long2LongEntrySet()) {
            h += SectionRebuildQueue.mix(e.getLongKey()) ^ SectionRebuildQueue.mix(e.getLongValue());
        }
        return h;
    }

    /** Picks the cell the flood starts from — head height, falling back to feet. */
    private BlockPos findStart(ScanRequest req) {
        BlockPos head = req.origin;
        if (req.view.classify(head.getX(), head.getY(), head.getZ()) == ChunkView.PASSABLE) {
            return head;
        }
        BlockPos feet = head.down();
        if (req.view.classify(feet.getX(), feet.getY(), feet.getZ()) == ChunkView.PASSABLE) {
            return feet;
        }
        return null;
    }

    /** Stage 1: breadth-first flood through passable cells, recording the top air per column. */
    private void flood(ScanRequest req, BlockPos start,
                       Long2IntOpenHashMap topAir, LongArrayList airColumns) {
        ChunkView view = req.view;
        int startX = start.getX();
        int startY = start.getY();
        int startZ = start.getZ();

        int minY = startY - MAX_VERTICAL_DOWN;
        int maxY = startY + MAX_VERTICAL_UP;
        long radiusSq = (long) req.radius * req.radius;

        LongOpenHashSet visited = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();

        // Per-column ceiling height, computed lazily and cached. This is what keeps the flood
        // inside the room the player is actually standing in: without it, any passable cell is
        // fair game, so the room becomes "everywhere reachable through every doorway within the
        // radius" and the neighbours' ceilings come off too.
        Long2IntOpenHashMap ceilings = new Long2IntOpenHashMap();
        ceilings.defaultReturnValue(Integer.MIN_VALUE);

        int startCeiling = ceilingOf(view, ceilings, startX, startZ, startY, maxY);
        this.lastStartCeiling = startCeiling;
        if (startCeiling == NO_CEILING) {
            // The player's own column is open to the sky. Bail immediately rather than flooding
            // thousands of outdoor columns only for solveRoofs to throw them all away.
            return;
        }
        int floor = startCeiling - req.ceilingTolerance;

        long startPacked = BlockPos.asLong(startX, startY, startZ);
        visited.add(startPacked);
        queue.enqueue(startPacked);

        while (!queue.isEmpty()) {
            tickBudget(req);

            if (visited.size() > req.maxVolume) {
                // Not a room — a cave system, or the open world through a doorway. Keep what we
                // have; the BFS ordering means it is a ball centred on the player.
                break;
            }

            long packed = queue.dequeueLong();
            int x = BlockPos.unpackLongX(packed);
            int y = BlockPos.unpackLongY(packed);
            int z = BlockPos.unpackLongZ(packed);

            long xz = RoomSnapshot.packXZ(x, z);
            int prev = topAir.get(xz);
            if (prev == Integer.MIN_VALUE) {
                airColumns.add(xz);
                topAir.put(xz, y);
            } else if (y > prev) {
                topAir.put(xz, y);
            }

            enqueue(view, queue, visited, ceilings, x + 1, y, z, startX, startY, startZ, radiusSq, minY, maxY, floor);
            enqueue(view, queue, visited, ceilings, x - 1, y, z, startX, startY, startZ, radiusSq, minY, maxY, floor);
            enqueue(view, queue, visited, ceilings, x, y, z + 1, startX, startY, startZ, radiusSq, minY, maxY, floor);
            enqueue(view, queue, visited, ceilings, x, y, z - 1, startX, startY, startZ, radiusSq, minY, maxY, floor);
            enqueue(view, queue, visited, ceilings, x, y + 1, z, startX, startY, startZ, radiusSq, minY, maxY, floor);
            enqueue(view, queue, visited, ceilings, x, y - 1, z, startX, startY, startZ, radiusSq, minY, maxY, floor);
        }
    }

    /**
     * Height of the first solid block above the local floor of a column, cached per scan.
     *
     * <p>Measured from the player's own Y: it first rises out of any solid it starts inside, so a
     * step or a raised platform in the same room still reports that room's ceiling rather than the
     * top of the step. {@link #NO_CEILING} means the column is open to the sky.
     */
    private static int ceilingOf(ChunkView view, Long2IntOpenHashMap cache,
                                 int x, int z, int startY, int maxY) {
        long xz = RoomSnapshot.packXZ(x, z);
        int cached = cache.get(xz);
        if (cached != Integer.MIN_VALUE) {
            return cached;
        }

        int y = startY;
        while (y <= maxY && view.classify(x, y, z) == ChunkView.SOLID) {
            y++;
        }
        while (y <= maxY && view.classify(x, y, z) != ChunkView.SOLID) {
            y++;
        }
        int result = y > maxY ? NO_CEILING : y;

        cache.put(xz, result);
        return result;
    }

    private static void enqueue(ChunkView view, LongArrayFIFOQueue queue, LongOpenHashSet visited,
                                Long2IntOpenHashMap ceilings, int x, int y, int z,
                                int startX, int startY, int startZ, long radiusSq,
                                int minY, int maxY, int minCeiling) {
        if (y < minY || y > maxY) {
            return;
        }
        // A doorway's lintel sits well below the room's ceiling, so refusing to duck under a lower
        // ceiling stops the flood at the threshold instead of letting it spill into the next room.
        // An adjacent space joined by a full-height opening still qualifies, which is right: that
        // is one room, not two.
        int ceiling = ceilingOf(view, ceilings, x, z, startY, maxY);
        if (ceiling == NO_CEILING || ceiling < minCeiling) {
            return;
        }
        long dx = x - startX;
        long dz = z - startZ;
        if (dx * dx + dz * dz > radiusSq) {
            return;
        }
        long packed = BlockPos.asLong(x, y, z);
        if (!visited.add(packed)) {
            return;
        }
        // UNKNOWN (unloaded chunk) is treated as solid so the flood cannot escape off the edge
        // of the captured region.
        if (view.classify(x, y, z) == ChunkView.PASSABLE) {
            queue.enqueue(packed);
        }
    }

    /**
     * Stage 2: for each column, find the first solid block above the air and the top of the
     * contiguous solid run it belongs to. Columns with no solid block within reach are open sky.
     */
    private Long2LongOpenHashMap solveRoofs(ScanRequest req, BlockPos start,
                                            Long2IntOpenHashMap topAir, LongArrayList airColumns) {
        ChunkView view = req.view;
        int searchTop = start.getY() + MAX_VERTICAL_UP + ROOF_SEARCH_MARGIN;

        Long2LongOpenHashMap roofs = new Long2LongOpenHashMap(airColumns.size());
        int openSky = 0;

        for (int i = 0; i < airColumns.size(); i++) {
            tickBudget(req);

            long xz = airColumns.getLong(i);
            int x = RoomSnapshot.unpackX(xz);
            int z = RoomSnapshot.unpackZ(xz);

            int y = topAir.get(xz) + 1;
            int roofBase = -1;
            while (y <= searchTop) {
                int cls = view.classify(x, y, z);
                if (cls == ChunkView.SOLID) {
                    roofBase = y;
                    break;
                }
                if (cls == ChunkView.UNKNOWN) {
                    break;
                }
                y++;
            }

            if (roofBase < 0) {
                roofs.put(xz, RoomSnapshot.packRoof(RoomSnapshot.OPEN_SKY, RoomSnapshot.OPEN_SKY));
                openSky++;
                continue;
            }

            // Everything covering this column, not just the slab immediately overhead.
            //
            // Following only the contiguous solid run stopped at the first-floor ceiling, so
            // standing downstairs you got that ceiling removed and then looked straight into the
            // underside of the second floor, which was explicitly protected. Air gaps are crossed
            // rather than treated as the end, so an upper storey's interior does not terminate the
            // walk and its floor, ceiling and the roof above all come out together.
            //
            // Culling the air between storeys costs nothing to draw, and the span is what makes
            // the sections covering it get re-meshed.
            int roofTop = roofBase;
            int limit = roofBase + req.coverHeight;
            for (int probe = roofBase + 1; probe <= limit; probe++) {
                int cls = view.classify(x, probe, z);
                if (cls == ChunkView.UNKNOWN) {
                    break;
                }
                if (cls == ChunkView.SOLID) {
                    roofTop = probe;
                }
            }
            roofs.put(xz, RoomSnapshot.packRoof(roofBase, roofTop));
        }

        this.lastOpenSkyColumns = openSky;
        if (openSky > airColumns.size() * OPEN_SKY_BAIL_RATIO) {
            // Mostly sky overhead: this is not an enclosed room. Publishing nothing lets the
            // occluder and cylinder tiers own the outdoors.
            return null;
        }
        return roofs;
    }

    /**
     * Stage 3: close the footprint. The flood only visits air, so wall columns, pillars and
     * anything else solid has no entry — which is why the roof directly above them survives and
     * you get a lattice of leftovers. This dilates the footprint outward by the wall thickness and
     * fills enclosed holes, giving those columns a roof span inherited from their neighbours.
     */
    private Long2LongOpenHashMap closeFootprint(ScanRequest req, Long2LongOpenHashMap roofs) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (LongIterator it = roofs.keySet().iterator(); it.hasNext(); ) {
            long xz = it.nextLong();
            int x = RoomSnapshot.unpackX(xz);
            int z = RoomSnapshot.unpackZ(xz);
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;
        }
        if (minX > maxX) {
            return null;
        }

        int pad = req.wallThickness + 1;
        int gx0 = minX - pad, gx1 = maxX + pad;
        int gz0 = minZ - pad, gz1 = maxZ + pad;
        int w = gx1 - gx0 + 1;
        int h = gz1 - gz0 + 1;
        if ((long) w * h > MAX_GRID_CELLS) {
            return roofs;
        }

        byte[] state = new byte[w * h];
        int[] base = new int[w * h];
        int[] top = new int[w * h];

        for (LongIterator it = roofs.keySet().iterator(); it.hasNext(); ) {
            long xz = it.nextLong();
            long packed = roofs.get(xz);
            int roofBase = (int) (packed >>> 32);
            int idx = (RoomSnapshot.unpackX(xz) - gx0) * h + (RoomSnapshot.unpackZ(xz) - gz0);
            if (roofBase == RoomSnapshot.OPEN_SKY) {
                // Never a dilation source, and never claimable — otherwise a hole in the ceiling
                // would get filled back in and culled as if it were roof.
                state[idx] = CELL_OPEN_SKY;
            } else {
                state[idx] = CELL_ROOM;
                base[idx] = roofBase;
                top[idx] = (int) packed;
            }
        }

        dilate(req, state, base, top, w, h, gx0, gz0);
        fillEnclosedHoles(req, state, base, top, w, h);

        Long2LongOpenHashMap out = new Long2LongOpenHashMap(roofs.size() * 2);
        for (int ix = 0; ix < w; ix++) {
            for (int iz = 0; iz < h; iz++) {
                int idx = ix * h + iz;
                if (state[idx] != CELL_ROOM && state[idx] != CELL_CLAIMED) {
                    continue;
                }
                out.put(RoomSnapshot.packXZ(gx0 + ix, gz0 + iz), RoomSnapshot.packRoof(base[idx], top[idx]));
            }
        }
        return out;
    }

    /** Grows the footprint outward to swallow the surrounding walls. */
    private void dilate(ScanRequest req, byte[] state, int[] base, int[] top, int w, int h,
                        int gx0, int gz0) {
        for (int pass = 0; pass < req.wallThickness; pass++) {
            tickBudget(req);
            byte[] before = state.clone();

            for (int ix = 0; ix < w; ix++) {
                for (int iz = 0; iz < h; iz++) {
                    int idx = ix * h + iz;
                    if (before[idx] != CELL_EMPTY) {
                        continue;
                    }
                    int b = Integer.MAX_VALUE, t = Integer.MIN_VALUE;
                    boolean any = false;

                    if (ix > 0 && isSource(before[idx - h])) {
                        any = true; b = Math.min(b, base[idx - h]); t = Math.max(t, top[idx - h]);
                    }
                    if (ix < w - 1 && isSource(before[idx + h])) {
                        any = true; b = Math.min(b, base[idx + h]); t = Math.max(t, top[idx + h]);
                    }
                    if (iz > 0 && isSource(before[idx - 1])) {
                        any = true; b = Math.min(b, base[idx - 1]); t = Math.max(t, top[idx - 1]);
                    }
                    if (iz < h - 1 && isSource(before[idx + 1])) {
                        any = true; b = Math.min(b, base[idx + 1]); t = Math.max(t, top[idx + 1]);
                    }

                    // Only swallow columns that are actually wall. Without this the dilation
                    // walks straight through a one-block wall and claims a strip of the next
                    // room's ceiling, which is the same "not my room" complaint by another route.
                    if (any && req.view.classify(gx0 + ix, b - 1, gz0 + iz) == ChunkView.SOLID) {
                        state[idx] = CELL_CLAIMED;
                        base[idx] = b;
                        top[idx] = t;
                    }
                }
            }
        }
    }

    /**
     * Marks every empty cell not reachable from the grid border as an interior hole — a pillar, a
     * block of interior wall — and gives it the roof span of its surroundings.
     */
    private void fillEnclosedHoles(ScanRequest req, byte[] state, int[] base, int[] top, int w, int h) {
        boolean[] outside = new boolean[w * h];
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();

        for (int ix = 0; ix < w; ix++) {
            seedOutside(state, outside, queue, ix, 0, w, h);
            seedOutside(state, outside, queue, ix, h - 1, w, h);
        }
        for (int iz = 0; iz < h; iz++) {
            seedOutside(state, outside, queue, 0, iz, w, h);
            seedOutside(state, outside, queue, w - 1, iz, w, h);
        }
        // Open-sky columns are anchors for the outside region: anything connected to one is
        // genuinely open, not an enclosed hole.
        for (int i = 0; i < state.length; i++) {
            if (state[i] == CELL_OPEN_SKY && !outside[i]) {
                outside[i] = true;
                queue.enqueue(i);
            }
        }

        while (!queue.isEmpty()) {
            tickBudget(req);
            int idx = (int) queue.dequeueLong();
            int ix = idx / h;
            int iz = idx % h;

            if (ix > 0) spreadOutside(state, outside, queue, idx - h);
            if (ix < w - 1) spreadOutside(state, outside, queue, idx + h);
            if (iz > 0) spreadOutside(state, outside, queue, idx - 1);
            if (iz < h - 1) spreadOutside(state, outside, queue, idx + 1);
        }

        for (int pass = 0; pass < MAX_HOLE_FILL_PASSES; pass++) {
            tickBudget(req);
            byte[] before = state.clone();
            boolean changed = false;

            for (int ix = 0; ix < w; ix++) {
                for (int iz = 0; iz < h; iz++) {
                    int idx = ix * h + iz;
                    if (before[idx] != CELL_EMPTY || outside[idx]) {
                        continue;
                    }
                    int b = Integer.MAX_VALUE, t = Integer.MIN_VALUE;
                    boolean any = false;

                    if (ix > 0 && isSource(before[idx - h])) {
                        any = true; b = Math.min(b, base[idx - h]); t = Math.max(t, top[idx - h]);
                    }
                    if (ix < w - 1 && isSource(before[idx + h])) {
                        any = true; b = Math.min(b, base[idx + h]); t = Math.max(t, top[idx + h]);
                    }
                    if (iz > 0 && isSource(before[idx - 1])) {
                        any = true; b = Math.min(b, base[idx - 1]); t = Math.max(t, top[idx - 1]);
                    }
                    if (iz < h - 1 && isSource(before[idx + 1])) {
                        any = true; b = Math.min(b, base[idx + 1]); t = Math.max(t, top[idx + 1]);
                    }

                    if (any) {
                        state[idx] = CELL_CLAIMED;
                        base[idx] = b;
                        top[idx] = t;
                        changed = true;
                    }
                }
            }
            if (!changed) {
                break;
            }
        }
    }

    private static void seedOutside(byte[] state, boolean[] outside, LongArrayFIFOQueue queue,
                                    int ix, int iz, int w, int h) {
        int idx = ix * h + iz;
        if (state[idx] == CELL_EMPTY && !outside[idx]) {
            outside[idx] = true;
            queue.enqueue(idx);
        }
    }

    private static void spreadOutside(byte[] state, boolean[] outside, LongArrayFIFOQueue queue, int idx) {
        if (!outside[idx] && (state[idx] == CELL_EMPTY || state[idx] == CELL_OPEN_SKY)) {
            outside[idx] = true;
            queue.enqueue(idx);
        }
    }

    private static boolean isSource(byte cell) {
        return cell == CELL_ROOM || cell == CELL_CLAIMED;
    }

    /**
     * Stage 4: the sections whose meshes this snapshot changes, each mapped to a hash of the
     * columns that affect it — so a later scan can re-mesh only what actually differs.
     */
    private Long2LongOpenHashMap buildSections(Long2LongOpenHashMap columns) {
        Long2LongOpenHashMap sections = new Long2LongOpenHashMap();
        for (LongIterator it = columns.keySet().iterator(); it.hasNext(); ) {
            long xz = it.nextLong();
            long packed = columns.get(xz);
            int roofBase = (int) (packed >>> 32);
            if (roofBase == RoomSnapshot.OPEN_SKY) {
                continue;
            }
            int roofTop = (int) packed;
            int x = RoomSnapshot.unpackX(xz);
            int z = RoomSnapshot.unpackZ(xz);
            int sx = x >> 4;
            int sz = z >> 4;

            // One block of slack in every direction. Removing a roof block exposes the faces of
            // its neighbours, and a neighbour sitting across a section boundary needs re-meshing
            // just as much as the roof itself does — otherwise the face-cull override in
            // AbstractRenderContextMixin computes the right answer for a mesh nobody rebuilds.
            int syMin = (roofBase - 1) >> 4;
            int syMax = (roofTop + 1) >> 4;
            boolean atMinX = (x & 15) == 0;
            boolean atMaxX = (x & 15) == 15;
            boolean atMinZ = (z & 15) == 0;
            boolean atMaxZ = (z & 15) == 15;

            // Folded into every section this column affects, including the neighbours it only
            // reaches through the one-block slack, so a section's hash covers everything that can
            // change its mesh.
            long contribution = SectionRebuildQueue.mix(xz) ^ SectionRebuildQueue.mix(packed);

            for (int sy = syMin; sy <= syMax; sy++) {
                addSection(sections, sx, sy, sz, contribution);
                if (atMinX) addSection(sections, sx - 1, sy, sz, contribution);
                if (atMaxX) addSection(sections, sx + 1, sy, sz, contribution);
                if (atMinZ) addSection(sections, sx, sy, sz - 1, contribution);
                if (atMaxZ) addSection(sections, sx, sy, sz + 1, contribution);
            }
        }
        return sections;
    }

    private static void addSection(Long2LongOpenHashMap sections, int sx, int sy, int sz, long contribution) {
        long key = ChunkSectionPos.asLong(sx, sy, sz);
        sections.put(key, sections.get(key) + contribution);
    }

    // ------------------------------------------------------------------ budget

    private void beginSlice() {
        this.sliceStart = System.nanoTime();
        this.sliceNodes = 0;
    }

    /**
     * Ends the current slice when either budget runs out, naps briefly so the scan cannot saturate
     * a core, and aborts if a newer request has arrived in the meantime.
     */
    private void tickBudget(ScanRequest req) {
        this.sliceNodes++;
        if (this.sliceNodes < req.nodesPerSlice
                && System.nanoTime() - this.sliceStart < req.nanosPerSlice) {
            return;
        }
        if (this.pending.get() != null) {
            throw Preempted.INSTANCE;
        }
        LockSupport.parkNanos(SLICE_GAP_NANOS);
        beginSlice();
    }

    // ------------------------------------------------------------------ types

    private static final class ScanRequest {
        final ChunkView view;
        final BlockPos origin;
        final RegistryKey<World> dimension;
        final int radius;
        final int wallThickness;
        final int ceilingTolerance;
        final int coverHeight;
        final int maxVolume;
        final int nodesPerSlice;
        final long nanosPerSlice;

        ScanRequest(ChunkView view, BlockPos origin, RegistryKey<World> dimension,
                    int radius, int wallThickness, int maxVolume) {
            this.view = view;
            this.origin = origin;
            this.dimension = dimension;
            this.radius = radius;
            this.wallThickness = wallThickness;
            this.ceilingTolerance = Math.max(0, Math.min(16, Config.GSON.instance().roomCeilingTolerance));
            int configured = Config.GSON.instance().roomCoverHeight;
            this.coverHeight = configured > 0 ? Math.min(128, configured) : DEFAULT_COVER_HEIGHT;
            this.maxVolume = maxVolume;
            this.nodesPerSlice = Math.max(256, Config.GSON.instance().roomNodesPerSlice);
            this.nanosPerSlice = 500_000L;
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
