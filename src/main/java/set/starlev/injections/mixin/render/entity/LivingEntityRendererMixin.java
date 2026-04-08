package set.starlev.injections.mixin.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.secret.features.SecretFunFeatures;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "setupRotations", at = @At("HEAD"))
    private void onSetupRotations(LivingEntityRenderState state, PoseStack poseStack, float f, float g, CallbackInfo ci) {
        if (!SecretFunFeatures.INSTANCE.isFlipEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // AvatarRenderState имеет публичное поле id — рефлексия не нужна
        if (state instanceof AvatarRenderState avatarState && avatarState.id == mc.player.getId()) {
            poseStack.translate(0.0D, state.boundingBoxHeight + 0.1F, 0.0D);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
    }
}
