package set.starlev.features.skyblock.scoreboard.elements

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.features.skyblock.HudScoreboard.ScoreboardLine
import set.starlev.features.skyblock.scoreboard.ScoreboardElement
import set.starlev.secret.features.SecretFunFeatures
import set.starlev.utils.ColorUtils

object ScoreboardElementPing : ScoreboardElement() {
    override fun getDisplay(): List<ScoreboardLine> {
        val mc = Minecraft.getInstance()
        val ping = mc.player?.let { mc.connection?.getPlayerInfo(it.uuid)?.latency } ?: 0
        val color = ColorUtils.parseColor(StarredHeltix.feature.skyblock.scoreboard.infoElements.ping.valueColorV2, 0xFF55FF55.toInt())
        val processed = SecretFunFeatures.processComponent(Component.literal("§7Ping: ").append(Component.literal("${ping}ms").withColor(color)), true)
        return listOf(ScoreboardLine("ping", processed, centered = false))
    }
    override fun showWhen(): Boolean = StarredHeltix.feature.skyblock.scoreboard.infoElements.ping.enabled

    override val configLine = "§aPing"
}