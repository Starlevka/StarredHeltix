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
        if (!StarredHeltix.feature.fishing.notifications.legendaryFishingNotifier) return
        
        val now = System.currentTimeMillis()
        
        if (legendaryActive || isEditing) {
            if (now < legendaryEndTime || isEditing) {
                val text = "§3§lВодяная Гидра!"
                cachedGraphics?.let { graphics ->
                    // Рисуем текст по центру элемента.
                    // HudElement.renderWithGraphics уже применил масштабирование и перемещение к (x, y).
                    // Поэтому мы рисуем в (x, y).
                    graphics.drawString(mc.font, net.minecraft.network.chat.Component.literal(text), x, y, 0xFFFFFFFF.toInt(), true)
                }
            } else {
                legendaryActive = false
            }
        }
    }

    override fun getWidth() = mc.font.width("ЛЕГЕНДАРНОЕ СУЩЕСТВО!")
    override fun getHeight() = mc.font.lineHeight
    
    override fun getDefaultScale(): Float = 1.5f
    
    override fun getDefaultX(): Int {
        val window = mc.window
        return (window.guiScaledWidth / 2) - (getScaledWidth() / 2)
    }
    
    override fun getDefaultY(): Int {
        val window = mc.window
        return (window.guiScaledHeight / 2) + 20
    }
}
