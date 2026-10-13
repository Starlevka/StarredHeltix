package set.starlev.utils

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import set.starlev.StarredHeltix
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.management.ManagementFactory
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * Буст игрового процесса: Windows MMCSS «Games» и Linux Feral GameMode.
 *
 * Зачем: у Minecraft жёсткий дедлайн (тик 50 мс, кадр ~16 мс). Если ОС не относится
 * к процессу как к игровому, потоки рендера и интегрированного сервера вытесняются
 * фоновыми задачами (браузер, антивирус, обновления) → микрофризы и просадки FPS.
 *
 * Windows — MMCSS (Multimedia Class Scheduler Service), задача «Games»: поток встаёт в
 * «игровую» очередь планировщика (avrt.dll) + HIGH_PRIORITY_CLASS клиенту и
 * THREAD_PRIORITY_HIGHEST потокам. Это то же, что делает мод Windows Priority Boost.
 *
 * Linux — Feral GameMode: libgamemode → D-Bus (com.feralinteractive.GameMode) → renice,
 * плюс setpriority(-20) потокам рендера и -15 потокам сервера.
 *
 * Нативные функции вызываются через JNA (net.java.dev.jna:jna, включён в мод), без
 * рефлексии: интерфейсы-обёртки ниже. Библиотеки нет в системе — бэкенд молча
 * выключается, клиент не падает.
 *
 * Важно: обе системы работают на ВЕСЬ процесс клиента, а не на код мода. Мод только
 * поднимает их один раз при старте, чтобы не зависеть от лаунчерных обёрток
 * (`gamemoderun`, Steam options). Настройки применяются при запуске — после переключения
 * нужен перезапуск игры.
 *
 * Кросс-поточность: и MMCSS, и setpriority(nice) применяются ТОЛЬКО из самого потока,
 * поэтому boost вызывается из трёх точек, каждая в своём потоке:
 *   • init()           → главный поток клиента («main»);
 *   • onRenderThread() → рендер-поток (вызов из renderHud, дальше одна volatile-проверка);
 *   • SERVER_STARTED   → поток интегрированного сервера.
 * Сторожевой поток (daemon, 2 с) поднимает Java-приоритет новых потоков рендера/сервера —
 * это единственное, что можно сделать «с чужого» потока.
 */
object GameModeBoost {
    private val LOGGER = StarredHeltix.LOGGER

    private const val WATCH_INTERVAL_MS = 2000L

    /** Потоки, которым уже подняли приоритет (чтобы не трогать повторно). */
    private val handled = ConcurrentHashMap.newKeySet<Thread>()

    /** Что реально применилось — для лога и `/sh boost`. */
    private val report = CopyOnWriteArrayList<String>()

    @Volatile
    private var initialized = false

    @Volatile
    private var running = false

    /** Флаг потока рендера: после первого буста остаётся одна volatile-проверка в кадре. */
    @Volatile
    private var renderThreadDone = false

    @Volatile
    private var watcher: Thread? = null

    @Volatile
    private var os: BoostOs = BoostOs.OTHER

    private val config
        get() = StarredHeltix.feature.optimization.performance

    // ─────────────────────────── публичный API ───────────────────────────

