package set.starlev.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import set.starlev.features.chat.ChatFormatting;
import set.starlev.secret.config.SecretMenuManager;
import set.starlev.secret.features.SecretFunFeatures;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), argsOnly = true)
    private Component onAddMessageSimple(Component component) {
        return applyStarlev(component);
    }

    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("HEAD"), argsOnly = true)
    private Component onAddMessageFull(Component component) {
        return applyStarlev(component);
    }

    private Component applyStarlev(Component component) {
        if (component == null) return null;
        
        Component processed = component;
        if (SecretMenuManager.INSTANCE.isConfigInitialized() && SecretMenuManager.INSTANCE.getSecretConfig().getFunCategory().getStarlevNameEffect()) {
            // В чате эффекты применяются всегда (force=true), если включены в конфиге
            processed = SecretFunFeatures.processComponent(processed, true);
        }
        
        Component finalComp = ChatFormatting.processComponent(processed);
        
        // Отладочный вывод в консоль для проверки работы миксина
        if (finalComp != processed) {
            System.out.println("[StarredHeltix] Chat message formatted: " + finalComp.getString());
        }
        
        return finalComp;
    }
}
