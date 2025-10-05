package set.starlev.starredheltix.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.starredheltix.client.StarredHeltixClient;
import set.starlev.starredheltix.util.player.PlayerPingUtil;

@Mixin(ClientPlayNetworkHandler.class)
public class PlayerListMixin {
    @Inject(method = "onPlayerList", at = @At("HEAD"))
    private void onPlayerList(PlayerListS2CPacket packet, CallbackInfo ci) {
        if (StarredHeltixClient.CONFIG != null && StarredHeltixClient.CONFIG.partyCommands.partyPingEnabled) {
            // Update our ping map with the latest information when player list changes
            for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
                if (entry.profile() != null && entry.profile().getName() != null) {
                    PlayerPingUtil.updatePlayerPing(entry.profile().getName(), entry.latency());
                }
            }
        }
    }
}