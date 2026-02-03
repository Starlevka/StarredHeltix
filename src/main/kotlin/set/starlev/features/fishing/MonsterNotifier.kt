package set.starlev.features.fishing

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.sounds.SoundEvents
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement
import set.starlev.features.chat.ChatEventsManager

object LegendaryFishingNotifier : HudElement("LegendaryFishingNotifier") {
    private val mc = Minecraft.getInstance()
    private var legendaryActive = false
    private var legendaryEndTime = 0L

    private fun isVisibleNow(now: Long = System.currentTimeMillis()): Boolean {
        if (!StarredHeltix.feature.fishing.notifications.legendaryFishingNotifier) return false
        if (!legendaryActive) return false
        if (now >= legendaryEndTime) return false
        return true
    }

    fun init() {
        ChatEventsManager.registerIncoming { message ->
            if (message.contains("Водяная Гидра пришла испытать Вашу силу.")) {
                onLegendaryBite()
            }
        }
    }

    private fun onLegendaryBite() {
        if (!StarredHeltix.feature.fishing.notifications.legendaryFishingNotifier) return
        
        legendaryActive = true
        legendaryEndTime = System.currentTimeMillis() + 5000
        mc.player?.playSound(SoundEvents.WITHER_SPAWN, 1.0f, 1.0f)
    }

    override fun render() {
        val now = System.currentTimeMillis()
        if (!isVisibleNow(now) && !isEditing) {
            if (legendaryActive && now >= legendaryEndTime) legendaryActive = false
            return
        }

        val text = "§3§lВодяная Гидра!"
        cachedGraphics?.let { graphics ->
            this.showBackground = StarredHeltix.feature.fishing.notifications.showBackground
            drawBackground(getWidth(), getHeight())
            graphics.drawString(mc.font, net.minecraft.network.chat.Component.literal(text), x, y, 0xFFFFFFFF.toInt(), true)
        }
    }

    override fun getWidth() = if (isVisibleNow() || isEditing) mc.font.width("ЛЕГЕНДАРНОЕ СУЩЕСТВО!") else 0
    override fun getHeight() = if (isVisibleNow() || isEditing) mc.font.lineHeight else 0
    
    override fun getDefaultScale(): Float = 1.7f
    
    override fun getDefaultX(): Int = 389
    
    override fun getDefaultY(): Int = 330
}