    /**
     * Точка входа, вызывается из StarredHeltix.onInitializeClient() (главный поток).
     * Идемпотентна: повторный вызов ничего не делает.
     */
    fun init() {
        if (initialized) return
        initialized = true

        os = BoostOs.detect()
        val cfg = try {
            config
        } catch (t: Throwable) {
            LOGGER.warn("[Boost] Конфиг недоступен, буст отключён: ${t.message}")
            return
        }

        if (!cfg.windows && !cfg.linux) {
            LOGGER.info("[Boost] Выключено в настройках (Оптимизация → Приоритет процесса)")
            return
        }

        // Тумблеры взаимоисключающие, но конфиг могли отредактировать руками — решаем по фактической ОС.
        val useWindows = cfg.windows && (cfg.linux.not() || os == BoostOs.WINDOWS)
        val useLinux = cfg.linux && (cfg.windows.not() || os == BoostOs.LINUX)

        when {
            useWindows && os != BoostOs.WINDOWS -> LOGGER.warn(
                "[Boost] Включён режим Windows, а система — ${os.label}. Пропускаю."
            )
            useLinux && os != BoostOs.LINUX -> LOGGER.warn(
                "[Boost] Включён режим Linux, а система — ${os.label}. Пропускаю."
            )
        }
        if (!useWindows && !useLinux) {
            LOGGER.info("[Boost] Нечего применять: система ${os.label}")
            return
        }

        running = true
        LOGGER.info("[Boost] Система: ${os.label}, режим: ${if (useWindows) "Windows" else "Linux"}")

        try {
            if (useWindows) {
                WindowsBoost.applyProcessLevel()
            } else {
                LinuxBoost.applyProcessLevel()
            }
        } catch (t: Throwable) {
            LOGGER.warn("[Boost] Системный буст не удался: ${t.message}")
        }

        // Главный поток клиента — он уже исполняет этот код.
        boostCurrentThread(if (useWindows) "client" else "client")

        // Поток интегрированного сервера вызывается из самого сервера.
        ServerLifecycleEvents.SERVER_STARTED.register { boostCurrentThread("server") }
        ClientLifecycleEvents.CLIENT_STOPPING.register { shutdown() }

        startWatcher()

        LOGGER.info("[Boost] Готово: ${report.joinToString("; ")}")
        LOGGER.info("[Boost] Настройки читаются только при старте — после переключения перезапусти игру")
    }

    /**
     * Вызывается из рендер-потока каждый кадр (renderHud). После первого буста внутри
     * остаётся одна volatile-проверка.
     */
    @JvmStatic
    fun onRenderThread() {
        if (!running || renderThreadDone) return
        renderThreadDone = true
        boostCurrentThread("render")
    }

    /** Откат всего, что успели применить (вызывается на CLIENT_STOPPING). */
    fun shutdown() {
        if (!running) return
        running = false
        watcher?.interrupt()
        watcher = null
        try {
            WindowsBoost.revertCurrentThread(Thread.currentThread())
        } catch (t: Throwable) {
            LOGGER.warn("[Boost] Не удалось снять MMCSS: ${t.message}")
        }
        try {
            LinuxBoost.shutdown()
        } catch (t: Throwable) {
            LOGGER.warn("[Boost] Не удалось выключить GameMode: ${t.message}")
        }
        LOGGER.info("[Boost] Остановлен. Итог: ${report.joinToString("; ")}")
    }

    /** Строка со статусом для `/sh boost`. */
    @JvmStatic
    fun status(): String = buildString {
        append("система: ").append(os.label)
        append(" | активен: ").append(if (running) "да" else "нет")
        if (report.isNotEmpty()) append(" | ").append(report.joinToString("; "))
    }

    // ─────────────────────────── внутреннее ───────────────────────────

    /**
     * Поднимает приоритет ТЕКУЩЕГО потока. Вызывается только из самого потока.
     * @param hint подсказка от точки вызова: client / render / server.
     */
    fun boostCurrentThread(hint: String) {
        if (!running) return
        val thread = Thread.currentThread()
        val role = classify(thread, hint) ?: return
        if (!handled.add(thread)) return

        applyJavaPriority(thread, role)
        val applied = try {
            when (os) {
                BoostOs.WINDOWS -> WindowsBoost.applyThread(thread, role)
                BoostOs.LINUX -> LinuxBoost.applyThread(role)
                else -> "только приоритет потока"
            }
        } catch (t: Throwable) {
            LOGGER.warn("[Boost] Поток ${thread.name}: ${t.message}")
            "ошибка: ${t.javaClass.simpleName}"
        }
        note("${thread.name} [$role]: $applied")
    }

