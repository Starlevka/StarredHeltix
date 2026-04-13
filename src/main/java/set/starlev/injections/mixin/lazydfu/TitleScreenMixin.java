package set.starlev.injections.mixin.lazydfu;

import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Убирает анимацию fade-in на главном экране меню.
 * Кнопки появляются мгновенно после загрузки.
 *
 * Исходный код: https://github.com/GUN2RAS/FastBoot
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Shadow private boolean fading;

    @Inject(method = "init", at = @At("RETURN"))
    private void disableFadeImmediately(CallbackInfo ci) {
        if (ConfigManager.INSTANCE.getFeaturesSafe().getOptimization().getFastBoot().enabled && ConfigManager.INSTANCE.getFeaturesSafe().getOptimization().getFastBoot().skipFadeAnimations) {
            this.fading = false;
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void ensureFadeDisabled(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (ConfigManager.INSTANCE.getFeaturesSafe().getOptimization().getFastBoot().enabled && ConfigManager.INSTANCE.getFeaturesSafe().getOptimization().getFastBoot().skipFadeAnimations) {
            this.fading = false;
        }
    }
}
