package set.starlev.injections.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import set.starlev.StarredHeltix;
import set.starlev.features.visual.Fullbright;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @ModifyReturnValue(method = "nightVisionScale", at = @At("RETURN"))
    private static float starredheltix$fullbrightNightVisionScale(float original) {
        return original;
    }
}
