package com.maxenonyme.highseas.sail;

import com.maxenonyme.createsubmarine.submarine.util.SablePhysicsHelper;
import com.maxenonyme.highseas.wind.WindConfig;
import com.maxenonyme.highseas.wind.WindManager;
import com.maxenonyme.highseas.wind.WindSample;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import com.maxenonyme.highseas.BoatBuoyancySystem;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import com.maxenonyme.highseas.config.HighSeasConfig;
import dev.eriksonn.aeronautics.content.particle.GustParticleData;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class SailWindSystem {
    private SailWindSystem() {
    }

    private static final double SERVER_STEP = 0.05;
    private static final double MAX_ACCEL = 3.0;
    private static final double REFERENCE_LENGTH = 8.0;
    private static final double SAIL_DRAG = 1.75;
    private static final double MAX_HIDDEN_DRAG = 8.0;
    private static final double HIDDEN_DRAG_BLEND = 0.08;

    private static final Map<UUID, Trim> TRIMS = new HashMap<>();

    private static final class Trim {
        double speed;
        double push;
        double modelled;
        double hidden;
    }

    private static final class Drive {
        final Vector3d windForward = new Vector3d();
        Vector3d fallback;
        double canvas;
        double weighted;
        double sailDrag;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        long gameTime = event.getServer().getTickCount();
        Map<SubLevel, Drive> drives = new IdentityHashMap<>();
        for (ServerLevel level : event.getServer().getAllLevels()) {
            ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container == null) {
                continue;
            }
            var subs = container.getAllSubLevels();
            for (ServerSubLevel ship : subs) {
                SubLevel root = BoatClassifier.rootOf(level, subs, ship, gameTime);
                if (root == null) {
                    continue;
                }
                collect(level, ship, root, gameTime, drives);
            }
        }
        drives.forEach(SailWindSystem::sail);
        if (!TRIMS.isEmpty()) {
            Set<UUID> sailing = new HashSet<>();
            for (SubLevel root : drives.keySet())
                sailing.add(root.getUniqueId());
            TRIMS.keySet().retainAll(sailing);
        }
    }

    private static Vector3d neutral(SailGroup group, Quaterniondc rootOrient) {
        Vec3 ln = group.localNormal();
        Vector3d rest = new Vector3d(ln.x, ln.y, ln.z);
        rootOrient.transform(rest);
        rest.y = 0.0;
        if (rest.lengthSquared() < 1.0e-9) {
            return new Vector3d();
        }
        return rest.normalize();
    }

    private static double bulgeSide(SailGroup group, Vec3 wind, Vector3d worldNormal) {
        if (group.supportSign() != 0) {
            return -group.supportSign();
        }
        double windDotN = wind.x * worldNormal.x + wind.y * worldNormal.y + wind.z * worldNormal.z;
        return windDotN >= 0 ? 1.0 : -1.0;
    }

    private static void pull(ServerLevel level, ServerSubLevel source, Pose3dc sailPose, Quaterniondc rootOrient,
            SailGroup group, double factor, double speedRatio, Vector3d windForward, double[] drive) {
        if (!BoatClassifier.inAir(level, source, group.localCenter()))
            return;
        Vec3 c = group.localCenter();
        Vector3d worldCenter = sailPose.transformPosition(new Vector3d(c.x, c.y, c.z));
        Vec3 ln = group.localNormal();
        Vector3d worldNormal = sailPose.orientation().transform(new Vector3d(ln.x, ln.y, ln.z));
        if (worldNormal.lengthSquared() < 1.0e-9)
            return;
        worldNormal.normalize();
        Vec3 wind = WindManager.getWind(level, worldCenter.x, worldCenter.y, worldCenter.z).vector();
        Vector3d rest = neutral(group, rootOrient);
        double side = bulgeSide(group, wind, worldNormal);
        double swing = rest.x * worldNormal.x + rest.z * worldNormal.z;
        double canvas = group.area() * factor * Math.abs(swing);
        double pointOfSail = SailForce.pointOfSail(wind, worldNormal.x, worldNormal.y, worldNormal.z,
                rest.x * side, rest.y * side, rest.z * side);
        windForward.fma(side * group.area() * factor * swing, rest);
        drive[0] += canvas;
        drive[1] += canvas * pointOfSail * SailForce.windFactor(wind);
        if (canvas > 0.1 && speedRatio > 0.05 && level.random.nextFloat() < 0.4f * speedRatio) {
            Vector3f dir = new Vector3f((float) wind.x, (float) wind.y, (float) wind.z);
            if (dir.lengthSquared() > 1.0e-6f) {
                dir.normalize();
                Quaternionf gust = new Quaternionf().rotationTo(new Vector3f(0.0f, 1.0f, 0.0f), dir);
                level.sendParticles(new GustParticleData(gust), worldCenter.x, worldCenter.y, worldCenter.z,
                        (int) Math.ceil(4 * speedRatio), 3.0, 3.0, 3.0, 0.0);
            }
        }
    }

    private static void collect(ServerLevel parentLevel, ServerSubLevel sailSource, SubLevel root, long gameTime,
            Map<SubLevel, Drive> drives) {
        if (sailSource.getPlot() == null || root.getPlot() == null) {
            return;
        }
        List<SailGroup> sails = SailWindRegistry.getSails(sailSource, gameTime);
        if (sails.isEmpty()) {
            return;
        }

        Pose3dc sailPose = sailSource.logicalPose();
        Quaterniondc sailOrient = sailPose.orientation();

        Quaterniondc rootOrient = sailSource == root ? root.logicalPose().orientation() : sailOrient;
        Vector3d forward = SailForce.sailForward(sailOrient, sails);
        if (forward == null) {
            return;
        }

        Object handle = SablePhysicsHelper.getHandle(root);
        if (handle == null) {
            return;
        }
        Vector3dc velocity = SablePhysicsHelper.getVelocity(handle);
        double forwardSpeed = 0.0;
        if (velocity != null) {
            forwardSpeed = velocity.x() * forward.x + velocity.y() * forward.y + velocity.z() * forward.z;
        }
        double speedRatio = Mth.clamp(Math.abs(forwardSpeed) / WindConfig.SAIL_SPEED_REFERENCE, 0.0, 1.0);

        Drive drive = drives.computeIfAbsent(root, r -> new Drive());
        if (drive.fallback == null)
            drive.fallback = forward;
        for (SailGroup group : sails) {
            if (group.axis() == Direction.Axis.Y)
                continue;
            Vec3 c = group.localCenter();
            drive.sailDrag += group.area() * DimensionPhysicsData.getAirPressure(parentLevel,
                    sailPose.transformPosition(new Vector3d(c.x, c.y, c.z)));
        }
        double[] sums = new double[2];
        UUID sourceId = sailSource.getUniqueId();
        for (SailGroup group : sails) {
            if (group.axis() == Direction.Axis.Y || FurlState.isFurled(sourceId, group.min()))
                continue;
            double factor = Math.min(1.0, (gameTime - group.startTick()) / 60.0);
            pull(parentLevel, sailSource, sailPose, rootOrient, group, factor, speedRatio, drive.windForward, sums);
        }
        for (DecayingSail ds : SailWindRegistry.getDecayingSails(sourceId, gameTime)) {
            if (ds.group().axis() == Direction.Axis.Y)
                continue;
            double factor = Math.max(0.0, (60.0 - (gameTime - ds.startTick())) / 60.0);
            pull(parentLevel, sailSource, sailPose, rootOrient, ds.group(), factor, 0.0, drive.windForward, sums);
        }
        drive.canvas += sums[0];
        drive.weighted += sums[1];
    }

    private static void sail(SubLevel root, Drive drive) {
        if (drive.canvas < 1.0e-6 || root.getPlot() == null) {
            return;
        }
        Object handle = SablePhysicsHelper.getHandle(root);
        if (handle == null) {
            return;
        }

        Vector3d dir = drive.windForward;
        dir.y = 0.0;
        if (dir.lengthSquared() > 1.0e-9)
            dir.normalize();
        else
            dir.set(drive.fallback.x, 0.0, drive.fallback.z).normalize();

        double length = BoatBuoyancySystem.keelLength(root.getUniqueId());
        if (length <= 0.0) {
            BoundingBox3ic bb = root.getPlot().getBoundingBox();
            length = Math.max(bb.maxX() - bb.minX(), bb.maxZ() - bb.minZ()) + 1;
        }
        double hullMass = Math.max(1.0, SablePhysicsHelper.readMass(root));
        double section = Math.cbrt(hullMass * hullMass);

        double hullSpeed = HighSeasConfig.sailHullSpeed * Math.sqrt(length);
        double rigging = Math.min(1.0, Math.sqrt(drive.canvas / (HighSeasConfig.sailCanvasNeeded * section)));
        double target = Math.min(HighSeasConfig.sailMaxSpeed, hullSpeed * rigging * drive.weighted / drive.canvas);

        Vector3dc velocity = SablePhysicsHelper.getVelocity(handle);
        double speed = velocity == null ? 0.0 : velocity.x() * dir.x + velocity.z() * dir.z;
        double response = HighSeasConfig.sailResponseTime * Mth.clamp(Math.sqrt(length / REFERENCE_LENGTH), 0.7, 2.0);
        UUID id = root.getUniqueId();
        double drag = BoatBuoyancySystem.forwardDrag(id, speed) + SAIL_DRAG * drive.sailDrag * speed / hullMass;

        Trim trim = TRIMS.get(id);
        if (trim == null) {
            trim = new Trim();
            TRIMS.put(id, trim);
        } else {
            double observed = (speed - trim.speed) / SERVER_STEP;
            double hidden = Mth.clamp(trim.push - trim.modelled - observed, -MAX_HIDDEN_DRAG, MAX_HIDDEN_DRAG);
            trim.hidden = Mth.lerp(HIDDEN_DRAG_BLEND, trim.hidden, hidden);
        }

        double accel = drag + trim.hidden + Math.min((target - speed) / response, MAX_ACCEL);
        trim.speed = speed;
        trim.modelled = drag;
        trim.push = Math.max(0.0, accel);
        if (accel <= 0.0) {
            return;
        }

        double total = accel * hullMass * SERVER_STEP;
        Vector3d forceWorld = new Vector3d(dir.x * total, 0.0, dir.z * total);
        root.logicalPose().orientation().conjugate(new Quaterniond()).transform(forceWorld);
        SablePhysicsHelper.applyLinearImpulse(handle, forceWorld);
    }
}
