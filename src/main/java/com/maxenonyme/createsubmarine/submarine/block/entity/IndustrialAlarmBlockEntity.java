package com.maxenonyme.createsubmarine.submarine.block.entity;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.maxenonyme.createsubmarine.CreateSubmarine;

public class IndustrialAlarmBlockEntity extends BlockEntity {

    private Object lightHandle;

    public IndustrialAlarmBlockEntity(BlockPos pos, BlockState state) {
        super(CreateSubmarine.INDUSTRIAL_ALARM_BE.get(), pos, state);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        Level lvl = level;
        if (lvl != null && lvl.isClientSide) {
            freeLight();
        }
    }

    public void tickClient(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(BlockStateProperties.LIT)) {
            freeLight();
        } else {
            if ((level.getGameTime() / 20L) % 2L == 0L) {
                setupLight();
            } else {
                freeLight();
            }
        }
    }

    private void setupLight() {
        if (lightHandle != null) return;
        PointLightData light = new PointLightData();
        light.setPosition(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        light.setBrightness(7.200f);
        light.setColor(1.0f, 0.0f, 0.0f);
        light.setRadius(4.900f);
        light.setOcclusionEnabled(false);
        lightHandle = VeilRenderSystem.renderer().getLightRenderer().addLight(light);
    }

    private void freeLight() {
        if (lightHandle instanceof LightRenderHandle handle) {
            handle.free();
            lightHandle = null;
        }
    }
}