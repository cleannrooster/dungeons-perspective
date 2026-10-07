package com.cleannrooster.dungeons_iso.api.cullers.room;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import com.cleannrooster.dungeons_iso.mixin.GameRendererAccessor;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3d;

import java.util.List;

/**
 * Draws the blocks the cullers removed back into the scene as translucent geometry.
 *
 * <p>Removing everything between the camera and the player is correct but looks wrong when the
 * player is deep inside something: 27 rays through fifteen blocks of sand excavate a shaft to the
 * surface, and what you see is a black pit. Drawing the removed volume back, with opacity rising
 * with distance from the player on screen, keeps the world reading as intact and leaves only a
 * see-through pocket around the player.
 *
 * <h2>Which faces to draw</h2>
 *
 * <p>The first attempt drew the hull of the removed volume — every face whose neighbour was not
 * also removed. That stacks: looking into a shaft you cross the hull at least twice, and at 0.85
 * alpha two layers already come out at 0.98, so the ghost read as solid.
 *
 * <p>The rule that does not stack is to draw the faces that <b>were being drawn before anything was
 * culled</b> — the ones vanilla's own face culling would keep. Interior blocks of the volume
 * contributed nothing to the original image and contribute nothing here, so the ghost has exactly
 * as many layers as the real terrain did. The newly exposed cut faces are not drawn at all, which
 * is right: they never existed.
 *
 * <h2>Why the geometry is cached</h2>
 *
 * <p>Deciding <i>what</i> to draw is expensive — a block state lookup, a model lookup and six
 * {@link Block#shouldDrawSide} tests per block — and it only changes when the cull set does.
 * Deciding <i>how opaque</i> each vertex is changes every frame, because it depends on where the
 * player is on screen. So the first is baked into a flat vertex array when the mask or snapshot
 * identity changes, and the per-frame path does nothing but project and write vertices.
 */
public final class GhostRenderer {

    /** Below this a quad contributes nothing worth submitting. */
    private static final float MIN_ALPHA = 0.02F;
    /** Model quad lookup wants a Random; the value never matters for full blocks. */
    private static final Random RANDOM = Random.create();
    /** Reused for the projection result. Render thread only. */
    private static final float[] SCREEN = new float[2];
    /** Reused for corner projection. Render thread only. */
    private static final float[] CORNER = new float[2];
    // Per-quad scratch, hoisted out of the frame loop. Render thread only.
    private static final float[] alpha = new float[4];
    private static final float[] cornerX = new float[4];
    private static final float[] cornerY = new float[4];
    /** Floats per cached vertex: x, y, z (origin-relative), u, v, nx, ny, nz, r, g, b. */
    private static final int STRIDE = 11;
    /** Safety ceiling on cached geometry, so a pathological cull set cannot allocate without bound. */
    private static final int MAX_VERTICES = 120_000;
    /**
     * Rebake at least this often, so edits to the world do not leave the ghost stale forever.
     *
     * <p>Only a safety net: a changed cull set rebakes immediately. At two seconds it had become
     * the main reason the bake ran at all, once unchanged results stopped republishing — re-doing
     * every shouldDrawSide, model lookup and lightmap sample to reproduce what was already there.
     */
    private static final long MAX_CACHE_AGE_MS = 10_000L;

    // ---- geometry cache, render thread only ----
    private static SightlineMask cachedMask;
    private static RoomSnapshot cachedSnapshot;
    private static long cachedAtMs;
    private static final float[] EMPTY_FLOATS = new float[0];
    private static final int[] EMPTY_INTS = new int[0];
    private static float[] geometry = EMPTY_FLOATS;
    private static int[] lights = EMPTY_INTS;
    private static int vertexCount;
    /** Positions are stored relative to this, so float precision stays usable far from origin. */
    private static double originX, originY, originZ;

    /** Size of the current bake, for the debug report. */
    public static volatile int lastVertexCount;

    private GhostRenderer() {
    }

