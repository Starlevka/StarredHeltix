package set.starlev.scripting.api

/**
 * JS API: Объект события с возможностью отмены.
 * Передаётся последним аргументом в onChatCancellable / onChatOutgoingCancellable.
 */
class JSCancellableEvent {
    var cancelled: Boolean = false
        private set

    /** Отменяет событие (сообщение не будет показано / отправлено). */
    fun cancel() {
        cancelled = true
    }
}
