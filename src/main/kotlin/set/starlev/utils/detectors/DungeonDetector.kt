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

        val title = ScoreboardDetector.getScoreboardTitle()
        if (looksLikeDungeonText(title)) return true

        val lines = ScoreboardDetector.getScoreboardText()
        for (line in lines) {
            if (looksLikeDungeonText(line)) return true
        }
        
        return false
    }

    private fun looksLikeDungeonText(text: String): Boolean {
        if (text.isBlank()) return false
        val s = text.lowercase()
        return s.contains("подзем", ignoreCase = true) ||
            s.contains("данж", ignoreCase = true) ||
            s.contains("dungeon", ignoreCase = true) ||
            s.contains("catacomb", ignoreCase = true) ||
            s.contains("катакомб", ignoreCase = true) ||
            s.contains("зачищено", ignoreCase = true) ||
            s.contains("cleared", ignoreCase = true) ||
            s.contains("boss", ignoreCase = true) ||
            s.contains("босс", ignoreCase = true) ||
            s.contains("очки", ignoreCase = true) ||
            s.contains("баллы", ignoreCase = true) ||
            s.contains("счёт", ignoreCase = true) ||
            s.contains("прогресс", ignoreCase = true)
    }
}
