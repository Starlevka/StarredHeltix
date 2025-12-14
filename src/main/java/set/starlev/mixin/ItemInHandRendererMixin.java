package set.starlev.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import set.starlev.features.visual.SwingAnimation;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @ModifyVariable(method = "applyItemArmAttackTransform", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float modifyAttackSwingProgress(float swingProgress) {
        if (SwingAnimation.INSTANCE.isSwingSpeedEnabled()) {
            return swingProgress * SwingAnimation.INSTANCE.getSwingSpeed();
        }
        return swingProgress;
    }

    @Redirect(method = "applyItemArmTransform", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 0))
    private void onSwingArmTranslate(PoseStack instance, float x, float y, float z) {
        if (SwingAnimation.INSTANCE.isEnabled()) {
            instance.translate(
                x * SwingAnimation.INSTANCE.getSwingX(),
                y * SwingAnimation.INSTANCE.getSwingY(),
                z * SwingAnimation.INSTANCE.getSwingZ()
            );
        } else {
            instance.translate(x, y, z);
        }
    }
}
