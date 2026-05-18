package com.maxenonyme.createsubmarine.submarine.block.entity;
import com.maxenonyme.createsubmarine.CreateSubmarine;
import com.maxenonyme.createsubmarine.submarine.compartment.CompartmentTracker;
import com.maxenonyme.createsubmarine.submarine.util.SubLevelRegistry;
import com.maxenonyme.createsubmarine.submarine.system.SubmarinePressureSystem;
import com.maxenonyme.createsubmarine.submarine.system.SubmarineHullManager;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import java.util.UUID;
public class HullControllerBlockEntity extends BlockEntity {
    private static final int SCAN_BUDGET = 1500;
    private UUID currentSubLevelId = null;
    private boolean subLevelRegistered = false;
    public HullControllerBlockEntity(BlockPos pos, BlockState state) {
        super(CreateSubmarine.CREATIVE_OXYGENATOR_BE.get(), pos, state);
    }
    public void tick() {
        if (level == null) return;
        SubLevelAccess subAccess = SableCompanion.INSTANCE.getContaining(level, worldPosition);
        if (subAccess instanceof SubLevel sub) {
            currentSubLevelId = sub.getUniqueId();
            long gameTick = level.getGameTime();
            if (!CompartmentTracker.isScanActive(currentSubLevelId)
                    && gameTick - CompartmentTracker.lastUpdateTick(currentSubLevelId) >= 20) {
                CompartmentTracker.beginScanIfIdle(currentSubLevelId, sub);
            }
            if (CompartmentTracker.isScanActive(currentSubLevelId)) {
                boolean done = CompartmentTracker.stepScan(currentSubLevelId, sub, SCAN_BUDGET, gameTick);
                if (done && !level.isClientSide) {
                    SubmarinePressureSystem.setSealedCompartments(
                        currentSubLevelId, CompartmentTracker.getCompartments(currentSubLevelId));
                }
            }
            LevelPlot plot = sub.getPlot();
            if (plot != null) {
                BoundingBox3ic bounds = plot.getBoundingBox();
                Vector3d dimensions = CompartmentTracker.getOrComputeDimensions(currentSubLevelId, bounds);
                Pose3dc pose = sub.logicalPose();
                if (CompartmentTracker.poseMovedEnough(currentSubLevelId, pose, 0.01, 1e-6)) {
                    SubmarineHullManager.updateHull(currentSubLevelId, pose.position(), dimensions, pose.orientation());
                    CompartmentTracker.updateAABB(currentSubLevelId, pose.position(), dimensions);
                    CompartmentTracker.recordPose(currentSubLevelId, pose);
                }
                if (!level.isClientSide && !subLevelRegistered) {
                    SubLevelRegistry.register(
                        currentSubLevelId, sub, level,
                        new SubLevelRegistry.PlotBounds(bounds.minX(), bounds.maxX(), bounds.minY(), bounds.maxY(), bounds.minZ(), bounds.maxZ())
                    );
                    subLevelRegistered = true;
                }
            }
        } else if (currentSubLevelId != null) {
            SubmarineHullManager.removeHull(currentSubLevelId);
            if (!level.isClientSide) {
                SubLevelRegistry.unregister(currentSubLevelId);
                SubmarinePressureSystem.clearSubmarine(currentSubLevelId);
            }
            CompartmentTracker.remove(currentSubLevelId);
            currentSubLevelId = null;
            subLevelRegistered = false;
        }
    }
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (currentSubLevelId != null) {
            SubmarineHullManager.removeHull(currentSubLevelId);
            SubLevelRegistry.unregister(currentSubLevelId);
            CompartmentTracker.remove(currentSubLevelId);
            SubmarinePressureSystem.clearSubmarine(currentSubLevelId);
            subLevelRegistered = false;
        }
    }
}