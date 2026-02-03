package set.starlev.features.misc.info

import net.minecraft.client.Minecraft
import set.starlev.hud.HudElement
import set.starlev.StarredHeltix
import set.starlev.utils.ColorUtils

// FPS HUD
object FpsHud : HudElement("FpsHud") {
    private val mc = Minecraft.getInstance()
    
    override fun render() {
        val config = StarredHeltix.feature.misc.general.hudStats.fps
        if (!config.hud) return
        val fps = mc.fps
        val text = "ФПС: $fps"

        showBackground = config.showBackground
        drawBackground(getWidth(), getHeight())
        cachedGraphics?.drawString(mc.font, text, x, y, ColorUtils.parseColor(config.colorV2, 0xFFFFFFFF.toInt()))
    }
    
    override fun getWidth(): Int = mc.font.width("FPS: 999")
    override fun getHeight(): Int = mc.font.lineHeight
    override fun getDefaultX() = 10
    override fun getDefaultY() = 10
}

// Ping HUD
object PingHud : HudElement("PingHud") {
    private val mc = Minecraft.getInstance()
    private var lastPingUpdateMs = 0L
    private var lastPingValue: Int? = null
    
    override fun render() {
        val config = StarredHeltix.feature.misc.general.hudStats.ping
        if (!config.hud) return
        val player = mc.player ?: return
        val now = System.currentTimeMillis()
        if (now - lastPingUpdateMs >= 500) {
            val entry = mc.connection?.getPlayerInfo(player.uuid)
            val ping = entry?.latency
            if (ping != null && ping > 0) {
                lastPingValue = ping
            }
            lastPingUpdateMs = now
        }
        val pingText = lastPingValue?.let { "${it}ms" } ?: "--"
        val text = "Пинг: $pingText"
        
        showBackground = config.showBackground
        drawBackground(getWidth(), getHeight())
        cachedGraphics?.drawString(mc.font, text, x, y, ColorUtils.parseColor(config.colorV2, 0xFFFFFFFF.toInt()))
    }
    
    override fun getWidth(): Int = mc.font.width("Ping: 999ms")
    override fun getHeight(): Int = mc.font.lineHeight
    override fun getDefaultX() = 10
    override fun getDefaultY() = 25
}

// CPS HUD
object CpsHud : HudElement("CpsHud") {
    private val mc = Minecraft.getInstance()
    
    override fun render() {
        val config = StarredHeltix.feature.misc.general.hudStats.cps
        if (!config.hud) return
        val cps = StatsTracker.getCps()
        val text = "КПС: $cps"
        
        showBackground = config.showBackground
        drawBackground(getWidth(), getHeight())
        cachedGraphics?.drawString(mc.font, text, x, y, ColorUtils.parseColor(config.colorV2, 0xFFFFFFFF.toInt()))
    }
    
    override fun getWidth(): Int = mc.font.width("CPS: 20")
    override fun getHeight(): Int = mc.font.lineHeight
    override fun getDefaultX() = 10
    override fun getDefaultY() = 40
}

// BPS HUD
object BpsHud : HudElement("BpsHud") {
    private val mc = Minecraft.getInstance()
    
    override fun render() {
        val config = StarredHeltix.feature.misc.general.hudStats.bps
        if (!config.hud) return
        val bps = StatsTracker.getBps()
        val text = "БВС: $bps"
        
        showBackground = config.showBackground
        drawBackground(getWidth(), getHeight())
        cachedGraphics?.drawString(mc.font, text, x, y, ColorUtils.parseColor(config.colorV2, 0xFFFFFFFF.toInt()))
    }
    
    override fun getWidth(): Int = mc.font.width("BPS: 20")
    override fun getHeight(): Int = mc.font.lineHeight
    override fun getDefaultX() = 10
    override fun getDefaultY() = 55
}
