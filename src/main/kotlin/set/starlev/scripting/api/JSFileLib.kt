package set.starlev.scripting.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import set.starlev.scripting.ModuleLoader
import set.starlev.scripting.ScriptConsole
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64

/**
 * JS API: Файловый IO в папке модуля + HTTP-запросы.
 *
 * Все операции ограничены папкой модуля `config/starredheltix/modules/<модуль>/`
 * (sandbox): путь не может выйти за её пределы через `..`.
 */
class JSFileLib(private val moduleDir: File, private val moduleName: String) {
    private val gsonPretty: Gson = GsonBuilder().setPrettyPrinting().create()
    private val gson: Gson = Gson()

    private fun resolve(fileName: String): File {
        // Джейлимся по РЕАЛЬНОЙ папке модуля (moduleDir), а не по имени:
        // metadata.name может не совпадать с именем каталога. Проверка — с завершающим
        // разделителем, иначе модуль «chat» дотянулся бы до «modules/chat-extra/**».
        val jail = moduleDir.absoluteFile.canonicalFile
        val f = File(jail, fileName).canonicalFile
        if (!(f.path == jail.path || f.path.startsWith(jail.path + File.separator))) {
            throw SecurityException("Доступ за пределами папки модуля запрещён: $fileName")
        }
        return f
    }

    /** Записать файл в папку модуля (папки создаются автоматически). */
    fun write(fileName: String, content: String): Boolean {
        return try {
            val f = resolve(fileName)
            f.parentFile?.mkdirs()
            f.writeText(content, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            ScriptConsole.error(moduleName, "file.write('$fileName'): ${e.message}", e)
            false
        }
    }

    /** Дописать в конец файла. */
    fun append(fileName: String, content: String): Boolean {
        return try {
            val f = resolve(fileName)
            f.parentFile?.mkdirs()
            f.appendText(content, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            ScriptConsole.error(moduleName, "file.append('$fileName'): ${e.message}", e)
            false
        }
    }

    /** Прочитать файл модуля (null, если не найден). */
    fun read(fileName: String): String? {
        return try {
            val f = resolve(fileName)
            if (!f.exists()) null else f.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            ScriptConsole.error(moduleName, "file.read('$fileName'): ${e.message}", e)
            null
        }
    }

    fun exists(fileName: String): Boolean = try { resolve(fileName).exists() } catch (e: Exception) { false }

    fun delete(fileName: String): Boolean = try { resolve(fileName).delete() } catch (e: Exception) { false }

    /** Список файлов в подпапке модуля (относительные имена). */
    fun list(dirName: String = ""): List<String> {
        return try {
            val dir = resolve(dirName)
            if (!dir.isDirectory) emptyList() else dir.listFiles()?.map { it.name } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==================== JSON-хранилище ====================

    /** Прочитать JSON-файл модуля как JS-объект (Map/List/primitive). */
    fun readJson(fileName: String): Any? {
        val text = read(fileName) ?: return null
        return try { gson.fromJson(text, Any::class.java) } catch (e: Exception) { null }
    }

    /** Записать JS-объект в JSON-файл (pretty). */
    fun writeJson(fileName: String, data: Any?): Boolean =
        write(fileName, gsonPretty.toJson(data ?: emptyMap<String, Any?>()))

    // ==================== HTTP ====================

    /**
     * GET-запрос, возвращает тело ответа строкой (или null при ошибке).
     * Поддерживаются только http/https. Ответ ограничен 8 МБ.
     */
    fun getUrlContent(url: String): String? {
        return try {
            val uri = URI(url)
            if (uri.scheme !in setOf("http", "https")) {
                throw SecurityException("Разрешены только http/https URL")
            }
            val conn = uri.toURL().openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 15000
            conn.setRequestProperty("User-Agent", "StarredHeltix-Module")
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.use { JSNet.readLimited(it) }
        } catch (e: Exception) {
            ScriptConsole.error(moduleName, "file.getUrlContent: ${e.message}", e)
            null
        }
    }

    // ==================== Кодирование ====================

    fun encodeBase64(text: String): String =
        Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))

    fun decodeBase64(text: String): String? =
        try { String(Base64.getDecoder().decode(text), Charsets.UTF_8) } catch (e: Exception) { null }
}
