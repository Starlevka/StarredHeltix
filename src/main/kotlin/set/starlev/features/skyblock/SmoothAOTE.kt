package set.starlev.features.skyblock

import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import set.starlev.config.ConfigManager
import set.starlev.utils.*
import kotlin.math.min

object SmoothAote {
    private var isInterpolating = false
    private var offset: Vec3 = Vec3.ZERO
    private var startTime: Long = 0
    private var duration: Long = 150
    private var lastUseTime: Long = 0

    fun onItemUse() {
        lastUseTime = System.currentTimeMillis()
    }

    fun onTeleport(newPos: Vec3) {
        if (!ConfigManager.features.skyblock.smoothAote.enabled) return

        // Проверяем, использовал ли игрок предмет недавно (0.5 сек)
        // Это предотвращает срабатывание на телепорт-пэдах
        if (System.currentTimeMillis() - lastUseTime > 500) return

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return

        val currentRenderPos = mc.gameRenderer.mainCamera.position

        val dist = currentRenderPos.distanceTo(newPos)
        if (dist < 2 || dist > 65) return

        val mainHandItem = player.mainHandItem
        val isTeleportItem = set.starlev.skyblock.ItemRegistry.isItem(mainHandItem, set.starlev.skyblock.ItemRegistry.SkyblockItem.ASPECT_OF_THE_END) ||
                            set.starlev.skyblock.ItemRegistry.isItem(mainHandItem, set.starlev.skyblock.ItemRegistry.SkyblockItem.ASPECT_OF_THE_VOID) ||
                            set.starlev.skyblock.ItemRegistry.isItem(mainHandItem, set.starlev.skyblock.ItemRegistry.SkyblockItem.HYPERION)

        if (!isTeleportItem) return

        // Если уже идёт интерполяция, вычисляем текущую визуальную позицию как стартовую
        val startPos = if (isInterpolating) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = min(elapsed.toDouble() / duration.toDouble(), 1.0)
            val ease = 1.0 - Math.pow(1.0 - progress, 4.0) // EaseOutQuart
            val factor = 1.0 - ease
            currentRenderPos // уже включает offset
        } else {
            currentRenderPos
        }

        offset = startPos.subtract(newPos)
        startTime = System.currentTimeMillis()
        duration = ConfigManager.features.skyblock.smoothAote.time.toLong()
        isInterpolating = true
    }

    fun onTeleport(oldRenderPos: Vec3, newPos: Vec3) {
        if (!ConfigManager.features.skyblock.smoothAote.enabled) return

        if (System.currentTimeMillis() - lastUseTime > 500) return

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return

        val dist = oldRenderPos.distanceTo(newPos)
        if (dist < 2 || dist > 65) return

        val mainHandItem = player.mainHandItem
        val isTeleportItem = set.starlev.skyblock.ItemRegistry.isItem(mainHandItem, set.starlev.skyblock.ItemRegistry.SkyblockItem.ASPECT_OF_THE_END) ||
                set.starlev.skyblock.ItemRegistry.isItem(mainHandItem, set.starlev.skyblock.ItemRegistry.SkyblockItem.ASPECT_OF_THE_VOID) ||
                set.starlev.skyblock.ItemRegistry.isItem(mainHandItem, set.starlev.skyblock.ItemRegistry.SkyblockItem.HYPERION)

        if (!isTeleportItem) return

        // Если уже идёт интерполяция, вычисляем текущую визуальную позицию как стартовую
        val startPos = if (isInterpolating) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = min(elapsed.toDouble() / duration.toDouble(), 1.0)
            val ease = 1.0 - Math.pow(1.0 - progress, 4.0) // EaseOutQuart
            val factor = 1.0 - ease
            oldRenderPos // уже включает offset
        } else {
            oldRenderPos
        }

        offset = startPos.subtract(newPos)
        startTime = System.currentTimeMillis()
        duration = ConfigManager.features.skyblock.smoothAote.time.toLong()
        isInterpolating = true
    }

    fun getOffset(): Vec3? {
        if (!isInterpolating) return null

        val elapsed = System.currentTimeMillis() - startTime
        val progress = min(elapsed.toDouble() / duration.toDouble(), 1.0)
        
        if (progress >= 1.0) {
            isInterpolating = false
            return null
        }
        
        // EaseOutQuart implementation (более плавный)
        // Formula: 1 - pow(1 - x, 4)
        val ease = 1.0 - Math.pow(1.0 - progress, 4.0)
        val factor = 1.0 - ease
        return offset.scale(factor)
    }
    
    fun isInterpolating(): Boolean = isInterpolating
}
