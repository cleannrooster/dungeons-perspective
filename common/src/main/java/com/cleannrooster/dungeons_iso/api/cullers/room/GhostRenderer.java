package com.cleannrooster.dungeons_iso.api.cullers.room;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/** Renderer-neutral ghost pass for the 26.x submit-node renderer. */
public final class GhostRenderer {
    private static final int MAX_QUADS = 120_000;
    private static final RandomSource RANDOM = RandomSource.create();
    private static SightlineMask cachedMask;
    private static RoomSnapshot cachedRoom;
    private static List<GhostQuad> cached = List.of();
    private static long cachedAtMs;
    public static volatile int lastVertexCount;

    private GhostRenderer() {
    }

    public static void submit(CameraRenderState camera, SubmitNodeCollector collector) {
        Minecraft client = Minecraft.getInstance();
        if (!Mod.enabled || !Config.GSON.instance().ghostCulledBlocks
                || client.level == null || client.getCameraEntity() == null || camera == null) {
            invalidate();
            return;
        }

        SightlineMask mask = SightlineScanner.INSTANCE.isActive() ? SightlineScanner.INSTANCE.mask() : null;
        if (mask != null && mask.suppressesCulling()) {
            mask = null;
        }
        RoomSnapshot room = RoomScanner.INSTANCE.isActive() ? RoomScanner.INSTANCE.snapshot() : null;
        ensureGeometry(mask, room);
        if (cached.isEmpty()) {
            return;
        }

        Vec3 cameraPos = camera.pos;
        Vec3 focus = client.getCameraEntity().getBoundingBox().getCenter();
        float maxAlpha = Math.clamp(Config.GSON.instance().ghostMaxAlpha, 0.0F, 1.0F);
        float clear = Math.max(0.0F, Config.GSON.instance().ghostClearScreen);
        float opaque = Math.max(clear + 0.01F, Config.GSON.instance().ghostOpaqueScreen);
        Matrix4f viewProjection = new Matrix4f(camera.projectionMatrix).mul(camera.viewRotationMatrix);
        float aspect = camera.projectionMatrix.m00() == 0.0F
                ? 1.0F : Math.abs(camera.projectionMatrix.m11() / camera.projectionMatrix.m00());
        Vector4f focusClip = new Vector4f((float) (focus.x - cameraPos.x),
                (float) (focus.y - cameraPos.y), (float) (focus.z - cameraPos.z), 1.0F);
        viewProjection.transform(focusClip);
        float focusX = focusClip.w > 0.0F ? focusClip.x / focusClip.w * aspect : 0.0F;
        float focusY = focusClip.w > 0.0F ? focusClip.y / focusClip.w : 0.0F;

        PoseStack pose = new PoseStack();
        pose.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        List<GhostQuad> geometry = cached;
        collector.submitCustomGeometry(pose,
                RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS),
                (transform, consumer) -> {
                    QuadInstance instance = new QuadInstance();
                    instance.setLightCoords(0x00F000F0);
                    Vector4f clip = new Vector4f();
                    for (GhostQuad ghost : geometry) {
                        boolean visible = false;
                        for (int vertex = 0; vertex < 4; vertex++) {
                            var p = ghost.quad.position(vertex);
                            clip.set((float) (ghost.x + p.x() - cameraPos.x),
                                    (float) (ghost.y + p.y() - cameraPos.y),
                                    (float) (ghost.z + p.z() - cameraPos.z), 1.0F);
                            viewProjection.transform(clip);
                            float distance = opaque;
                            if (clip.w > 0.0F) {
                                float dx = clip.x / clip.w * aspect - focusX;
                                float dy = clip.y / clip.w - focusY;
                                distance = (float) Math.sqrt(dx * dx + dy * dy);
                            }
                            float ramp = Math.clamp((distance - clear) / (opaque - clear), 0.0F, 1.0F);
                            int alpha = Math.round(255.0F * maxAlpha * ramp);
                            visible |= alpha >= 5;
                            instance.setColor(vertex, (alpha << 24) | ghost.tint);
                        }
                        if (!visible) {
                            continue;
                        }
                        // Custom geometry consumers do not apply the submitted pose implicitly.
                        // putBlockBakedQuad writes its coordinates directly and therefore left the
                        // ghost in absolute world space. Apply both the camera-relative base pose
                        // and this block's translation explicitly.
                        PoseStack.Pose blockPose = transform.copy();
                        blockPose.pose().translate(ghost.x, ghost.y, ghost.z);
                        consumer.putBakedQuad(blockPose, ghost.quad, instance);
                    }
                });
        lastVertexCount = cached.size() * 4;
    }

    public static void invalidate() {
        cachedMask = null;
        cachedRoom = null;
        cached = List.of();
        cachedAtMs = 0L;
        lastVertexCount = 0;
    }

    private static void ensureGeometry(SightlineMask mask, RoomSnapshot room) {
        long now = System.currentTimeMillis();
        if (mask == cachedMask && room == cachedRoom && now - cachedAtMs < 10_000L) {
            return;
        }
        cachedMask = mask;
        cachedRoom = room;
        cachedAtMs = now;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            cached = List.of();
            return;
        }

        ArrayList<GhostQuad> result = new ArrayList<>();
        Baker baker = new Baker(result, mask, room);
        if (room != null) {
            room.forEachCulledBlock(baker::block);
        }
        if (mask != null && result.size() < MAX_QUADS) {
            for (LongIterator it = mask.blocks().iterator(); it.hasNext() && result.size() < MAX_QUADS; ) {
                long packed = it.nextLong();
                baker.block(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));
            }
        }
        cached = List.copyOf(result);
    }

    private static final class Baker {
        private final List<GhostQuad> output;
        private final LongOpenHashSet seen = new LongOpenHashSet();
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        private final BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
        private final ArrayList<BlockStateModelPart> parts = new ArrayList<>();
        private final SightlineMask mask;
        private final RoomSnapshot room;

        private Baker(List<GhostQuad> output, SightlineMask mask, RoomSnapshot room) {
            this.output = output;
            this.mask = mask;
            this.room = room;
        }

        private void block(int x, int y, int z) {
            if (output.size() >= MAX_QUADS || !seen.add(BlockPos.asLong(x, y, z))) {
                return;
            }
            Minecraft client = Minecraft.getInstance();
            BlockState state = client.level.getBlockState(pos.set(x, y, z));
            if (state.isAir()) {
                return;
            }
            parts.clear();
            client.getModelManager().getBlockStateModelSet().get(state).collectParts(RANDOM, parts);
            for (BlockStateModelPart part : parts) {
                add(part.getQuads(null), x, y, z);
                for (Direction direction : Direction.values()) {
                    neighbour.set(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ());
                    // Never draw a face between two removed blocks. Translucent/cutout blocks do
                    // not occlude these faces in vanilla, and each hidden face would add another
                    // alpha layer until a roof or canopy became effectively opaque.
                    if (!isRemoved(neighbour.getX(), neighbour.getY(), neighbour.getZ())
                            && Block.shouldRenderFace(state, client.level.getBlockState(neighbour), direction)) {
                        add(part.getQuads(direction), x, y, z);
                    }
                }
            }
        }

        private boolean isRemoved(int x, int y, int z) {
            return (mask != null && mask.isOccluding(x, y, z))
                    || (room != null && room.test(x, y, z) == RoomSnapshot.CULL);
        }

        private void add(List<BakedQuad> quads, int x, int y, int z) {
            for (BakedQuad quad : quads) {
                if (output.size() >= MAX_QUADS) {
                    return;
                }
                int tint = 0x00FFFFFF;
                if (quad.materialInfo().isTinted()) {
                    var source = Minecraft.getInstance().getBlockColors().getTintSource(
                            Minecraft.getInstance().level.getBlockState(pos),
                            quad.materialInfo().tintIndex());
                    if (source != null) {
                        tint = source.colorInWorld(Minecraft.getInstance().level.getBlockState(pos),
                                Minecraft.getInstance().level, pos) & 0x00FFFFFF;
                    }
                }
                output.add(new GhostQuad(x, y, z, quad, tint));
            }
        }
    }

    private record GhostQuad(int x, int y, int z, BakedQuad quad, int tint) {
    }
}
