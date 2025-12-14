package set.starlev.features.chat

import set.starlev.StarredHeltix

object MessageFilterManager {
    private val config get() = StarredHeltix.feature.chat.messageFilter

    fun shouldAllowMessage(message: String): Boolean {
        if (!config.enabled || config.filters.isEmpty()) return true
        return config.filters.none { message.startsWith(it) }
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
