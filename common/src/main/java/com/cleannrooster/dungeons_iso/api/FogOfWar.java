package com.cleannrooster.dungeons_iso.api;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mod.Mod;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.minecraft.world.effect.MobEffects.DARKNESS;

public class FogOfWar {

    public FogOfWar() {
        map = new LinkedHashMap<>();
    }
    Minecraft client;
    GameRenderer renderer;
    Camera camera;
    Window window;
    Entity cameraEntity;
    Vector2d res;
    public List<HitResult> realPoints;

    public HashMap<Long, HitResult> map;
    private final Random random = new Random();

    private static long packXY(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    public HashMap<Long, HitResult> getMap() {

        HashMap<Long, HitResult> map = new HashMap<>();

        client = Minecraft.getInstance();
        renderer = client.gameRenderer;
        camera = client.gameRenderer.mainCamera();
        window = client.getWindow();
        cameraEntity = camera.entity();
        res = new Vector2d(window.getWidth(), window.getHeight());
        double aspect = res.x / res.y;
        double fov2 =
                Math.toRadians(camera.getFov()) / 2.0;

        for (int x = 0; x < res.x*1.1; x = (int) (x+res.x/24)) {
            for (int y = 0; y < res.y*1.1; y = (int) (y+res.y/(13.5))){
            Vector2d coords = new Vector2d(x, y).div(res).mul(2.0).sub(new Vector2d(1.0));
            coords.x *= aspect;
            coords.y = -coords.y;

            Vector2d offsets = coords.mul(Math.tan(fov2));
            Vector3d forward = camera.rotation().transform(new Vector3d(0.0, 0.0, -1.0));
            Vector3d right = camera.rotation().transform(new Vector3d(1.0, 0.0, 0.0));
            Vector3d up = camera.rotation().transform(new Vector3d(0.0, 1.0, 0.0));
            Vector3d dir = forward.add(right.mul(offsets.x).add(up.mul(offsets.y))).normalize();
            Vector3d orth = camera.rotation().transform(new Vector3d(0.0, 0.0, -1.0)).normalize();
            Vec3 rayDir = Config.GSON.instance().ortho ? new Vec3(orth.x, orth.y, orth.z) : new Vec3(dir.x, dir.y, dir.z);
                Vec3 end =                             (Config.GSON.instance().ortho ? camera.position().add(new Vec3(camera.leftVector()).scale(Mod.zoomMetric*Mod.getZoom()).scale(coords.x).scale(-0.72)).add(new Vec3(camera.upVector()).scale(Mod.zoomMetric*Mod.getZoom()).scale(coords.y).scale(0.72)) :camera.position()).add(rayDir.scale((1+Mod.getZoom())*Mod.zoomMetric*3F));

            HitResult hitResult0 = raycast(Config.GSON.instance().ortho ? camera.position().add(new Vec3(camera.leftVector()).scale(Mod.zoomMetric*Mod.getZoom()).scale(coords.x).scale(-0.72)).add(new Vec3(camera.upVector()).scale(Mod.zoomMetric*Mod.getZoom()).scale(coords.y).scale(0.72)) :camera.position(),end,new RaycastContextCull(
                        Config.GSON.instance().ortho ? camera.position().add(new Vec3(camera.leftVector()).scale(Mod.zoomMetric*Mod.getZoom()).scale(coords.x).scale(-0.72)).add(new Vec3(camera.upVector()).scale(Mod.zoomMetric*Mod.getZoom()).scale(coords.y).scale(0.72)) :camera.position(),
                        end,
CustomShapeTypes.CULLED,
                    ClipContext.Block.VISUAL,
                        ClipContext.Fluid.NONE,
                        cameraEntity
                ),(innerContext, pos) -> {
                    BlockState blockState = client.player.level().getBlockState(pos);
                    FluidState fluidState = client.player.level().getFluidState(pos);
                    Vec3 vec3d = innerContext.getFrom();
                    Vec3 vec3d2 = innerContext.getTo();
                    VoxelShape voxelShape = innerContext.getBlockShape(blockState, client.player.level(), pos);
                    BlockHitResult firstResult = client.level.clipWithInteractionOverride(vec3d, vec3d2, pos, voxelShape, blockState);


                    VoxelShape voxelShape2 = innerContext.getFluidShape(fluidState, client.player.level(), pos);
                    BlockHitResult blockHitResult2 = voxelShape2.clip(vec3d, vec3d2, pos);
                    double d = firstResult == null ? Double.MAX_VALUE : innerContext.getFrom().distanceToSqr(firstResult.getLocation());
                    double e = blockHitResult2 == null ? Double.MAX_VALUE : innerContext.getFrom().distanceToSqr(blockHitResult2.getLocation());
                    return d <= e ? firstResult : blockHitResult2;
                }, (innerContext) -> {
                    Vec3 vec3d = innerContext.getFrom().subtract(innerContext.getTo());
                    return BlockHitResult.miss(innerContext.getTo(), Direction.getApproximateNearest(vec3d.x, vec3d.y, vec3d.z), BlockPos.containing(innerContext.getTo()));
                });

                map.put(packXY(x, y), hitResult0);
            }

        }
        return map;
    }
    public List<Vec2> points;
    public void populatePoints() {
        points = new ArrayList<>();
         realPoints = new ArrayList<>();

        if(map != null) {
            for(Map.Entry<Long, HitResult> entry : map.entrySet()) {
                HitResult hitResult = entry.getValue();
                if (hitResult != null && hitResult.getLocation() != null) {
                    long key = entry.getKey();
                    int px = (int)(key >> 32);
                    int py = (int)(key & 0xFFFFFFFFL);
                    Vec3 vec3d = hitResult.getLocation();
                    HitResult result = cameraEntity.level().clip(new ClipContext(cameraEntity.getEyePosition(), vec3d.add(0,cameraEntity.getEyeHeight(cameraEntity.getPose()),0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, cameraEntity));
                    if ((cameraEntity instanceof LivingEntity living && (BlockCuller.angleBetween(hitResult.getLocation().subtract(cameraEntity.getEyePosition()),living.getLookAngle()) > 50 || client.getCameraEntity() instanceof LivingEntity livingEntity && livingEntity.hasEffect(DARKNESS)) && hitResult.getLocation().distanceTo(living.getEyePosition()) >= Mod.getZoom()*2) ||(((result.getLocation() != null && cameraEntity.level().getBrightness(LightLayer.BLOCK, BlockPos.containing(result.getLocation()))+cameraEntity.level().getBrightness(LightLayer.SKY, BlockPos.containing(result.getLocation())) <= 6) || (!(result instanceof BlockHitResult result1 && result1.getType().equals(HitResult.Type.MISS)))) &&  hitResult.getLocation().distanceTo(cameraEntity.getEyePosition()) > Mod.getZoom()*2)) {
                        realPoints.add(hitResult);
                        points.add(new Vec2(px, py));
                    }
                }
            }
        }

    }
    float[] offsetsArray = new float[42*18*2];


    public void render(GuiGraphicsExtractor context, float delta){
        context.pose().pushMatrix();

        if(points != null) {
            // Ensure offsets array is large enough
            int requiredSize = points.size() * 2;
            if (offsetsArray.length < requiredSize) {
                offsetsArray = new float[requiredSize];
            }

            for (int i = 0; i < points.size(); i++) {
                Vec2 point = points.get(i);
                int idx = i * 2;
                float a = Math.clamp((float) (offsetsArray[idx] + random.nextGaussian() * -offsetsArray[idx] + random.nextGaussian()), -50, 50);
                float b = Math.clamp((float) (offsetsArray[idx + 1] + random.nextGaussian() * -offsetsArray[idx + 1] + random.nextGaussian()), -50, 50);
                var c = window.calculateScale(client.options.guiScale().get(),false);
                context.blit(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath("dungeons_iso","textures/shader/sample.png"), (int) point.x/c+(int)a/c-150/c, (int) point.y/c+(int)b/c-150/c,0f,0f,300/c,300/c,300/c,300/c,300/c,300/c);

                offsetsArray[idx] = a;
                offsetsArray[idx + 1] = b;
            }
        }
        context.pose().popMatrix();


    }
    private static <T, C> T raycast(Vec3 start, Vec3 end, C context, BiFunction<C, BlockPos, T> blockHitFactory, Function<C, T> missFactory) {
        if (start.equals(end)) {
            return missFactory.apply(context);
        } else {
            double d = Mth.lerp(-1.0E-7, end.x, start.x);
            double e = Mth.lerp(-1.0E-7, end.y, start.y);
            double f = Mth.lerp(-1.0E-7, end.z, start.z);
            double g = Mth.lerp(-1.0E-7, start.x, end.x);
            double h = Mth.lerp(-1.0E-7, start.y, end.y);
            double i = Mth.lerp(-1.0E-7, start.z, end.z);
            int j = Mth.floor(g);
            int k = Mth.floor(h);
            int l = Mth.floor(i);
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(j, k, l);
            T object = blockHitFactory.apply(context, mutable);
            if (object != null) {
                return object;
            } else {
                double m = d - g;
                double n = e - h;
                double o = f - i;
                int p = Mth.sign(m);
                int q = Mth.sign(n);
                int r = Mth.sign(o);
                double s = p == 0 ? Double.MAX_VALUE : (double)p / m;
                double t = q == 0 ? Double.MAX_VALUE : (double)q / n;
                double u = r == 0 ? Double.MAX_VALUE : (double)r / o;
                double v = s * (p > 0 ? 1.0 - Mth.frac(g) : Mth.frac(g));
                double w = t * (q > 0 ? 1.0 - Mth.frac(h) : Mth.frac(h));
                double x = u * (r > 0 ? 1.0 - Mth.frac(i) : Mth.frac(i));

                Object object2;
                do {
                    if (!(v <= 1.0) && !(w <= 1.0) && !(x <= 1.0)) {
                        return missFactory.apply(context);
                    }

                    if (v < w) {
                        if (v < x) {
                            j += p;
                            v += s;
                        } else {
                            l += r;
                            x += u;
                        }
                    } else if (w < x) {
                        k += q;
                        w += t;
                    } else {
                        l += r;
                        x += u;
                    }

                    object2 = blockHitFactory.apply(context, mutable.set(j, k, l));
                } while(object2 == null);

                return (T) object2;
            }
        }
    }
}
