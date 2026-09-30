package set.starlev.injections.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.visual.Fullbright;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {
    @Unique
    private final float starredheltix$brightness = 16.0f;

    @ModifyExpressionValue(method = "extract", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 0), require = 0)
    private float starredheltix$getGamma(float original) {
        if (Fullbright.INSTANCE.isEnabled()) {
            return starredheltix$brightness;
        }
        return original;
    }

    @ModifyExpressionValue(method = "extract", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 1), require = 0)
    private float starredheltix$getDarkness(float original) {
        if (Fullbright.INSTANCE.isEnabled()) {
            return 0.0f;
        }
        return original;
    }

    @Inject(method = "extract", at = @At("RETURN"))
    private void starredheltix$forceFullbright(LightmapRenderState state, float partialTick, CallbackInfo ci) {
        if (Fullbright.INSTANCE.isEnabled()) {
            state.brightness = 1.0f;
            state.darknessEffectScale = 0.0f;
            state.skyFactor = 1.5f;
            state.blockFactor = 1.8f;
            state.bossOverlayWorldDarkening = 0.0f;
            state.nightVisionEffectIntensity = 0.0f;
            state.blockLightTint = new org.joml.Vector3f(1.0f, 1.0f, 1.0f);
            state.skyLightColor = new org.joml.Vector3f(1.0f, 1.0f, 1.0f);
            state.ambientColor = new org.joml.Vector3f(1.0f, 1.0f, 1.0f);
            state.nightVisionColor = new org.joml.Vector3f(1.0f, 1.0f, 1.0f);
            state.needsUpdate = true;
        }
    }
}
