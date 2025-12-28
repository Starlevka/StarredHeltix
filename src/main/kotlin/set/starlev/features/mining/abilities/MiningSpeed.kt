package set.starlev.features.mining

import net.minecraft.client.Minecraft
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement

object SpeedBoostCooldownHud : HudElement("SpeedBoostCooldownHud") {
    private val mc = Minecraft.getInstance()

    private var lastSpeedBoostTime = 0L

    private val cooldownDuration: Long
        get() = (StarredHeltix.feature.mining.abilities.abilityCooldown.speedBoostCooldown.toIntOrNull() ?: 120) * 1000L

    override fun render() {
        val config = StarredHeltix.feature.mining.abilities.abilityCooldown
        if (!config.speedBoostEnabled) return

        val currentTime = System.currentTimeMillis()
        val timeSinceLastUse = currentTime - lastSpeedBoostTime
        val remainingTime = ((cooldownDuration - timeSinceLastUse) / 1000.0).coerceAtLeast(0.0)

        val text = if (isEditing) "§a§lС: 120.0" else "§a§lС: ${String.format("%.1f", remainingTime)}"
        if (isEditing || remainingTime > 0) {
            cachedGraphics?.drawString(mc.font, text, x, y, 0xFFFFFFFF.toInt())
        }
    }

    override fun getWidth(): Int = mc.font.width("С: 120.0")
    override fun getHeight(): Int = mc.font.lineHeight

    override fun getDefaultX(): Int = Minecraft.getInstance().window.guiScaledWidth / 2 - 120
    override fun getDefaultY(): Int = Minecraft.getInstance().window.guiScaledHeight / 2 + 20

    fun onSpeedBoostUsed() {
        lastSpeedBoostTime = System.currentTimeMillis()
    }
}