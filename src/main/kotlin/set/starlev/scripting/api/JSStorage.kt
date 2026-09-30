package set.starlev.scripting.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import set.starlev.scripting.ScriptConsole
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * JS API: Хранилище данных модуля (`data.json`)
 */
class JSStorage(private val moduleDir: File, private val moduleName: String) {
    private val dataFile = File(moduleDir, "data.json")
    private val data = ConcurrentHashMap<String, Any>()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    init {
        load()
    }

    fun get(key: String): Any? {
        return data[key]
    }

    fun get(key: String, defaultValue: Any?): Any? {
        return data[key] ?: defaultValue
    }

    fun set(key: String, value: Any?) {
        if (value == null) {
            data.remove(key)
        } else {
            data[key] = value
        }
    }

    fun has(key: String): Boolean {
        return data.containsKey(key)
    }

    fun remove(key: String): Boolean {
        return data.remove(key) != null
    }

    fun clear() {
        data.clear()
    }

    fun load() {
        try {
            if (dataFile.exists()) {
                val json = dataFile.readText(Charsets.UTF_8)
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val map: Map<String, Any>? = gson.fromJson(json, type)
                if (map != null) {
                    data.clear()
                    data.putAll(map)
                }
            }
        } catch (e: Exception) {
            ScriptConsole.error(moduleName, "Не удалось загрузить data.json: ${e.message}")
        }
    }

    fun save() {
        try {
            moduleDir.mkdirs()
            val json = gson.toJson(data)
            dataFile.writeText(json, Charsets.UTF_8)
        } catch (e: Exception) {
            ScriptConsole.error(moduleName, "Не удалось сохранить data.json: ${e.message}")
        }
    }
}
