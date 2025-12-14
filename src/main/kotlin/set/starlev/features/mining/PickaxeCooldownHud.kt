package set.starlev.features.mining

import net.minecraft.client.Minecraft
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement

object PickaxeCooldownHud : HudElement("PickaxeCooldownHud") {
    private val mc = Minecraft.getInstance()

    private var lastPickaxeBoostTime = 0L

    private val cooldownMs: Long
        get() = (StarredHeltix.feature.mining.abilityCooldown.pickaxeBoostCooldown.toIntOrNull() ?: 60) * 1000L

    override fun render() {
        val config = StarredHeltix.feature.mining.abilityCooldown
        if (!config.pickaxeBoostEnabled) return

        val currentTime = System.currentTimeMillis()
        val timeSinceLastUse = currentTime - lastPickaxeBoostTime
        val remainingTime = ((cooldownMs - timeSinceLastUse) / 1000.0).coerceAtLeast(0.0)

        val text = if (isEditing) "§lК: 60.0" else "§lК: ${String.format("%.1f", remainingTime)}"
        if (isEditing || remainingTime > 0) {
            cachedGraphics?.drawString(mc.font, text, x, y, 0xFFFF0000.toInt())
        }
    }

    override fun getWidth(): Int = mc.font.width("К: 60.0")
    override fun getHeight(): Int = mc.font.lineHeight

    override fun getDefaultX(): Int = Minecraft.getInstance().window.guiScaledWidth / 2 + 60
    override fun getDefaultY(): Int = Minecraft.getInstance().window.guiScaledHeight / 2 + 20

    fun onPickaxeBoostUsed() {
        lastPickaxeBoostTime = System.currentTimeMillis()
    }
}