package set.starlev.utils

import com.github.benmanes.caffeine.cache.Caffeine
import net.minecraft.util.FormattedCharSequence
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Централизованный менеджер кэширования для оптимизации производительности.
 */
object CacheManager {
    private val regexCache = ConcurrentHashMap<String, Regex>()

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

    // Кэш для обработанных эффектов текста (SecretFunFeatures)
    // Ключ: Текст + Хэш стиля
    private val textEffectCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build<String, net.minecraft.network.chat.Component>()

    private val componentHashCache = Caffeine.newBuilder()
        .weakKeys()
        .maximumSize(5000)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build<Any, Int>()

    private val skyblockIdCache = Caffeine.newBuilder()
        .weakKeys()
        .maximumSize(5000)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build<Any, String?>()

    private val stringDedupCache = Caffeine.newBuilder()
        .maximumSize(20000)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build<String, String>()
    
    /**
      * Получить скомпилированный Regex из кэша.
      * Кэш всегда включён — избегаем повторной компиляции паттернов.
      */
    fun getRegex(pattern: String): Regex {
        return regexCache.getOrPut(pattern) { Regex(pattern) }
    }

    fun getComponentHash(componentObject: Any): Int {
        return componentHashCache.get(componentObject) { componentObject.hashCode() }
    }

    fun getCachedSkyblockId(componentObject: Any, provider: () -> String?): String? {
        return skyblockIdCache.get(componentObject) { provider() }
    }

    fun dedupString(value: String): String {
        return stringDedupCache.get(value) { it }
    }

    /**
     * Получить кэшированный скорборд.
     */
    fun getCachedScoreboard(objectiveId: String): List<String>? {
        return scoreboardCaffeine.getIfPresent(objectiveId)
    }

    /**
     * Сохранить скорборд в кэш.
     */
    fun cacheScoreboard(objectiveId: String, lines: List<String>) {
        scoreboardCaffeine.put(objectiveId, lines)
    }

    /**
     * Получить кэшированный таб-лист.
     */
    fun getCachedTabList(type: String): List<String>? {
        return tabListCaffeine.getIfPresent(type)
    }

    /**
     * Сохранить таб-лист в кэш.
     */
    fun cacheTabList(type: String, lines: List<String>) {
        tabListCaffeine.put(type, lines)
    }

    /**
     * Получить кэшированную ширину текста.
     */
    fun getCachedTextWidth(text: String): Int? {
        return textWidthCache.getIfPresent(text)
    }

    /**
     * Сохранить ширину текста в кэш.
     */
    fun cacheTextWidth(text: String, width: Int) {
        textWidthCache.put(text, width)
    }

    /**
     * Получить кэшированную компоновку текста.
     */
    fun getCachedLayout(text: String, maxWidth: Int): List<FormattedCharSequence>? {
        return textLayoutCache.getIfPresent("$text|$maxWidth")
    }

    /**
     * Сохранить компоновку текста в кэш.
     */
    fun cacheLayout(text: String, maxWidth: Int, lines: List<FormattedCharSequence>) {
        textLayoutCache.put("$text|$maxWidth", lines)
    }

    /**
     * Получить кэшированный компонент с эффектами.
     */
    fun getCachedTextEffect(text: String, styleHash: Int): net.minecraft.network.chat.Component? {
        return textEffectCache.getIfPresent("$text|$styleHash")
    }

    /**
     * Сохранить компонент с эффектами в кэш.
     */
    fun cacheTextEffect(text: String, styleHash: Int, component: net.minecraft.network.chat.Component) {
        textEffectCache.put("$text|$styleHash", component)
    }

    /**
     * Получить кэшированный лор предмета.
     */
    fun getCachedLore(hash: Int): List<String>? {
        return itemLoreCache.getIfPresent(hash)
    }

    /**
     * Сохранить лор предмета в кэш.
     */
    fun cacheLore(hash: Int, lore: List<String>) {
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
        textEffectCache.invalidateAll()
        componentHashCache.invalidateAll()
        skyblockIdCache.invalidateAll()
        stringDedupCache.invalidateAll()
    }
}
