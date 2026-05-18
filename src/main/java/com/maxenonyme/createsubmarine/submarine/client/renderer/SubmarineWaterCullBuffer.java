package com.maxenonyme.createsubmarine.submarine.client.renderer;

import com.maxenonyme.createsubmarine.submarine.compartment.CompartmentTracker;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionContainer;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionRegion;
import dev.ryanhcode.sable.util.BoundedBitVolume3i;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SubmarineWaterCullBuffer {
    private static final double POSE_MOVE_THRESHOLD_SQ = 0.25;
    private static final double DEFAULT_RADIUS = 16.0;

    private static final Map<UUID, WaterOcclusionRegion> regions = new HashMap<>();
    private static final Map<UUID, Vector3d> lastClientPose = new ConcurrentHashMap<>();
    private static boolean renderingSubmarineFluid = false;

    private SubmarineWaterCullBuffer() {}

    public static boolean isRenderingSubmarineFluid() { return renderingSubmarineFluid; }
    public static void beginSubmarineFluidRender() { renderingSubmarineFluid = true; }
    public static void endSubmarineFluidRender() { renderingSubmarineFluid = false; }

    public static void clearSodiumPoseCache(UUID id) { lastClientPose.remove(id); }

    public static void syncSubmarinePoses() {
        for (Map.Entry<UUID, SubLevelAccess> entry : CompartmentTracker.getSubsSnapshot().entrySet()) {
            UUID id = entry.getKey();
            Vector3dc p = entry.getValue().logicalPose().position();
            Vector3d last = lastClientPose.get(id);
            if (last != null && last.distanceSquared(p) < POSE_MOVE_THRESHOLD_SQ) continue;

            Vector3d dims = CompartmentTracker.getCachedDimensions(id);
            double r = dims != null ? Math.max(dims.x, Math.max(dims.y, dims.z)) * 0.75 : DEFAULT_RADIUS;
            CompartmentTracker.setWorldAABB(id, new AABB(
                    p.x() - r, p.y() - r, p.z() - r,
                    p.x() + r, p.y() + r, p.z() + r));
            if (last == null) lastClientPose.put(id, new Vector3d(p));
            else last.set(p);
        }
    }

    public static void updateSubmarineOcclusion(UUID id, Collection<BlockPos> blocks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        mc.execute(() -> {
            WaterOcclusionContainer<?> container = WaterOcclusionContainer.getContainer(mc.level);
            if (container == null) return;
            WaterOcclusionRegion old = regions.remove(id);
            if (old != null) container.removeRegion(old);
            if (blocks == null || blocks.isEmpty()) return;
            BoundedBitVolume3i volume = BoundedBitVolume3i.fromBlocks(blocks);
            if (volume == null) return;
            WaterOcclusionRegion region = container.addRegion(volume);
            if (region != null) regions.put(id, region);
        });
    }
}
