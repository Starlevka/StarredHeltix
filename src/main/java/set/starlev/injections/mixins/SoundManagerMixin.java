package set.starlev.injections.mixins;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.scripting.SHScriptEngine;

/**
 * Перехват воспроизведения звуков для JS-модулей.
 * @author Starlev
 */
@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("HEAD"))
    private void onSoundPlay(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (!SHScriptEngine.INSTANCE.isLoaded() || sound == null) return;
        // ВАЖНО: только getIdentifier() — getVolume()/getPitch() требуют resolve(instance),
        // который происходит внутри SoundEngine.play, и до него кидают NPE (26.2).
        SHScriptEngine.INSTANCE.fireTrigger("soundPlay", sound.getIdentifier().toString());
    }
}
