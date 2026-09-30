package set.starlev.scripting

import net.minecraft.network.chat.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Кастомные строки скорборда от JS-модулей (интеграция с HudScoreboard).
 * Каждая строка привязывается к модулю-владельцу и автоматом удаляется при выгрузке.
 */
object ModuleCustomScoreboard {
    data class Entry(
        val module: String, // lower
        val id: String,
        val component: Component,
        val anchor: String, // AFTER_XP / BOTTOM
        val order: Int
    )

    private val entries = ConcurrentHashMap<String, Entry>()

    private fun key(module: String, id: String) = "${module.lowercase()}:$id"

    fun add(module: String, id: String, text: String, anchor: String = "BOTTOM", order: Int = 99) {
        val comp = Component.literal(text.replace('&', '§'))
        val k = key(module, id)
        // normalize anchor
        val a = anchor.uppercase().let { if (it == "AFTER_XP" || it == "BOTTOM") it else "BOTTOM" }
        entries[k] = Entry(module.lowercase(), id, comp, a, order)
    }

    fun set(module: String, id: String, text: String) {
        val k = key(module, id)
        val existing = entries[k] ?: run {
            add(module, id, text)
            return
        }
        entries[k] = existing.copy(component = Component.literal(text.replace('&', '§')))
    }

    fun remove(module: String, id: String) {
        entries.remove(key(module, id))
    }

    fun removeForModule(module: String) {
        val lower = module.lowercase()
        entries.entries.removeIf { it.value.module == lower }
    }

    fun clearAll() = entries.clear()

    fun getAll(): List<Entry> = entries.values.toList()

    fun getForModule(module: String): List<Entry> = entries.values.filter { it.module == module.lowercase() }
}
