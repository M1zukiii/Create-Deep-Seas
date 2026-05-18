package com.maxenonyme.createsubmarine.submarine.network;

import com.maxenonyme.createsubmarine.CreateSubmarine;
import com.maxenonyme.createsubmarine.submarine.block.entity.ElectrolyzerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ElectrolyzerTogglePayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ElectrolyzerTogglePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateSubmarine.MOD_ID, "electrolyzer_toggle"));

    public static final StreamCodec<FriendlyByteBuf, ElectrolyzerTogglePayload> CODEC = StreamCodec.of(
        (buf, payload) -> buf.writeBlockPos(payload.pos()),
        buf -> new ElectrolyzerTogglePayload(buf.readBlockPos())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ElectrolyzerTogglePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player == null) return;
            BlockPos pos = payload.pos();
            double dx = (pos.getX() + 0.5) - player.getX();
            double dy = (pos.getY() + 0.5) - player.getY();
            double dz = (pos.getZ() + 0.5) - player.getZ();
            if (dx * dx + dy * dy + dz * dz > 64.0) return;
            if (player.level().getBlockEntity(pos) instanceof ElectrolyzerBlockEntity be) {
                be.toggleEnabled();
            }
        });
    }
}