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
        String soundName = packet.getSound().value().location().toString();
        if (soundName.contains("fishing_bobber.splash") || soundName.contains("fishing_bobber.throw")) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && client.player.fishing != null) {
                FishingHook hook = client.player.fishing;
                
                // Проверяем дистанцию от звука до поплавка владельца
                double dx = hook.getX() - packet.getX();
                double dy = hook.getY() - packet.getY();
                double dz = hook.getZ() - packet.getZ();
                double distSq = dx * dx + dy * dy + dz * dz;

                // Если звук всплеска произошел непосредственно в месте нахождения поплавка игрока (радиус ~0.5 блока)
                if (distSq < 0.25) { 
                    FishingNotifier.INSTANCE.onBite();
                }
            }
        }
    }
}
