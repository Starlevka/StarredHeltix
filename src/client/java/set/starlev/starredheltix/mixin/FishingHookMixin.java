package set.starlev.starredheltix.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.starredheltix.util.qol.FishingNotifier;

@Mixin(ClientPlayNetworkHandler.class)
public class FishingHookMixin {

    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.world == null || client.player == null) return;
        
        // Check if this is a fishing bobber
        if (packet.getEntity(client.world) instanceof FishingBobberEntity bobber) {
            if (bobber.getOwner() == client.player) {

                
        
            }
        }
    }

    @Inject(method = "onPlaySound", at = @At("HEAD"))
    private void onPlaySound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        String soundName = packet.getSound().value().id().toString();
        if (soundName.equals("minecraft:entity.fishing_bobber.splash")) {
            FishingNotifier.onFishingBite();
        }
    }
}