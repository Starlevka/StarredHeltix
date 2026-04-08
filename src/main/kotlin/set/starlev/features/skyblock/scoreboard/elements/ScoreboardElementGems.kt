package set.starlev.features.skyblock.scoreboard.elements

import net.minecraft.network.chat.Component
import set.starlev.features.skyblock.HudScoreboard.ScoreboardLine
import set.starlev.features.skyblock.scoreboard.ScoreboardElement
import set.starlev.secret.features.SecretFunFeatures
import set.starlev.utils.detectors.TabListDetector

object ScoreboardElementGems : ScoreboardElement() {
    override fun getDisplay(): List<ScoreboardLine> {
        val gemsLine = TabListDetector.getGemsLine() ?: return emptyList()
        val processed = SecretFunFeatures.processComponent(Component.literal(gemsLine), true)
        return listOf(ScoreboardLine("gems", processed, centered = false))
    }
    
    override val configLine = "§dГемы"
}