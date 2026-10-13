package set.starlev.injections.mixins;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.visual.SkyColor;

/**
 * Цвет горизонта/тумана — порт из Skysoft (FogRendererMixin).
 * Сигнатура computeFogColor сверена с `scripts/SOURCES/26.2.X SOURCE CODE`.
 */
@Mixin(FogRenderer.class)
public class FogRendererMixin {

    @Inject(method = "computeFogColor", at = @At("TAIL"))
    private void starlevUseCustomHorizonColor(
            Camera camera,
            float partialTicks,
            ClientLevel level,
            int renderDistance,
            float darkenWorldAmount,
            Vector4f destination,
            CallbackInfo ci
    ) {
        SkyColor.INSTANCE.applyHorizonColor(camera, destination);
    }
}
