package set.starlev.injections.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.PlayerHeadSpecialRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.visual.HeadGlint;

/**
 * Добавляет зачарованный блеск (glint) для голов игрока.
 * Контролируется настройкой visuals.headGlint.
 */
@Mixin(value = PlayerHeadSpecialRenderer.class, remap = true)
public class PlayerHeadSpecialRendererMixin {

    @Shadow
    @Final
    private SkullModelBase modelBase;

    @Inject(method = "submit", at = @At("TAIL"))
    private void starredheltix$headGlint(
            PlayerSkinRenderCache.RenderInfo argument,
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
        state.animationPos = 0.0F;
        submitNodeCollector.order(1).submitModel(
                this.modelBase, state, poseStack, RenderTypes.entityGlint(),
                lightCoords, OverlayTexture.NO_OVERLAY, outlineColor, (ModelFeatureRenderer.CrumblingOverlay) null
        );
    }
}
