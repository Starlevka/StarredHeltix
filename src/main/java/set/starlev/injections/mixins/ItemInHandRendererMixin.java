package set.starlev.injections.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.features.visual.HideOffhand;
import set.starlev.features.visual.SwingAnimation;
import set.starlev.features.visual.swing.SwingReplacementResult;
import set.starlev.features.visual.swing.SwingTransforms;
import set.starlev.features.visual.swing.SwingVisuals;

/**
 * Кастомные анимации удара и трансформации предмета в руке.
 * + Скрытие второй руки от первого лица (порт HeldItemRendererMixin из heltix-mod).
 */
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

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void starredheltix$transformHeldItem(
        LivingEntity entity,
        ItemStack itemStack,
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int light,
        CallbackInfo ci
    ) {
        if (displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            SwingTransforms.INSTANCE.apply(itemStack, poseStack);
            SwingVisuals.INSTANCE.apply(itemStack, poseStack);
        }
    }

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void starredheltix$hideOffhand(
        AbstractClientPlayer player,
        float frameInterp,
        float xRot,
        InteractionHand hand,
        float attack,
        ItemStack itemStack,
        float inverseArmHeight,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int light,
        CallbackInfo ci
    ) {
        if (hand == InteractionHand.OFF_HAND && HideOffhand.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "submitArmWithItem", at = @At("HEAD"))
    private void starredheltix$beginSwing(
        AbstractClientPlayer player,
        float frameInterp,
        float xRot,
        InteractionHand hand,
        float attack,
        ItemStack itemStack,
        float inverseArmHeight,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int light,
        CallbackInfo ci
    ) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND
            ? player.getMainArm()
            : player.getMainArm().getOpposite();
        SwingVisuals.INSTANCE.begin(itemStack, attack, arm);
    }

    @Inject(method = "submitArmWithItem", at = @At("TAIL"))
    private void starredheltix$endSwing(
        AbstractClientPlayer player,
        float frameInterp,
        float xRot,
        InteractionHand hand,
        float attack,
        ItemStack itemStack,
        float inverseArmHeight,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int light,
        CallbackInfo ci
    ) {
        SwingVisuals.INSTANCE.end();
    }

    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void starredheltix$replaceSwing(
        float attack,
        PoseStack poseStack,
        int invert,
        HumanoidArm arm,
        CallbackInfo ci
    ) {
        if (SwingVisuals.INSTANCE.replaceVanillaSwing() == SwingReplacementResult.REPLACED) {
            ci.cancel();
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
