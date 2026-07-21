package set.starlev.events

import set.starlev.StarredHeltix
import java.util.concurrent.CopyOnWriteArrayList

class TypedEvent<T> {
    private val listeners = CopyOnWriteArrayList<(T) -> Unit>()

    fun subscribe(listener: (T) -> Unit): Runnable {
        listeners.add(listener)
        return Runnable { listeners.remove(listener) }
    }

    fun unsubscribe(listener: (T) -> Unit) {
        listeners.remove(listener)
    }

    fun post(event: T) {
        for (listener in listeners) {
            try {
                listener(event)
            } catch (e: Exception) {
                StarredHeltix.LOGGER.error("[TypedEvent] Ошибка в слушателе: ${e.message}", e)
            }
        }
    }

    fun clear() {
        listeners.clear()
    }

    fun listenerCount(): Int = listeners.size
}
