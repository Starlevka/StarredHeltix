package set.starlev.features.skyblock.scoreboard.elements

import net.minecraft.network.chat.Component
import set.starlev.features.skyblock.HudScoreboard.ScoreboardLine
import set.starlev.features.skyblock.scoreboard.ScoreboardElement
import set.starlev.secret.features.SecretFunFeatures
import set.starlev.utils.detectors.TabListDetector

object ScoreboardElementBank : ScoreboardElement() {
    override fun getDisplay(): List<ScoreboardLine> {
        val bankLine = TabListDetector.getBankLine() ?: return emptyList()
        val processed = SecretFunFeatures.processComponent(Component.literal(bankLine), true)
        return listOf(ScoreboardLine("bank", processed, centered = false))
    }
    
    override val configLine = "§6Банк"
}