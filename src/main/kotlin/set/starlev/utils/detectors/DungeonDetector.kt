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

    /**
     * Сбросить кэш при смене мира/сервера (вызывается из WorldChangeHandler).
     */
    fun resetCache() {
        lastCheckTime = 0
        cachedResult = false
    }

    /**
     * Лобби подземелия (хаб) — это НЕ инстанс данжа.
     *
     * Нужно фичам, которые ищут боссов/меню данжа: `isInDungeon()` считает хаб
     * подземельем (см. detectDungeon — любое измерение с «dungeon» в имени), из-за
     * чего они срабатывают прямо в лобби.
     *
     * Разделители в имени измерения НЕ фиксированы: хаб на Heltix — `minecraft:dungeonhub`
     * (без подчёркивания, см. `Waypoints.kt`), где-то встречается `…_hub` / `…_lobby`.
     * Поэтому сравниваем «хвост» пути без `_`/`-`, а не по точному имени.
     */
    fun isDungeonLobby(): Boolean {
        val level = mc.level ?: return false
        val dimId = level.dimension().identifier().toString()
        if (dimId == DUNGEON_HUB_DIMENSION) return true
        val tail = dimId.substringAfter(':').replace("_", "").replace("-", "")
        return tail.endsWith("hub", ignoreCase = true) ||
            tail.endsWith("lobby", ignoreCase = true) ||
            tail.endsWith("lobbys", ignoreCase = true)
    }

    /** Настоящий заход в подземелье: не лобби/хаб. */
    fun isInDungeonInstance(): Boolean = !isDungeonLobby() && isInDungeon()

    private const val DUNGEON_HUB_DIMENSION = "minecraft:dungeonhub"

    private fun detectDungeon(): Boolean {
        val level = mc.level ?: return false
        val dimId = level.dimension().identifier().toString()
        
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
            s.contains("склеп", ignoreCase = true) ||
            s.contains("crypt", ignoreCase = true)
    }
}
