package com.maxenonyme.createsubmarine.submarine.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;

@Mixin(Entity.class)
public abstract class EntityWaterPhysicsMixin {

    @Inject(method = "isInWater", at = @At("HEAD"), cancellable = true)
    private void createsubmarine$isInWater(CallbackInfoReturnable<Boolean> cir) {
        if (createsubmarine$isInsideAirtightSub()) cir.setReturnValue(false);
    }

    @Inject(method = "getFluidHeight(Lnet/minecraft/tags/TagKey;)D", at = @At("HEAD"), cancellable = true)
    private void createsubmarine$getFluidHeight(net.minecraft.tags.TagKey<?> fluidTag, CallbackInfoReturnable<Double> cir) {
        if (createsubmarine$isInsideAirtightSub()) cir.setReturnValue(0.0D);
    }

    @Inject(method = "updateInWaterStateAndDoFluidPushing", at = @At("HEAD"), cancellable = true)
    private void createsubmarine$cancelWaterPushing(CallbackInfoReturnable<Boolean> cir) {
        if (createsubmarine$isInsideAirtightSub()) cir.setReturnValue(false);
    }

    @Inject(method = "isEyeInFluid", at = @At("HEAD"), cancellable = true)
    private void createsubmarine$isEyeInFluid(net.minecraft.tags.TagKey<?> fluidTag, CallbackInfoReturnable<Boolean> cir) {
        if (createsubmarine$isInsideAirtightSub()) cir.setReturnValue(false);
    }

    @Inject(method = "updateFluidOnEyes", at = @At("HEAD"), cancellable = true)
    private void createsubmarine$updateFluidOnEyes(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (createsubmarine$isInsideAirtightSub()) {
            ci.cancel();
        }
    }

    @Inject(method = "getEyeInFluidType", at = @At("HEAD"), cancellable = true, remap = false)
    private void createsubmarine$getEyeInFluidType(CallbackInfoReturnable<net.neoforged.neoforge.fluids.FluidType> cir) {
        if (createsubmarine$isInsideAirtightSub()) {
            cir.setReturnValue(net.neoforged.neoforge.common.NeoForgeMod.EMPTY_TYPE.value());
        }
    }

    @Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
    private void createsubmarine$updateSwimming(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (createsubmarine$isInsideAirtightSub()) {
            Entity entity = (Entity) (Object) this;
            entity.setSwimming(false);
            ci.cancel();
        }
    }

    @Unique
    private boolean createsubmarine$isInsideAirtightSub() {
        Entity entity = (Entity) (Object) this;
        if (entity.level() == null) return false;
        
        net.minecraft.world.phys.Vec3 eyePos = new net.minecraft.world.phys.Vec3(entity.getX(), entity.getEyeY(), entity.getZ());
        if (com.maxenonyme.createsubmarine.submarine.compartment.CompartmentTracker.isOccludedExact(entity.level(), eyePos)) return true;
        
        net.minecraft.world.phys.Vec3 feetPos = entity.position();
        return com.maxenonyme.createsubmarine.submarine.compartment.CompartmentTracker.isOccludedExact(entity.level(), feetPos);
    }
}
