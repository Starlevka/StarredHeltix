package set.starlev.utils

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import set.starlev.StarredHeltix
import set.starlev.utils.detectors.LocationDetector
import java.io.*
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Discord Rich Presence
 * Application ID: 1415296884267941969
 */
object DiscordRPC {
    private const val APP_ID = "1415296884267941969"
    private var ipc: DiscordIPC? = null
    private var isInitialized = false
    private var lastLocation: String? = null
    private var lastUpdate = 0L
    private var startTimestamp: Long = System.currentTimeMillis() / 1000

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register {
            val cfg = StarredHeltix.feature.about.discordRPC
            if (!cfg.enabled) {
                if (isInitialized) shutdown()
                return@register
            }
            if (!isInitialized) {
                tryInit()
            }
            if (isInitialized) {
                val now = System.currentTimeMillis()
                val loc = LocationDetector.getCurrentLocationClean()
                val changed = loc != lastLocation
                if (changed || now - lastUpdate > 15_000L) {
                    lastLocation = loc
                    lastUpdate = now
                    updatePresence()
                }
                // Читаем ответы Discord чтобы не забить буфер
                try { ipc?.readAny() } catch (_: Exception) {}
            }
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register {
            shutdown()
        }
    }

    private fun tryInit() {
        try {
            val newIpc = DiscordIPC(APP_ID)
            newIpc.connect()
            ipc = newIpc
            isInitialized = true
            startTimestamp = System.currentTimeMillis() / 1000
            updatePresence()
        } catch (e: Exception) {
            StarredHeltix.LOGGER.warn("[DiscordRPC] Init failed (Discord не запущен?): ${e.message}")
            isInitialized = false
            ipc = null
        }
    }

    private fun updatePresence() {
        val cfg = StarredHeltix.feature.about.discordRPC
        if (!cfg.enabled || ipc == null) return
        try {
            val mc = Minecraft.getInstance()
            val version = StarredHeltix.modVersion
            val location = LocationDetector.getCurrentLocationClean()
            val details = if (cfg.showVersion) "StarredHeltix v$version" else "Heltix Skyblock"
            val state = buildString {
                if (cfg.showLocation && !location.isNullOrBlank()) {
                    append(location.take(60))
                } else {
                    append("В игре")
                }
                if (cfg.showIP) {
                    if (isNotEmpty()) append(" • ")
                    append("starlev.heltix.net")
                }
            }.take(128)

            val finalDetails: String
            val finalState: String
            if (mc.level == null || mc.player == null) {
                finalDetails = details
                finalState = "В меню"
            } else {
                finalDetails = details
                finalState = state
            }

            ipc?.setActivity(
                details = finalDetails,
                state = finalState,
                largeImageKey = "icon",
                largeImageText = "starlev.heltix.net • StarredHeltix",
                smallImageKey = "star",
                smallImageText = "StarredHeltix v$version",
                startTimestamp = startTimestamp
            )
        } catch (e: Exception) {
            StarredHeltix.LOGGER.warn("[DiscordRPC] Update failed: ${e.message}")
            // Если отвалился - пробуем переподключиться в следующий тик
            if (e.message?.contains("Broken pipe") == true || e.message?.contains("Connection") == true) {
                shutdown()
            }
        }
    }

    fun shutdown() {
        if (!isInitialized && ipc == null) return
        try {
            ipc?.close()
        } catch (_: Exception) {}
        ipc = null
        isInitialized = false
        lastLocation = null
    }

    fun forceUpdate() {
        if (StarredHeltix.feature.about.discordRPC.enabled) {
            if (!isInitialized) tryInit() else updatePresence()
        } else {
            shutdown()
        }
    }

    /**
     * Минимальный Discord IPC клиент - pipe/socket без JNA.
     * Работает на Windows (\\.\pipe\discord-ipc-N) и Linux/Mac (Unix socket).
     */
    private class DiscordIPC(private val clientId: String) {
        private var channel: SocketChannel? = null
        private var raf: RandomAccessFile? = null
        private var isWindowsPipe = false
        private val gson = Gson()

        fun connect() {
            // Пробуем 0..9
            for (i in 0..9) {
                if (tryConnect(i)) {
                    // Handshake opcode 0
                    val handshake = JsonObject().apply {
                        addProperty("v", 1)
                        addProperty("client_id", clientId)
                    }
                    send(0, handshake.toString())
                    // Ждём READY (opcode 1)
                    Thread.sleep(200)
                    readAny()
                    return
                }
            }
            throw IOException("Discord IPC not found (Discord не запущен?)")
        }

