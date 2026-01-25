package com.cleannrooster.dungeons_iso.api;

import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.mixin.GameRendererAccessor;
import com.cleannrooster.dungeons_iso.mod.Mod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.WindowFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.GuiAtlasManager;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.LightType;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.cleannrooster.dungeons_iso.api.cullers.GenericCuller3.angleBetween;
import static net.minecraft.entity.effect.StatusEffects.DARKNESS;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;

public class FogOfWar {
    private static final Logger log = LoggerFactory.getLogger(FogOfWar.class);

    public FogOfWar() {
        map = new LinkedHashMap<>();
    }
    MinecraftClient client;
    GameRenderer renderer;
    Camera camera;
    Window window;
    Entity cameraEntity;
    Vector2d res;
    public List<HitResult> realPoints;

    public HashMap<List<Integer>,HitResult> map;
    public HashMap<List<Integer>,HitResult> getMap() {

        HashMap<List<Integer>,HitResult> map = new HashMap<>();

        client = MinecraftClient.getInstance();
        renderer = client.gameRenderer;
        camera = client.gameRenderer.getCamera();
        window = client.getWindow();
        cameraEntity = camera.getFocusedEntity();
        res = new Vector2d(window.getFramebufferWidth(), window.getFramebufferHeight());
        double aspect = res.x / res.y;
        double fov2 =
                Math.toRadians(((GameRendererAccessor) renderer).callGetFov(camera, renderer.getCamera().getLastTickDelta(), true)) / 2.0;

        for (int x = 0; x < res.x*1.1; x = (int) (x+res.x/24)) {
            for (int y = 0; y < res.y*1.1; y = (int) (y+res.y/(13.5))){
            Vector2d coords = new Vector2d(x, y).div(res).mul(2.0).sub(new Vector2d(1.0));
            coords.x *= aspect;
            coords.y = -coords.y;

            Vector2d offsets = coords.mul(Math.tan(fov2));
            Vector3d forward = camera.getRotation().transform(new Vector3d(0.0, 0.0, -1.0));
            Vector3d right = camera.getRotation().transform(new Vector3d(1.0, 0.0, 0.0));
            Vector3d up = camera.getRotation().transform(new Vector3d(0.0, 1.0, 0.0));
            Vector3d dir = forward.add(right.mul(offsets.x).add(up.mul(offsets.y))).normalize();
            Vector3d orth = camera.getRotation().transform(new Vector3d(0.0, 0.0, -1.0)).normalize();
            Vec3d rayDir = Config.GSON.instance().ortho ? new Vec3d(orth.x, orth.y, orth.z) : new Vec3d(dir.x, dir.y, dir.z);
                Vec3d end =                             (Config.GSON.instance().ortho ? camera.getPos().add(new Vec3d(camera.getDiagonalPlane()).multiply(Mod.zoomMetric*Mod.getZoom()).multiply(coords.x).multiply(-0.72)).add(new Vec3d(camera.getVerticalPlane()).multiply(Mod.zoomMetric*Mod.getZoom()).multiply(coords.y).multiply(0.72)) :camera.getPos()).add(rayDir.multiply((1+Mod.getZoom())*Mod.zoomMetric*3F));

            HitResult hitResult0 = raycast(Config.GSON.instance().ortho ? camera.getPos().add(new Vec3d(camera.getDiagonalPlane()).multiply(Mod.zoomMetric*Mod.getZoom()).multiply(coords.x).multiply(-0.72)).add(new Vec3d(camera.getVerticalPlane()).multiply(Mod.zoomMetric*Mod.getZoom()).multiply(coords.y).multiply(0.72)) :camera.getPos(),end,new RaycastContextCull(
                        Config.GSON.instance().ortho ? camera.getPos().add(new Vec3d(camera.getDiagonalPlane()).multiply(Mod.zoomMetric*Mod.getZoom()).multiply(coords.x).multiply(-0.72)).add(new Vec3d(camera.getVerticalPlane()).multiply(Mod.zoomMetric*Mod.getZoom()).multiply(coords.y).multiply(0.72)) :camera.getPos(),
                        end,
CustomShapeTypes.CULLED,
                    RaycastContext.ShapeType.VISUAL,
                        RaycastContext.FluidHandling.NONE,
                        cameraEntity
                ),(innerContext, pos) -> {
                    BlockState blockState = client.player.getWorld().getBlockState(pos);
                    FluidState fluidState = client.player.getWorld().getFluidState(pos);
                    Vec3d vec3d = innerContext.getStart();
                    Vec3d vec3d2 = innerContext.getEnd();
                    VoxelShape voxelShape = innerContext.getBlockShape(blockState, client.player.getWorld(), pos);
                    BlockHitResult firstResult = client.world.raycastBlock(vec3d, vec3d2, pos, voxelShape, blockState);


                    VoxelShape voxelShape2 = innerContext.getFluidShape(fluidState, client.player.getWorld(), pos);
                    BlockHitResult blockHitResult2 = voxelShape2.raycast(vec3d, vec3d2, pos);
                    double d = firstResult == null ? Double.MAX_VALUE : innerContext.getStart().squaredDistanceTo(firstResult.getPos());
                    double e = blockHitResult2 == null ? Double.MAX_VALUE : innerContext.getStart().squaredDistanceTo(blockHitResult2.getPos());
                    return d <= e ? firstResult : blockHitResult2;
                }, (innerContext) -> {
                    Vec3d vec3d = innerContext.getStart().subtract(innerContext.getEnd());
                    return BlockHitResult.createMissed(innerContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), BlockPos.ofFloored(innerContext.getEnd()));
                });
            /*    BlockHitResult scanUp = client.player.getWorld().raycast(
                        new RaycastContext(
                                hitResult0.getPos(),Config.GSON.instance().ortho ? camera.getPos().add(new Vec3d(camera.getDiagonalPlane()).multiply(Mod.zoomMetric*Mod.getZoom()).multiply(coords.x).multiply(-0.72)).add(new Vec3d(camera.getVerticalPlane()).multiply(Mod.zoomMetric*Mod.getZoom()).multiply(coords.y).multiply(0.72)) :camera.getPos(), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE,client.player)

                );
                BlockHitResult scanDown = client.player.getWorld().raycast(
                        new RaycastContext(
                                scanUp.getPos(),scanUp.getPos().add(hitResult0.getPos().subtract(scanUp.getPos()).normalize().multiply(renderer.getFarPlaneDistance()*2)), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE,client.player)

                );*/

                map.put(List.of(x,y),hitResult0);
            }

        }
        return map;
    }
    public List<Vec2f> points;
    public void populatePoints() {
        points = new ArrayList<>();
         realPoints = new ArrayList<>();

        if(map != null) {
            for(List<Integer> integers : map.keySet()) {
                HitResult hitResult = map.get(integers);
                if (hitResult != null && hitResult.getPos() != null) {
                    Vec3d vec3d = hitResult.getPos();
                    HitResult result = cameraEntity.getWorld().raycast(new RaycastContext(cameraEntity.getEyePos(), vec3d.add(0,cameraEntity.getEyeHeight(cameraEntity.getPose()),0), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, cameraEntity));
                    if ((cameraEntity instanceof LivingEntity living && (angleBetween(hitResult.getPos().subtract(cameraEntity.getEyePos()),living.getRotationVector()) > 50 || client.getCameraEntity() instanceof LivingEntity livingEntity && livingEntity.hasStatusEffect(DARKNESS)) && hitResult.getPos().distanceTo(living.getEyePos()) >= Mod.getZoom()*2) ||(((result.getPos() != null && cameraEntity.getWorld().getLightLevel(LightType.BLOCK, BlockPos.ofFloored(result.getPos()))+cameraEntity.getWorld().getLightLevel(LightType.SKY, BlockPos.ofFloored(result.getPos())) <= 6) || (!(result instanceof BlockHitResult result1 && result1.getType().equals(HitResult.Type.MISS)))) &&  hitResult.getPos().distanceTo(cameraEntity.getEyePos()) > Mod.getZoom()*2)) {
                        realPoints.add(hitResult);
                        points.add(new Vec2f(integers.get(0), integers.get(1)));
                    }
                }
            }
        }

    }
    List<Float> offsets =  new ArrayList<>(Collections.nCopies(42*18*2,0f));


    public void render(DrawContext context, float delta){
        context.getMatrices().push();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Random random = new Random();

        if(points != null) {

            for (Vec2f point : points) {
                var a = Math.clamp((float) (offsets.get(points.indexOf(point))+random.nextGaussian()*-(offsets.get(points.indexOf(point)))+random.nextGaussian()),-50,50);
                var b =Math.clamp((float) (offsets.get(points.indexOf(point)+1)+random.nextGaussian()*-(offsets.get(points.indexOf(point)+1))+random.nextGaussian()),-50,50);
                var c = window.calculateScaleFactor(client.options.getGuiScale().getValue(),false);
                context.drawTexture(Identifier.of("dungeons_iso","textures/shader/sample.png"), (int) point.x/c+(int)a/c-150/c, (int) point.y/c+(int)b/c-150/c,0,0,300/c,300/c,300/c,300/c);

                offsets.set(points.indexOf(point), a);
                offsets.set(points.indexOf(point)+1, b);

            }
        }
        RenderSystem.disableBlend();
        context.getMatrices().pop();


    }
    private static <T, C> T raycast(Vec3d start, Vec3d end, C context, BiFunction<C, BlockPos, T> blockHitFactory, Function<C, T> missFactory) {
        if (start.equals(end)) {
            return missFactory.apply(context);
        } else {
            double d = MathHelper.lerp(-1.0E-7, end.x, start.x);
            double e = MathHelper.lerp(-1.0E-7, end.y, start.y);
            double f = MathHelper.lerp(-1.0E-7, end.z, start.z);
            double g = MathHelper.lerp(-1.0E-7, start.x, end.x);
            double h = MathHelper.lerp(-1.0E-7, start.y, end.y);
            double i = MathHelper.lerp(-1.0E-7, start.z, end.z);
            int j = MathHelper.floor(g);
            int k = MathHelper.floor(h);
            int l = MathHelper.floor(i);
            BlockPos.Mutable mutable = new BlockPos.Mutable(j, k, l);
            T object = blockHitFactory.apply(context, mutable);
            if (object != null) {
                return object;
            } else {
                double m = d - g;
                double n = e - h;
                double o = f - i;
                int p = MathHelper.sign(m);
                int q = MathHelper.sign(n);
                int r = MathHelper.sign(o);
                double s = p == 0 ? Double.MAX_VALUE : (double)p / m;
                double t = q == 0 ? Double.MAX_VALUE : (double)q / n;
                double u = r == 0 ? Double.MAX_VALUE : (double)r / o;
                double v = s * (p > 0 ? 1.0 - MathHelper.fractionalPart(g) : MathHelper.fractionalPart(g));
                double w = t * (q > 0 ? 1.0 - MathHelper.fractionalPart(h) : MathHelper.fractionalPart(h));
                double x = u * (r > 0 ? 1.0 - MathHelper.fractionalPart(i) : MathHelper.fractionalPart(i));

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
