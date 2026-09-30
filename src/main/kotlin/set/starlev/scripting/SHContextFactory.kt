package set.starlev.scripting

import org.mozilla.javascript.ClassShutter
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.WrapFactory
import java.util.concurrent.atomic.AtomicLong

/**
 * Кастомная фабрика контекстов Rhino для StarredHeltix.
 * Настраивает ES6, оптимизацию байткода и песочницу (sandboxing).
 *
 * InstructionObserver: защита от зависаний. Кривой модуль с while(true) в tick-триггере
 * больше не замораживает клиент — скрипт прерывается ошибкой после лимита времени.
 */
class SHContextFactory : ContextFactory() {

    @Suppress("DEPRECATION")
    override fun makeContext(): Context {
        val cx = super.makeContext()
        cx.optimizationLevel = 9
        cx.languageVersion = Context.VERSION_ES6
        cx.wrapFactory = SHWrapFactory()
        cx.setClassShutter(SHClassShutter)
        // Каждые N инструкций Rhino вызывает observeInstructionCount (и в interpreted,
        // и в compiled-режиме — threshold>0 включает генерацию счётчиков в байткод)
        cx.instructionObserverThreshold = INSTRUCTION_THRESHOLD
        return cx
    }

    override fun hasFeature(cx: Context, featureIndex: Int): Boolean {
        return when (featureIndex) {
            Context.FEATURE_ENABLE_JAVA_MAP_ACCESS -> true
            Context.FEATURE_LOCATION_INFORMATION_IN_ERROR -> true
            else -> super.hasFeature(cx, featureIndex)
        }
    }

    /**
     * Вызывается Rhino каждые INSTRUCTION_THRESHOLD инструкций выполняемого скрипта.
     * Проверяем реальное время: превышение EXECUTION_TIME_LIMIT_MS -> Error (документированный
     * способ прервать скрипт). Триггер с ошибкой снимается штатным механизмом SHTrigger.
     */
    override fun observeInstructionCount(cx: Context, instructionCount: Int) {
        val dl = EXECUTION_DEADLINE.get()
        val deadline = dl.get()
        if (deadline > 0 && System.nanoTime() > deadline) {
            dl.set(0)
            throw Error("StarredHeltix: скрипт превысил лимит времени (${EXECUTION_TIME_LIMIT_MS}мс) — вероятно, бесконечный цикл")
        }
    }

    companion object {
        /**
         * Лимит инструкций Rhino между вызовами observeInstructionCount.
         * Мелкое значение = чаще проверка времени = быстрее прерывание зависшего скрипта,
         * но чуть больше накладных расходов на интерпретацию.
         */
        const val INSTRUCTION_THRESHOLD = 10_000

        /** Максимальное время непрерывного выполнения одного вызова триггера/скрипта, мс. */
        const val EXECUTION_TIME_LIMIT_MS = 3_000L

        /** nanoTime-дедлайн текущего выполнения (0 = не отслеживается). ThreadLocal — net.getAsync колбэки бегут на SHNet-пуле параллельно тику. */
        private val EXECUTION_DEADLINE = ThreadLocal.withInitial { AtomicLong(0) }

        /** Установить дедлайн времени для текущего выполнения скрипта (вызывается обёрткой вызова). */
        fun beginTracking() {
            EXECUTION_DEADLINE.get().set(System.nanoTime() + EXECUTION_TIME_LIMIT_MS * 1_000_000)
        }

        /** Снять дедлайн (после завершения выполнения). */
        fun endTracking() {
            EXECUTION_DEADLINE.get().set(0)
        }

        internal fun getDeadline(): AtomicLong = EXECUTION_DEADLINE.get()
    }
}

/**
 * Обёртка со отключённым javaPrimitiveWrap.
 */
class SHWrapFactory : WrapFactory() {
    init {
        isJavaPrimitiveWrap = false
    }
}

/**
 * Песочница безопасности: блокирует небезопасные Java-классы (Runtime, ProcessBuilder, Reflection, etc.)
 */
