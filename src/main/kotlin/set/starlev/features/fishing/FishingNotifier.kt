package set.starlev.features.fishing

import net.minecraft.client.Minecraft
import net.minecraft.sounds.SoundEvents
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement

object FishingNotifier : HudElement("FishingNotifier") {
    private val mc = Minecraft.getInstance()
    private var notificationActive = false
    private var notificationEndTime = 0L

    fun onBite() {
        if (!StarredHeltix.feature.fishing.fishingNotifier.enabled) return
        
        notificationActive = true
        notificationEndTime = System.currentTimeMillis() + 2000
        mc.player?.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.5f, 1.5f)
    }

    override fun render() {
        if (!StarredHeltix.feature.fishing.fishingNotifier.enabled) return
        if (!notificationActive && !isEditing) return
        if (System.currentTimeMillis() >= notificationEndTime && !isEditing) {
            notificationActive = false
            return
        }

        val text = "§lТЯНИ!"
        cachedGraphics?.drawString(mc.font, text, x, y, 0xFFFF0000.toInt())
    }

    override fun getWidth() = mc.font.width("ТЯНИ!") * 2
    override fun getHeight() = mc.font.lineHeight * 2
    
    override fun getDefaultX(): Int {
        val window = mc.window
        return (window.guiScaledWidth / 2) - (getWidth() / 2)
    }
    
    override fun getDefaultY(): Int {
        val window = mc.window
        return (window.guiScaledHeight / 2) - 60
    }
}