    /**
     * Called from the world render, after terrain. {@code matrices} is not camera-relative here, so
     * positions are offset by the camera manually — matching how the rest of this mixin draws.
     */
    public static void render(MatrixStack matrices, VertexConsumerProvider buffers,
                              Camera camera, GameRenderer gameRenderer) {
        if (!Mod.enabled || !Config.GSON.instance().ghostCulledBlocks) {
            invalidate();
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null || client.cameraEntity == null) {
            invalidate();
            return;
        }

        float maxAlpha = Math.max(0F, Math.min(1F, Config.GSON.instance().ghostMaxAlpha));
        if (maxAlpha <= MIN_ALPHA) {
            return;
        }
        float clearAt = Math.max(0F, Config.GSON.instance().ghostClearScreen);
        // The ramp needs somewhere to happen, so the far edge is always kept ahead of the near one
        // however the two are configured.
        float opaqueAt = Math.max(clearAt + 0.01F, Config.GSON.instance().ghostOpaqueScreen);

        SightlineMask mask = SightlineScanner.INSTANCE.isActive()
                ? SightlineScanner.INSTANCE.mask() : null;
        if (mask != null && mask.suppressesCulling()) {
            mask = null;
        }
        RoomSnapshot snapshot = RoomScanner.INSTANCE.isActive()
                ? RoomScanner.INSTANCE.snapshot() : null;

        ensureGeometry(client, world, mask, snapshot);
        if (vertexCount == 0) {
            return;
        }

        // Opacity is driven by distance on screen, not in the world. Under an isometric camera a
        // world-space sphere around the player projects to an ellipse, and blocks far along the
        // view axis sit near the player in 3D while being nowhere near them as drawn.
        //
        // Projected through the camera basis and FOV rather than through a matrix, matching how
        // MouseMixin turns a cursor position into a world ray. Going through a matrix meant
        // depending on which matrix was actually in force, and getting that wrong does not fail
        // loudly — it silently drops the perspective divide, leaving a "screen distance" that is
        // really a camera-space offset in blocks and a pocket whose shape swims with camera yaw.
        MatrixStack.Entry entry = matrices.peek();
        Vec3d cameraPos = camera.getPos();
        Projector projector = new Projector(camera, gameRenderer);

        Vec3d focus = client.cameraEntity.getBoundingBox().getCenter();
        float[] screen = new float[2];
        if (!projector.project(focus.x, focus.y, focus.z, cameraPos, screen)) {
            // The player is behind the camera; nothing sensible to measure against.
            return;
        }
        float focusX = screen[0];
        float focusY = screen[1];
        lastVertexCount = vertexCount;

        VertexConsumer consumer = buffers.getBuffer(
                RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));

        double offX = originX - cameraPos.x;
        double offY = originY - cameraPos.y;
        double offZ = originZ - cameraPos.z;

        // Screen x runs to +/- the aspect ratio in these units, y to +/- 1. Anything outside that
        // box is off screen.
        float halfWidth = client.getWindow().getFramebufferHeight() <= 0 ? 1.0F
                : (float) client.getWindow().getFramebufferWidth()
                        / client.getWindow().getFramebufferHeight();

