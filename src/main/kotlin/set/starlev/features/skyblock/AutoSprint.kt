package set.starlev.features.skyblock

import net.minecraft.client.Minecraft
import set.starlev.StarredHeltix

object AutoSprint {
    private val config get() = StarredHeltix.feature.skyblock.autoSprint

    fun tick() {
        if (!StarredHeltix.feature.skyblock.autoSprint) return

        val player = Minecraft.getInstance().player ?: return
        if (player.input.hasForwardImpulse()) {
            player.isSprinting = true
        }
    }
}
