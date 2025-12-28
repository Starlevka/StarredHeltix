package set.starlev.features.visual

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.world.effect.MobEffects
import set.starlev.StarredHeltix
import set.starlev.config.categories.OptimizationConfig

object Fullbright {
    fun isEnabled(): Boolean {
        return StarredHeltix.feature.optimization.visualOptimizations.fullbright
    }

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player ?: return@register

            if (isEnabled()) {
                // Если Fullbright включен, убираем эффект ночного зрения, так как он мешает/дублирует
                if (player.hasEffect(MobEffects.NIGHT_VISION)) {
                    player.removeEffect(MobEffects.NIGHT_VISION)
                }
            }
        }
    }
}
