package set.starlev.features.skyblock.scoreboard.elements

import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.features.skyblock.HudScoreboard.ScoreboardLine
import set.starlev.features.skyblock.scoreboard.ScoreboardElement
import set.starlev.features.misc.info.StatsTracker
import set.starlev.secret.features.SecretFunFeatures
import set.starlev.utils.ColorUtils

object ScoreboardElementCPS : ScoreboardElement() {
    override fun getDisplay(): List<ScoreboardLine> {
        val color = ColorUtils.parseColor(StarredHeltix.feature.skyblock.scoreboard.infoElements.cps.valueColorV2, 0xFF55FF55.toInt())
        val processed = SecretFunFeatures.processComponent(Component.literal("§7CPS: ").append(Component.literal("${StatsTracker.getCps()}").withColor(color)), true)
        return listOf(ScoreboardLine("cps", processed, centered = false))
    }
    override fun showWhen(): Boolean = StarredHeltix.feature.skyblock.scoreboard.infoElements.cps.enabled

    override val configLine = "§aCPS"
}