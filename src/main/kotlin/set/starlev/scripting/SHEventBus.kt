package set.starlev.scripting

import org.mozilla.javascript.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Межмодульная шина событий: модуль A публикует `events.emit("myevent", data)`,
 * модуль B получает через `events.on("myevent", function(data) { ... })`.
 *
 * Подписки привязаны к модулю: при выгрузке модуля все его подписки снимаются.
 * Системные события (начинаются с "sh.") зарезервированы для движка.
 */
object SHEventBus {

    private val listeners = ConcurrentHashMap<String, CopyOnWriteArrayList<BusListener>>()

    class BusListener(val moduleName: String, val callable: Callable) { var errors: Int = 0 }

    private val errorBudget = 10

    /** Подписка модуля на событие. */
    fun on(moduleName: String, event: String, callable: Callable) {
        addListener(event, BusListener(moduleName, callable))
    }

    /** Добавить готового слушателя (для events.once). */
    fun addListener(event: String, listener: BusListener) {
        listeners.computeIfAbsent(event.lowercase()) { CopyOnWriteArrayList() }
            .add(listener)
    }

    /**
     * Публикация события. Все колбэки выполняются синхронно в контексте Rhino
     * (под watchdog; упавший N раз подряд слушатель снимается, остальные не трогаем).
     * @return количество вызванных подписчиков
     */
    fun emit(event: String, vararg args: Any?): Int {
        val subs = listeners[event.lowercase()] ?: return 0
        var fired = 0
        SHScriptEngine.withContext { cx ->
            for (sub in subs) {
                set.starlev.scripting.SHContextFactory.beginTracking()
                try {
                    val jsArgs = args.map { org.mozilla.javascript.Context.javaToJS(it, SHScriptEngine.globalScope) }.toTypedArray()
                    sub.callable.call(cx, SHScriptEngine.globalScope, SHScriptEngine.globalScope, jsArgs)
                    sub.errors = 0
                    fired++
                } catch (e: Throwable) {
                    ScriptConsole.error(sub.moduleName, "Ошибка в обработчике события '$event': ${e.message}", e)
                    if (++sub.errors >= errorBudget) {
                        ScriptConsole.error(sub.moduleName, "Слушатель '$event' снят после $errorBudget подряд ошибок.")
                        subs.remove(sub)
                    }
                } finally {
                    set.starlev.scripting.SHContextFactory.endTracking()
                }
            }
        }
        return fired
    }

    /** Список имён событий с активными подписчиками (для отладки). */
    fun getActiveEvents(): List<String> =
        listeners.filter { it.value.isNotEmpty() }.keys.sorted()

    /** Снять подписки модуля (при выгрузке). */
    fun removeForModule(moduleName: String) {
        listeners.values.forEach { it.removeIf { sub -> sub.moduleName.equals(moduleName, ignoreCase = true) } }
    }

    /** Точечное снятие одной подписки (для events.once). */
    fun removeListener(event: String, listener: BusListener) {
        listeners[event.lowercase()]?.remove(listener)
    }

    /** Очистить всё (выгрузка движка). */
    fun clearAll() {
        listeners.clear()
    }
}
