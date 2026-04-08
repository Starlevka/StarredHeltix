package set.starlev.utils.detectors

import set.starlev.StarredHeltix
import set.starlev.utils.CacheManager

/**
 * Детектор текущей локации на основе данных scoreboard.
 * Определяет локацию по символу ⏣/ф и заголовку скорборда.
 */
object LocationDetector {

    private var cachedLocation: String? = null
    private var lastPollTime: Long = 0
    private const val POLL_INTERVAL_MS = 2000L

    /**
        * Получить текущую локацию (кэшируется на 2 секунду).
        * Возвращает полную строку из scoreboard, содержащую локацию.
        * @return Строка scoreboard с локацией или null если не определена
        */
    fun getCurrentLocation(): String? {
        val now = System.currentTimeMillis()
        if (now - lastPollTime < POLL_INTERVAL_MS) return cachedLocation

        lastPollTime = now
        val location = detectLocation()
        cachedLocation = location
        return location
    }

    /**
        * Получить чистое название локации без цветовых кодов.
        */
    fun getCurrentLocationClean(): String? {
        return getCurrentLocation()?.let { cleanLocationName(it) }
    }

    /**
        * Проверить, находится ли игрок в определённой локации.
        * @param locationName название локации (регистронезависимо)
        */
    fun isInLocation(locationName: String): Boolean {
        val current = getCurrentLocationClean() ?: return false
        return current.contains(locationName, ignoreCase = true)
    }

    /**
        * Проверить, находится ли игрок в одной из перечисленных локаций.
        */
    fun isInAnyLocation(vararg locationNames: String): Boolean {
        val current = getCurrentLocationClean() ?: return false
        return locationNames.any { current.contains(it, ignoreCase = true) }
    }

    private fun detectLocation(): String? {
        val lines = ScoreboardDetector.getScoreboardText()

        // Ищем строку, содержащую символ ⏣ или ф
        for (line in lines) {
            val cleanLine = cleanLocationName(line)
            if (cleanLine.contains('⏣') || cleanLine.contains('ф')) {
                val result = cleanLine
                    .replace("⏣", "")
                    .replace("ф", "")
                    .trim()
                set.starlev.StarredHeltix.LOGGER.info("[LocationDetector] Найдена локация: '$result' из строки: '$cleanLine'")
                return result
            }
        }

        // Фоллбэк: заголовок scoreboard
        val title = ScoreboardDetector.getScoreboardTitle()
        if (title.isNotBlank() && !title.equals("www.hypixel.net", ignoreCase = true)) {
            // LOGGER.info("[LocationDetector] Фоллбэк по заголовку: '$title'")
            return cleanLocationName(title)
        }

        // LOGGER.info("[LocationDetector] Локация не найдена. Lines: [${lines.joinToString(" | ")}]")
        return null
    }

    /**
        * Очистить строку от цветовых кодов, но сохранить текст локации.
        */
    private fun cleanLocationName(raw: String): String {
        return raw
            .replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "")
            .replace("~", "")
            .trim()
    }

    /**
     * Сбросить кэш (вызывать при смене мира/сервера).
     */
    fun resetCache() {
        cachedLocation = null
        lastPollTime = 0
    }
}