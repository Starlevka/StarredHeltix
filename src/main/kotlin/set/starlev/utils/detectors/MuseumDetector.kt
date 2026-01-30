package set.starlev.utils.detectors

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import set.starlev.utils.ContainerUtils
import set.starlev.utils.detectors.ContainerDetector
import set.starlev.utils.detectors.ScoreboardDetector
import net.minecraft.world.item.Items

/**
 * Детектор для меню "Музей" на Skyblock.
 * Использует скорборд и структуру предметов для надежного обнаружения.
 */
object MuseumDetector {
    private var isMuseumOpen = false
    
    // Паттерны для скорборда. Используем простые слова, так как matcher.find() найдет их в любом месте строки.
    private val MUSEUM_KEYWORDS = listOf(
        "Музей", "Атриум", "Оружейня", "Зал брони", "Зал прочего",
    )

    private val MUSEUM_KEYWORDS_NORMALIZED = MUSEUM_KEYWORDS.map { normalize(it) }.filter { it.isNotEmpty() }

    fun init() {
        ContainerDetector.registerOpen { screen ->
            isMuseumOpen = isMuseumMenu(screen)
            
        }

        ContainerDetector.registerClose { _ ->
            isMuseumOpen = false
        }
    }

    /**
     * Проверяет, находится ли игрок в зоне Музея по скорборду.
     */
    fun isInMuseumZone(): Boolean {
        val scoreboardTitle = ScoreboardDetector.getScoreboardTitle()
        val scoreboardLines = ScoreboardDetector.getScoreboardText()

        // Проверка заголовка
        val normalizedTitle = normalize(scoreboardTitle)
        if (matchesMuseumKeyword(normalizedTitle)) return true

        // Проверка строк
        for (line in scoreboardLines) {
            val normalizedLine = normalize(line)
            if (matchesMuseumKeyword(normalizedLine)) return true
        }
        return false
    }

    /**
     * Проверяет, является ли экран меню "Музеем".
     */
    fun isMuseumMenu(screen: AbstractContainerScreen<*>): Boolean {
        // 0. Проверка по скорборду (как просил пользователь - это основной триггер)
        if (isInMuseumZone()) return true

        // 1. Проверка по заголовку контейнера
        val containerTitle = normalize(screen.title.string)
        if (matchesMuseumKeyword(containerTitle)) return true
        
        // 2. Проверка по структуре предметов (запасной вариант)
        val items = ContainerUtils.getContainerItems(screen)
        val grayDyeCount = items.count { it.`is`(Items.GRAY_DYE) }
        if (grayDyeCount >= 1) return true

        return false
    }

    /**
     * Возвращает true, если меню Музея сейчас открыто.
     */
    fun isOpen(): Boolean = isMuseumOpen

    private fun normalize(text: String): String {
        return text
            .replace(Regex("§."), "")
            .replace('\u00A0', ' ')
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{Nd} ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun matchesMuseumKeyword(text: String): Boolean {
        if (text.isBlank()) return false
        return MUSEUM_KEYWORDS_NORMALIZED.any { text.contains(it) }
    }
}
