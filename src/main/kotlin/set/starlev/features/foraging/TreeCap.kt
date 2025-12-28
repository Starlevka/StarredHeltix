package set.starlev.features.foraging

import net.minecraft.client.Minecraft
import net.minecraft.world.item.Items
import org.slf4j.LoggerFactory
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement
import set.starlev.skyblock.ItemRegistry

object TreeCapCooldown : HudElement("TreeCapCooldown") {
    private val mc = Minecraft.getInstance()
    private var lastBreakTime = 0L
    private var isOnCooldown = false

    override fun render() {
        val config = StarredHeltix.feature.foraging.axes.treeCapCooldown
        if (!config.enabled) return

        val currentTime = System.currentTimeMillis()
        val timeSinceLastBreak = currentTime - lastBreakTime
        val cooldownMs = (config.cooldown.toDoubleOrNull() ?: 2.0) * 1000
        val remainingTime = ((cooldownMs - timeSinceLastBreak) / 1000.0).coerceAtLeast(0.0)

        val text = if (isEditing) "§a§l2.0" else if (remainingTime > 0) "§c§l${String.format("%.1f", remainingTime)}" else ""
        
        if (text.isEmpty()) return
        val pose = cachedGraphics?.pose() ?: return
        pose.pushMatrix()
        pose.translate(x.toFloat(), y.toFloat())
        pose.scale(2f, 2f)
        cachedGraphics?.drawString(mc.font, text, 0, 0, 0xFFFFFFFF.toInt())
        pose.popMatrix()
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

    override fun getWidth() = mc.font.width(StarredHeltix.feature.foraging.axes.treeCapCooldown.cooldown) * 2
    override fun getHeight() = mc.font.lineHeight * 2
    
    override fun getDefaultX(): Int {
        val window = mc.window
        return (window.guiScaledWidth / 2) + 30
    }
    
    override fun getDefaultY(): Int {
        val window = mc.window
        return (window.guiScaledHeight / 2) - 5
    }
}
