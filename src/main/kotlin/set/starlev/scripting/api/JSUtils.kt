package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import org.mozilla.javascript.Callable
import set.starlev.scripting.SHScriptEngine
import set.starlev.scripting.ScriptConsole
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * JS API: Утилиты, таймеры и вспомогательные функции.
 * Таймеры привязаны к модулю: при выгрузке модуля его таймеры автоматически очищаются
 * (иначе живой setInterval через storage.save() мог пересоздать удалённую папку модуля).
 */
class JSUtils(private val moduleName: String) {
    private val client: Minecraft get() = Minecraft.getInstance()

    fun playSound(name: String, volume: Float = 1.0f, pitch: Float = 1.0f) {
        client.execute {
            val player = client.player ?: return@execute
            val soundLoc = Identifier.tryParse(name) ?: return@execute
            val soundEvent = BuiltInRegistries.SOUND_EVENT.getOptional(soundLoc).orElse(null) ?: return@execute
            client.level?.playSound(player, player.x, player.y, player.z, soundEvent, SoundSource.PLAYERS, volume, pitch)
        }
    }

    fun copyToClipboard(text: String) {
        client.execute {
            client.keyboardHandler.setClipboard(text)
        }
    }

    fun getClipboard(): String {
        return client.keyboardHandler.clipboard
    }

    fun getFps(): Int {
        return Minecraft.getInstance().fps
    }

    fun getSystemTime(): Long {
        return System.currentTimeMillis()
    }

    fun formatNumber(number: Double): String {
        return String.format("%,.1f", number)
    }

    fun formatInteger(number: Long): String {
        return String.format("%,d", number)
    }

