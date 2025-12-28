package set.starlev.features.misc

import net.minecraft.client.Minecraft
import set.starlev.StarredHeltix

object AutoSprint {
    private val config get() = StarredHeltix.feature.misc.general.autoSprint

    fun tick() {
        if (!StarredHeltix.feature.misc.general.autoSprint) return

        val player = Minecraft.getInstance().player ?: return
        if (player.input.hasForwardImpulse()) {
            player.isSprinting = true
        }
    }
}
