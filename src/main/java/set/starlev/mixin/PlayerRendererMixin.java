package set.starlev.mixin;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import set.starlev.secret.config.SecretMenuManager;
import set.starlev.secret.features.SecretFunFeatures;

@Mixin(AvatarRenderer.class)
public abstract class PlayerRendererMixin {
    @org.spongepowered.asm.mixin.injection.ModifyArg(
        method = "submitNameTag", 
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V"), 
        index = 3
    )
    private Component onModifyNameTagArg(Component component) {
        if (!SecretMenuManager.INSTANCE.isConfigInitialized()) {
            return component;
        }
        if (component == null) return null;
        // В именах над игроками эффекты применяются всегда (force=true), если включены в конфиге
        return SecretFunFeatures.processComponent(component, true);
    }
}
