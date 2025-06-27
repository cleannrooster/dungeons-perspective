package com.cleannrooster.dungeons_iso.api.cullers;

import com.cleannrooster.dungeons_iso.api.BlockCuller;
import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import com.cleannrooster.dungeons_iso.compat.SodiumCompat;
import com.cleannrooster.dungeons_iso.mod.Mod;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class GenericCuller3 implements BlockCuller {
    public  List<BlockPos> culledBlocks = new ArrayList<>(1000);

    @Override
    public boolean shouldForceCull() {
        return true;
    }

    @Override
    public boolean shouldForceNonCull() {
        return true;
    }

    @Override
    public boolean cullBlocks(  BlockPos blockPos, Camera camera, Entity cameraEntity) {
        try {

        if( this.shouldCull(blockPos,camera,cameraEntity)){
            if(culledBlocks.size() < 1000) {
                culledBlocks.add(blockPos);
                TransparentBlock block = new TransparentBlock(blockPos, 0);
                SodiumCompat.transparentBlocks.put(blockPos, SodiumCompat.transparentBlocks.getOrDefault(blockPos, block));
            }
            else{
                culledBlocks = new ArrayList<>(1000);
                SodiumCompat.transparentBlocks = new LinkedHashMap<>();
                culledBlocks.add(blockPos);
                TransparentBlock block = new TransparentBlock(blockPos, 0);
                SodiumCompat.transparentBlocks.put(blockPos, SodiumCompat.transparentBlocks.getOrDefault(blockPos, block));

            }

            return true;
        }
        else{
            return false;
        }
        }
        catch (Exception ignored){

        }
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
    public final Vec3d getRotationVec(Entity entity, float tickDelta) {
        return entity.getRotationVector(entity.getPitch(tickDelta), entity.getHeadYaw());
    }

    public boolean shouldCull(BlockPos blockPos, Camera camera, Entity cameraEntity){
        if( ((MinecraftClientAccessor)MinecraftClient.getInstance()).shouldRebuild() && camera != null && cameraEntity != null) {
            List<Vec3d> vec3ds = new ArrayList<>();


        var posBehindPlayerUp = cameraEntity.getEyePos().subtract(getRotationVec(cameraEntity,camera.getLastTickDelta()).multiply(-4)).add(0,blockPos.getY()-cameraEntity.getEyeY(),0) ;
            var vec7 = (blockPos.toCenterPos().subtract(camera.getPos()).normalize()) ;
            var vec8 = cameraEntity.getPos().subtract(camera.getPos()).normalize();
        var vec1 = blockPos.toCenterPos().subtract(cameraEntity.getPos()).normalize();
        var vec4 = blockPos.toCenterPos().subtract(camera.getPos()).normalize().multiply(-1);
        var vec6 = blockPos.toCenterPos().subtract(cameraEntity.getPos()).normalize();
        var vec2 = camera.getPos().subtract(cameraEntity.getPos()).normalize();
        var theta =angleBetween(vec8, vec7)  ;
        var phi =angleBetween(vec1,  vec2)  ;
        var chi =angleBetween(vec6,  new Vec3d(0,1,0))  ;
        var factor = 0.1*(Math.min(10,Math.min(cameraEntity.getWorld().getTime()-Mod.startTime+2,Mod.endTime)))*45*Math.pow(0.9,Mod.zoom);
        var factor2 = 0.1*(Math.min(10,Math.min(cameraEntity.getWorld().getTime()-Mod.startTime+2,Mod.endTime)))*45*Math.pow(0.9,Mod.zoom);

        if(!isIgnoredType(cameraEntity.getWorld().getBlockState(blockPos).getBlock()) && blockPos.toCenterPos().getY() > cameraEntity.getPos().getY() + 1 &&
                (((  theta < factor  ) && chi < 60 ) ||(  camera.getPos().distanceTo(blockPos.toCenterPos()) < 5))){
            return true;
        }
        else {
            return false;

        }
        }
        else{
            return false;
        }
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
        if(!(isIgnoredType(cameraEntity.getWorld().getBlockState(blockPos).getBlock())) && blockPos != null && (cameraEntity instanceof PlayerEntity player && blockPos.toCenterPos().distanceTo(cameraEntity.getEyePos()) > player.getBlockInteractionRange())
                && blockPos.toCenterPos().getY() > cameraEntity.getY()+1){
            if(new Vec3d(0,1,0).dotProduct(blockPos.toCenterPos().subtract(cameraEntity.getPos()).normalize())>0.5F) {
                return true;
            }
        }
        return false;
    }

    List<Class<? extends Block>> ignoredTypes = List.of(VaultBlock.class, SpawnerBlock.class, TrialSpawnerBlock.class, WallMountedBlock.class,LadderBlock.class, DoorBlock.class);
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
    public List<BlockPos> getCulledBlocks() {
        return culledBlocks;
    }

    @Override
    public void resetCulledBlocks() {
    }

}
