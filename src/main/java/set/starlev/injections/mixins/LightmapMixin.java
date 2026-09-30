package set.starlev.injections.mixins;

import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.visual.Fullbright;

@Mixin(Lightmap.class)
public class LightmapMixin {
    private static final Vector3fc WHITE = new Vector3f(1.0f, 1.0f, 1.0f);

    @Inject(method = "render", at = @At("HEAD"))
    private void starredheltix$forceFullbrightLightmap(LightmapRenderState renderState, CallbackInfo ci) {
        if (!Fullbright.INSTANCE.isEnabled()) return;
        // Серверная защита от "простого" фулбрайта (только gamma) — форсим все параметры лайтмапа напрямую в точке рендера
        // даже если LightmapRenderStateExtractor был переопределён сервером/шейдером
        renderState.brightness = 1.0f;
        renderState.darknessEffectScale = 0.0f;
        renderState.skyFactor = 1.5f;
        renderState.blockFactor = 1.8f;
        renderState.bossOverlayWorldDarkening = 0.0f;
        renderState.nightVisionEffectIntensity = 0.0f;
        renderState.blockLightTint = WHITE;
        renderState.skyLightColor = WHITE;
        renderState.ambientColor = WHITE;
        renderState.nightVisionColor = WHITE;
        renderState.needsUpdate = true;
    }
}