    /** Максимальный Java-приоритет: рендер — MAX, сервер — MAX-1. */
    private fun applyJavaPriority(thread: Thread, role: BoostRole) {
        try {
            val target = if (role == BoostRole.RENDER) Thread.MAX_PRIORITY else Thread.MAX_PRIORITY - 1
            if (thread.priority < target) thread.priority = target
        } catch (t: Throwable) {
            // SecurityException / неверный диапазон — не критично
        }
    }

    private fun startWatcher() {
        if (watcher != null) return
        val thread = Thread({
            while (running) {
                try {
                    Thread.sleep(WATCH_INTERVAL_MS)
                } catch (t: InterruptedException) {
                    return@Thread
                }
                scanThreads()
            }
        }, "StarredHeltix-Boost")
        thread.isDaemon = true
        thread.priority = Thread.NORM_PRIORITY
        try {
            thread.start()
            watcher = thread
        } catch (t: Throwable) {
            watcher = null
        }
    }

    /**
     * Сторожевой проход: поднимает Java-приоритет потоков, которые появились позже
     * (чанки, «Server thread» до хука SERVER_STARTED). Нативный буст (MMCSS/nice)
     * из чужого потока невозможен — тут только Thread.setPriority.
     */
    private fun scanThreads() {
        try {
            var root = Thread.currentThread().threadGroup
            while (root != null && root.parent != null) {
                root = root.parent
            }
            val snapshot = arrayOfNulls<Thread>(maxOf(root.activeCount() * 2, 32))
            val count = root.enumerate(snapshot, true)
            for (i in 0 until count) {
                val thread = snapshot[i] ?: continue
                if (!thread.isAlive || handled.contains(thread)) continue
                val role = classify(thread, null) ?: continue
                handled.add(thread)
                applyJavaPriority(thread, role)
            }
        } catch (t: Throwable) {
            // Сторож — тихий, ошибки игнорируем
        }
    }

    private fun classify(thread: Thread, hint: String?): BoostRole? {
        val name = thread.name.lowercase(Locale.ROOT)
        val h = (hint ?: "").lowercase(Locale.ROOT)
        if (h.contains("server") || name.contains("server") || name.contains("integrated")) {
            return BoostRole.SERVER
        }
        if (h.contains("render") || h.contains("client") ||
            name.contains("render") || name.contains("client") || name == "main"
        ) {
            return BoostRole.RENDER
        }
        return null
    }

    private fun note(message: String) {
        report.add(message)
        LOGGER.info("[Boost] $message")
    }

    /**
     * Сброс взаимного исключения тумблеров: включены обе платформы → оставляем ту,
     * что совпадает с системой. Возвращает true, если конфиг изменился.
     * Вызывается из ConfigManager при загрузке (конфиг могли отредактировать руками).
     */
    fun enforceSinglePlatform(osName: String): Boolean {
        val cfg = try {
            config
        } catch (t: Throwable) {
            return false
        }
        if (!cfg.windows || !cfg.linux) return false
        val keepWindows = osName.startsWith("Windows")
        if (keepWindows) cfg.linux = false else cfg.windows = false
        LOGGER.warn(
            "[Boost] Включены Windows и Linux одновременно — оставляю ${if (keepWindows) "Windows" else "Linux"} " +
                "(система $osName)"
        )
        return true
    }

    // ───────────────────────────── Windows ─────────────────────────────

    /** avrt.dll — Multimedia Class Scheduler Service. */
    private interface AvrtLibrary : Library {
        fun AvSetMmThreadCharacteristicsW(taskName: WString, taskIndex: Memory): Pointer?
        fun AvRevertMmThreadCharacteristics(avrtHandle: Pointer): Boolean
    }

    /** kernel32.dll — классы приоритета процесса и потока. */
    private interface Kernel32Library : Library {
        fun SetPriorityClass(hProcess: Pointer, priorityClass: Int): Boolean
        fun SetThreadPriority(hThread: Pointer, priority: Int): Boolean
    }

    private object WindowsBoost {
        private const val HIGH_PRIORITY_CLASS = 0x00000080
        private const val THREAD_PRIORITY_HIGHEST = 2

