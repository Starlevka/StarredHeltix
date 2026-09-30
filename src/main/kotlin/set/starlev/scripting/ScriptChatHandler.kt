package set.starlev.scripting

import org.mozilla.javascript.Callable
import set.starlev.scripting.api.JSCancellableEvent
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Маршрутизатор отменяемых событий чата для JS-модулей.
 *
 * Миксины чата (ClientPacketListenerMixin) вызывают [onIncoming]/[onOutgoing]
 * ДО добавления сообщения в чат. Если модуль отменил событие — сообщение не отображается.
 *
 * Каждая подписка хранит имя модуля: при выгрузке модуля её подписки снимаются.
 */
object ScriptChatHandler {

    class IncomingSubscription(val moduleName: String, val callable: Callable) { var errors: Int = 0 }
    class OutgoingSubscription(val moduleName: String, val callable: Callable) { var errors: Int = 0 }

    private val errorBudget = 10

    private val incoming = CopyOnWriteArrayList<IncomingSubscription>()
    private val outgoing = CopyOnWriteArrayList<OutgoingSubscription>()

    fun addIncoming(moduleName: String, callable: Callable) {
        incoming.add(IncomingSubscription(moduleName, callable))
    }

    fun addOutgoing(moduleName: String, callable: Callable) {
        outgoing.add(OutgoingSubscription(moduleName, callable))
    }

    fun removeForModule(moduleName: String) {
        incoming.removeIf { it.moduleName.equals(moduleName, ignoreCase = true) }
        outgoing.removeIf { it.moduleName.equals(moduleName, ignoreCase = true) }
    }

    fun clearAll() {
        incoming.clear()
        outgoing.clear()
    }

    /**
     * Вызывается из ClientPacketListenerMixin (до показа сообщения).
     * @return true, если сообщение нужно отменить (не показывать)
     */
    fun onIncoming(message: String): Boolean {
        if (incoming.isEmpty()) return false
        var cancelled = false
        SHScriptEngine.withContext { cx ->
            for (sub in incoming) {
                if (cancelled) break
                set.starlev.scripting.SHContextFactory.beginTracking()
                try {
                    val event = JSCancellableEvent()
                    val jsEvent = org.mozilla.javascript.Context.javaToJS(event, SHScriptEngine.globalScope)
                    val jsMessage = org.mozilla.javascript.Context.javaToJS(message, SHScriptEngine.globalScope)
                    sub.callable.call(cx, SHScriptEngine.globalScope, SHScriptEngine.globalScope, arrayOf(jsMessage, jsEvent))
                    sub.errors = 0
                    if (event.cancelled) cancelled = true
                } catch (e: Throwable) {
                    ScriptConsole.error(sub.moduleName, "Ошибка в onChatCancellable: ${e.message}", e)
                    if (++sub.errors >= errorBudget) {
                        ScriptConsole.error(sub.moduleName, "onChatCancellable отключён после $errorBudget подряд ошибок.")
                        incoming.remove(sub)
                    }
                } finally {
                    set.starlev.scripting.SHContextFactory.endTracking()
                }
            }
        }
        return cancelled
    }

    /**
     * Вызывается из ClientPacketListenerMixin (до отправки на сервер).
     * @return true, если сообщение нужно отменить (не отправлять)
     */
    fun onOutgoing(message: String): Boolean {
        if (outgoing.isEmpty()) return false
        var cancelled = false
        SHScriptEngine.withContext { cx ->
            for (sub in outgoing) {
                if (cancelled) break
                set.starlev.scripting.SHContextFactory.beginTracking()
                try {
                    val event = JSCancellableEvent()
                    val jsEvent = org.mozilla.javascript.Context.javaToJS(event, SHScriptEngine.globalScope)
                    val jsMessage = org.mozilla.javascript.Context.javaToJS(message, SHScriptEngine.globalScope)
                    sub.callable.call(cx, SHScriptEngine.globalScope, SHScriptEngine.globalScope, arrayOf(jsMessage, jsEvent))
                    sub.errors = 0
                    if (event.cancelled) cancelled = true
                } catch (e: Throwable) {
                    ScriptConsole.error(sub.moduleName, "Ошибка в onChatOutgoingCancellable: ${e.message}", e)
                    if (++sub.errors >= errorBudget) {
                        ScriptConsole.error(sub.moduleName, "onChatOutgoingCancellable отключён после $errorBudget подряд ошибок.")
                        outgoing.remove(sub)
                    }
                } finally {
                    set.starlev.scripting.SHContextFactory.endTracking()
                }
            }
        }
        return cancelled
    }
}
