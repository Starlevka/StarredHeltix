package set.starlev.features.skyblock.scoreboard.elements

import net.minecraft.network.chat.Component
import set.starlev.features.skyblock.HudScoreboard.ScoreboardLine
import set.starlev.features.skyblock.scoreboard.ScoreboardElement
import set.starlev.features.misc.info.StatsTracker
import set.starlev.secret.features.SecretFunFeatures

object ScoreboardElementCPS : ScoreboardElement() {
    override fun getDisplay(): List<ScoreboardLine> {
        val processed = SecretFunFeatures.processComponent(Component.literal("§7CPS: §a${StatsTracker.getCps()}"), true)
        return listOf(ScoreboardLine("cps", processed, centered = false))
    }
    
    override val configLine = "§aCPS"
}