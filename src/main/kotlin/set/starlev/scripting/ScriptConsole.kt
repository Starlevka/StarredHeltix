package set.starlev.scripting

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Консоль для JS-скриптов и модулей StarredHeltix.
 * Выводит сообщения в чат, SLF4J логгер и файл config/starredheltix/modules/console.log
 */
object ScriptConsole {
    private val LOGGER = LoggerFactory.getLogger("StarredHeltix-Script")
    private val logDir = File("config/starredheltix/modules").apply { mkdirs() }
    private val logFile = File(logDir, "console.log")
    // DateTimeFormatter вместо SimpleDateFormat: SimpleDateFormat НЕ потокобезопасен,
    // а log() вызывается с client/netty/timer-потоков.
    private val timeFormat = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
    private val writeLock = Any()

    init {
        try {
            if (!logFile.exists()) {
                logFile.createNewFile()
            }
        } catch (e: Exception) {
            LOGGER.error("Не удалось создать console.log", e)
        }
    }

    fun log(module: String, message: String) {
        val formatted = "[$module] $message"
        LOGGER.info(formatted)
        appendToFile("INFO", module, message)
    }

    fun info(module: String, message: String) {
        log(module, message)
    }

    fun warn(module: String, message: String) {
        val formatted = "[$module] $message"
        LOGGER.warn(formatted)
        appendToFile("WARN", module, message)
        if (set.starlev.StarredHeltix.feature.scripting.logWarningsToChat) {
            printToChat("§e[SH-Script:$module] $message")
        }
    }

    fun error(module: String, message: String, throwable: Throwable? = null) {
        val formatted = "[$module] $message"
        LOGGER.error(formatted, throwable)
        appendToFile("ERROR", module, "$message ${throwable?.stackTraceToString() ?: ""}")
        if (set.starlev.StarredHeltix.feature.scripting.logErrorsToChat) {
            printToChat("§c[SH-Script:$module] $message")
        }
    }

    fun printToChat(message: String) {
        val client = Minecraft.getInstance()
        client.execute {
            client.gui.hud.chat.addClientSystemMessage(Component.literal(message))
        }
    }

    fun clear() {
        try {
            logFile.writeText("")
        } catch (e: Exception) {
            LOGGER.error("Не удалось очистить console.log", e)
        }
    }

    private fun appendToFile(level: String, module: String, text: String) {
        try {
            val timestamp = java.time.LocalTime.now().format(timeFormat)
            val line = "[$timestamp] [$level] [$module] $text"
            // Под lock: параллельные потоки не перемешивают строки.
            synchronized(writeLock) {
                java.nio.file.Files.write(
                    logFile.toPath(),
                    (line + System.lineSeparator()).toByteArray(Charsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
                )
            }
        } catch (ignored: Exception) {
        }
    }
}
