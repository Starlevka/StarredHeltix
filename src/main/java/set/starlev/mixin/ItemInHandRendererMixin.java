package set.starlev.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.features.visual.SwingAnimation;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Shadow
    private float mainHandHeight;

    @Shadow
    private float offHandHeight;

    @Shadow
    private float oMainHandHeight;

    @Shadow
    private float oOffHandHeight;

    @Redirect(method = "applyItemArmTransform", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 0))
    private void onSwingArmTranslate(PoseStack instance, float x, float y, float z, PoseStack poseStack, HumanoidArm arm, float equipProgress) {
        float effectiveY = y;
        if (SwingAnimation.INSTANCE.isNoEquipEnabled()) {
            effectiveY = -0.52f;
        }

        if (SwingAnimation.INSTANCE.isEnabled()) {
            float side = (arm == HumanoidArm.RIGHT ? 1.0f : -1.0f);
            
            instance.translate(
                x + side * (float) SwingAnimation.INSTANCE.getOffX(),
                effectiveY + (float) SwingAnimation.INSTANCE.getOffY(),
                z + (float) SwingAnimation.INSTANCE.getOffZ()
            );
        } else {
            instance.translate(x, effectiveY, z);
        }
    }

    @Redirect(method = "swingArm", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 0))
    private void onSwingArmTranslateRedirect(PoseStack instance, float x, float y, float z) {
        if (SwingAnimation.INSTANCE.isEnabled()) {
            instance.translate(
                    x * (float) SwingAnimation.INSTANCE.getSwingX(),
                    y * (float) SwingAnimation.INSTANCE.getSwingY(),
                    z * (float) SwingAnimation.INSTANCE.getSwingZ()
            );
        } else {
            instance.translate(x, y, z);
        }
    }

    @Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("HEAD"), cancellable = true)
    private void onShouldInstantlyReplaceVisibleItem(ItemStack from, ItemStack to, CallbackInfoReturnable<Boolean> cir) {
        if (SwingAnimation.INSTANCE.isNoEquipEnabled()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (SwingAnimation.INSTANCE.isNoEquipEnabled()) {
            this.mainHandHeight = 1.0f;
            this.offHandHeight = 1.0f;
            this.oMainHandHeight = 1.0f;
            this.oOffHandHeight = 1.0f;
        }
    }
}
