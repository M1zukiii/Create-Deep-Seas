package com.maxenonyme.createsubmarine.submarine.mixin.compat;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import foundry.veil.platform.VeilEventPlatform;

//WARNING! The version used by Create Aeronautics contains an incompatibility with Sodium. I will either submit a pull request
//On their GitHub or inject a fix

@Mixin(targets = "dev.eriksonn.aeronautics.AeronauticsClient", remap = false)
public class AeronauticsVeilCrashFixMixin {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean createsubmarine$warned = false;

    // Aeronautics tries to register Veil events that we (or Veil itself) already registered.
    // Swallow the IAE on first call, log once so we know it happened.
    @Redirect(method = "registerEvents", at = @At(value = "INVOKE", target = "Lfoundry/veil/platform/VeilEventPlatform;onVeilRegisterBlockLayers(Lfoundry/veil/api/event/VeilRegisterBlockLayersEvent;)V", remap = false), remap = false, require = 0)
    private static void createsubmarine$redirectBlockLayers(VeilEventPlatform instance,
            foundry.veil.api.event.VeilRegisterBlockLayersEvent event) {
        try {
            instance.onVeilRegisterBlockLayers(event);
        } catch (IllegalArgumentException e) {
            if (!createsubmarine$warned) {
                LOGGER.warn("Suppressed Aeronautics Veil event re-registration: {}", e.getMessage());
                createsubmarine$warned = true;
            }
        }
    }

    @Redirect(method = "registerEvents", at = @At(value = "INVOKE", target = "Lfoundry/veil/platform/VeilEventPlatform;onVeilRenderLevelStage(Lfoundry/veil/api/event/VeilRenderLevelStageEvent;)V", remap = false), remap = false, require = 0)
    private static void createsubmarine$redirectRenderLevelStage(VeilEventPlatform instance,
            foundry.veil.api.event.VeilRenderLevelStageEvent event) {
        try {
            instance.onVeilRenderLevelStage(event);
        } catch (IllegalArgumentException e) {
        }
    }

    @Redirect(method = "registerEvents", at = @At(value = "INVOKE", target = "Lfoundry/veil/platform/VeilEventPlatform;onVeilRegisterFixedBuffers(Lfoundry/veil/api/event/VeilRegisterFixedBuffersEvent;)V", remap = false), remap = false, require = 0)
    private static void createsubmarine$redirectFixedBuffers(VeilEventPlatform instance,
            foundry.veil.api.event.VeilRegisterFixedBuffersEvent event) {
        try {
            instance.onVeilRegisterFixedBuffers(event);
        } catch (IllegalArgumentException e) {
        }
    }
}