object SHClassShutter : ClassShutter {
    // Разрешаем всю стандартную библиотеку: java.* и kotlin.*.
    // ВАЖНО: blockedPrefixes проверяется ПЕРВЫМ, так что опасные пакеты
    // (reflect, Runtime, Process, Thread, io, nio, net, sun) остаются запрещены.
    private val allowedPrefixes = listOf(
        "set.starlev.scripting.", // движок: ScriptConsole, JSUtils, триггеры и т.п. (доверенные)
        "set.starlev.scripting.api.",
        "java.",
        "javax.",
        "kotlin.",
        "kotlinx.",
        // ==== Библиотеки для модулей (Java.type) ====
        // Fabric API: события, команды, networking, rendering (без нутра net.fabricmc.fabric.impl)
        "net.fabricmc.fabric.api.",
        "net.fabricmc.api.",
        // JOML — векторная/матричная математика (обязательно для продвинутого рендера)
        "org.joml.",
        // MoulConfig — декларативные GUI настроек (как в самом моде)
        "io.github.notenoughupdates.moulconfig."
    )

    private val blockedPrefixes = listOf(
        // ==== Пути побега из песочницы Rhino (закрывают ClassShutter-обходы) ====
        // ВНИМАНИЕ: "java.lang.Class" здесь НЕ блокируем — это ломает сам Rhino (javaToJS(Class)
        // для Java.type и для глобалов Display/Text/Image кидает на старте). Побег через Class
        // прикрыт точечно в JavaGlobal.type + тем, что все опасные ЦЕЛЕВЫЕ пакеты ниже заблокированы:
        // даже получив хендл Class, обернуть его результаты (ClassLoader/invoke/foreign/...) нельзя.
        "java.lang.invoke.",
        "java.lang.foreign.",
        "java.lang.module.",
        "java.lang.ref.",
        "java.lang.management.",
        "java.lang.SecurityManager",
        "java.lang.ProcessEnvironment",
        // javax.script/ScriptEngineManager -> глобальный Rhino ContextFactory (без шаттера и watchdog).
        "javax.script.",
        "javax.tools.",
        "javax.naming.",
        "javax.management.",
        "javax.rmi.",
        // kotlin.reflect.* — обход блокировки java.lang.reflect.*
        "kotlin.reflect.",

        // ==== Классические блоки ====
        "java.lang.reflect.",
        "java.lang.Runtime",
        "java.lang.Process",
        "java.lang.ProcessBuilder",
        "java.lang.System",
        "java.lang.ClassLoader",
        "java.lang.Thread",
        "java.lang.ThreadGroup",
        "java.io.",
        "java.nio.",
        "java.net.",
        "kotlin.io.",
        "kotlinx.io.",
        "sun.",
        "com.sun.",
        // Внутренности fabric/moulconfig — только публичные API
        "net.fabricmc.fabric.impl.",
        "net.fabricmc.fabric.mixin.",
        "io.github.notenoughupdates.moulconfig.internal."

        // ВНИМАНИЕ: внутренности движка (set.starlev.scripting.SHScriptEngine/JavaGlobal/SHTrigger/...)
        // здесь НЕ блокируются. Их классы ЛЕГИТОННО пересекают границу JS (движок инжектит
        // JavaGlobal как глобал "Java", отдаёт SHTrigger наружу из events.onTick и т.д.) —
        // блокировка в ClassShutter ломает оборачивание этих объектов (краш на старте/при подписке).
        // Побег через Java.type прикрыт списком DENIED_CLASS_PREFIXES в JavaGlobal.type
        // (load()/importClass()/Packages уже убраны — Java.type единственный путь к классам).
    )

    override fun visibleToScripts(fullClassName: String): Boolean {
        for (blocked in blockedPrefixes) {
            if (fullClassName.startsWith(blocked)) return false
        }
        for (allowed in allowedPrefixes) {
            if (fullClassName.startsWith(allowed)) return true
        }
        return false
    }
}
