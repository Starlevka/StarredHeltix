package set.starlev.events

/**
 * Маркер интерфейса для событий, которые можно отменить.
 *
 * Если обработчик события вызвал [cancel], последующие обработчики
 * не будут вызваны (событие считается обработанным).
 *
 * Пример:
 * ```kotlin
 * class SlotClickEvent(...) : Cancellable {
 *     override var cancelled: Boolean = false
 * }
 *
 * EventBus.subscribe<SlotClickEvent>(priority = -100) { event ->
 *     if (должно_заблокировать(event)) event.cancel()
 * }
 * ```
 *
 * Преимущества перед cancellation через возвращаемое значение:
 * — Поддержка нескольких обработчиков на одном событии.
 * — Приоритеты: высокоприоритетный обработчик может отменить до того, как сработают остальные.
 * — Явное состояние: cancelled виден из любого места, а не только из return.
 */
interface Cancellable {
    /** true, если событие было отменено каким-либо обработчиком. */
    var cancelled: Boolean

    /** Отменяет событие. После этого другие обработчики не вызовутся. */
    fun cancel() {
        cancelled = true
    }

    /** @return true, если событие было отменено. */
    fun isCancelled(): Boolean = cancelled
}