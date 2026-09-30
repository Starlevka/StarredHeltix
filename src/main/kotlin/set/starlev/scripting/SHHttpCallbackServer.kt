package set.starlev.scripting

import org.mozilla.javascript.Callable
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Локальный HTTP-сервер для OAuth-колбэков JS-модулей (net.listen).
 *
 * Слушает ТОЛЬКО loopback (127.0.0.1) — запросы извне не принимаются.
 * Каждый запрос выполняется в своём потоке; JS-колбэк получает (path, query, headers)
 * и возвращает тело ответа (HTML/текст). Тело запроса игнорируется.
 *
 * Жизненный цикл: start() из JSNet.listen, stop() при выгрузке модуля/движка.
 */
class SHHttpCallbackServer private constructor(
    private val moduleName: String,
    private val port: Int,
    private val callback: Callable
) {
    private val running = AtomicBoolean(true)
    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        acceptThread?.interrupt()
    }

    private fun loop() {
        while (running.get()) {
            val socket = try {
                serverSocket?.accept() ?: break
            } catch (e: SocketTimeoutException) {
                continue
            } catch (_: Exception) {
                break // сокет закрыт
            }
            if (!running.get()) {
                try { socket.close() } catch (_: Exception) {}
                break
            }
            handleClient(socket)
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 5000
            // Ответ шлём только loopback-клиентам (сервер и так привязан к 127.0.0.1)
            socket.use { sock ->
                val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))
                val requestLine = reader.readLine() ?: return
                val headers = HashMap<String, String>()
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) break
                    val idx = line.indexOf(':')
                    if (idx > 0) headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
                }

                // "GET /callback?code=x&state=y HTTP/1.1"
                val parts = requestLine.split(" ")
                val rawPath = if (parts.size >= 2) parts[1] else "/"
                val path: String
                val query: HashMap<String, String>
                val qIndex = rawPath.indexOf('?')
                if (qIndex >= 0) {
                    path = rawPath.substring(0, qIndex)
                    query = HashMap()
                    for (pair in rawPath.substring(qIndex + 1).split('&')) {
                        if (pair.isBlank()) continue
                        val kv = pair.split('=', limit = 2)
                        val k = java.net.URLDecoder.decode(kv[0], Charsets.UTF_8)
                        val v = if (kv.size > 1) java.net.URLDecoder.decode(kv[1], Charsets.UTF_8) else ""
                        query[k] = v
                    }
                } else {
                    path = rawPath
                    query = HashMap()
                }

                // Колбэк JS — в Rhino-контексте НА КЛИЕНТСКОМ ПОТОКЕ (модуль может трогать client/игрока).
                // Accept-тред ждёт ответ через latch с таймаутом (это не netty, блокировать можно).
                val body: String = try {
                    val holder = java.util.concurrent.atomic.AtomicReference<String>()
                    val latch = java.util.concurrent.CountDownLatch(1)
                    net.minecraft.client.Minecraft.getInstance().execute {
                        set.starlev.scripting.SHContextFactory.beginTracking()
                        try {
                            val result = SHScriptEngine.withContext { cx ->
                                callback.call(
                                    cx, SHScriptEngine.globalScope, SHScriptEngine.globalScope,
                                    arrayOf(path, query, headers)
                                )
                            }
                            holder.set(result?.toString() ?: "OK")
                        } catch (e: Throwable) {
                            ScriptConsole.error(moduleName, "Ошибка в net.listen колбэке: ${e.message}", e)
                            holder.set("<h2>StarredHeltix: ошибка обработки колбэка (см. лог)</h2>")
                        } finally {
                            set.starlev.scripting.SHContextFactory.endTracking()
                            latch.countDown()
                        }
                    }
                    if (!latch.await(8, java.util.concurrent.TimeUnit.SECONDS)) {
                        ScriptConsole.error(moduleName, "net.listen: колбэк не ответил за 8с")
                        "<h2>StarredHeltix: таймаут обработки колбэка</h2>"
                    } else (holder.get() ?: "OK")
                } catch (e: Throwable) {
                    ScriptConsole.error(moduleName, "Ошибка в net.listen колбэке: ${e.message}", e)
                    "<h2>StarredHeltix: ошибка обработки колбэка (см. лог)</h2>"
                }

                val payload = body.toByteArray(Charsets.UTF_8)
                val response = buildString {
                    append("HTTP/1.1 200 OK\r\n")
                    append("Content-Type: text/html; charset=utf-8\r\n")
                    append("Content-Length: ").append(payload.size).append("\r\n")
                    append("Connection: close\r\n")
                    append("\r\n")
                }.toByteArray(Charsets.UTF_8)
                sock.getOutputStream().apply {
                    write(response)
                    write(payload)
                    flush()
                }
            }
        } catch (_: Exception) {
        }
    }

    companion object {
        /**
         * Запустить сервер. null — порт занят.
         */
        fun start(moduleName: String, port: Int, callback: Callable): SHHttpCallbackServer? {
            val socket = try {
                ServerSocket(port, 16, InetAddress.getLoopbackAddress())
            } catch (e: Exception) {
                return null
            }
            val server = SHHttpCallbackServer(moduleName, port, callback)
            server.serverSocket = socket
            server.acceptThread = Thread({ server.loop() }, "SHHttpListen-$moduleName").apply {
                isDaemon = true
                start()
            }
            return server
        }
    }
}
