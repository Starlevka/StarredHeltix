package set.starlev.scripting

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.resources.Identifier
import set.starlev.scripting.ModuleLoader
import java.io.File
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * Динамические текстуры для JS-модулей: загрузка изображения по URL
 * (обложки альбомов, иконки, карты) в GPU с привязкой к модулю.
 *
 * ```js
 * render.loadImage("https://i.scdn.co/image/abc", "cover");
 * // в HUD/overlay:
 * ctx.drawTexture("starredheltix:module/<module>/cover", 0, 0, 64, 64);
 * render.removeImage("cover");
 * render.clearImages(); // при выгрузке модуля вызывается автоматически
 * ```
 *
 * Формат: JPEG/PNG (что умеет NativeImage.read). Всё выполняется вне рендер-потока
 * кроме финальной регистрации (client.execute). Кэш GPU-текстур чистится при unload.
 */
object SHModuleTextures {
    private const val NAMESPACE = "starredheltix"
    private const val PATH_PREFIX = "module/"
    private const val MAX_IMAGE_BYTES = 8L * 1024 * 1024

    private val texturePool = java.util.concurrent.Executors.newCachedThreadPool { r ->
        Thread(r, "SHTexture-worker").apply { isDaemon = true }
    }

    /** key: "<module>:<id>" lower — путь текстуры для быстрой проверки. */
    private val registered = ConcurrentHashMap<String, Identifier>()

    /**
     * Асинхронно скачать и загрузить изображение. Вызывается из JS (любой поток).
     * @param url http(s)-ссылка на PNG/JPEG
     * @param id локальный идентификатор внутри модуля (пере-загрузка с тем же id заменяет текстуру)
     */
    fun loadImage(moduleName: String, url: String, id: String): Boolean {
        val key = "${moduleName.lowercase()}:${id.lowercase()}"
        texturePool.submit {
            var conn: java.net.HttpURLConnection? = null
            try {
                val uri = URI(url)
                if (uri.scheme !in setOf("http", "https")) {
                    ScriptConsole.error(moduleName, "loadImage: разрешены только http/https URL")
                    return@submit
                }
                conn = uri.toURL().openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 7000
                conn.readTimeout = 15000
                conn.setRequestProperty("User-Agent", "StarredHeltix-Module")
                if (conn.responseCode !in 200..299) {
                    ScriptConsole.warn(moduleName, "loadImage('$id'): HTTP ${conn.responseCode}")
                    return@submit
                }
                val bytes = conn.inputStream.use { ins ->
                    val buf = java.io.ByteArrayOutputStream()
                    val chunk = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val n = ins.read(chunk)
                        if (n < 0) break
                        total += n
                        if (total > MAX_IMAGE_BYTES) {
                            ScriptConsole.warn(moduleName, "loadImage('$id'): изображение больше ${MAX_IMAGE_BYTES / 1024 / 1024} МБ")
                            return@use null
                        }
                        buf.write(chunk, 0, n)
                    }
                    buf.toByteArray()
                } ?: return@submit
                Minecraft.getInstance().execute {
                    registerOnClient(moduleName, id, key, bytes)
                }
            } catch (e: Exception) {
                ScriptConsole.error(moduleName, "loadImage('$id'): ${e.message}")
            } finally {
                try { conn?.disconnect() } catch (_: Exception) {}
            }
        }
        return true
    }

    /**
     * Загрузить изображение из файла в папке модуля (песочница) в GPU.
     * Формат PNG/JPEG, до 8 МБ.
     *
     * ```js
     * render.loadImageFromFile("icons/coin.png", "coin");
     * ctx.drawTexture("starredheltix:module/<module>/coin", 0, 0, 16, 16);
     * ```
     */
    fun loadImageFromFile(moduleName: String, fileName: String, id: String): Boolean {
        val file = try {
            val moduleDir = ModuleLoader.modulesDir.absoluteFile
            val f = File(moduleDir, moduleName).resolve(fileName).canonicalFile
            if (!f.path.startsWith(File(moduleDir, moduleName).canonicalFile.path)) {
                ScriptConsole.error(moduleName, "loadImageFromFile('$fileName'): доступ за пределами папки модуля запрещён")
                return false
            }
            f
        } catch (e: Exception) {
            ScriptConsole.error(moduleName, "loadImageFromFile('$fileName'): ${e.message}")
            return false
        }
        if (!file.exists() || !file.isFile) {
            ScriptConsole.error(moduleName, "loadImageFromFile('$fileName'): файл не найден")
            return false
        }
        if (file.length() > MAX_IMAGE_BYTES) {
            ScriptConsole.error(moduleName, "loadImageFromFile('$fileName'): файл больше ${MAX_IMAGE_BYTES / 1024 / 1024} МБ")
            return false
        }
        return try {
            val bytes = file.readBytes()
            val key = "${moduleName.lowercase()}:${id.lowercase()}"
            Minecraft.getInstance().execute {
                registerOnClient(moduleName, id, key, bytes)
            }
            true
        } catch (e: Exception) {
            ScriptConsole.error(moduleName, "loadImageFromFile('$fileName'): чтение не удалось: ${e.message}")
            false
        }
    }

    /** Регистрация скачанных байтов как DynamicTexture (только рендер-поток). */
    private fun registerOnClient(moduleName: String, id: String, key: String, bytes: ByteArray) {
        try {
            val textureId = Identifier.fromNamespaceAndPath(NAMESPACE, PATH_PREFIX + moduleName.lowercase() + "/" + id.lowercase())
            val tm: TextureManager = Minecraft.getInstance().textureManager

            // Замена: старую текстуру закрываем (GPU-память)
            registered[key]?.let { old ->
                tm.getTexture(old).close()
                tm.release(old)
            }

            val dynamic = DynamicTexture({ "SHModuleTexture/$key" }, com.mojang.blaze3d.platform.NativeImage.read(bytes))
            tm.register(textureId, dynamic)
            registered[key] = textureId
        } catch (e: Exception) {
            ScriptConsole.error(moduleName, "loadImage('$id'): не удалось декодировать (нужен PNG/JPEG): ${e.message}")
        }
    }

    /** Identifier текстуры модуля (для проверки существования из Kotlin). */
    fun getTextureId(moduleName: String, id: String): Identifier? = registered["${moduleName.lowercase()}:${id.lowercase()}"]

    /** Удалить текстуру модуля (GPU + реестр). */
    fun removeImage(moduleName: String, id: String) {
        val key = "${moduleName.lowercase()}:${id.lowercase()}"
        val textureId = registered.remove(key) ?: return
        Minecraft.getInstance().execute {
            try {
                val tm = Minecraft.getInstance().textureManager
                tm.getTexture(textureId).close()
                tm.release(textureId)
            } catch (_: Exception) {
            }
        }
    }

    /** Удалить ВСЕ текстуры модуля (при выгрузке). */
    fun removeForModule(moduleName: String) {
        val prefix = "${moduleName.lowercase()}:"
        registered.keys.filter { it.startsWith(prefix) }.toList().forEach { key ->
            val idPart = key.removePrefix(prefix)
            removeImage(moduleName, idPart)
        }
    }

    /** Удалить все текстуры всех модулей (выгрузка движка). */
    fun clearAll() {
        registered.keys.toList().forEach { key ->
            val module = key.substringBefore(':')
            removeImage(module, key.substringAfter(':'))
        }
    }
}
