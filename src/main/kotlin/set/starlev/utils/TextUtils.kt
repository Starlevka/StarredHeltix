package set.starlev.utils

/**
 * Единые текстовые хелперы — замена разбросанных `Regex("§[0-9a-fk-or...]")` и `stripColor`.
 * Делегирует кэширование Regex в CacheManager.
 */
object TextUtils {
    private const val COLOR_CODES = "(?i)§[0-9a-fk-orlnmxz]"

    fun stripColor(text: String): String {
        return text.replace(CacheManager.getRegex(COLOR_CODES), "")
    }

    fun stripAll(text: String): String = stripColor(text).trim()

    fun cleanLocation(text: String): String = stripColor(text).replace("~", "").trim()
}

// Обратная совместимость: топ-левел `stripColor` теперь делегирует в TextUtils
fun stripColor(text: String): String = TextUtils.stripColor(text)
