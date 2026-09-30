package set.starlev.scripting

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File

/**
 * Метаданные модуля StarredHeltix (metadata.json).
 */
data class ModuleMetadata(
    @SerializedName("name")
    val name: String,

    @SerializedName("version")
    val version: String = "1.0.0",

    @SerializedName("author")
    val author: String = "Unknown",

    @SerializedName("description")
    val description: String = "",

    @SerializedName("entry")
    val entry: String = "index.js",

    @SerializedName("requires")
    val requires: List<String> = emptyList(),

    /** Модуль-библиотека: сам по себе ничего не делает, даёт exports другим модулям через require("имя"). */
    @SerializedName("library")
    val library: Boolean = false,

    @SerializedName("minModVersion")
    val minModVersion: String? = null,

    @SerializedName("ignored")
    val ignored: Boolean = false
) {
    companion object {
        private val gson = Gson()

        /**
         * Gson обходит Kotlin-конструктор: поля, отсутствующие в JSON, становятся
         * null вместо значений по умолчанию. Нормализуем вручную.
         */
        fun normalize(raw: ModuleMetadata?): ModuleMetadata? {
            if (raw == null) return null
            val name = (raw.name as String?) ?: return null
            if (name.isBlank()) return null
            return raw.copy(
                version = (raw.version as String?) ?: "1.0.0",
                author = (raw.author as String?) ?: "Unknown",
                description = (raw.description as String?) ?: "",
                entry = (raw.entry as String?) ?: "index.js",
                requires = (raw.requires as List<String>?) ?: emptyList()
            )
        }

        fun fromJson(json: String): ModuleMetadata? {
            return try {
                normalize(gson.fromJson(json, ModuleMetadata::class.java))
            } catch (e: Exception) {
                ScriptConsole.error("Loader", "Ошибка парсинга metadata.json: ${e.message}")
                null
            }
        }

        fun fromFile(file: File): ModuleMetadata? {
            return try {
                if (!file.exists()) return null
                // normalize внутри отклонит metadata.json без имени — загрузчик возьмёт имя папки
                fromJson(file.readText(Charsets.UTF_8))
            } catch (e: Exception) {
                ScriptConsole.error("Loader", "Ошибка парсинга metadata.json в ${file.parentFile.name}: ${e.message}")
                null
            }
        }
    }
}
