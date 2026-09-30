package set.starlev.injections.mixins;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.StarredHeltix;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("HEAD"), cancellable = true)
    private void onPlay(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (sound == null) return;
        String id = sound.getIdentifier().toString();

        // JS-модули: events.onSoundPlayWithCriteria(...) с критерием и отменой.
        // getVolume()/getPitch() до resolve() кидает NPE (26.2) — передаём дефолты 1.0.
        if (set.starlev.scripting.SHScriptEngine.INSTANCE.isLoaded()
                && set.starlev.scripting.ScriptSoundHandler.INSTANCE.onSound(id, 1.0f, 1.0f, null)) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
            return;
        }

        if (!StarredHeltix.getFeature().getMusic().getDisableExplosionSounds()
                && !StarredHeltix.getFeature().getMusic().getDisableFireworkSounds()
                && !StarredHeltix.getFeature().getMusic().getDisableBatSounds()) return;

        String path = sound.getIdentifier().getPath();
        String idStr = id.toLowerCase();
        String pathLower = path.toLowerCase();
        set.starlev.config.categories.SoundConfig cfg = StarredHeltix.getFeature().getMusic();
        boolean isBat = idStr.contains("entity.bat") || pathLower.contains("entity.bat") || pathLower.contains("entity/bat") || idStr.contains("entity/bat");
        boolean block =
                (cfg.getDisableExplosionSounds() && (pathLower.contains("explode") || pathLower.contains("tnt") || pathLower.contains("explosion") || idStr.contains("explode")))
                        || (cfg.getDisableFireworkSounds() && (pathLower.contains("firework") || idStr.contains("firework")))
                        || (cfg.getDisableBatSounds() && isBat);
        if (block) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}