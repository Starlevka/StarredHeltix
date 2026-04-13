package set.starlev.features.skyblock.scoreboard.elements

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.features.skyblock.HudScoreboard.ScoreboardLine
import set.starlev.features.skyblock.scoreboard.ScoreboardElement
import set.starlev.secret.features.SecretFunFeatures
import set.starlev.utils.ColorUtils

object ScoreboardElementFPS : ScoreboardElement() {
    override fun getDisplay(): List<ScoreboardLine> {
        val mc = Minecraft.getInstance()
        val color = ColorUtils.parseColor(StarredHeltix.feature.skyblock.scoreboard.infoElements.fps.valueColorV2, 0xFF55FF55.toInt())
        val processed = SecretFunFeatures.processComponent(Component.literal("§7FPS: ").append(Component.literal("${mc.fps}").withColor(color)), true)
        return listOf(ScoreboardLine("fps", processed, centered = false))
    }
    override fun showWhen(): Boolean = StarredHeltix.feature.skyblock.scoreboard.infoElements.fps.enabled

    override val configLine = "§aFPS"
}