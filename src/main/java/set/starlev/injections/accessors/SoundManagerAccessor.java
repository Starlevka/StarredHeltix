package set.starlev.injections.accessors;

import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Ð”Ð¾ÑÑ‚ÑƒÐ¿ Ðº Ð¿Ñ€Ð¸Ð²Ð°Ñ‚Ð½Ñ‹Ð¼ Ñ€ÐµÐµÑÑ‚Ñ€Ð°Ð¼ Ð·Ð²ÑƒÐºÐ¾Ð² Ð´Ð»Ñ ÐºÐ°ÑÑ‚Ð¾Ð¼Ð½Ñ‹Ñ… ogg-Ñ„Ð°Ð¹Ð»Ð¾Ð² JS-Ð¼Ð¾Ð´ÑƒÐ»ÐµÐ¹
 * (sound.playFile). ÐÐ½Ð°Ð»Ð¾Ð³ Ð¼ÐµÑ…Ð°Ð½Ð¸Ð·Ð¼Ð° ctjs Sound.bootstrap().
 * Ð’Ð¾Ð·Ð²Ñ€Ð°Ñ‰Ñ‘Ð½Ð½Ñ‹Ðµ Map Ð¼ÑƒÑ‚Ð°Ð±ÐµÐ»ÑŒÐ½Ñ‹ â€” Ð½Ð°Ð¿Ð¾Ð»Ð½ÐµÐ½Ð¸Ðµ/Ð¾Ñ‡Ð¸ÑÑ‚ÐºÐ° Ð¸Ð· SHModuleSounds.
 */
@Mixin(SoundManager.class)
public interface SoundManagerAccessor {
    @Accessor("registry")
    Map<Identifier, WeighedSoundEvents> shGetRegistry();

    @Accessor("soundCache")
    Map<Identifier, Resource> shGetSoundCache();
}

