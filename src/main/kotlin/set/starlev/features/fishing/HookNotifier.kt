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

    private fun isVisibleNow(now: Long = System.currentTimeMillis()): Boolean {
        if (!StarredHeltix.feature.fishing.notifications.fishingNotifier) return false
        if (!notificationActive) return false
        if (now >= notificationEndTime) return false
        return true
    }

    fun onBite() {
        if (!StarredHeltix.feature.fishing.notifications.fishingNotifier) return
        
        notificationActive = true
        notificationEndTime = System.currentTimeMillis() + 2000
        mc.player?.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.5f, 1.5f)
    }

    override fun render() {
        val now = System.currentTimeMillis()
        if (!isVisibleNow(now) && !isEditing) {
            if (notificationActive && now >= notificationEndTime) notificationActive = false
            return
        }

        val text = "§c§lТЯНИ!"
        cachedGraphics?.let { graphics ->
            this.showBackground = StarredHeltix.feature.fishing.notifications.showBackground
            drawBackground(getWidth(), getHeight())
            graphics.drawString(mc.font, net.minecraft.network.chat.Component.literal(text), x, y, 0xFFFFFFFF.toInt(), true)
        }
    }

    override fun getWidth() = if (isVisibleNow() || isEditing) mc.font.width("ТЯНИ!") else 0
    override fun getHeight() = if (isVisibleNow() || isEditing) mc.font.lineHeight else 0
    
    override fun getDefaultScale(): Float = 2.1f
    
    override fun getDefaultX(): Int = 457
    
    override fun getDefaultY(): Int = 210
}
