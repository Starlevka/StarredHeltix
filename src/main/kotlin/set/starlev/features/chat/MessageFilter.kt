package set.starlev.features.chat

import set.starlev.StarredHeltix

object MessageFilterManager {
    private val config get() = StarredHeltix.feature.chat.general.messageFilter

    fun shouldAllowMessage(message: String): Boolean {
        if (!config.enabled || config.filters.isEmpty()) return true
        
        // Убираем цветовые коды для корректной фильтрации, если они есть
        val cleanMessage = message.replace(Regex("§[0-9a-fk-orA-FK-OR]"), "")
        val prefix = cleanMessage.substringBefore(':')
        
        return config.filters.none { filter ->
            val cleanFilter = filter.replace(Regex("§[0-9a-fk-orA-FK-OR]"), "").trim()
            if (cleanFilter.isEmpty()) return@none false
            
            // Проверяем: 
            // 1. Сообщение начинается с фильтра
            // 2. Фильтр содержится в префиксе (до первого двоеточия - обычно это отправитель)
            cleanMessage.startsWith(cleanFilter, ignoreCase = true) || 
            prefix.contains(cleanFilter, ignoreCase = true)
        }
    }

    fun addFilter(filter: String) {
        if (filter.isNotEmpty() && !config.filters.contains(filter)) {
            config.filters.add(filter)
            StarredHeltix.configManager.saveConfig("filter-add")
        }
    }

    fun removeFilter(filter: String) {
        if (config.filters.remove(filter)) {
            StarredHeltix.configManager.saveConfig("filter-remove")
        }
    }
}
