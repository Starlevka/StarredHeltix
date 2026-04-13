package com.modfast.mixin;

import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.DrawContext;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Shadow private boolean doBackgroundFade;

    @Inject(method = "init", at = @At("RETURN"))
    private void disableFadeImmediately(CallbackInfo ci) {
        this.doBackgroundFade = false;
    }
    
    @Inject(method = "render", at = @At("HEAD"))
    private void ensureFadeDisabled(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        this.doBackgroundFade = false;
    }
}
