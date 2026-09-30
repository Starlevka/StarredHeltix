package set.starlev.injections.mixins;

import net.minecraft.client.renderer.entity.BatRenderer;
import net.minecraft.client.renderer.entity.state.BatRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.StarredHeltix;
import set.starlev.utils.detectors.DungeonDetector;

@Mixin(BatRenderer.class)
public class BatRendererMixin {
    private static final Identifier GREEN_BAT_LOCATION = Identifier.fromNamespaceAndPath("starredheltix", "textures/entity/green_bat.png");

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void onGetTextureLocation(BatRenderState state, CallbackInfoReturnable<Identifier> cir) {
        // Оптимизация: кэшируем флаг на 1с, чтобы не дёргать конфиг/детектор для каждой мыши каждый кадр.
        // DungeonDetector внутри уже кэширован на 1с.
        long now = System.currentTimeMillis();
        if (now - starredheltix$lastCheckMs > 1000L) {
            starredheltix$lastCheckMs = now;
            try {
                starredheltix$cachedEnabled = StarredHeltix.Companion.getFeature().getDungeons().getVisuals().getGreenTextures().getGreenBats()
                    && DungeonDetector.INSTANCE.isInDungeon();
            } catch (Throwable ignored) {
                starredheltix$cachedEnabled = false;
            }
        }
        if (starredheltix$cachedEnabled) {
            cir.setReturnValue(GREEN_BAT_LOCATION);
        }
    }

    private static volatile boolean starredheltix$cachedEnabled = false;
    private static volatile long starredheltix$lastCheckMs = 0L;
}
