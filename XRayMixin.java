package com.bedro.mixin;

import com.bedro.Modules;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class XRayMixin {
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true, require = 0)
    private static void bedro$xray(BlockState state, BlockState neighbor, Direction dir, CallbackInfoReturnable<Boolean> cir) {
        if (Modules.XRAY != null && Modules.XRAY.enabled) {
            cir.setReturnValue(Modules.isXrayBlock(state.getBlock()));
        }
    }
}
