package com.maxenonyme.createsubmarine.submarine.client.renderer;

import com.maxenonyme.createsubmarine.CreateSubmarine;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionContainer;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionRegion;
import dev.ryanhcode.sable.util.BoundedBitVolume3i;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


// WARNING
// This feature will be used for boats, but it doesn't suit me yet, so I think I'll put it in experimental mode.


@EventBusSubscriber(modid = CreateSubmarine.MOD_ID, value = Dist.CLIENT)
public final class PermanentWaterCullingTest {

    public static final boolean ENABLED = false;

    private static final int UPDATE_INTERVAL_TICKS = 40;
    private static final double CAMERA_PROBE_OFFSET = 0.35;

    private static final Map<UUID, WaterOcclusionRegion> TEST_REGIONS = new HashMap<>();
    private static final Map<UUID, Set<BlockPos>> CACHED_BLOCKS = new HashMap<>();
    private static final Map<UUID, Set<BlockPos>> CACHED_INTERIOR = new HashMap<>();
    private static final Map<UUID, Long> LAST_SCAN_TICK = new HashMap<>();
    private static volatile boolean cameraInsideTestSub = false;

    private PermanentWaterCullingTest() {}

    private record ScanResult(Set<BlockPos> all, Set<BlockPos> interior) {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            clearAllState();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            clearAllState();
            return;
        }
        updateRegions(mc);
        cameraInsideTestSub = computeCameraInside(mc);
    }

    private static void updateRegions(Minecraft mc) {
        Level level = mc.level;
        if (level == null) return;
        WaterOcclusionContainer<?> occlusionContainer = WaterOcclusionContainer.getContainer(level);
        if (occlusionContainer == null) return;

        SubLevelContainer subContainer;
        try {
            subContainer = SubLevelContainer.getContainer(level);
        } catch (Throwable t) {
            return;
        }
        if (subContainer == null) return;

        long now = level.getGameTime();
        List<? extends SubLevel> allSubs = subContainer.getAllSubLevels();
        Set<UUID> seenIds = new HashSet<>();

        for (SubLevel sub : allSubs) {
            UUID id = sub.getUniqueId();
            if (id == null) continue;
            seenIds.add(id);

            boolean hasRegion = TEST_REGIONS.containsKey(id);

            if (!isAtSurface(sub)) {
                if (hasRegion) deactivateSub(id, occlusionContainer);
                continue;
            }

            Long lastScan = LAST_SCAN_TICK.get(id);
            Set<BlockPos> blocks = CACHED_BLOCKS.get(id);
            boolean needRescan = lastScan == null || blocks == null
                    || (now - lastScan) >= UPDATE_INTERVAL_TICKS;

            if (needRescan) {
                ScanResult scan = collectHullAndInterior(sub);
                blocks = scan.all();
                CACHED_BLOCKS.put(id, blocks);
                CACHED_INTERIOR.put(id, scan.interior());
                LAST_SCAN_TICK.put(id, now);
            } else if (hasRegion) {
                continue;
            }
            if (blocks == null) continue;

            if (blocks.isEmpty()) {
                if (hasRegion) deactivateSub(id, occlusionContainer);
                continue;
            }

            WaterOcclusionRegion old = TEST_REGIONS.remove(id);
            if (old != null) occlusionContainer.removeRegion(old);

            BoundedBitVolume3i volume = BoundedBitVolume3i.fromBlocks(blocks);
            if (volume == null) continue;
            WaterOcclusionRegion region = occlusionContainer.addRegion(volume);
            if (region != null) TEST_REGIONS.put(id, region);
        }

        Iterator<Map.Entry<UUID, WaterOcclusionRegion>> it = TEST_REGIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, WaterOcclusionRegion> e = it.next();
            if (!seenIds.contains(e.getKey())) {
                occlusionContainer.removeRegion(e.getValue());
                CACHED_BLOCKS.remove(e.getKey());
                CACHED_INTERIOR.remove(e.getKey());
                LAST_SCAN_TICK.remove(e.getKey());
                it.remove();
            }
        }
    }

    private static void deactivateSub(UUID id, WaterOcclusionContainer<?> occlusionContainer) {
        WaterOcclusionRegion old = TEST_REGIONS.remove(id);
        if (old != null) occlusionContainer.removeRegion(old);
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!cameraInsideTestSub) return;
        event.setNearPlaneDistance(2.0f);
        event.setFarPlaneDistance(40.0f);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!cameraInsideTestSub) return;
        event.setRed(0.02f);
        event.setGreen(0.05f);
        event.setBlue(0.2f);
    }

    private static boolean computeCameraInside(Minecraft mc) {
        if (TEST_REGIONS.isEmpty()) return false;
        Camera camera = mc.gameRenderer.getMainCamera();
        if (camera == null) return false;
        Vec3 cameraPos = camera.getPosition();
        SubLevelContainer subContainer;
        try {
            subContainer = SubLevelContainer.getContainer(mc.level);
        } catch (Throwable t) {
            return false;
        }
        if (subContainer == null) return false;

        Vector3d probe = new Vector3d();
        for (UUID id : TEST_REGIONS.keySet()) {
            Set<BlockPos> interior = CACHED_INTERIOR.get(id);
            if (interior == null || interior.isEmpty()) continue;
            SubLevel sub = subContainer.getSubLevel(id);
            if (sub == null) continue;

            for (double dx = -CAMERA_PROBE_OFFSET; dx <= CAMERA_PROBE_OFFSET; dx += CAMERA_PROBE_OFFSET) {
                for (double dy = -CAMERA_PROBE_OFFSET; dy <= CAMERA_PROBE_OFFSET; dy += CAMERA_PROBE_OFFSET) {
                    for (double dz = -CAMERA_PROBE_OFFSET; dz <= CAMERA_PROBE_OFFSET; dz += CAMERA_PROBE_OFFSET) {
                        probe.set(cameraPos.x + dx, cameraPos.y + dy, cameraPos.z + dz);
                        try {
                            sub.logicalPose().transformPositionInverse(probe);
                        } catch (Throwable ignored) {
                            continue;
                        }
                        BlockPos localPos = BlockPos.containing(probe.x, probe.y, probe.z);
                        if (interior.contains(localPos)) return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isAtSurface(SubLevel sub) {
        Level oceanLevel;
        try {
            oceanLevel = sub.getLevel();
        } catch (Throwable t) {
            return false;
        }
        if (oceanLevel == null) return false;
        BoundingBox3dc bb;
        try {
            bb = sub.boundingBox();
        } catch (Throwable t) {
            return false;
        }
        if (bb == null) return false;

        int minX = (int) Math.floor(bb.minX());
        int maxX = (int) Math.ceil(bb.maxX());
        int minZ = (int) Math.floor(bb.minZ());
        int maxZ = (int) Math.ceil(bb.maxZ());
        int topY = (int) Math.ceil(bb.maxY()) + 1;
        int botY = (int) Math.floor(bb.minY()) - 1;

        boolean waterAbove = false;
        boolean waterBelow = false;

        for (int x = minX; x <= maxX; x += 2) {
            for (int z = minZ; z <= maxZ; z += 2) {
                if (!waterAbove && isWaterAt(oceanLevel, x, topY, z)) waterAbove = true;
                if (!waterBelow && isWaterAt(oceanLevel, x, botY, z)) waterBelow = true;
                if (waterAbove && waterBelow) return false;
            }
        }
        return waterBelow && !waterAbove;
    }

    private static boolean isWaterAt(Level level, int x, int y, int z) {
        try {
            LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            if (chunk == null) return false;
            return chunk.getFluidState(new BlockPos(x, y, z)).is(FluidTags.WATER);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static ScanResult collectHullAndInterior(SubLevel sub) {
        Set<BlockPos> all = new HashSet<>();
        Set<BlockPos> interior = new HashSet<>();
        LevelPlot plot = sub.getPlot();
        if (plot == null) return new ScanResult(all, interior);
        BoundingBox3ic bb = plot.getBoundingBox();
        if (bb == null) return new ScanResult(all, interior);

        int minX = bb.minX(), maxX = bb.maxX();
        int minY = bb.minY(), maxY = bb.maxY();
        int minZ = bb.minZ(), maxZ = bb.maxZ();
        int sX = maxX - minX + 1;
        int sY = maxY - minY + 1;
        int sZ = maxZ - minZ + 1;
        if (sX <= 0 || sY <= 0 || sZ <= 0) return new ScanResult(all, interior);

        boolean[][][] solid = new boolean[sX][sY][sZ];

        ChunkPos lastPos = null;
        LevelChunk lastChunk = null;
        try {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    ChunkPos cpos = new ChunkPos(x >> 4, z >> 4);
                    if (lastPos == null || !lastPos.equals(cpos)) {
                        lastPos = cpos;
                        lastChunk = plot.getChunk(plot.toLocal(cpos));
                    }
                    if (lastChunk == null) continue;
                    for (int y = minY; y <= maxY; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = lastChunk.getBlockState(pos);
                        if (!state.isAir()) {
                            solid[x - minX][y - minY][z - minZ] = true;
                            all.add(pos);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            return new ScanResult(all, interior);
        }

        markInterior(solid, sX, sY, sZ, minX, minY, minZ, dominantUpAxis(sub), all, interior);
        return new ScanResult(all, interior);
    }

    private static int dominantUpAxis(SubLevel sub) {
        Vector3d worldUp = new Vector3d(0, 1, 0);
        try {
            sub.logicalPose().transformNormalInverse(worldUp);
        } catch (Throwable ignored) {
            return 1;
        }
        double ax = Math.abs(worldUp.x);
        double ay = Math.abs(worldUp.y);
        double az = Math.abs(worldUp.z);
        if (ay >= ax && ay >= az) return 1;
        if (ax >= az) return 0;
        return 2;
    }

    
    private static void markInterior(boolean[][][] solid, int sX, int sY, int sZ,
                                     int minX, int minY, int minZ, int upAxis,
                                     Set<BlockPos> all, Set<BlockPos> interior) {
        int axisA, axisB, sLayer, sA, sB;
        switch (upAxis) {
            case 0 -> { axisA = 1; axisB = 2; sLayer = sX; sA = sY; sB = sZ; }
            case 2 -> { axisA = 0; axisB = 1; sLayer = sZ; sA = sX; sB = sY; }
            default -> { axisA = 0; axisB = 2; sLayer = sY; sA = sX; sB = sZ; }
        }

        int[] aMin = new int[sB];
        int[] aMax = new int[sB];
        int[] bMin = new int[sA];
        int[] bMax = new int[sA];
        int[] c = new int[3];

        for (int layer = 0; layer < sLayer; layer++) {
            c[upAxis] = layer;

            for (int b = 0; b < sB; b++) {
                aMin[b] = -1; aMax[b] = -1;
                c[axisB] = b;
                for (int a = 0; a < sA; a++) {
                    c[axisA] = a;
                    if (solid[c[0]][c[1]][c[2]]) {
                        if (aMin[b] == -1) aMin[b] = a;
                        aMax[b] = a;
                    }
                }
            }
            for (int a = 0; a < sA; a++) {
                bMin[a] = -1; bMax[a] = -1;
                c[axisA] = a;
                for (int b = 0; b < sB; b++) {
                    c[axisB] = b;
                    if (solid[c[0]][c[1]][c[2]]) {
                        if (bMin[a] == -1) bMin[a] = b;
                        bMax[a] = b;
                    }
                }
            }
            for (int a = 0; a < sA; a++) {
                if (bMin[a] < 0) continue;
                c[axisA] = a;
                for (int b = 0; b < sB; b++) {
                    if (aMin[b] < 0) continue;
                    c[axisB] = b;
                    if (solid[c[0]][c[1]][c[2]]) continue;
                    if (a > aMin[b] && a < aMax[b] && b > bMin[a] && b < bMax[a]) {
                        BlockPos pos = new BlockPos(c[0] + minX, c[1] + minY, c[2] + minZ);
                        all.add(pos);
                        interior.add(pos);
                    }
                }
            }
        }
    }

    private static void clearAllState() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && !TEST_REGIONS.isEmpty()) {
            WaterOcclusionContainer<?> container = WaterOcclusionContainer.getContainer(mc.level);
            if (container != null) {
                for (WaterOcclusionRegion r : TEST_REGIONS.values()) {
                    container.removeRegion(r);
                }
            }
        }
        TEST_REGIONS.clear();
        CACHED_BLOCKS.clear();
        CACHED_INTERIOR.clear();
        LAST_SCAN_TICK.clear();
        cameraInsideTestSub = false;
    }
}
