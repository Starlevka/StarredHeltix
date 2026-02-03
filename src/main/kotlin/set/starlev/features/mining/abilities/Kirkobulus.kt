package set.starlev.features.mining

import net.minecraft.client.Minecraft
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement

object PickaxeCooldownHud : HudElement("PickaxeCooldownHud") {
    private val mc = Minecraft.getInstance()

    private var lastPickaxeBoostTime = 0L

    fun init() {
        set.starlev.features.chat.ChatEventsManager.registerIncoming { message ->
            if (Regex(".*Вы использовали Киркобулус!.*").matches(message)) {
                onPickaxeBoostUsed()
            }
        }
    }

    private val cooldownMs: Long
        get() = (StarredHeltix.feature.mining.abilities.abilityCooldown.pickaxeBoostCooldown.toIntOrNull() ?: 60) * 1000L

    override fun render() {
        val config = StarredHeltix.feature.mining.abilities.abilityCooldown
        if (!config.pickaxeBoostEnabled) return

        val currentTime = System.currentTimeMillis()
        val timeSinceLastUse = currentTime - lastPickaxeBoostTime
        val remainingTime = ((cooldownMs - timeSinceLastUse) / 1000.0).coerceAtLeast(0.0)

        val text = if (isEditing) "§c§lК: 60.0" else "§c§lК: ${String.format("%.1f", remainingTime)}"
        if (isEditing || remainingTime > 0) {
            val width = mc.font.width(text)
            val height = mc.font.lineHeight
            this.showBackground = config.showBackground
            
            // Используем centerAnchor = true для центрирования
            drawBackground(width, height, 4, true)
            
            // Отрисовываем текст центрировано
            cachedGraphics?.drawString(mc.font, text, x - width / 2, y, 0xFFFFFFFF.toInt(), true)
        }
    }

    override fun getWidth(): Int = mc.font.width("К: 60.0")
    override fun getHeight(): Int = mc.font.lineHeight

    override fun getDefaultScale(): Float = 2.6000004f
    override fun getDefaultX(): Int = 306
    override fun getDefaultY(): Int = 292

    fun onPickaxeBoostUsed() {
        lastPickaxeBoostTime = System.currentTimeMillis()
    }
}