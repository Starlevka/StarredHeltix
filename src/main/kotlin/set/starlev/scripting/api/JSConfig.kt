package set.starlev.scripting.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import set.starlev.scripting.ModuleLoader
import set.starlev.scripting.ScriptConsole
import java.io.File

/**
 * JS API: Конфигурация модуля (`config.json` в папке модуля).
 *
 * Отличие от `storage`: конфиг — настройки, которые игрок может менять
 * (клавиша по умолчанию, пороги, тумблеры); storage — произвольные данные.
 *
 * ```js
 * config.define({
 *     showHud: true,
 *     threshold: 10,
 *     message: "&aГотово!"
 * });
 * var threshold = config.get("threshold", 10);
 * config.set("threshold", threshold + 1);
 * ```
 */
class JSConfig(private val moduleDir: File, private val moduleName: String) {
    private val dataFile = File(moduleDir, "config.json")
    private val data = LinkedHashMap<String, Any>()
    private val schemaMap = LinkedHashMap<String, Any>()
    private val gsonPretty: Gson = GsonBuilder().setPrettyPrinting().create()
    private val gson: Gson = Gson()

    init {
        load()
    }

    /**
     * Регистрирует значения по умолчанию (существующие значения не перезаписываются).
     * vararg вместо перегрузки: Rhino не разрешает (Any,Any)/(Any) надёжно.
     * define(values) или define(values, schema).
     * Схема: ключ -> {type: "switch"|"slider"|"dropdown"|"color"|"text", min, max, step, options, description}.
     */
    fun define(vararg args: Any) {
        if (args.isEmpty()) return
        val defaults = args[0]
        if (args.size >= 2) defineSchema(args[1])
        val map = asMap(defaults) ?: return
        for ((key, value) in map) {
            if (key !in data) data[key] = value
        }
        save()
    }

    fun get(key: String, defaultValue: Any? = null): Any? = data[key] ?: defaultValue

    fun set(key: String, value: Any?) {
        if (value == null) {
            data.remove(key)
        } else {
            data[key] = value
        }
        save()
    }

    fun has(key: String): Boolean = data.containsKey(key)

    fun remove(key: String) {
        data.remove(key)
        save()
    }

    /** Все значения как JS-объект. */
    fun getAll(): Map<String, Any> = LinkedHashMap(data)

    /**
     * Схема редакторов GUI (config.json "__schema"): ключ -> {type, min, max, step, options}.
     * Регистрируется из JS: config.defineSchema({...}) или вторым аргументом define(values, schema).
     * Значения-настройки хранятся отдельно от схемы.
     */
    fun defineSchema(schema: Any) {
        val map = asMap(schema) ?: return
        val normalized = LinkedHashMap<String, Any>()
        for ((key, value) in map) {
            normalized[key] = when (value) {
                is Map<*, *> -> value
                is org.mozilla.javascript.Scriptable -> {
                    val m = LinkedHashMap<String, Any>()
                    for (id in value.ids) {
                        val k = id.toString()
                        val v = value.get(k, value)
                        if (v != org.mozilla.javascript.Scriptable.NOT_FOUND && v != null) m[k] = v
                    }
                    m
                }
                else -> mapOf("type" to value.toString())
            }
        }
        schemaMap.clear()
        schemaMap.putAll(normalized)
        data["__schema"] = normalized
        save()
    }

    /** Схема для ModuleConfigGui (редактируемая копия). */
    fun getSchema(): Map<String, Any> = LinkedHashMap(schemaMap)

    /** Открыть меню настроек модуля (MoulConfig) с выделенной категорией этого модуля. */
    fun openMenu() {
        set.starlev.scripting.ModuleConfigGui.openModule(moduleName)
    }

    fun load() {
        try {
            if (dataFile.exists()) {
                val map: Map<*, *>? = gson.fromJson(dataFile.readText(Charsets.UTF_8), Map::class.java)
                data.clear()
                schemaMap.clear()
                if (map != null) {
                    for ((k, v) in map) {
                        if (k == null) continue
                        if (k == "__schema" && v is Map<*, *>) {
                            for ((sk, sv) in v) {
                                if (sk != null && sv != null) schemaMap[sk.toString()] = sv
                            }
                        } else if (v != null) {
                            data[k.toString()] = v
                        }
                    }
                }
            }
        } catch (e: Exception) {
            ScriptConsole.error(moduleName, "Не удалось загрузить config.json: ${e.message}")
        }
    }

    fun save() {
        try {
            moduleDir.mkdirs()
            val out = LinkedHashMap<String, Any?>(data)
            if (schemaMap.isNotEmpty()) out["__schema"] = schemaMap
            dataFile.writeText(gsonPretty.toJson(out), Charsets.UTF_8)
        } catch (e: Exception) {
            ScriptConsole.error(moduleName, "Не удалось сохранить config.json: ${e.message}")
        }
    }

    private fun asMap(obj: Any?): Map<String, Any>? {
        return when (obj) {
            is Map<*, *> -> obj.entries.associate { it.key.toString() to it.value!! }
            is org.mozilla.javascript.Scriptable -> {
                val ids = obj.ids
                val map = LinkedHashMap<String, Any>()
                for (id in ids) {
                    val key = id.toString()
                    val v = obj.get(key, obj)
                    if (v != org.mozilla.javascript.Scriptable.NOT_FOUND && v != null) {
                        map[key] = v
                    }
                }
                map
            }
            else -> null
        }
    }
}
