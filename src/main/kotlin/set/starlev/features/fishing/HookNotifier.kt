package set.starlev.features.fishing

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.sounds.SoundEvents
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement

object FishingNotifier : HudElement("FishingNotifier") {
    private val mc = Minecraft.getInstance()
    private var notificationActive = false
    private var notificationEndTime = 0L

    fun onBite() {
        if (!StarredHeltix.feature.fishing.notifications.fishingNotifier) return
        
        notificationActive = true
        notificationEndTime = System.currentTimeMillis() + 2000
        mc.player?.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.5f, 1.5f)
    }

    override fun render() {
        if (!StarredHeltix.feature.fishing.notifications.fishingNotifier) return
        
        val now = System.currentTimeMillis()
        
        if (notificationActive || isEditing) {
            if (now < notificationEndTime || isEditing) {
                val text = "§c§lТЯНИ!"
                cachedGraphics?.let { graphics ->
                    // Рисуем текст по центру элемента. 
                    // HudElement.renderWithGraphics уже применил масштабирование и перемещение к (x, y).
                    // Поэтому мы рисуем в (x, y).
                    graphics.drawString(mc.font, net.minecraft.network.chat.Component.literal(text), x, y, 0xFFFFFFFF.toInt(), true)
                }
            } else {
                notificationActive = false
            }
        }
    }

    override fun getWidth() = mc.font.width("ТЯНИ!")
    override fun getHeight() = mc.font.lineHeight
    
    override fun getDefaultScale(): Float = 2.0f
    
    override fun getDefaultX(): Int {
        val window = mc.window
        return (window.guiScaledWidth / 2) - (getScaledWidth() / 2)
    }
    
    override fun getDefaultY(): Int {
        val window = mc.window
        return (window.guiScaledHeight / 2) - 60
    }
}
