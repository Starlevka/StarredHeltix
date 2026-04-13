package com.modfast.mixin;

import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Util.class)
public abstract class UtilMixin {

    /**
     * Maximize the threads available to Minecraft's background executor immediately. 
     * In modern versions it's already unbounded, but we force it to ensure other mods don't reduce it, 
     * and we use all cores without subtracting 1 to maximize burst loading speed.
     */
    @Inject(method = "getAvailableBackgroundThreads", at = @At("HEAD"), cancellable = true)
    private static void maximizeThreads(CallbackInfoReturnable<Integer> cir) {
        // Use all available processors, or at least a big number for parallel IO
        cir.setReturnValue(Math.max(Runtime.getRuntime().availableProcessors(), 4));
    }
}