        /** Псевдо-хендлы ядра: текущий процесс -1, текущий поток -2. */
        private const val CURRENT_PROCESS = -1L
        private const val CURRENT_THREAD = -2L

        /** Задача MMCSS для игр: квант по игровой политике вместо фоновой. */
        private const val MMCSS_TASK = "Games"

        @Volatile
        private var avrt: AvrtLibrary? = null

        @Volatile
        private var kernel32: Kernel32Library? = null

        /** Результат AvSetMmThreadCharacteristicsW по потокам — для отката при выходе. */
        private val mmcssHandles = ConcurrentHashMap<Thread, Pointer>()

        fun applyProcessLevel() {
            val kernel = kernel32()
            if (kernel == null) {
                note("kernel32 недоступна — приоритет процесса пропущен")
                return
            }
            val ok = try {
                kernel.SetPriorityClass(Pointer.createConstant(CURRENT_PROCESS), HIGH_PRIORITY_CLASS)
            } catch (t: Throwable) {
                note("SetPriorityClass: ${t.javaClass.simpleName}")
                return
            }
            note(if (ok) "HIGH_PRIORITY_CLASS клиенту" else "HIGH_PRIORITY_CLASS отклонён (нужны права)")
        }

        /** Вызывать ТОЛЬКО из целевого потока и только один раз: MMCSS регистрирует вызывающий поток. */
        fun applyThread(thread: Thread, role: BoostRole): String {
            val avrtLib = avrt()
            if (avrtLib == null) return "avrt.dll недоступна"

            val handle = try {
                // taskIndex — out-параметр, и он же фактически in: значение обязано быть 0,
                // иначе avrt отвечает ERROR_INVALID_TASK_INDEX (1551). Memory(4) в JNA 5.19
                // не зануляется, поэтому пишем ноль явно.
                val taskIndex = Memory(4)
                taskIndex.setInt(0, 0)
                avrtLib.AvSetMmThreadCharacteristicsW(WString(MMCSS_TASK), taskIndex)
            } catch (t: Throwable) {
                return "AvSetMmThreadCharacteristicsW: ${t.javaClass.simpleName}"
            }
            // Повторный вызов на том же потоке даёт ERROR_THREAD_ALREADY_JOINED (1552) —
            // поэтому вызываем ровно один раз на поток (гарантирует handled во внешнем коде).
            if (handle == null) return "MMCSS «$MMCSS_TASK» не выдался (поток уже в задаче?)"

            mmcssHandles[thread] = handle
            setThreadPriority()
            return "MMCSS «$MMCSS_TASK» + THREAD_PRIORITY_HIGHEST"
        }

        fun revertCurrentThread(thread: Thread) {
            val handle = mmcssHandles.remove(thread) ?: return
            val avrtLib = avrt() ?: return
            try {
                avrtLib.AvRevertMmThreadCharacteristics(handle)
                LOGGER.info("[Boost] MMCSS снят с потока ${thread.name}")
            } catch (t: Throwable) {
                LOGGER.warn("[Boost] Не удалось снять MMCSS: ${t.message}")
            }
        }

        private fun setThreadPriority() {
            try {
                val kernel = kernel32() ?: return
                kernel.SetThreadPriority(Pointer.createConstant(CURRENT_THREAD), THREAD_PRIORITY_HIGHEST)
            } catch (t: Throwable) {
                // Не критично: MMCSS уже выдан
            }
        }

        private fun avrt(): AvrtLibrary? {
            avrt?.let { return it }
            return try {
                Native.load("Avrt", AvrtLibrary::class.java).also { avrt = it }
            } catch (t: Throwable) {
                note("avrt.dll не загрузилась: ${t.message}")
                null
            }
        }

        private fun kernel32(): Kernel32Library? {
            kernel32?.let { return it }
            return try {
                Native.load("kernel32", Kernel32Library::class.java).also { kernel32 = it }
            } catch (t: Throwable) {
                note("kernel32 не загрузилась: ${t.message}")
                null
            }
        }
    }

