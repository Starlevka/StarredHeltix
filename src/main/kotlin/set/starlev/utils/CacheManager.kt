package set.starlev.utils

import com.github.benmanes.caffeine.cache.Caffeine
import set.starlev.StarredHeltix
import net.minecraft.util.FormattedCharSequence
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Централизованный менеджер кэширования для оптимизации производительности.
 */
object CacheManager {
    private val regexCache = ConcurrentHashMap<String, Regex>()
    private val aiMathCache = ConcurrentHashMap<String, String?>()

    // Кэш для компоновки текста (Текст + Ширина -> Список строк)
    private val textLayoutCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build<String, List<FormattedCharSequence>>()

    // Кэш для скорборда (ID объектива -> Список строк)
    private val scoreboardCaffeine = Caffeine.newBuilder()
        .maximumSize(10)
        .expireAfterWrite(50, TimeUnit.MILLISECONDS) // Примерно 1 тик
        .build<String, List<String>>()
    // Кэш для таб-листа (Тип данных -> Список строк)
    private val tabListCaffeine = Caffeine.newBuilder()
        .maximumSize(10)
        .expireAfterWrite(100, TimeUnit.MILLISECONDS) // Примерно 2 тика
        .build<String, List<String>>()

    // Кэш для Lore предметов с ограничением по размеру и времени жизни
    private val itemLoreCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build<Int, List<String>>()

    // Кэш для ширины текста
    private val textWidthCache = Caffeine.newBuilder()
        .maximumSize(2000)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build<String, Int>()
    
    /**
     * Получить скомпилированный Regex из кэша.
     */
    fun getRegex(pattern: String): Regex {
        if (!StarredHeltix.feature.optimization.performance.cacheRegex) {
            return Regex(pattern)
        }
        return regexCache.getOrPut(pattern) { Regex(pattern) }
    }

    /**
     * Кэшировать результат решения математического выражения.
     */
    fun getCachedAiMath(message: String, provider: () -> String?): String? {
        if (!StarredHeltix.feature.optimization.performance.cacheRegex) return provider()
        return aiMathCache.getOrPut(message) { provider() }
    }

    /**
     * Получить кэшированный скорборд.
     */
    fun getCachedScoreboard(objectiveId: String): List<String>? {
        if (!StarredHeltix.feature.optimization.performance.cacheScoreboard) return null
        return scoreboardCaffeine.getIfPresent(objectiveId)
    }

    /**
     * Сохранить скорборд в кэш.
     */
    fun cacheScoreboard(objectiveId: String, lines: List<String>) {
        if (!StarredHeltix.feature.optimization.performance.cacheScoreboard) return
        scoreboardCaffeine.put(objectiveId, lines)
    }

    /**
     * Получить кэшированный таб-лист.
     */
    fun getCachedTabList(type: String): List<String>? {
        if (!StarredHeltix.feature.optimization.performance.cacheScoreboard) return null // Используем ту же настройку
        return tabListCaffeine.getIfPresent(type)
    }

    /**
     * Сохранить таб-лист в кэш.
     */
    fun cacheTabList(type: String, lines: List<String>) {
        if (!StarredHeltix.feature.optimization.performance.cacheScoreboard) return
        tabListCaffeine.put(type, lines)
    }

    /**
     * Получить кэшированную ширину текста.
     */
    fun getCachedTextWidth(text: String): Int? {
        if (!StarredHeltix.feature.optimization.performance.cacheRegex) return null
        return textWidthCache.getIfPresent(text)
    }

    /**
     * Сохранить ширину текста в кэш.
     */
    fun cacheTextWidth(text: String, width: Int) {
        if (!StarredHeltix.feature.optimization.performance.cacheRegex) return
        textWidthCache.put(text, width)
    }

    /**
     * Получить кэшированную компоновку текста.
     */
    fun getCachedLayout(text: String, maxWidth: Int): List<FormattedCharSequence>? {
        if (!StarredHeltix.feature.optimization.performance.cacheRegex) return null
        return textLayoutCache.getIfPresent("$text|$maxWidth")
    }

    /**
     * Сохранить компоновку текста в кэш.
     */
    fun cacheLayout(text: String, maxWidth: Int, lines: List<FormattedCharSequence>) {
        if (!StarredHeltix.feature.optimization.performance.cacheRegex) return
        textLayoutCache.put("$text|$maxWidth", lines)
    }

    /**
     * Получить кэшированный лор предмета.
     */
    fun getCachedLore(hash: Int): List<String>? {
        if (!StarredHeltix.feature.optimization.performance.cacheItemLore) return null
        return itemLoreCache.getIfPresent(hash)
    }

    /**
     * Сохранить лор предмета в кэш.
     */
    fun cacheLore(hash: Int, lore: List<String>) {
        if (!StarredHeltix.feature.optimization.performance.cacheItemLore) return
        itemLoreCache.put(hash, lore)
    }

    /**
     * Очистить весь кэш (например, при выходе с сервера).
     */
    fun clearAll() {
        regexCache.clear()
        scoreboardCaffeine.invalidateAll()
        tabListCaffeine.invalidateAll()
        itemLoreCache.invalidateAll()
        textWidthCache.invalidateAll()
        textLayoutCache.invalidateAll()
        aiMathCache.clear()
    }
}
