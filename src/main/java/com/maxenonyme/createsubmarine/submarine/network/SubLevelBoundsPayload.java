package com.maxenonyme.createsubmarine.submarine.network;
import com.maxenonyme.createsubmarine.submarine.util.SubLevelRegistry;
import com.maxenonyme.createsubmarine.CreateSubmarine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.UUID;
public record SubLevelBoundsPayload(UUID id, int minY, int maxY) implements CustomPacketPayload {
    public static final Type<SubLevelBoundsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateSubmarine.MOD_ID, "bounds_sync"));
    public static final StreamCodec<FriendlyByteBuf, SubLevelBoundsPayload> CODEC = CustomPacketPayload.codec(
        SubLevelBoundsPayload::write,
        SubLevelBoundsPayload::new
    );
    public SubLevelBoundsPayload(FriendlyByteBuf buffer) {
        this(buffer.readUUID(), buffer.readInt(), buffer.readInt());
    }
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(id);
        buffer.writeInt(minY);
        buffer.writeInt(maxY);
    }
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static void handle(final SubLevelBoundsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            SubLevelRegistry.updateBounds(payload.id(), payload.minY(), payload.maxY());
        });
    }
}
