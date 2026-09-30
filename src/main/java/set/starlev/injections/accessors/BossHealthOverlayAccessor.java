package set.starlev.injections.accessors;

import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

/**
 * @author Starlev
 * @reason Ð”Ð¾ÑÑ‚ÑƒÐ¿ Ðº Ð¿Ñ€Ð¸Ð²Ð°Ñ‚Ð½Ð¾Ð¹ ÐºÐ°Ñ€Ñ‚Ðµ ÑÐ¾Ð±Ñ‹Ñ‚Ð¸Ð¹ Ð±Ð¾ÑÑÐ±Ð°Ñ€Ð¾Ð² Ð´Ð»Ñ BossBars API
 *         JS-Ð¼Ð¾Ð´ÑƒÐ»ÐµÐ¹ (ctjs-Ð¿Ð°Ñ€Ð¸Ñ‚ÐµÑ‚).
 */
@Mixin(BossHealthOverlay.class)
public interface BossHealthOverlayAccessor {
    @Accessor("events")
    Map<UUID, LerpingBossEvent> shGetBossEvents();
}

