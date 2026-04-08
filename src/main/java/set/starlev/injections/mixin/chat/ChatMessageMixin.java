package set.starlev.injections.mixin.chat;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.chat.ChatEventsManager;
import set.starlev.features.chat.mod.MacroCheck;

@Mixin(ClientPacketListener.class)
public class ChatMessageMixin {
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
        }
    }
}
