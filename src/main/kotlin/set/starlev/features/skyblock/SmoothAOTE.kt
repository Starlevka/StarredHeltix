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

        // Offset method: We want to smooth the jump from currentRenderPos to newPos.
        // The camera (without us) will jump to newPos immediately.
        // So we add an offset that starts at (currentRenderPos - newPos) and decays to 0.
        // (currentRenderPos - newPos) + newPos = currentRenderPos. (Start matches visual pos)
        // 0 + newPos = newPos. (End matches target)
        
        // currentRenderPos from mainCamera already includes the previous offset if we were interpolating,
        // because CameraMixin applies it to the camera position.
        
        offset = currentRenderPos.subtract(newPos)
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

        offset = oldRenderPos.subtract(newPos)
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
        
        // EaseOutCubic implementation (standard for smooth teleports)
        // Formula: 1 - pow(1 - x, 3)
        // We need the factor to go from 1.0 (start) to 0.0 (end)
        // So we calculate ease(progress) which goes 0->1, then return offset * (1 - ease)
        
        val ease = 1.0 - Math.pow(1.0 - progress, 3.0)
        
        val factor = 1.0 - ease
        
        return offset.scale(factor)
    }
    
    fun isInterpolating(): Boolean = isInterpolating
}
