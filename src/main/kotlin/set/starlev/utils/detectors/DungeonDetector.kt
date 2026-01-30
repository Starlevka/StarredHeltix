package set.starlev.utils.detectors

import net.minecraft.client.Minecraft

object DungeonDetector {
    private val mc = Minecraft.getInstance()
    private var lastCheckTime: Long = 0
    private var cachedResult: Boolean = false

    fun isInDungeon(): Boolean {
        val level = mc.level ?: return false
        val now = System.currentTimeMillis()
        
        // Кэшируем результат на 1 секунду (1000 мс), чтобы не опрашивать скорборд каждый кадр
        if (now - lastCheckTime < 1000) {
            return cachedResult
        }
        
        lastCheckTime = now
        cachedResult = detectDungeon()
        return cachedResult
    }

    private fun detectDungeon(): Boolean {
        val level = mc.level ?: return false
        val dimId = level.dimension().location().toString()
        
        // 1. Проверка по ID измерения
        if (dimId.startsWith("minecraft:dungeon_") || dimId.contains("dungeon", ignoreCase = true)) return true
        
        return false
    }
}
