package set.starlev.injections.mixins;

import net.minecraft.client.multiplayer.ClientPacketListener;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.chat.ChatEventsManager;
import set.starlev.features.chat.mod.MacroCheck;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import set.starlev.features.skyblock.NpcDialogueOverlay;
import set.starlev.secret.features.Povtorayshkins;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.phys.Vec3;
import set.starlev.features.skyblock.SmoothAote;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void onSendChatInject(String message, CallbackInfo ci) {
        if (MacroCheck.INSTANCE.checkAnswer(message)) {
            ci.cancel();
            return;
        }
        
        // Полностью убираем перехват и модификацию сообщения при отправке.
        // Пусть оно уходит как есть, а мы будем раскрашивать его ПРИ ПОЛУЧЕНИИ.
        if (ChatEventsManager.INSTANCE.onOutgoingMessage(message)) {
            ci.cancel();
            return;
        }

        if (set.starlev.scripting.ScriptChatHandler.INSTANCE.onOutgoing(message)) {
            ci.cancel();
        }
    }


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

        if (set.starlev.scripting.ScriptChatHandler.INSTANCE.onIncoming(messageText)) {
            ci.cancel();
            return;
        }

        ChatEventsManager.INSTANCE.onIncomingMessage(messageText);
    }

    @Inject(method = "handlePlayerChat", at = @At("HEAD"), cancellable = true)
    private void onReceivePlayerChat(ClientboundPlayerChatPacket packet, CallbackInfo ci) {
        Component unsigned = packet.unsignedContent();
        // 26.2: сервер может слать подписанный player-chat без unsignedContent
        // (пати/ЛС-каналы Heltix) — собираем видимый текст из подписанного тела
        // и декорации ChatType; иначе сообщение молча терялось для всех фич.
        Component message = unsigned;
        String messageText;
        if (unsigned != null) {
            messageText = unsigned.getString();
        } else {
            try {
                message = packet.chatType().decorate(Component.literal(packet.body().content()));
                messageText = message.getString();
            } catch (Exception e) {
                return;
            }
        }

        if (NpcDialogueOverlay.INSTANCE.onChat(message)) {
            ci.cancel();
            return;
        }

        if (!set.starlev.features.chat.MessageFilterManager.INSTANCE.shouldAllowMessage(messageText)) {
            ci.cancel();
            return;
        }

        if (set.starlev.scripting.ScriptChatHandler.INSTANCE.onIncoming(messageText)) {
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

        if (set.starlev.scripting.ScriptChatHandler.INSTANCE.onIncoming(messageText)) {
            ci.cancel();
            return;
        }

        ChatEventsManager.INSTANCE.onIncomingMessage(messageText);
    }


    @Inject(method = "sendChat", at = @At("TAIL"))
    private void onSendChatTail(String message, CallbackInfo ci) {
        Povtorayshkins.INSTANCE.onPlayerSendMessage(message);
    }


    private Vec3 starredheltix$preMoveCameraPos;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void onHandleMovePlayerHead(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        this.starredheltix$preMoveCameraPos = Minecraft.getInstance().gameRenderer.mainCamera().position();
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void onHandleMovePlayer(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().player != null) {
            Vec3 pre = this.starredheltix$preMoveCameraPos;
            Vec3 post = Minecraft.getInstance().player.position();
            if (pre != null) {
                SmoothAote.INSTANCE.onTeleport(pre, post);
            } else {
                SmoothAote.INSTANCE.onTeleport(post);
            }
        }
    }

    @Inject(method = "handleParticleEvent", at = @At("HEAD"))
    private void onParticle(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        try {
            set.starlev.features.fishing.GlowingMushroomHighlight.INSTANCE.onParticle(packet);
        } catch (Exception e) {}
    }

    // Порт ClientPlayNetworkHandlerMixin из heltix-mod: isSecureChatEnforced -> true
    // В 26.2 переехал в ClientPacketListener.enforcesSecureChat() (private), скрывает тост UNSECURE_SERVER_WARNING
    @ModifyReturnValue(method = "enforcesSecureChat", at = @At("RETURN"))
    private boolean starredheltix$forceSecureChat(boolean original) {
        return true;
    }

}