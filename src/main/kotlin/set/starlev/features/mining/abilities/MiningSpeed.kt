package set.starlev.features.mining

import net.minecraft.client.Minecraft
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement

object SpeedBoostCooldownHud : HudElement("SpeedBoostCooldownHud") {
    private val mc = Minecraft.getInstance()

    private var lastSpeedBoostTime = 0L

    fun init() {
        set.starlev.features.chat.ChatEventsManager.registerIncoming { message ->
            if (Regex(".*Вы использовали Увеличение скорости копания!.*").matches(message)) {
                onSpeedBoostUsed()
            }
        }
    }

    private val cooldownDuration: Long
        get() = (StarredHeltix.feature.mining.abilities.abilityCooldown.speedBoostCooldown.toIntOrNull() ?: 120) * 1000L

    override fun render() {
        val config = StarredHeltix.feature.mining.abilities.abilityCooldown
        if (!config.speedBoostEnabled) return

        val currentTime = System.currentTimeMillis()
        val timeSinceLastUse = currentTime - lastSpeedBoostTime
        val remainingTime = ((cooldownDuration - timeSinceLastUse) / 1000.0).coerceAtLeast(0.0)

        if (remainingTime <= 0 && !isEditing) return

        val text = if (isEditing) "§a§lС: 60.0" else "§a§lС: ${String.format("%.1f", remainingTime)}"
        val width = mc.font.width(text)
        val height = mc.font.lineHeight
        this.showBackground = config.showBackground

        drawBackground(width, height, 4, true)
        cachedGraphics?.drawString(mc.font, text, x - width / 2, y, 0xFFFFFFFF.toInt(), true)
    }

    override fun getWidth(): Int {
        val config = StarredHeltix.feature.mining.abilities.abilityCooldown
        if (!config.speedBoostEnabled) return 0
        val remainingTime = ((cooldownDuration - (System.currentTimeMillis() - lastSpeedBoostTime)) / 1000.0).coerceAtLeast(0.0)
        if (remainingTime <= 0 && !isEditing) return 0
        return mc.font.width("С: 120.0")
    }

    override fun getHeight(): Int {
        val config = StarredHeltix.feature.mining.abilities.abilityCooldown
        if (!config.speedBoostEnabled) return 0
        val remainingTime = ((cooldownDuration - (System.currentTimeMillis() - lastSpeedBoostTime)) / 1000.0).coerceAtLeast(0.0)
        if (remainingTime <= 0 && !isEditing) return 0
        return mc.font.lineHeight
    }

    override fun getDefaultScale(): Float = 2.6000004f
    override fun getDefaultX(): Int = 296
    override fun getDefaultY(): Int = 248

    fun onSpeedBoostUsed() {
        lastSpeedBoostTime = System.currentTimeMillis()
    }
}
