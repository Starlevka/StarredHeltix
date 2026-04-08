package set.starlev.injections.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import set.starlev.StarredHeltix;
import set.starlev.features.visual.Fullbright;

@Mixin(GameRenderer.class)
public class GameRendererFullbrightMixin {
    @ModifyReturnValue(method = "getNightVisionScale", at = @At("RETURN"))
    private static float starredheltix$fullbrightNightVisionScale(float original) {
        if (Fullbright.INSTANCE.isEnabled()) {
            // Если включена опция "Убрать ночное зрение", полностью отключаем эффект
            if (StarredHeltix.Companion.getFeature().getOptimization().getVisualOptimizations().getFullbrightRemoveNightVision()) {
                return 0.0F;
            }
            return 1.0F;
        }
        return original;
    }
}
