package set.starlev.scripting.api

import com.google.gson.Gson
import org.mozilla.javascript.Scriptable
import set.starlev.scripting.SHHttpCallbackServer
import set.starlev.scripting.ScriptConsole
import set.starlev.scripting.SHScriptEngine
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/**
 * JS API: Объект `net` — HTTP-запросы для модулей.
 *
 * Расширение file.getUrlContent: заголовки, POST, JSON, код ответа.
 *
 * ```js
 * // GET с заголовками
 * var r = net.get("https://api.example.com/data", { "Authorization": "Bearer TOKEN" });
 * if (r.isOk()) { var data = JSON.parse(r.getBody()); }
 *
 * // POST (x-www-form-urlencoded)
 * var resp = net.post("https://accounts.spotify.com/api/token", net.encodeForm({
 *     grant_type: "refresh_token", refresh_token: token, client_id: id
 * }));
 *
 * // POST JSON + парсинг ответа
 * var out = net.postJson("https://api.example.com/api", { hello: "world" });
 *
 * // Асинхронно (не блокирует поток вызова)
 * net.getAsync("https://example.com", function(resp) { ... });
 * ```
 *
 * Синхронные методы БЛОКИРУЮТ поток вызова — из tick/chat-триггеров используйте
 * асинхронные варианты или обёртку setTimeout.
 */
class JSNet(private val moduleName: String) {

    /** Ответ HTTP: статус, тело, заголовки. Класс доверенный (пакет scripting.api). */
    class Response internal constructor(
        val status: Int,
        val body: String,
        val headers: Map<String, String>
    ) {
        fun isOk(): Boolean = status in 200..299
        fun isNotFound(): Boolean = status == 404
        fun isUnauthorized(): Boolean = status == 401
        fun getHeader(name: String): String? = headers[name.lowercase()]
        override fun toString(): String = "HttpResponse{status=$status, body=${body.take(120)}}"
    }

    private val gson = Gson()

    // ==================== Локальный HTTP-сервер (OAuth-колбэки) ====================

