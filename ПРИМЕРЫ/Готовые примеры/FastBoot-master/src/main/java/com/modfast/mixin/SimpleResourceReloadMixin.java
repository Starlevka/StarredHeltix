package com.modfast.mixin;

import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.SimpleResourceReload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(SimpleResourceReload.class)
public class SimpleResourceReloadMixin {

    @Inject(method = "createSynchronizer", at = @At("RETURN"), cancellable = true)
    private void hookSynchronizer(ResourceReloader reloader, CompletableFuture<?> future, Executor applyExecutor, CallbackInfoReturnable<ResourceReloader.Synchronizer> cir) {
        ResourceReloader.Synchronizer original = cir.getReturnValue();
        cir.setReturnValue(new ResourceReloader.Synchronizer() {
            @Override
            public <T> CompletableFuture<T> whenPrepared(T preparedObject) {
                // System.out.println("[MODFAST DEBUG] Reloader PREPARED: " + reloader.getClass().getSimpleName());
                return original.whenPrepared(preparedObject).thenApply(res -> {
                    // System.out.println("[MODFAST DEBUG] Reloader APPLIED: " + reloader.getClass().getSimpleName());
                    return res;
                });
            }
        });
    }
}
