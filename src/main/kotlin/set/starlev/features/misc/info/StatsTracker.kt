package set.starlev.features.misc.info

import net.minecraft.client.Minecraft
import java.util.LinkedList

object StatsTracker {
    private val clickTimestamps = LinkedList<Long>()
    private val blockBreakTimestamps = LinkedList<Long>()
    private var lastAttackDown = false

    fun tick() {
        val mc = Minecraft.getInstance()
        if (mc.player == null) return
        if (mc.screen != null) return
        val down = mc.options.keyAttack.isDown
        if (down && !lastAttackDown) {
            registerClick()
        }
        lastAttackDown = down
    }
    
    // CPS
    fun registerClick() {
        clickTimestamps.add(System.currentTimeMillis())
        cleanUp(clickTimestamps)
    }
    
    fun getCps(): Int {
        cleanUp(clickTimestamps)
        return clickTimestamps.size
    }
    
    // BPS
    fun registerBlockBreak() {
        blockBreakTimestamps.add(System.currentTimeMillis())
        cleanUp(blockBreakTimestamps)
    }
    
    fun getBps(): Int {
        cleanUp(blockBreakTimestamps)
        return blockBreakTimestamps.size
    }
    
    private fun cleanUp(list: LinkedList<Long>) {
        val now = System.currentTimeMillis()
        while (list.isNotEmpty() && now - list.first() > 1000) {
            list.removeFirst()
        }
    }
}
