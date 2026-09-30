package set.starlev.injections.accessors;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Ð”Ð¾ÑÑ‚ÑƒÐ¿ Ðº protected-Ð¿Ð¾Ð»ÑÐ¼ AbstractSoundInstance Ð´Ð»Ñ ÐºÐ°ÑÑ‚Ð¾Ð¼Ð½Ñ‹Ñ… Ð¸Ð½ÑÑ‚Ð°Ð½ÑÐ¾Ð²
 * Ð·Ð²ÑƒÐºÐ° JS-Ð¼Ð¾Ð´ÑƒÐ»ÐµÐ¹ (Ð³Ñ€Ð¾Ð¼ÐºÐ¾ÑÑ‚ÑŒ/Ð¿Ð¸Ñ‚Ñ‡/Ð¿Ð¾Ð·Ð¸Ñ†Ð¸Ñ/Ð·Ð°Ñ†Ð¸ÐºÐ»Ð¸Ð²Ð°Ð½Ð¸Ðµ).
 * Ð˜Ð¼ÐµÐ½Ð° shGet/shSet â€” Ñ‡Ñ‚Ð¾Ð±Ñ‹ Ð½Ðµ Ð¿ÐµÑ€ÐµÑÐµÐºÐ°Ñ‚ÑŒÑÑ Ñ Ð¿ÑƒÐ±Ð»Ð¸Ñ‡Ð½Ñ‹Ð¼Ð¸ Ð³ÐµÑ‚Ñ‚ÐµÑ€Ð°Ð¼Ð¸ ÐºÐ»Ð°ÑÑÐ°.
 */
@Mixin(AbstractSoundInstance.class)
public interface AbstractSoundInstanceAccessor {
    @Accessor("sound")
    void shSetSound(Sound sound);

    @Accessor("volume")
    void shSetVolume(float volume);

    @Accessor("pitch")
    void shSetPitch(float pitch);

    @Accessor("x")
    void shSetX(double x);

    @Accessor("y")
    void shSetY(double y);

    @Accessor("z")
    void shSetZ(double z);

    @Accessor("looping")
    void shSetLooping(boolean looping);

    @Accessor("delay")
    void shSetDelay(int delay);

    @Accessor("attenuation")
    void shSetAttenuation(SoundInstance.Attenuation attenuation);

    @Accessor("relative")
    void shSetRelative(boolean relative);

    @Accessor("source")
    SoundSource shGetSource();
}

