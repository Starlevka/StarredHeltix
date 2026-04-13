package set.starlev.injections.mixin.lazydfu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Убирает задержку и анимацию fade-out на загрузочном экране.
 * Загрузочный экран исчезает мгновенно после завершения загрузки ресурсов.
 *
 * Исходный код: https://github.com/GUN2RAS/FastBoot
 */
@Mixin(LoadingOverlay.class)
public class LoadingOverlayMixin {

    @Shadow private ReloadInstance reload;
    @Shadow private long fadeOutStart;

    @Inject(method = "isReadyToFadeOut", at = @At("HEAD"), cancellable = true)
    private void skipGracePeriod(CallbackInfoReturnable<Boolean> cir) {
        if (ConfigManager.INSTANCE.getFeaturesSafe().getOptimization().getFastBoot().enabled && ConfigManager.INSTANCE.getFeaturesSafe().getOptimization().getFastBoot().skipFadeAnimations) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void skipFadeOut(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (ConfigManager.INSTANCE.getFeaturesSafe().getOptimization().getFastBoot().enabled && ConfigManager.INSTANCE.getFeaturesSafe().getOptimization().getFastBoot().skipFadeAnimations) {
            if (this.reload != null && fadeOutStart > -1L) {
                Minecraft.getInstance().setOverlay(null);
            }
        }
    }
}
