package set.starlev.scripting

import org.mozilla.javascript.Callable
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable

enum class SHPriority(val value: Int) {
    HIGHEST(0),
    HIGH(1),
    NORMAL(2),
    LOW(3),
    LOWEST(4)
}

/**
 * Триггер события для JS-модуля.
 */
class SHTrigger(
    val moduleName: String,
    val type: String,
    val callable: Callable,
    var priority: SHPriority = SHPriority.NORMAL
) : Comparable<SHTrigger> {

    var isCancelled: Boolean = false
        private set

    var isActive: Boolean = true

    /** Подряд идущие ошибки: после порога триггер глушим (раньше — с первой же ошибки,
     *  и обычный транзиентный NPE от mc.player==null навсегда убивал обработчик). */
    private var consecutiveErrors = 0

    /** Потолок подряд идущих ошибок для авто-отключения. */
    private val errorBudget = 10

    /** Потратить 1 единицу бюджета. @return true, если триггер пора снять. */
    private fun spendErrorBudget(): Boolean {
        consecutiveErrors++
        return consecutiveErrors >= errorBudget
    }

    /** Необязательный хук доп. очистки при unregister (например, снять слушателя из
     *  ScriptSoundHandler/ScriptParticleHandler, которые живут вне общего реестра триггеров). */
    var onUnregisterHook: (() -> Unit)? = null

    fun unregister() {
        isActive = false
        SHScriptEngine.removeTrigger(this)
        try { onUnregisterHook?.invoke() } catch (_: Exception) {}
        onUnregisterHook = null
    }

    fun invoke(cx: Context, scope: Scriptable, vararg args: Any?): Any? {
        if (!isActive) return null
        return try {
            val jsArgs = args.map { Context.javaToJS(it, scope) }.toTypedArray()
            val r = callable.call(cx, scope, scope, jsArgs)
            consecutiveErrors = 0 // успех обнуляет счётчик
            r
        } catch (t: Throwable) {
            val watchdog = t is Error && t.message?.contains("превысил лимит времени") == true
            ScriptConsole.error(moduleName, "Исключение в триггере '$type': ${t.message}", t)
            // Watchdog-Error (бесконечный цикл) или переполнение бюджета — снимаем триггер,
            // чтобы не вешать клиент/не спамить логи. Обычная транзиентная ошибка — переживаем.
            if (watchdog || spendErrorBudget()) {
                if (!watchdog) ScriptConsole.error(moduleName, "Триггер '$type' отключён после $errorBudget подряд ошибок.")
                unregister()
            }
            null
        }
    }

    override fun compareTo(other: SHTrigger): Int {
        val prioCompare = this.priority.value.compareTo(other.priority.value)
        if (prioCompare != 0) return prioCompare
        return this.hashCode().compareTo(other.hashCode())
    }
}
