package com.cleannrooster.dungeons_iso.api.cullers;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.compat.SodiumCompat;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public class FloodCuller implements BlockCuller {
    public Stream<BlockPos> culledBlocks = Stream.empty();

    @Override
    public boolean shouldForceCull() {
        return true;
    }

    @Override
    public boolean shouldForceNonCull() {
        return false;
    }

    @Override
    public boolean cullBlocks(  BlockPos blockPos, Camera camera, Entity cameraEntity) {

        return false;
    }

    @Override
    public float blockTransparancy(BlockPos pos) {
        return 0;
    }
    public static double angleBetween(Vec3d a, Vec3d b) {
        double cosineTheta = a.dotProduct(b) / (a.length() * b.length());
        double angle = Math.acos(cosineTheta) * 57.29577951308232;
        return Double.isNaN(angle) ? 0.0 : angle;
    }
    public final Vec3d getRotationVec(net.minecraft.entity.Entity entity, float tickDelta) {
        float f = entity.getPitch(tickDelta) * 0.017453292F;
        float g = -entity.getHeadYaw() * 0.017453292F;
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d((double)(i * j), (double)(-k), (double)(h * j));    }

    public boolean shouldCull(BlockPos blockPos, Camera camera, Entity cameraEntity){
            return cameraEntity.getWorld().getBlockState(blockPos).getCameraCollisionShape(cameraEntity.getWorld(), blockPos, ShapeContext.of(cameraEntity)).isEmpty();


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
    @Override
    public boolean shouldIgnoreBlockPick(BlockPos blockPos, Camera camera, Entity cameraEntity) {

        return false;
    }

    List<Class<? extends Block>> ignoredTypes = List.of(WallMountedBlock.class, DoorBlock.class);
    public boolean isIgnoredType(Block block){
        for(Class<? extends Block> ignoredType : ignoredTypes){
            if(ignoredType.isInstance(block)){
                return true;
            }
        }
        return false;
    }
    @Override
    public int frequency() {
        return 01;
    }

    @Override
    public List<BlockPos> getCulledBlocks(BlockPos blockPos, Camera camera, Entity cameraEntity) {



        LinkedHashMap<BlockPos,Boolean> visited = new LinkedHashMap<BlockPos,Boolean>();
        Stack<BlockPos> stack = new Stack<>();
        stack.push(cameraEntity.getBlockPos().up());
        List<BlockPos> builder = new ArrayList<>(List.of());
        while (!stack.isEmpty()  ) {
            BlockPos p = stack.pop();
            int x = p.getX();
            int z = p.getZ();
            builder.add(p);

            if(p.isWithinDistance(cameraEntity.getBlockPos(), 0.05 * (Math.min(20, Math.min(cameraEntity.getWorld().getTime() - Mod.startTime, Mod.endTime)))*16)) {
                if (!visited.containsKey(p.north())  ) {
                    if(this.shouldCull(p.north(), camera, cameraEntity)) {
                        stack.push(p.north());
                        visited.put(p.north(), true);
                        builder.add(p.north());
                    }
                }
                if (!visited.containsKey(p.east()) ) {
                    if( this.shouldCull(p.east(), camera, cameraEntity)) {
                        stack.push(p.east());
                        visited.put(p.east(), true);
                        builder.add(p.east());
                    }

                }
                if (!visited.containsKey(p.west())  ) {
                    if(this.shouldCull(p.west(), camera, cameraEntity)) {
                        stack.push(p.west());
                        visited.put(p.west(), true);
                        builder.add(p.west());
                    }

                }
                if (!visited.containsKey(p.south())  ) {
                    if(this.shouldCull(p.south(), camera, cameraEntity)) {
                        stack.push(p.south());
                        visited.put(p.south(), true);
                        builder.add(p.south());
                    }

                }
            }
        }

        return builder;
    }

    @Override
    public void resetCulledBlocks() {
    }
    public boolean isAboveFlood(BlockPos  blockPos, Camera camera, Entity cameraEntity , Stream<BlockPos> stream) {
        return stream.anyMatch(pos ->{ return blockPos.getX() == pos.getX() && blockPos.getY() > pos.getY() && blockPos.getZ() == pos.getZ();});
    }
}
