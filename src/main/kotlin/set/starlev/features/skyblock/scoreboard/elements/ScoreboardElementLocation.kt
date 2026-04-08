package set.starlev.features.skyblock.scoreboard.elements

import net.minecraft.network.chat.Component
import set.starlev.features.skyblock.HudScoreboard.ScoreboardLine
import set.starlev.features.skyblock.scoreboard.ScoreboardElement
import set.starlev.secret.features.SecretFunFeatures
import set.starlev.utils.detectors.ScoreboardDetector

object ScoreboardElementLocation : ScoreboardElement() {
    override fun getDisplay(): List<ScoreboardLine> {
        val lines = ScoreboardDetector.getScoreboardText()
        for (line in lines) {
            if (line.contains("Location") || line.contains("Местоположение")) {
                val processed = SecretFunFeatures.processComponent(Component.literal(line), true)
                return listOf(ScoreboardLine("location", processed, centered = false))
            }
        }
        return emptyList()
    }
    
    override val configLine = "§aЛокация"
}