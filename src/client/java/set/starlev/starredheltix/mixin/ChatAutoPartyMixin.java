package set.starlev.starredheltix.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.starredheltix.client.StarredHeltixClient;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatAutoPartyMixin {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (StarredHeltixClient.CONFIG.general.enabled && 
            StarredHeltixClient.CONFIG.partyCommands.autoPartyChat && 
            message.startsWith("!")) {
            ci.cancel();
            ((ClientPlayNetworkHandler)(Object)this).sendChatCommand("pc " + message);
        }
    }
}