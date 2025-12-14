package set.starlev.features.misc

import net.minecraft.client.Minecraft
import set.starlev.StarredHeltix

object AutoSprint {
    private val config get() = StarredHeltix.feature.misc.autoSprint

    fun tick() {
        if (!config.enabled) return

        val player = Minecraft.getInstance().player ?: return
        if (player.input.hasForwardImpulse()) {
            player.isSprinting = true
        }
    }
}
