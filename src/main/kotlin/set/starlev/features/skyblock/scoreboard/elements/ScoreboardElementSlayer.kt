package set.starlev.features.skyblock.scoreboard.elements

import net.minecraft.network.chat.Component
import set.starlev.features.skyblock.HudScoreboard.ScoreboardLine
import set.starlev.features.skyblock.scoreboard.ScoreboardElement
import set.starlev.features.combat.slayer.SlayerScoreboard
import set.starlev.secret.features.SecretFunFeatures

object ScoreboardElementSlayer : ScoreboardElement() {
    override fun getDisplay(): List<ScoreboardLine> {
        val lines = SlayerScoreboard.getExtraLines()
        return lines.mapIndexed { i, component ->
            val processed = SecretFunFeatures.processComponent(component, true)
            ScoreboardLine("slayer:$i", processed, centered = false)
        }
    }
    
    override val configLine = "§cСлеер"
}