    /** SHA-256 хеш строки в hex (для OAuth PKCE, подписей API и т.п.). */
    fun sha256Hex(text: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /** SHA-256 хеш в base64url (именно этот формат требует OAuth 2.0 PKCE code_challenge). */
    fun sha256Base64Url(text: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    /** Случайная строка [length] символов из A-Z/a-z/0-9-_ (для генерации code_verifier). */
    fun randomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        return (1..length.coerceIn(1, 512)).map { chars.random() }.joinToString("")
    }

    // ==================== Настройки производительности (графика) ====================

    /** Текущая дальность прорисовки (в чанках). */
    fun getRenderDistance(): Int = client.options.renderDistance().get()

    fun setRenderDistance(dist: Int) {
        client.execute { client.options.renderDistance().set(dist.coerceIn(2, 32)) }
    }

    /** Режим облаков: "off" | "fast" | "fancy". */
    fun getCloudsMode(): String = client.options.cloudStatus().get().name.lowercase()

    fun setCloudsMode(mode: String) {
        val status = when (mode.lowercase()) {
            "off", "false" -> net.minecraft.client.CloudStatus.OFF
            "fast" -> net.minecraft.client.CloudStatus.FAST
            "fancy", "true" -> net.minecraft.client.CloudStatus.FANCY
            else -> return
        }
        client.execute { client.options.cloudStatus().set(status) }
    }

    /** Радиус прорисовки облаков (в чанках, 2-128). */
    fun getCloudRange(): Int = client.options.cloudRange().get()

    fun setCloudRange(range: Int) {
        client.execute { client.options.cloudRange().set(range.coerceIn(2, 128)) }
    }

    /** Частицы: "all" | "decreased" | "minimal". */
    fun getParticleStatus(): String = client.options.particles().get().name.lowercase()

    fun setParticleStatus(mode: String) {
        val status = when (mode.lowercase()) {
            "all" -> net.minecraft.server.level.ParticleStatus.ALL
            "decreased" -> net.minecraft.server.level.ParticleStatus.DECREASED
            "minimal" -> net.minecraft.server.level.ParticleStatus.MINIMAL
            else -> return
        }
        client.execute { client.options.particles().set(status) }
    }

    fun isEntityShadows(): Boolean = client.options.entityShadows().get()

    fun setEntityShadows(enabled: Boolean) {
        client.execute { client.options.entityShadows().set(enabled) }
    }

    // ==================== Таймеры (привязаны к модулю) ====================

    fun setTimeout(callable: Callable, delayMs: Long = 0): Int =
        schedule(callable, delayMs, null, moduleName)

    fun setInterval(callable: Callable, periodMs: Long = 0): Int =
        schedule(callable, 0L, periodMs, moduleName)

    fun clearTimeout(id: Int) {
        clearTimer(id)
    }

    fun clearInterval(id: Int) {
        clearTimer(id)
    }

    companion object {
        // Daemon-потоки: иначе пул не даёт JVM завершиться после закрытия окна
        // (26.2 ClientShutdownWatchdog крашит игру "Client shutdown from post-main").
        @Volatile
        private var scheduler = newScheduler()

        private fun newScheduler() = Executors.newScheduledThreadPool(2) { runnable ->
            Thread(runnable, "StarredHeltix-JSTimer").apply { isDaemon = true }
        }

        private fun executor(): java.util.concurrent.ScheduledExecutorService {
            // После shutdownScheduler() (закрытие игры / reload) пересоздаём пул
            if (scheduler.isShutdown) scheduler = newScheduler()
            return scheduler
        }

        private val timerCounter = AtomicInteger(0)
        private val runningTimers = ConcurrentHashMap<Int, ScheduledFuture<*>>()
        /** timerId -> moduleName, чтобы уметь чистить таймеры конкретного модуля. */
        private val timerOwners = ConcurrentHashMap<Int, String>()

        private fun schedule(callable: Callable, delayMs: Long, periodMs: Long?, ownerModule: String): Int {
            val id = timerCounter.incrementAndGet()
            timerOwners[id] = ownerModule
            val scheduler = executor()
            // Колбэки таймеров исполняются в КЛИЕНТСКОМ потоке (модули трогают player/chat/storage),
            // под watchdog (beginTracking/endTracking), как и fireTrigger. Пул-тред только планирует.
            if (periodMs != null) {
                val future = scheduler.scheduleAtFixedRate({
                    Minecraft.getInstance().execute {
                        set.starlev.scripting.SHContextFactory.beginTracking()
                        try {
                            SHScriptEngine.withContext { cx ->
                                callable.call(cx, SHScriptEngine.globalScope, SHScriptEngine.globalScope, emptyArray())
                            }
                        } catch (e: Throwable) {
                            ScriptConsole.error(ownerModule, "Ошибка в setInterval: ${e.message}", e)
                            clearTimer(id)
                        } finally {
                            set.starlev.scripting.SHContextFactory.endTracking()
                        }
                    }
                }, periodMs, periodMs, TimeUnit.MILLISECONDS)
                runningTimers[id] = future
                return id
            } else {
                val future = scheduler.schedule({
                    Minecraft.getInstance().execute {
                        set.starlev.scripting.SHContextFactory.beginTracking()
                        try {
                            SHScriptEngine.withContext { cx ->
                                callable.call(cx, SHScriptEngine.globalScope, SHScriptEngine.globalScope, emptyArray())
                            }
                        } finally {
                            set.starlev.scripting.SHContextFactory.endTracking()
                            runningTimers.remove(id)
                            timerOwners.remove(id)
                        }
                    }
                }, delayMs, TimeUnit.MILLISECONDS)
                runningTimers[id] = future
                return id
            }
        }

        fun clearTimer(id: Int) {
            runningTimers.remove(id)?.cancel(true)
            timerOwners.remove(id)
        }

        /** Остановить планировщик таймеров (закрытие игры). После этого движок не работает. */
        fun shutdownScheduler() {
            scheduler.shutdownNow()
            timerOwners.clear()
        }

        /** Очистить все таймеры (выгрузка движка). */
        fun clearAllTimers() {
            runningTimers.values.forEach { it.cancel(true) }
            runningTimers.clear()
            timerOwners.clear()
        }

        /** Очистить таймеры конкретного модуля (выгрузка модуля). */
        fun clearTimersForModule(moduleName: String) {
            timerOwners.entries
                .filter { it.value.equals(moduleName, ignoreCase = true) }
                .map { it.key }
                .toList()
                .forEach { id ->
                    runningTimers.remove(id)?.cancel(true)
                    timerOwners.remove(id)
                }
        }
    }
}
