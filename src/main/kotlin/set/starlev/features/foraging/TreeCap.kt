package set.starlev.features.foraging

import net.minecraft.client.Minecraft
import net.minecraft.world.item.Items
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement
import set.starlev.skyblock.ItemRegistry

object TreeCapCooldown : HudElement("TreeCapCooldown") {
    private val mc = Minecraft.getInstance()
    private var lastBreakTime = 0L
    private var isOnCooldown = false

    private fun getRemainingSeconds(): Double {
        val config = StarredHeltix.feature.foraging.axes.treeCapCooldown
        if (!config.enabled) return 0.0

        val currentTime = System.currentTimeMillis()
        val timeSinceLastBreak = currentTime - lastBreakTime
        val cooldownMs = (config.cooldown.toDoubleOrNull() ?: 2.0) * 1000
        return ((cooldownMs - timeSinceLastBreak) / 1000.0).coerceAtLeast(0.0)
    }

    private fun calculateSize(text: String): Pair<Int, Int> {
        val width = mc.font.width(text) + 8 // padding * 2
        val height = mc.font.lineHeight + 8 // padding * 2
        return width to height
    }

    override fun render() {
        val config = StarredHeltix.feature.foraging.axes.treeCapCooldown
        if (!config.enabled) return

        val remainingTime = getRemainingSeconds()
        if (remainingTime <= 0 && !isEditing) return

        val text = if (isEditing) "§c§l2.0" else "§c§l${String.format("%.1f", remainingTime)}"
        val (width, height) = calculateSize(text)
        val padding = 4
        this.showBackground = config.showBackground
        // Используем centerAnchor = true для центрирования фона относительно позиции X
        drawBackground(width, height, 0, true)

        // Отрисовываем текст так, чтобы x был центром
        cachedGraphics?.drawString(mc.font, text, x - mc.font.width(text) / 2, y + padding, 0xFFFFFFFF.toInt(), true)
    }

    fun onLogBreak(blockName: String) {
        val config = StarredHeltix.feature.foraging.axes.treeCapCooldown
        if (!config.enabled) return
        
        val currentTime = System.currentTimeMillis()
        val cooldownMs = (config.cooldown.toDoubleOrNull() ?: 2.0) * 1000
        
        // Обновляем время последнего слома только если кулдаун уже прошел
        if (currentTime - lastBreakTime >= cooldownMs) {
            val item = mc.player?.mainHandItem?.hoverName?.string ?: ""
            if (item.contains("Treecapitator") || item.contains("Jungle Axe") || item.contains("Древоточец") || item.contains("Джунглевый топор")) {
                lastBreakTime = currentTime
            }
        }
    }

    fun isVisible(): Boolean {
        val config = StarredHeltix.feature.foraging.axes.treeCapCooldown
        return config.enabled
    }

    override fun getWidth(): Int {
        val remainingTime = getRemainingSeconds()
        if (remainingTime <= 0 && !isEditing) return 0
        val text = if (isEditing) "§c§l2.0" else "§c§l${String.format("%.1f", remainingTime)}"
        return calculateSize(text).first
    }

    override fun getHeight(): Int {
        val remainingTime = getRemainingSeconds()
        if (remainingTime <= 0 && !isEditing) return 0
        val text = if (isEditing) "§c§l2.0" else "§c§l${String.format("%.1f", remainingTime)}"
        return calculateSize(text).second
    }
    
    override fun getDefaultScale(): Float = 1.8000002f
    
    override fun getDefaultX(): Int = 515
    
    override fun getDefaultY(): Int = 238
}