    // ────────────────────────────── Linux ──────────────────────────────

    /** libc — nice потока (в Linux who=0 значит «вызывающий поток»). */
    private interface LibcLibrary : Library {
        fun setpriority(which: Int, who: Int, prio: Int): Int
    }

    /** libgamemode — Feral GameMode. */
    private interface GameModeLibrary : Library {
        fun gamemode_request_start(): Int
        fun gamemode_request_end(): Int
    }

    private object LinuxBoost {
        private const val PRIO_PROCESS = 0
        private const val NICE_RENDER = -20
        private const val NICE_SERVER = -15
        private const val NICE_PROCESS = -20

        private const val DBUS_SERVICE = "com.feralinteractive.GameMode"
        private const val DBUS_PATH = "/com/feralinteractive/GameMode"

        @Volatile
        private var libc: LibcLibrary? = null

        @Volatile
        private var gameModeActive = false

        @Volatile
        private var dbusRegistered = false

        fun applyProcessLevel() {
            when (val rc = gameModeStart()) {
                0 -> {
                    gameModeActive = true
                    note("GameMode включён (libgamemode)")
                }
                null -> {
                    note("libgamemode не найден — пробуем D-Bus")
                    tryDbus()
                }
                else -> {
                    note("GameMode отказал (rc=$rc) — пробуем D-Bus")
                    tryDbus()
                }
            }
            if (!gameModeActive) {
                if (reniceSelf()) {
                    val pid = currentPid()
                    note("nice $NICE_PROCESS процессу (pid=$pid)")
                } else {
                    note("GameMode и renice недоступны — только приоритет потоков")
                }
            }
        }

        /** Вызывать ТОЛЬКО из целевого потока — setpriority меняет nice вызывающего. */
        fun applyThread(role: BoostRole): String {
            val lib = libc() ?: return "libc недоступна"
            val nice = if (role == BoostRole.RENDER) NICE_RENDER else NICE_SERVER
            val rc = try {
                lib.setpriority(PRIO_PROCESS, 0, nice)
            } catch (t: Throwable) {
                return "setpriority: ${t.javaClass.simpleName}"
            }
            return when {
                rc == 0 -> "nice $nice"
                rc < 0 -> "nice $nice отклонён (нужны права)"
                else -> "nice $nice отклонён (rc=$rc)"
            }
        }

        fun shutdown() {
            if (gameModeActive) {
                gameModeActive = false
                gameModeLibrary()?.let { lib ->
                    try {
                        lib.gamemode_request_end()
                        note("GameMode выключен")
                    } catch (t: Throwable) {
                        LOGGER.warn("[Boost] gamemode_request_end: ${t.message}")
                    }
                }
            }
            if (dbusRegistered) {
                dbusRegistered = false
                val pid = currentPid()
                exec(
                    arrayOf(
                        "dbus-send", "--session", "--dest=$DBUS_SERVICE", "--type=method_call",
                        DBUS_PATH, "$DBUS_SERVICE.UnRegisterGame", "int32:$pid"
                    ), 3000L
                )
                note("регистрация GameMode снята (D-Bus)")
            }
        }

        private fun tryDbus() {
            val pid = currentPid()
            val result = exec(
                arrayOf(
                    "dbus-send", "--session", "--dest=$DBUS_SERVICE", "--type=method_call", "--print-reply",
                    DBUS_PATH, "$DBUS_SERVICE.RegisterGame", "int32:$pid"
                ), 3000L
            )
            if (result.ok) {
                gameModeActive = true
                dbusRegistered = true
                note("GameMode включён (D-Bus)")
            } else {
                note("D-Bus GameMode недоступен: ${result.text.lineSequence().firstOrNull().orEmpty()}")
            }
        }

        private fun reniceSelf(): Boolean {
            val pid = currentPid()
            val result = exec(
                arrayOf("renice", "-n", "$NICE_PROCESS", "-p", "$pid"), 5000L
            )
            if (result.ok) return true
            note("renice отклонён (нужны права или limits.conf)")
            return false
        }

