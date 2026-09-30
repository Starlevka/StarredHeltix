package set.starlev.injections.mixins;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.secret.features.SecretFunFeatures;

/**
 * @author Starlev
 * @reason Секретная функция «Большие головы» — масштаб головы игроков (только визуально, клиент)
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void starredheltix$scaleHead(AvatarRenderState state, CallbackInfo ci) {
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        float scale = 1.0f;
        if (SecretFunFeatures.INSTANCE.isBigHeadsEnabled()) {
            scale = SecretFunFeatures.INSTANCE.getBigHeadScale();
        }
        model.head.xScale = scale;
        model.head.yScale = scale;
        model.head.zScale = scale;
    }
}
