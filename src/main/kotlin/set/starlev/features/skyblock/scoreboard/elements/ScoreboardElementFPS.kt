package set.starlev.features.skyblock.scoreboard.elements

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.features.skyblock.HudScoreboard.ScoreboardLine
import set.starlev.features.skyblock.scoreboard.ScoreboardElement
import set.starlev.secret.features.SecretFunFeatures

object ScoreboardElementFPS : ScoreboardElement() {
    override fun getDisplay(): List<ScoreboardLine> {
        val mc = Minecraft.getInstance()
        val processed = SecretFunFeatures.processComponent(Component.literal("§7FPS: §a${mc.fps}"), true)
        return listOf(ScoreboardLine("fps", processed, centered = false))
    }
    
    override val configLine = "§aFPS"
}