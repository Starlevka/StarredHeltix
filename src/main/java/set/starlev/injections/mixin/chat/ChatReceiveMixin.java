package set.starlev.injections.mixin.chat;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.chat.ChatEventsManager;
import set.starlev.features.overlays.NpcDialogueOverlay;

@Mixin(ClientPacketListener.class)
public class ChatReceiveMixin {
    @Inject(method = "handleSystemChat", at = @At("HEAD"), cancellable = true)
    private void onReceiveChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        Component message = packet.content();
        String messageText = message.getString();
        
        if (NpcDialogueOverlay.INSTANCE.onChat(message)) {
            ci.cancel();
            return;
        }
        
        if (!set.starlev.features.chat.MessageFilterManager.INSTANCE.shouldAllowMessage(messageText)) {
            ci.cancel();
            return;
        }
        
        ChatEventsManager.INSTANCE.onIncomingMessage(messageText);
    }

    @Inject(method = "handlePlayerChat", at = @At("HEAD"), cancellable = true)
    private void onReceivePlayerChat(ClientboundPlayerChatPacket packet, CallbackInfo ci) {
        Component message = packet.unsignedContent();
        if (message == null) {
            return;
        }
        String messageText = message.getString();

        if (NpcDialogueOverlay.INSTANCE.onChat(message)) {
            ci.cancel();
            return;
        }

        if (!set.starlev.features.chat.MessageFilterManager.INSTANCE.shouldAllowMessage(messageText)) {
            ci.cancel();
            return;
        }

        ChatEventsManager.INSTANCE.onIncomingMessage(messageText);
    }

    @Inject(method = "handleDisguisedChat", at = @At("HEAD"), cancellable = true)
    private void onReceiveDisguisedChat(ClientboundDisguisedChatPacket packet, CallbackInfo ci) {
        Component message = packet.message();
        String messageText = message.getString();

        if (NpcDialogueOverlay.INSTANCE.onChat(message)) {
            ci.cancel();
            return;
        }

        if (!set.starlev.features.chat.MessageFilterManager.INSTANCE.shouldAllowMessage(messageText)) {
            ci.cancel();
            return;
        }

        ChatEventsManager.INSTANCE.onIncomingMessage(messageText);
    }
}
