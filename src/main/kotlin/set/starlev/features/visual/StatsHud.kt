package set.starlev.features.visual

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.config.categories.VisualsConfig
import set.starlev.render.HudElement
import set.starlev.render.drawString
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.StatsDetectors

/**
 * Отдельные HUD для CPS/FPS/Ping/BPS (категория Визуалы).
 * Альтернатива строкам кастомного скорборда: каждый показатель — своим
 * элементом с позицией/масштабом из редактора HUD.
 */
abstract class StatHud(id: String) : HudElement(id) {
    protected val mc = Minecraft.getInstance()

    protected abstract fun cfg(): VisualsConfig.StatHudConfig
    protected abstract fun label(): String
    protected abstract fun value(): String

    private fun plainText(): String = "${label()}: ${value()}"

    override fun render() {
        if (!cfg().enabled && !isEditing) return
        cachedGraphics?.let { graphics ->
            this.showBackground = cfg().showBackground
            drawBackground(getWidth(), getHeight())
            val color = ColorUtils.parseColor(cfg().valueColorV2, 0xFF55FF55.toInt())
            val text = Component.literal("§7${label()}: ")
                .append(Component.literal(value()).withColor(color))
            graphics.drawString(mc.font, text, x, y, 0xFFFFFFFF.toInt(), true)
        }
    }

    override fun getWidth(): Int = mc.font.width(plainText())

    override fun getHeight(): Int = mc.font.lineHeight
}

object CpsHud : StatHud("CpsHud") {
    override fun cfg() = StarredHeltix.feature.visuals.statsHud.cps
    override fun label() = "CPS"
    override fun value() = "${StatsDetectors.getCps()}"
    override fun getDefaultX() = 2
    override fun getDefaultY() = 40
}

object FpsHud : StatHud("FpsHud") {
    override fun cfg() = StarredHeltix.feature.visuals.statsHud.fps
    override fun label() = "FPS"
    override fun value() = "${mc.fps}"
    override fun getDefaultX() = 2
    override fun getDefaultY() = 52
}

object PingHud : StatHud("PingHud") {
    override fun cfg() = StarredHeltix.feature.visuals.statsHud.ping
    override fun label() = "Ping"
    override fun value() = "${mc.player?.let { mc.connection?.getPlayerInfo(it.uuid)?.latency } ?: 0}ms"
    override fun getDefaultX() = 2
    override fun getDefaultY() = 64
}

object BpsHud : StatHud("BpsHud") {
    override fun cfg() = StarredHeltix.feature.visuals.statsHud.bps
    override fun label() = "BPS"
    override fun value() = "${StatsDetectors.getBps()}"
    override fun getDefaultX() = 2
    override fun getDefaultY() = 76
}
