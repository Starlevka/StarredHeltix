package set.starlev.injections.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.visual.HideOffhand;
import set.starlev.secret.features.SecretFunFeatures;

/**
 * @author Starlev
 * @reason Секретная функция «Большие головы» — поднимает ник над увеличенной головой
 *         + порт PlayerEntityRendererMixin из heltix-mod: скрытие левой руки (renderLeftHand) при включении «Скрытие второй руки»
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true)
    private void starredheltix$hideLeftHand(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, Identifier skinTexture, boolean sleeveVisible, CallbackInfo ci) {
        if (HideOffhand.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void starredheltix$liftNameTag(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        if (state.nameTagAttachment == null) return;
        float scale = 1.0f;
        if (SecretFunFeatures.INSTANCE.isBigHeadsEnabled()) {
            scale = SecretFunFeatures.INSTANCE.getBigHeadScale();
        }
        if (scale == 1.0f) return;

        Vec3 old = state.nameTagAttachment;
        // Верх головы игрока ~1.5 блока: 0.3 (шея) + 1.5 * scale, + 0.5 запас под ник
        state.nameTagAttachment = new Vec3(old.x, 0.3 + 1.5 * scale + 0.5, old.z);
    }
}
