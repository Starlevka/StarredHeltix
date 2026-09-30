package set.starlev.features.chat

import set.starlev.StarredHeltix
import set.starlev.utils.CacheManager

object MessageFilterManager {
    private val config get() = StarredHeltix.feature.chat.messageFilter
    private val colorRegex get() = CacheManager.getRegex("§[0-9a-fk-orA-FK-OR]")

    // Нормализуем фильтр один раз на проверку: нижний регистр, без цветов, схлопнутые пробелы
    private fun normalizeFilter(raw: String): String {
        val noColor = colorRegex.replace(raw, "")
        return noColor.trim().replace(Regex("\\s+"), " ").lowercase()
    }

    private fun normalizeMessage(message: String): String {
        val noColor = colorRegex.replace(message, " ").replace("§", " ")
        return noColor.replace(Regex("\\s+"), " ").trim().lowercase()
    }

    fun shouldAllowMessage(message: String): Boolean {
        if (!config.enabled || config.filters.isEmpty()) return true
        val cleanMessage = normalizeMessage(message)
        if (cleanMessage.isEmpty()) return true

        return config.filters.none { filter ->
            val f = normalizeFilter(filter)
            if (f.isEmpty()) return@none false
            // contains по нормализованной строке — ловит и префиксы, и отправителя,
            // и многословные фразы независимо от цветов/пробелов
            cleanMessage.contains(f)
        }
    }

    /** Совместимость: старая сигнатура с Component — фильтруем по .string */
    fun shouldAllowMessage(component: net.minecraft.network.chat.Component): Boolean {
        return try {
            shouldAllowMessage(component.string)
        } catch (_: Exception) {
            true
        }
    }

    fun addFilter(filter: String) {
        val f = filter.trim()
        if (f.isNotEmpty() && !config.filters.contains(f)) {
            config.filters.add(f)
            StarredHeltix.configManager.saveConfig("filter-add")
        }
    }

    fun removeFilter(filter: String) {
        if (config.filters.remove(filter)) {
            StarredHeltix.configManager.saveConfig("filter-remove")
        }
    }
}