        private fun tryConnect(index: Int): Boolean {
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            return if (isWindows) {
                try {
                    val pipePath = "\\\\.\\pipe\\discord-ipc-$index"
                    raf = RandomAccessFile(pipePath, "rw")
                    isWindowsPipe = true
                    true
                } catch (_: Exception) {
                    false
                }
            } else {
                val tmpDir = System.getenv("XDG_RUNTIME_DIR")
                    ?: System.getenv("TMPDIR")
                    ?: System.getProperty("java.io.tmpdir")
                    ?: "/tmp"
                val paths = listOf(
                    Paths.get(tmpDir, "discord-ipc-$index"),
                    Paths.get("/tmp/discord-ipc-$index"),
                    Paths.get(System.getProperty("user.home"), ".discord-ipc-$index")
                )
                for (p in paths) {
                    try {
                        if (!Files.exists(p)) continue
                        val addr = UnixDomainSocketAddress.of(p)
                        val ch = SocketChannel.open(addr)
                        ch.configureBlocking(false)
                        channel = ch
                        isWindowsPipe = false
                        return true
                    } catch (_: Exception) {
                        continue
                    }
                }
                false
            }
        }

        fun setActivity(
            details: String,
            state: String,
            largeImageKey: String,
            largeImageText: String,
            smallImageKey: String,
            smallImageText: String,
            startTimestamp: Long
        ) {
            val activity = JsonObject().apply {
                addProperty("details", details)
                addProperty("state", state)
                add("timestamps", JsonObject().apply {
                    addProperty("start", startTimestamp)
                })
                add("assets", JsonObject().apply {
                    addProperty("large_image", largeImageKey)
                    addProperty("large_text", largeImageText)
                    if (smallImageKey.isNotEmpty()) {
                        addProperty("small_image", smallImageKey)
                        addProperty("small_text", smallImageText)
                    }
                })
            }
            val args = JsonObject().apply {
                addProperty("pid", ProcessHandle.current().pid())
                add("activity", activity)
            }
            val payload = JsonObject().apply {
                addProperty("cmd", "SET_ACTIVITY")
                add("args", args)
                addProperty("nonce", System.currentTimeMillis().toString())
            }
            send(1, payload.toString())
        }

        fun clearActivity() {
            val payload = JsonObject().apply {
                addProperty("cmd", "SET_ACTIVITY")
                add("args", JsonObject().apply {
                    addProperty("pid", ProcessHandle.current().pid())
                })
                addProperty("nonce", System.currentTimeMillis().toString())
            }
            send(1, payload.toString())
        }

        private fun send(opcode: Int, data: String) {
            val bytes = data.toByteArray(StandardCharsets.UTF_8)
            val buffer = ByteBuffer.allocate(8 + bytes.size).order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(opcode)
            buffer.putInt(bytes.size)
            buffer.put(bytes)
            buffer.flip()
            if (isWindowsPipe) {
                raf?.write(buffer.array())
            } else {
                channel?.let {
                    while (buffer.hasRemaining()) it.write(buffer)
                }
            }
        }

        fun readAny(): String? {
            return try {
                if (isWindowsPipe) {
                    val raf = this.raf ?: return null
                    if (raf.length() == 0L) return null
                    // Windows pipe не даёт селект - пробуем неблокирующее чтение
                    return null
                } else {
                    val ch = channel ?: return null
                    val header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                    val read = ch.read(header)
                    if (read <= 0) return null
                    if (header.position() < 8) return null
                    header.flip()
                    val opcode = header.int
                    val len = header.int
                    if (len <= 0 || len > 8192) return null
                    val body = ByteBuffer.allocate(len)
                    var total = 0
                    while (total < len) {
                        val r = ch.read(body)
                        if (r <= 0) break
                        total += r
                    }
                    if (total < len) return null
                    String(body.array(), StandardCharsets.UTF_8)
                }
            } catch (_: Exception) {
                null
            }
        }

        fun close() {
            try {
                // Очищаем presence
                try { clearActivity() } catch (_: Exception) {}
                raf?.close()
                channel?.close()
            } catch (_: Exception) {}
            raf = null
            channel = null
        }
    }
}
