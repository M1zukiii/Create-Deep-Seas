package com.maxenonyme.createsubmarine.submarine.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "dev.eriksonn.aeronautics.neoforge.events.AeroNeoForgeClientEvents$ModBusEvents", remap = false)
public class AeronauticsRenderTypeCrashFixMixin {

    @Redirect(
        method = "clientSetup",
        at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/ChunkRenderTypeSet;of([Lnet/minecraft/client/renderer/RenderType;)Lnet/neoforged/neoforge/client/ChunkRenderTypeSet;", remap = false),
        remap = false,
        require = 0
    )
    private static net.neoforged.neoforge.client.ChunkRenderTypeSet createsubmarine$redirectChunkRenderType(net.minecraft.client.renderer.RenderType[] renderTypes) {
        try {
            return net.neoforged.neoforge.client.ChunkRenderTypeSet.of(renderTypes);
        } catch (IllegalArgumentException e) {
            return net.neoforged.neoforge.client.ChunkRenderTypeSet.of(net.minecraft.client.renderer.RenderType.translucent());
        }
    }
}