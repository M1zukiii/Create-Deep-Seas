package com.maxenonyme.createsubmarine.submarine.system;
import com.maxenonyme.createsubmarine.submarine.util.SubLevelRegistry;
import com.maxenonyme.createsubmarine.submarine.math.OrientedBoundingBox3d;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3d;
import java.util.Map;
import java.util.UUID;
public class SubmarineInteractionSystem {
    private static final java.util.concurrent.ConcurrentHashMap<UUID, Vector3d> LAST_POSITIONS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<UUID, Double> SUB_VELOCITIES = new java.util.concurrent.ConcurrentHashMap<>();
    private static int tickCounter = 0;

    public static void clearAll() {
        LAST_POSITIONS.clear();
        SUB_VELOCITIES.clear();
        tickCounter = 0;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        Map<UUID, OrientedBoundingBox3d> hulls = SubmarineHullManager.getActiveHulls();
        if (hulls.isEmpty()) {
            LAST_POSITIONS.clear();
            SUB_VELOCITIES.clear();
            return;
        }
        for (Map.Entry<UUID, OrientedBoundingBox3d> entry : hulls.entrySet()) {
            UUID id = entry.getKey();
            OrientedBoundingBox3d obb = entry.getValue();
            Level level = SubLevelRegistry.getLevel(id);
            if (level instanceof ServerLevel serverLevel) {
                Vector3d currentPos = new Vector3d(obb.getPosition());
                Vector3d lastPos = LAST_POSITIONS.get(id);
                double velocity = 0;
                if (lastPos != null) {
                    velocity = currentPos.distance(lastPos);
                }
                LAST_POSITIONS.put(id, currentPos);
                SUB_VELOCITIES.put(id, velocity);
                blockEntities(serverLevel, obb, velocity);
                if (velocity > 0.01 && tickCounter % 5 == 0) {
                    clearVegetation(serverLevel, obb);
                }
            }
        }
    }
    private static void blockEntities(ServerLevel level, OrientedBoundingBox3d obb, double velocity) {
        Vector3d pos = obb.getPosition();
        Vector3d dim = obb.getDimensions();
        boolean isHighSpeed = velocity > 1.0;
        double maxDim = Math.max(dim.x, Math.max(dim.y, dim.z)) * 0.8;
        AABB searchBox = new AABB(
            pos.x - maxDim, pos.y - maxDim, pos.z - maxDim,
            pos.x + maxDim, pos.y + maxDim, pos.z + maxDim
        );
        for (Entity entity : level.getEntitiesOfClass(Entity.class, searchBox)) {
            if (entity instanceof WaterAnimal || entity.getType().is(net.minecraft.tags.EntityTypeTags.AQUATIC)) {
                if (SubmarineHullManager.contains(obb, entity.getX(), entity.getY(), entity.getZ())) {
                    Vec3 center = new Vec3(pos.x, pos.y, pos.z);
                    Vec3 pushDir = entity.position().subtract(center).normalize();
                    if (pushDir.lengthSqr() < 0.01) pushDir = new Vec3(0, 1, 0);
                    if (isHighSpeed) {
                        entity.setDeltaMovement(entity.getDeltaMovement().add(pushDir.scale(velocity * 1.5)));
                        entity.hurt(level.damageSources().generic(), (float)(velocity * 10.0));
                    } else {
                        entity.setDeltaMovement(entity.getDeltaMovement().add(pushDir.scale(0.2)));
                    }
                    entity.hasImpulse = true;
                }
            }
        }
    }
    private static void clearVegetation(ServerLevel level, OrientedBoundingBox3d obb) {
        Vector3d pos = obb.getPosition();
        Vector3d dim = obb.getDimensions();
        double maxDim = Math.max(dim.x, Math.max(dim.y, dim.z));
        int minX = (int) Math.floor(pos.x - maxDim);
        int maxX = (int) Math.ceil(pos.x + maxDim);
        int minY = (int) Math.floor(pos.y - maxDim);
        int maxY = (int) Math.ceil(pos.y + maxDim);
        int minZ = (int) Math.floor(pos.z - maxDim);
        int maxZ = (int) Math.ceil(pos.z + maxDim);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (SubmarineHullManager.contains(obb, x + 0.5, y + 0.5, z + 0.5)) {
                        mutablePos.set(x, y, z);
                        BlockState state = level.getBlockState(mutablePos);
                        if (isVegetation(state)) {
                            clearVegetationColumn(level, mutablePos);
                            if (RAND.nextFloat() < 0.1f) {
                                level.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE, x + 0.5, y + 0.5, z + 0.5, 2, 0.2, 0.2, 0.2, 0.01);
                            }
                        }
                    }
                }
            }
        }
    }
    private static void clearVegetationColumn(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        while (isVegetation(level.getBlockState(cursor))) {
            level.removeBlock(cursor, false);
            level.setBlock(cursor, Blocks.WATER.defaultBlockState(), 3);
            cursor.move(net.minecraft.core.Direction.UP);
            if (cursor.getY() > level.getMaxBuildHeight()) break;
        }
    }
    private static final java.util.Random RAND = new java.util.Random();
    private static boolean isVegetation(BlockState state) {
        return state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT) ||
               state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS);
    }
}