        for (int v = 0; v + 3 < vertexCount; v += 4) {
            int base = v * STRIDE;

            // The boundary is a closed shell, so a ray through it crosses twice — near side and
            // far. Dropping the far side leaves exactly one layer, which is what keeps the ghost
            // from compounding with itself.
            double faceX = originX + geometry[base] - cameraPos.x;
            double faceY = originY + geometry[base + 1] - cameraPos.y;
            double faceZ = originZ + geometry[base + 2] - cameraPos.z;
            if (faceX * geometry[base + 5] + faceY * geometry[base + 6]
                    + faceZ * geometry[base + 7] > 0) {
                continue;
            }

            // Project the corners once and keep them: the pocket test and the alpha both need them.
            boolean projected = true;
            float minSX = Float.MAX_VALUE, maxSX = -Float.MAX_VALUE;
            float minSY = Float.MAX_VALUE, maxSY = -Float.MAX_VALUE;
            for (int i = 0; i < 4 && projected; i++) {
                int o = (v + i) * STRIDE;
                projected = projector.project(originX + geometry[o], originY + geometry[o + 1],
                        originZ + geometry[o + 2], cameraPos, CORNER);
                if (!projected) {
                    break;
                }
                cornerX[i] = CORNER[0];
                cornerY[i] = CORNER[1];
                if (CORNER[0] < minSX) minSX = CORNER[0];
                if (CORNER[0] > maxSX) maxSX = CORNER[0];
                if (CORNER[1] < minSY) minSY = CORNER[1];
                if (CORNER[1] > maxSY) maxSY = CORNER[1];
            }
            // A corner behind the camera means the quad straddles the near plane and its screen
            // extent is meaningless. Those are right on top of the camera; drop them.
            if (!projected) {
                continue;
            }

            // Entirely off screen. The cull set is a volume around the camera-to-player line and a
            // good part of it lands outside the view, so this drops a real fraction of the geometry
            // for the cost of four comparisons — and the screen extent it tests was already
            // computed for the pocket check below, so it is genuinely free.
            if (minSX > halfWidth || maxSX < -halfWidth || minSY > 1.0F || maxSY < -1.0F) {
                continue;
            }

            // The clear pocket has to be a property of the whole quad, not of its corners.
            //
            // Alpha is interpolated across a quad, so a block face near the camera — which can
            // cover a large part of the screen — has all four corners outside the pocket, all of
            // them opaque, while its middle sits right over the player. That is what was hiding
            // the player: not blending order, but a face too big for a per-vertex gradient to
            // hollow out. Dropping any quad whose screen extent reaches the pocket at all
            // guarantees nothing is drawn there, whatever its size.
            if (maxSX >= focusX - clearAt && minSX <= focusX + clearAt
                    && maxSY >= focusY - clearAt && minSY <= focusY + clearAt) {
                continue;
            }

            boolean visible = false;
            for (int i = 0; i < 4; i++) {
                float dx = cornerX[i] - focusX;
                float dy = cornerY[i] - focusY;
                alpha[i] = curve((float) Math.sqrt(dx * dx + dy * dy), clearAt, opaqueAt, maxAlpha);
                if (alpha[i] > MIN_ALPHA) {
                    visible = true;
                }
            }
            if (!visible) {
                continue;
            }

            for (int i = 0; i < 4; i++) {
                int o = (v + i) * STRIDE;
                consumer.vertex(entry,
                                (float) (offX + geometry[o]),
                                (float) (offY + geometry[o + 1]),
                                (float) (offZ + geometry[o + 2]))
                        .color(geometry[o + 8], geometry[o + 9], geometry[o + 10], alpha[i])
                        .texture(geometry[o + 3], geometry[o + 4])
                        .overlay(OverlayTexture.DEFAULT_UV)
                        .light(lights[v + i])
                        .normal(entry, geometry[o + 5], geometry[o + 6], geometry[o + 7]);
            }
        }
    }

    // ------------------------------------------------------------------ geometry cache

    /** Drops the cache and the arrays behind it. Safe to call every frame. */
    public static void invalidate() {
        cachedMask = null;
        cachedSnapshot = null;
        vertexCount = 0;
        // Released rather than merely ignored: at the vertex ceiling these are several megabytes,
        // and they are static, so holding them keeps the mask and snapshot alive with them.
        geometry = EMPTY_FLOATS;
        lights = EMPTY_INTS;
    }

    /** Rebakes only when the cull set has actually been replaced, or the cache has gone stale. */
    private static void ensureGeometry(MinecraftClient client, ClientWorld world,
                                       SightlineMask mask, RoomSnapshot snapshot) {
        long now = System.currentTimeMillis();
        boolean stale = mask != cachedMask
                || snapshot != cachedSnapshot
                // Block edits do not replace the mask, so without this the ghost could keep drawing
                // geometry for blocks that have since been mined.
                || now - cachedAtMs > MAX_CACHE_AGE_MS;
        if (!stale) {
            return;
        }

        cachedMask = mask;
        cachedSnapshot = snapshot;
        cachedAtMs = now;
        bake(client, world, mask, snapshot);
    }

    /** True if this block is one the cullers removed, from either source. */
    private static boolean isRemoved(SightlineMask mask, RoomSnapshot snapshot, int x, int y, int z) {
        if (mask != null && mask.isOccluding(x, y, z)) {
            return true;
        }
        return snapshot != null && snapshot.test(x, y, z) == RoomSnapshot.CULL;
    }

    private static void bake(MinecraftClient client, ClientWorld world,
                             SightlineMask mask, RoomSnapshot snapshot) {
        vertexCount = 0;
        if (mask == null && snapshot == null) {
            return;
        }

        // Origin is the camera entity's block, so stored offsets stay small and float precision
        // holds up at far-flung world coordinates.
        Vec3d anchor = client.cameraEntity != null ? client.cameraEntity.getPos() : Vec3d.ZERO;
        originX = Math.floor(anchor.x);
        originY = Math.floor(anchor.y);
        originZ = Math.floor(anchor.z);

        Baker baker = new Baker(client, world, mask, snapshot);
        if (mask != null) {
            LongSet blocks = mask.blocks();
            for (LongIterator it = blocks.iterator(); it.hasNext(); ) {
                long packed = it.nextLong();
                baker.block(BlockPos.unpackLongX(packed),
                        BlockPos.unpackLongY(packed), BlockPos.unpackLongZ(packed));
            }
        }
        if (snapshot != null) {
            snapshot.forEachCulledBlock(baker::block);
        }

        geometry = baker.verts.toFloatArray();
        lights = baker.lights.toIntArray();
        vertexCount = baker.lights.size();
    }

    /** Walks the cull set once and flattens the visible faces into arrays. */
    private static final class Baker {
        final MinecraftClient client;
        final ClientWorld world;
        final FloatArrayList verts = new FloatArrayList();
        final IntArrayList lights = new IntArrayList();
        final BlockPos.Mutable pos = new BlockPos.Mutable();
        final BlockPos.Mutable neighbour = new BlockPos.Mutable();

        final SightlineMask mask;
        final RoomSnapshot snapshot;

        Baker(MinecraftClient client, ClientWorld world, SightlineMask mask, RoomSnapshot snapshot) {
            this.client = client;
            this.world = world;
            this.mask = mask;
            this.snapshot = snapshot;
        }

        void block(int x, int y, int z) {
            if (this.lights.size() >= MAX_VERTICES) {
                return;
            }
            this.pos.set(x, y, z);
            BlockState state = this.world.getBlockState(this.pos);
            if (state.isAir()) {
                return;
            }

            BakedModel model = this.client.getBlockRenderManager().getModel(state);
            long seed = state.getRenderingSeed(this.pos);

            for (Direction dir : Direction.values()) {
                this.neighbour.set(x + dir.getOffsetX(), y + dir.getOffsetY(), z + dir.getOffsetZ());
                // Two conditions, and both matter.
                //
                // Vanilla's own visibility test, against the unmodified world: a face buried
                // between two solid blocks was never drawn, so ghosting it would add a layer of
                // alpha that was never in the original image.
                if (!Block.shouldDrawSide(state, this.world, this.pos, dir, this.neighbour)) {
                    continue;
                }
                // And only the outer boundary of what was removed. Faces onto other removed blocks
                // are interior to the hole. In a house that is every upper storey's floor and
                // ceiling, each a fully visible surface in its own right, and each adding another
                // layer of alpha until the stack goes opaque and hides the player underneath it.
                if (isRemoved(this.mask, this.snapshot,
                        this.neighbour.getX(), this.neighbour.getY(), this.neighbour.getZ())) {
                    continue;
                }
                RANDOM.setSeed(seed);
                emit(state, model.getQuads(state, dir, RANDOM), x, y, z,
                        WorldRenderer.getLightmapCoordinates(this.world, this.neighbour));
            }

            // Direction-less quads belong to no face and are always drawn.
            RANDOM.setSeed(seed);
            emit(state, model.getQuads(state, null, RANDOM), x, y, z,
                    WorldRenderer.getLightmapCoordinates(this.world, this.pos));
        }

        private void emit(BlockState state, List<BakedQuad> quads, int bx, int by, int bz, int light) {
            for (int q = 0; q < quads.size(); q++) {
                if (this.lights.size() >= MAX_VERTICES) {
                    return;
                }
                BakedQuad quad = quads.get(q);
                int[] data = quad.getVertexData();
                int stride = data.length / 4;

                Direction face = quad.getFace();
                float nx = face.getOffsetX();
                float ny = face.getOffsetY();
                float nz = face.getOffsetZ();

                // Grass and foliage textures are stored greyscale and coloured at render time from
                // the biome, so a quad emitted at flat white comes out grey. Vanilla multiplies in
                // this tint per quad; without it the ghost desaturates exactly the blocks people
                // notice most.
                float tintR = 1.0F, tintG = 1.0F, tintB = 1.0F;
                if (quad.hasColor()) {
                    int rgb = this.client.getBlockColors()
                            .getColor(state, this.world, this.pos, quad.getColorIndex());
                    tintR = ((rgb >> 16) & 0xFF) / 255.0F;
                    tintG = ((rgb >> 8) & 0xFF) / 255.0F;
                    tintB = (rgb & 0xFF) / 255.0F;
                }
                // Directional shading, the same top-bright/side-dark falloff the chunk mesh gets.
                // Without it the ghost is flat and reads as a decal rather than as the world.
                float shade = this.world.getBrightness(face, quad.hasShade());
                tintR *= shade;
                tintG *= shade;
                tintB *= shade;

                for (int i = 0; i < 4; i++) {
                    int o = i * stride;
                    this.verts.add((float) (bx + Float.intBitsToFloat(data[o]) - originX));
                    this.verts.add((float) (by + Float.intBitsToFloat(data[o + 1]) - originY));
                    this.verts.add((float) (bz + Float.intBitsToFloat(data[o + 2]) - originZ));
                    this.verts.add(Float.intBitsToFloat(data[o + 4]));
                    this.verts.add(Float.intBitsToFloat(data[o + 5]));
                    this.verts.add(nx);
                    this.verts.add(ny);
                    this.verts.add(nz);

                    // The quad's own baked vertex colour is packed little-endian, red in the low
                    // byte — the order vanilla reads it back in.
                    int packed = data[o + 3];
                    this.verts.add((packed & 0xFF) / 255.0F * tintR);
                    this.verts.add(((packed >> 8) & 0xFF) / 255.0F * tintG);
                    this.verts.add(((packed >> 16) & 0xFF) / 255.0F * tintB);

                    this.lights.add(light);
                }
            }
        }
    }

    // ------------------------------------------------------------------ projection

    /**
     * World to screen, in units where 1.0 is the distance from the centre of the screen to the top
     * edge — so a circle of a given radius in these units is a circle on screen at any camera
     * angle, which a matrix-based mapping was failing to be.
     *
     * <p>Built from the camera's own basis and field of view, the same way {@code MouseMixin} turns
     * a cursor position into a world ray, so the two agree by construction. Aspect ratio cancels
     * out: it appears when normalising x into NDC and again when converting NDC back into units of
     * screen height.
     */
    private static final class Projector {
        private final Vec3d forward;
        private final Vec3d right;
        private final Vec3d up;
        private final boolean ortho;
        /** Perspective: tan(fov/2). Orthographic: half the view height, in blocks. */
        final double scale;

        Projector(Camera camera, GameRenderer gameRenderer) {
            Vector3d f = camera.getRotation().transform(new Vector3d(0.0, 0.0, -1.0));
            Vector3d r = camera.getRotation().transform(new Vector3d(1.0, 0.0, 0.0));
            Vector3d u = camera.getRotation().transform(new Vector3d(0.0, 1.0, 0.0));
            this.forward = new Vec3d(f.x, f.y, f.z);
            this.right = new Vec3d(r.x, r.y, r.z);
            this.up = new Vec3d(u.x, u.y, u.z);

            this.ortho = Config.GSON.instance().ortho;
            if (this.ortho) {
                // Matches Ortho.createOrthoMatrix, whose half-height is getZoom() * 2.
                this.scale = Math.max(1.0E-4, Mod.getZoom() * 2.0);
            } else {
                double fov = ((GameRendererAccessor) gameRenderer)
                        .callGetFov(camera, camera.getLastTickDelta(), true);
                this.scale = Math.max(1.0E-4, Math.tan(Math.toRadians(fov) / 2.0));
            }
        }

        /** False when the point is behind the camera, where the divide is meaningless. */
        boolean project(double wx, double wy, double wz, Vec3d cameraPos, float[] out) {
            double dx = wx - cameraPos.x;
            double dy = wy - cameraPos.y;
            double dz = wz - cameraPos.z;

            double sx = dx * this.right.x + dy * this.right.y + dz * this.right.z;
            double sy = dx * this.up.x + dy * this.up.y + dz * this.up.z;

            if (this.ortho) {
                out[0] = (float) (sx / this.scale);
                out[1] = (float) (sy / this.scale);
                return true;
            }

            double depth = dx * this.forward.x + dy * this.forward.y + dz * this.forward.z;
            if (depth <= 1.0E-4) {
                return false;
            }
            out[0] = (float) (sx / (depth * this.scale));
            out[1] = (float) (sy / (depth * this.scale));
            return true;
        }
    }

    /**
     * Fully clear out to {@code clearAt}, then smoothstep up to {@code maxAlpha} at
     * {@code opaqueAt} — both measured on screen. The flat inner section is what guarantees a
     * genuinely see-through pocket around the player rather than a gradient that is merely faint
     * there.
     */
    private static float curve(float dist, float clearAt, float opaqueAt, float maxAlpha) {
        if (dist <= clearAt) {
            return 0F;
        }
        float t = Math.min(1.0F, (dist - clearAt) / (opaqueAt - clearAt));
        // Smoothstep, so neither end of the ramp has a visible rim.
        return maxAlpha * t * t * (3.0F - 2.0F * t);
    }
}