    /**
     * Запустить локальный HTTP-сервер на порту [port] для OAuth-колбэков.
     * На каждый запрос вызывается [callback](path, queryMap, headers) — верните
     * строку (HTML/текст) для ответа браузеру.
     *
     * Сервер живёт до выгрузки модуля. Повторный вызов перезапускает сервер.
     * Слушает только 127.0.0.1 (наружу не торчит). Тело запросов игнорируется (GET).
     *
     * ```js
     * net.listen(9876, function(path, query, headers) {
     *     if (query.code) { /* OAuth-колбэк */ }
     *     return "<h2>Можно закрыть вкладку</h2>";
     * });
     * net.stopListen();
     * ```
     * @return true, если сервер запущен
     */
    fun listen(port: Int, callback: org.mozilla.javascript.Callable): Boolean {
        stopListen()
        if (port !in 1024..65535) {
            ScriptConsole.error(moduleName, "net.listen: порт должен быть 1024-65535, получен $port")
            return false
        }
        val server = SHHttpCallbackServer.start(moduleName, port, callback) ?: run {
            ScriptConsole.error(moduleName, "net.listen: порт $port занят")
            return false
        }
        synchronized(servers) { servers[moduleName.lowercase()] = server }
        ScriptConsole.info(moduleName, "net.listen: локальный сервер запущен на http://127.0.0.1:$port/")
        return true
    }

    /** Остановить локальный сервер модуля (если запущен). */
    fun stopListen() {
        synchronized(servers) { servers.remove(moduleName.lowercase()) }?.stop()
    }

    companion object {
        /** Лимит тела ответа: кривой модуль не должен выесть ОЗУ огромным файлом. */
        const val MAX_RESPONSE_BYTES = 8L * 1024 * 1024

        /** Чтение InputStream с лимитом MAX_RESPONSE_BYTES (защита ОЗУ). */
        internal fun readLimited(ins: java.io.InputStream): String {
            val buf = java.io.ByteArrayOutputStream()
            val chunk = ByteArray(8192)
            var total = 0L
            while (true) {
                val n = ins.read(chunk)
                if (n < 0) break
                total += n
                if (total > MAX_RESPONSE_BYTES) {
                    throw SecurityException("Ответ превышает лимит ${MAX_RESPONSE_BYTES / 1024 / 1024} МБ")
                }
                buf.write(chunk, 0, n)
            }
            return buf.toString("UTF-8")
        }

        /** moduleName -> активный локальный HTTP-сервер. */
        private val servers = ConcurrentHashMap<String, SHHttpCallbackServer>()

        // Переиспользуемый daemon-пул для всех net.getAsync — без утечки потока на вызов
        private val netAsyncPool = java.util.concurrent.Executors.newCachedThreadPool { r ->
            Thread(r, "SHNet-async").apply { isDaemon = true }
        }

        /** Остановить сервер конкретного модуля (при выгрузке). */
        fun stopForModule(moduleName: String) {
            synchronized(servers) { servers.remove(moduleName.lowercase()) }?.stop()
        }

        /** Остановить все серверы (выгрузка движка). */
        fun stopAll() {
            synchronized(servers) { servers.values.toList() }.forEach { it.stop() }
            synchronized(servers) { servers.clear() }
        }
    }

    // ==================== Синхронные ====================

    /** GET-запрос. headers — JS-объект { "Имя": "Значение" } или null. */
    fun get(url: String, headers: Scriptable? = null): Response =
        request("GET", url, null, null, headers) ?: Response(-1, "", emptyMap())

    /** GET-запрос с парсингом JSON-ответа в JS-объект (Map/List). null при ошибке или не-JSON. */
    fun getJson(url: String, headers: Scriptable? = null): Any? {
        val r = get(url, headers)
        if (!r.isOk()) return null
        return parseJson(r.body)
    }

    /** POST с телом (строкой). По умолчанию x-www-form-urlencoded. */
    fun post(url: String, body: String, headers: Scriptable? = null, contentType: String = "application/x-www-form-urlencoded"): Response =
        request("POST", url, body, contentType, headers) ?: Response(-1, "", emptyMap())

    /** POST JSON-объекта; ответ парсится в JS-объект. */
    fun postJson(url: String, data: Any?, headers: Scriptable? = null): Any? {
        val r = request("POST", url, gson.toJson(data ?: emptyMap<String, Any?>()), "application/json; charset=utf-8", headers)
            ?: return null
        if (!r.isOk()) return null
        return parseJson(r.body)
    }

    /** PUT/DELETE/PATCH: request("PUT", url, body, contentType, headers). */
    fun request(method: String, url: String, body: String? = null, contentType: String? = null, headers: Scriptable? = null): Response? {
        return try {
            val conn = open(url, method, headers)
            if (contentType != null && body != null) {
                conn.setRequestProperty("Content-Type", contentType)
            }
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            }
            readResponse(conn)
        } catch (e: Exception) {
            ScriptConsole.error(moduleName, "net.$method('$url'): ${e.message}")
            null
        }
    }

    // ==================== Асинхронные ====================

    /** Асинхронный GET: колбэк вызывается в фоновом потоке с Response. */
    fun getAsync(url: String, callback: org.mozilla.javascript.Callable, headers: Scriptable? = null) {
        netAsyncPool.submit {
            val r = try { request("GET", url, null, null, headers) } catch (e: Exception) { null }
            callCallback(callback, r)
        }
    }

    // ==================== Хелперы ====================

    /** Кодировать JS-объект в x-www-form-urlencoded тело (для OAuth и подобных API). */
    fun encodeForm(data: Scriptable?): String {
        if (data == null) return ""
        return data.ids.mapNotNull { keyObj ->
            val key = keyObj.toString()
            val value = data.get(key, data)
            if (value == null || value == org.mozilla.javascript.Scriptable.NOT_FOUND) null
            else "${urlEncode(key)}=${urlEncode(jsToString(value))}"
        }.joinToString("&")
    }

    private fun parseJson(text: String): Any? {
        return try {
            gson.fromJson(text, Any::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun jsToString(value: Any?): String {
        return when (value) {
            null -> ""
            is String -> value
            is CharSequence -> value.toString()
            is Number, is Boolean -> value.toString()
            else -> try {
                org.mozilla.javascript.Context.jsToJava(value, String::class.java) as? String ?: value.toString()
            } catch (e: Exception) {
                value.toString()
            }
        }
    }

    private fun urlEncode(s: String): String =
        URLEncoder.encode(s, StandardCharsets.UTF_8)

    private fun headersToMap(headers: Scriptable?): Map<String, String> {
        if (headers == null) return emptyMap()
        val map = LinkedHashMap<String, String>()
        for (id in headers.ids) {
            val v = headers.get(id.toString(), headers)
            if (v != null && v != org.mozilla.javascript.Scriptable.NOT_FOUND) {
                map[id.toString()] = jsToString(v)
            }
        }
        return map
    }

    private fun open(urlString: String, method: String, headers: Scriptable?): HttpURLConnection {
        val uri = URI(urlString)
        if (uri.scheme !in setOf("http", "https")) {
            throw SecurityException("Разрешены только http/https URL")
        }
        val conn = uri.toURL().openConnection() as HttpURLConnection
        conn.requestMethod = method.uppercase()
        conn.connectTimeout = 7000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", "StarredHeltix-Module")
        headersToMap(headers).forEach { (k, v) -> conn.setRequestProperty(k, v) }
        return conn
    }

    private fun readResponse(conn: HttpURLConnection): Response {
        val status = conn.responseCode
        val headerMap = conn.headerFields
            .filter { it.key != null }
            .mapValues { it.value.joinToString(", ") }
        val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.use { ins -> readLimited(ins) } ?: ""
        conn.disconnect()
        return Response(status, text, headerMap)
    }

    private fun callCallback(callback: org.mozilla.javascript.Callable, response: Response?) {
        // HTTP-колбэк приходит с SHNet-пула — маршалим в клиентский поток под watchdog.
        net.minecraft.client.Minecraft.getInstance().execute {
            set.starlev.scripting.SHContextFactory.beginTracking()
            try {
                SHScriptEngine.withContext { cx ->
                    callback.call(cx, SHScriptEngine.globalScope, SHScriptEngine.globalScope, arrayOf(response))
                }
            } catch (e: Throwable) {
                ScriptConsole.error(moduleName, "Ошибка в net-колбэке: ${e.message}", e)
            } finally {
                set.starlev.scripting.SHContextFactory.endTracking()
            }
        }
    }
}
