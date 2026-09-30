package set.starlev.injections.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SkullSpecialRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.visual.HeadGlint;

/**
 * Добавляет зачарованный блеск (glint) для черепов.
 * Контролируется настройкой visuals.headGlint.
 */
@Mixin(value = SkullSpecialRenderer.class, remap = true)
public class SkullSpecialRendererMixin {

    @Shadow
    @Final
    private SkullModelBase model;

    @Shadow
    @Final
    private float animation;

    @Inject(method = "submit", at = @At("TAIL"))
    private void starredheltix$skullGlint(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor,
            CallbackInfo ci
    ) {
        if (!hasFoil) return;
        if (!HeadGlint.INSTANCE.isEnabled()) return;
        SkullModelBase.State state = new SkullModelBase.State();
        state.animationPos = this.animation;
        submitNodeCollector.order(1).submitModel(
                this.model, state, poseStack, RenderTypes.entityGlint(),
                lightCoords, OverlayTexture.NO_OVERLAY, outlineColor, (ModelFeatureRenderer.CrumblingOverlay) null
        );
    }
}
