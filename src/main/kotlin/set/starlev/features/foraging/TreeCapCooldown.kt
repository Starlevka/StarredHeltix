package set.starlev.features.foraging

import net.minecraft.client.Minecraft
import net.minecraft.world.item.Items
import org.slf4j.LoggerFactory
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement

object TreeCapCooldown : HudElement("TreeCapCooldown") {
    private val mc = Minecraft.getInstance()
    private val logger = LoggerFactory.getLogger("StarredHeltix/TreeCapCooldown")
    private var lastBreakTime = 0L
    private var isOnCooldown = false

    fun onBlockBreak(blockName: String) {
        val config = StarredHeltix.feature.foraging.treeCapCooldown
        logger.info("onBlockBreak called: blockName=$blockName, enabled=${config.enabled}")
        
        if (!config.enabled) {
            logger.info("TreeCapCooldown disabled")
            return
        }
        
        val player = mc.player
        if (player == null) {
            logger.info("Player is null")
            return
        }
        val itemStack = player.mainHandItem
        logger.info("Player has item: ${itemStack.item}")
        
        // Проверяем что в руке деревянный или золотой топор
        if (itemStack.item != Items.WOODEN_AXE && itemStack.item != Items.GOLDEN_AXE) {
            logger.info("Not wooden or golden axe")
            return
        }

        logger.info("TreeCapCooldown STARTED!")
        lastBreakTime = System.currentTimeMillis()
        isOnCooldown = true
    }

    fun onLogBreak(blockName: String) {
        val config = StarredHeltix.feature.foraging.treeCapCooldown
        logger.info("onLogBreak called: blockName=$blockName, enabled=${config.enabled}")
        
        if (!config.enabled) {
            logger.info("TreeCapCooldown disabled")
            return
        }
        
        val player = mc.player
        if (player == null) {
            logger.info("Player is null")
            return
        }
        val itemStack = player.mainHandItem
        logger.info("Player has item: ${itemStack.item}")
        
        // Проверяем что в руке деревянный или золотой топор для лога
        if (itemStack.item != Items.WOODEN_AXE && itemStack.item != Items.GOLDEN_AXE) {
            logger.info("Not wooden or golden axe for log break")
            return
        }

        logger.info("TreeCapCooldown log break detected!")
        lastBreakTime = System.currentTimeMillis()
        isOnCooldown = true
    }

    override fun render() {
        val config = StarredHeltix.feature.foraging.treeCapCooldown
        if (!config.enabled) return

        val remaining = getRemainingCooldown()
        if (remaining <= 0 && !isEditing) {
            isOnCooldown = false
            return
        }

        val text = if (isEditing) "§l${config.cooldown}" else "§l${String.format("%.1f", remaining / 1000.0)}"
        cachedGraphics?.drawString(mc.font, text, x, y, 0xFFFF0000.toInt())
    }

    private fun getRemainingCooldown(): Long {
        if (!isOnCooldown) return 0L
        val config = StarredHeltix.feature.foraging.treeCapCooldown
        val duration = (config.cooldown.toDoubleOrNull() ?: 2.0).toLong() * 1000L
        return (duration - (System.currentTimeMillis() - lastBreakTime)).coerceAtLeast(0L)
    }

    override fun getWidth() = mc.font.width(StarredHeltix.feature.foraging.treeCapCooldown.cooldown) * 2
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
