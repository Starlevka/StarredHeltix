package set.starlev.features.skyblock.scoreboard.elements

import net.minecraft.network.chat.Component
import set.starlev.features.skyblock.HudScoreboard.ScoreboardLine
import set.starlev.features.skyblock.scoreboard.ScoreboardElement
import set.starlev.features.misc.info.StatsTracker
import set.starlev.secret.features.SecretFunFeatures

object ScoreboardElementBPS : ScoreboardElement() {
    override fun getDisplay(): List<ScoreboardLine> {
        val processed = SecretFunFeatures.processComponent(Component.literal("§7BPS: §a${StatsTracker.getBps()}"), true)
        return listOf(ScoreboardLine("bps", processed, centered = false))
    }
    
    override val configLine = "§aBPS"
}