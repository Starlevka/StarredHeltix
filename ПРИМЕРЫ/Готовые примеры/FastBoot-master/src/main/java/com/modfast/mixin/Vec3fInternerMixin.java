package com.modfast.mixin;

import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.render.model.ModelBaker$Vec3fInternerImpl")
public class Vec3fInternerMixin {

    /**
     * By skipping the Guava Strong Interner, we trade a few MBs of RAM for MASSIVE 
     * parallelism speedups on high-core-count CPUs (like the user's i7-14700K).
     * The interner is a severe lock-contention bottleneck when up to 28 threads bake models simultaneously.
     */
    @Inject(method = "intern", at = @At("HEAD"), cancellable = true)
    private void skipInternerContention(Vector3fc vec, CallbackInfoReturnable<Vector3fc> cir) {
        cir.setReturnValue(vec);
    }
}