        /** gamemode_request_start: 0 = ок, -1 = библиотеки нет, другое = отказ демона. */
        private fun gameModeStart(): Int? {
            val lib = gameModeLibrary() ?: return null
            return try {
                lib.gamemode_request_start()
            } catch (t: Throwable) {
                null
            }
        }

        private fun gameModeLibrary(): GameModeLibrary? = try {
            Native.load("gamemode", GameModeLibrary::class.java)
        } catch (t: Throwable) {
            try {
                Native.load("gamemodeauto", GameModeLibrary::class.java)
            } catch (t2: Throwable) {
                null
            }
        }

        private fun libc(): LibcLibrary? {
            libc?.let { return it }
            return try {
                Native.load("c", LibcLibrary::class.java).also { libc = it }
            } catch (t: Throwable) {
                note("libc не загрузилась: ${t.message}")
                null
            }
        }
    }

    private enum class BoostRole { RENDER, SERVER }

    private enum class BoostOs(val label: String) {
        WINDOWS("Windows"),
        LINUX("Linux"),
        ANDROID("Android"),
        OTHER("прочее");

        companion object {
            fun detect(): BoostOs {
                // PojavLauncher/Fold Craft запускают JVM с os.name=Linux, поэтому
                // Android проверяем первым — по наличию android.os.Process.
                if (isAndroid()) return ANDROID
                val os = try {
                    System.getProperty("os.name", "generic").lowercase(Locale.ROOT)
                } catch (t: Throwable) {
                    return OTHER
                }
                return when {
                    os.contains("win") -> WINDOWS
                    os.contains("linux") || os.contains("nix") || os.contains("nux") || os.contains("aix") -> LINUX
                    else -> OTHER
                }
            }

            private fun isAndroid(): Boolean {
                try {
                    Class.forName("android.os.Process")
                    return true
                } catch (t: Throwable) {
                    // не Android — идём дальше
                }
                return try {
                    val vendor = System.getProperty("java.vendor", "") +
                        System.getProperty("java.vm.vendor", "") +
                        System.getProperty("java.runtime.name", "")
                    vendor.lowercase(Locale.ROOT).contains("android")
                } catch (t: Throwable) {
                    false
                }
            }
        }
    }

    // ─────────────────────────── мелкие утилиты ───────────────────────────

    private fun currentPid(): Int {
        return try {
            val name = ManagementFactory.getRuntimeMXBean().name
            val at = name.indexOf('@')
            if (at > 0) name.substring(0, at).toInt() else 0
        } catch (t: Throwable) {
            0
        }
    }

    private class ExecResult(val ok: Boolean, val text: String)

    /**
     * Запуск внешней команды с таймаутом. Вывод читает daemon-поток, чтобы процесс
     * не завис на заполненном буфере (dbus-send/renice иногда зависают в ожидании).
     */
    private fun exec(command: Array<String>, timeoutMs: Long): ExecResult {
        var started: Process? = null
        return try {
            val process = ProcessBuilder(*command).redirectErrorStream(true).start()
            started = process
            val output = StringBuilder()
            val reader = Thread({
                try {
                    BufferedReader(InputStreamReader(process.inputStream)).use { stream ->
                        while (true) {
                            val line = stream.readLine() ?: break
                            synchronized(output) { output.append(line).append('\n') }
                        }
                    }
                } catch (t: Throwable) {
                    // Поток читателя умирает вместе с процессом — это нормально
                }
            }, "StarredHeltix-Boost-Exec")
            reader.isDaemon = true
            reader.start()

            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            reader.join(300L)
            if (!finished) {
                process.destroyForcibly()
                return ExecResult(false, "таймаут ${timeoutMs}мс: ${command.joinToString(" ")}")
            }
            ExecResult(process.exitValue() == 0, synchronized(output) { output.toString() })
        } catch (t: Throwable) {
            try {
                started?.destroyForcibly()
            } catch (ignored: Throwable) {
            }
            ExecResult(false, t.toString())
        }
    }
}
