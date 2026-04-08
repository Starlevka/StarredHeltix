package set.starlev.injections.mixin.sounds;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.StarredHeltix;

@Mixin(value = SoundManager.class, priority = 1000)
public class SoundEngineMixin {

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("HEAD"), cancellable = true)
    private void onPlay(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (!StarredHeltix.getFeature().getMusic().getDisableExplosionSounds()) return;

        String path = sound.getLocation().getPath();
        if (path.contains("explode") || path.contains("tnt") || path.contains("explosion")) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}