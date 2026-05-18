package com.maxenonyme.createsubmarine.submarine.system;

import com.maxenonyme.createsubmarine.submarine.math.LevelReusedVectors;
import com.maxenonyme.createsubmarine.submarine.math.OrientedBoundingBox3d;
import net.minecraft.world.phys.AABB;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SubmarineHullManager {
    private static final Map<UUID, OrientedBoundingBox3d> AIRTIGHT_HULLS = new ConcurrentHashMap<>();
    private static final Map<UUID, AABB> HULL_AABBS = new ConcurrentHashMap<>();
    private static final LevelReusedVectors SINK = new LevelReusedVectors();

    public static void updateHull(UUID id, Vector3dc position, Vector3dc dimensions, Quaterniondc orientation) {
        OrientedBoundingBox3d obb = AIRTIGHT_HULLS.computeIfAbsent(id, k -> new OrientedBoundingBox3d(SINK));
        obb.set(position, dimensions, orientation);

        double maxDim = Math.max(dimensions.x(), Math.max(dimensions.y(), dimensions.z())) * 0.75;
        HULL_AABBS.put(id, new AABB(
            position.x() - maxDim, position.y() - maxDim, position.z() - maxDim,
            position.x() + maxDim, position.y() + maxDim, position.z() + maxDim
        ));
    }

    public static void removeHull(UUID id) {
        AIRTIGHT_HULLS.remove(id);
        HULL_AABBS.remove(id);
    }

    public static Map<UUID, OrientedBoundingBox3d> getActiveHulls() {
        return AIRTIGHT_HULLS;
    }

    public static boolean contains(OrientedBoundingBox3d obb, double x, double y, double z) {
        Vector3d center = obb.getPosition();
        Vector3d dim = obb.getDimensions();
        Vector3d local = new Vector3d(x - center.x, y - center.y, z - center.z);
        obb.getOrientation().conjugate(new Quaterniond()).transform(local);
        return Math.abs(local.x) <= dim.x * 0.5 &&
               Math.abs(local.y) <= dim.y * 0.5 &&
               Math.abs(local.z) <= dim.z * 0.5;
    }
}
