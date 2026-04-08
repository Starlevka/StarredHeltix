package set.starlev.injections.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import set.starlev.features.visual.Fullbright;

@Mixin(LightTexture.class)
public abstract class LightmapTextureManagerMixin {
    @Unique
    private final float brightness = 10000.0f;

    @ModifyExpressionValue(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 1))
    private float getGamma(float original) {
        if (Fullbright.INSTANCE.isEnabled()) {
            return brightness;
        }
        return original;
    }
}
