package set.starlev.injections.mixins;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import set.starlev.features.chat.ChatFormatting;
import set.starlev.secret.config.SecretMenuManager;
import set.starlev.secret.features.SecretFunFeatures;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", at = @At("HEAD"), argsOnly = true)
    private Component onAddMessageFull(Component component) {
        return applyStarlev(component);
    }

    private Component applyStarlev(Component component) {
        if (component == null) return null;

        Component processed = component;
        if (SecretMenuManager.INSTANCE.isConfigInitialized()) {
            // В чате эффекты применяются всегда (force=true), если включены в конфиге
            processed = SecretFunFeatures.processComponent(processed, true);
        }

        Component finalComp = ChatFormatting.processComponent(processed);

        return finalComp;
    }
}
