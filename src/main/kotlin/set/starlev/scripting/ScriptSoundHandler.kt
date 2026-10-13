package set.starlev.scripting

import org.mozilla.javascript.Callable
import set.starlev.scripting.api.JSCancellableEvent
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Cancellable sound handler: registerSoundPlay with criteria + event.cancel()
 * criteria = substring or regex string, null = match all
 */
object ScriptSoundHandler {
    data class Entry(val moduleName: String, val callable: Callable, var criteria: String? = null, var isRegex: Boolean = false)

    private val listeners = CopyOnWriteArrayList<Entry>()

    fun add(moduleName: String, callable: Callable, criteria: String? = null): Entry {
        val e = Entry(moduleName, callable, criteria, criteria?.let { it.startsWith("/") && it.endsWith("/") } ?: false)
        listeners.add(e)
        return e
    }

    /** Снять один конкретный слушатель (по хэндлу, который вернул [add]). */
    fun remove(e: Entry) { listeners.remove(e) }

    fun removeForModule(moduleName: String) {
        listeners.removeIf { it.moduleName.equals(moduleName, ignoreCase = true) }
    }

    fun clearAll() { listeners.clear() }

    /**
     * Вызывается из SoundManagerMixin HEAD, cancellable.
     * @return true если какой-то handler отменил звук
     */
    fun onSound(soundId: String, volume: Float, pitch: Float, pos: Triple<Double,Double,Double>? = null): Boolean {
        if (listeners.isEmpty()) return false
        var cancelled = false
        for (e in listeners) {
            val crit = e.criteria
            if (crit != null) {
                val matches = if (e.isRegex) {
                    try { Regex(crit.removeSurrounding("/")).containsMatchIn(soundId) } catch (_: Exception) { false }
                } else soundId.contains(crit, ignoreCase = true)
                if (!matches) continue
            }
            val event = JSCancellableEvent()
            SHScriptEngine.withContext { cx ->
                set.starlev.scripting.SHContextFactory.beginTracking()
                try {
                    // Аргументы: (name, volume, pitch, event)
                    e.callable.call(cx, SHScriptEngine.globalScope, SHScriptEngine.globalScope, arrayOf(soundId, volume, pitch, event))
                } catch (t: Throwable) {
                    // Не выносим весь модуль из-за одного падения слушателя звука.
                    ScriptConsole.error(e.moduleName, "Ошибка в onSoundPlay: ${t.message}", t)
                } finally {
                    set.starlev.scripting.SHContextFactory.endTracking()
                }
            }
            if (event.cancelled) cancelled = true
        }
        return cancelled
    }
}
