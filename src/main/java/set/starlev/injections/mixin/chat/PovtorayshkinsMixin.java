package set.starlev.injections.mixin.chat;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.secret.features.Povtorayshkins;

@Mixin(ClientPacketListener.class)
public class PovtorayshkinsMixin {
    @Inject(method = "sendChat", at = @At("TAIL"))
    private void onSendChatTail(String message, CallbackInfo ci) {
        Povtorayshkins.INSTANCE.onPlayerSendMessage(message);
    }
}