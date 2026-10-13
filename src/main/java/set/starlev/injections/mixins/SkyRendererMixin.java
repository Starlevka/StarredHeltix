package set.starlev.injections.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.visual.SkyColor;

@Mixin(SkyRenderer.class)
public class SkyRendererMixin {

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onExtractRenderState(ClientLevel level, float partialTick, Camera camera, SkyRenderState state, CallbackInfo ci) {
        // Здесь при необходимости можно обрабатывать кастомную погоду и другие визуальные эффекты
    }

    /** Цвет купола неба (аргумент renderSkyDisc). */
    @ModifyVariable(method = "renderSkyDisc", at = @At("HEAD"), argsOnly = true)
    private int starlevUseCustomSkyColor(int original) {
        return SkyColor.INSTANCE.skyColor(original);
    }

    /** Цвет пустоты — Vector4f(0,0,0,1), который создаётся внутри renderDarkDisc. */
    @ModifyExpressionValue(method = "renderDarkDisc", at = @At(value = "NEW", target = "(FFFF)Lorg/joml/Vector4f;"))
    private Vector4f starlevUseCustomVoidColor(Vector4f original) {
        return SkyColor.INSTANCE.voidColor(original);
    }
}
