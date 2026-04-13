package com.modfast.mixin;

import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceReload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.gui.DrawContext;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {

    @Shadow private ResourceReload reload;
    @Shadow private long reloadCompleteTime;

    @Inject(method = "isInGracePeriod", at = @At("HEAD"), cancellable = true)
    private void skipGracePeriod(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void skipFadeOut(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (this.reload != null && this.reload.isComplete() && this.reloadCompleteTime > -1L) {
            MinecraftClient.getInstance().setOverlay(null);
        }
    }
}
