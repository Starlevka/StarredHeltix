package set.starlev.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.fishing.FishingNotifier;

@Mixin(ClientPacketListener.class)
public class FishingHookMixin {
    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void onPlaySound(ClientboundSoundPacket packet, CallbackInfo ci) {
        // Check for fishing splash sound
        String soundName = packet.getSound().value().location().toString();
        if (soundName.equals("minecraft:entity.fishing_bobber.splash") ||
            soundName.equals("entity.fishing_bobber.splash")) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && client.player.fishing != null) {
                FishingNotifier.INSTANCE.onBite();
            }
        }
    }
}
