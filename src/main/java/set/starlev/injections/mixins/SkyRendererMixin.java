package set.starlev.injections.mixins;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public class SkyRendererMixin {

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onExtractRenderState(ClientLevel level, float partialTick, Camera camera, SkyRenderState state, CallbackInfo ci) {
        // Здесь при необходимости можно обрабатывать кастомную погоду и другие визуальные эффекты
    }
}
