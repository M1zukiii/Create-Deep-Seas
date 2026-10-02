package com.maxenonyme.createsubmarine.submarine.block.entity;

import com.maxenonyme.createsubmarine.CreateSubmarine;
import com.maxenonyme.createsubmarine.submarine.system.SubmarinePressureSystem;
import com.maxenonyme.createsubmarine.submarine.system.SubmarineSinkingSystem;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import com.maxenonyme.createsubmarine.submarine.client.VeilLights;
import com.maxenonyme.createsubmarine.submarine.config.SubmarineConfig;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class IndustrialAlarmBlockEntity extends BlockEntity {
    private static final int[] STAGES = { 40, 30, 20, 10, 4 };
    private static final int STAGE_DEPTH = 5;

    public int period;
    private int scan;
    private int weakest = -1;
    private Object lightHandle;

    public IndustrialAlarmBlockEntity(BlockPos pos, BlockState state) {
        super(CreateSubmarine.INDUSTRIAL_ALARM_BE.get(), pos, state);
    }

    public boolean isOn(long gameTime) {
        return period > 0 && gameTime % period < Math.max(1, period / 2);
    }

    public boolean isShining(long gameTime) {
        if (SubmarineConfig.SERVER_SPEC.isLoaded() && SubmarineConfig.ALARM_STEADY_LIGHT.get())
            return period > 0;
        return isOn(gameTime);
    }

    public void tickServer() {
        if (++scan % 10 == 0)
            updatePeriod();
        boolean lit = isShining(level.getGameTime());
        BlockState state = getBlockState();
        if (state.getValue(BlockStateProperties.LIT) != lit)
            level.setBlock(worldPosition, state.setValue(BlockStateProperties.LIT, lit), Block.UPDATE_CLIENTS);
    }

    private void updatePeriod() {
        int wanted = 0;
        SubLevelAccess sub = SableCompanion.INSTANCE.getContaining(level, worldPosition);
        if (sub != null) {
            UUID id = sub.getUniqueId();
            if (scan % 40 == 0 || weakest < 0)
                weakest = SubmarinePressureSystem.getWeakestHullDepth(id, level);
            int depth = SubmarinePressureSystem.getCachedDepth(id);
            if (SubmarineSinkingSystem.isCrashing(id)) {
                wanted = STAGES[STAGES.length - 1];
            } else if (weakest > 0 && depth > weakest) {
                wanted = STAGES[Math.min(STAGES.length - 1, (depth - weakest - 1) / STAGE_DEPTH)];
            }
        }
        if (wanted == period)
            return;
        period = wanted;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void tickClient() {
        long time = level.getGameTime();
        if (!isShining(time)) {
            freeLight();
            return;
        }
        Vec3 world = Sable.HELPER.projectOutOfSubLevel(level, Vec3.atCenterOf(worldPosition));
        if (!VeilLights.usableForAlarm()) {
            freeLight();
        } else if (lightHandle instanceof LightRenderHandle<?> handle && handle.getLightData() instanceof PointLightData light) {
            light.setPosition(world.x, world.y, world.z);
            handle.markDirty();
        } else {
            PointLightData light = new PointLightData();
            light.setPosition(world.x, world.y, world.z);
            light.setBrightness(7.2f);
            light.setColor(1.0f, 0.0f, 0.0f);
            light.setRadius(6.5f);
            light.setOcclusionEnabled(false);
            lightHandle = VeilRenderSystem.renderer().getLightRenderer().addLight(light);
        }
        if (time % period == 0) {
            float pitch = (time / period) % 2 == 0 ? 1.0f : 0.8f;
            level.playLocalSound(world.x, world.y, world.z, CreateSubmarine.INDUSTRIAL_ALARM_SOUND.get(),
                    SoundSource.BLOCKS, 1.5f, pitch, false);
        }
    }

    private void freeLight() {
        if (lightHandle instanceof LightRenderHandle<?> handle) {
            handle.free();
            lightHandle = null;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide)
            freeLight();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && level.isClientSide)
            freeLight();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Period", period);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        period = tag.getInt("Period");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
