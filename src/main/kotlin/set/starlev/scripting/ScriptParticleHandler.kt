package set.starlev.scripting

import org.mozilla.javascript.Callable
import set.starlev.scripting.api.JSCancellableEvent
import java.util.concurrent.CopyOnWriteArrayList

object ScriptParticleHandler {
    data class Entry(val moduleName: String, val callable: Callable)

    private val listeners = CopyOnWriteArrayList<Entry>()

    fun add(moduleName: String, callable: Callable) { listeners.add(Entry(moduleName, callable)) }
    fun removeForModule(moduleName: String) { listeners.removeIf { it.moduleName.equals(moduleName, ignoreCase = true) } }
    fun clearAll() { listeners.clear() }

    fun onParticle(type: String, x: Double, y: Double, z: Double, dx: Double, dy: Double, dz: Double): Boolean {
        if (listeners.isEmpty()) return false
        var cancelled = false
        for (e in listeners) {
            val event = JSCancellableEvent()
            SHScriptEngine.withContext { cx ->
                try {
                    // CTJS: (particle, event) where particle has x/y/z etc.
                    val obj = mapOf("name" to type, "x" to x, "y" to y, "z" to z, "dx" to dx, "dy" to dy, "dz" to dz)
                    e.callable.call(cx, SHScriptEngine.globalScope, SHScriptEngine.globalScope, arrayOf(obj, event))
                } catch (t: Throwable) {
                    ScriptConsole.error(e.moduleName, "Ошибка в onSpawnParticle: ${t.message}", t)
                }
            }
            if (event.cancelled) cancelled = true
        }
        return cancelled
    }
}
