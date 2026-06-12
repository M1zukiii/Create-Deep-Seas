package com.maxenonyme.createsubmarine.submarine.mixin;
import com.maxenonyme.createsubmarine.submarine.compartment.CompartmentTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    private void createsubmarine$onGetBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        BlockState lied = CompartmentTracker.getLiedBlockState(((LevelChunk) (Object) this).getLevel(), pos);
        if (lied != null) cir.setReturnValue(lied);
    }
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void createsubmarine$onSetBlockState(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        if (cir.getReturnValue() == null) return;
        CompartmentTracker.onPlotBlockChanged(((LevelChunk) (Object) this).getLevel(), pos);
    }
}
