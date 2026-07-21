package set.starlev.events

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Мини event bus для межфичевой коммуникации в StarredHeltix.
 *
 * Вдохновлён Meteor Orbit ([https://github.com/MeteorDevelopment/orbit](https://github.com/MeteorDevelopment/orbit)),
 * но без внешних зависимостей и annotation processor — только Kotlin и ConcurrentHashMap.
 *
 * ## Зачем
 * — Детектор публикует доменный факт (например, «локация изменилась»), фичи подписываются.
 * — Фичи общаются между собой без прямых зависимостей.
 * — Потенциальная замена polling-логики в будущем для других фич.
 *
 * ## API
 * — [subscribe] с reified типом: `EventBus.subscribe<MyEvent> { ... }`
 * — [subscribe] с приоритетом: `EventBus.subscribe<MyEvent>(priority = -10) { ... }`
 * — [unsubscribe] с тем же handler'ом, что и при подписке.
 * — [post] для публикации события.
 * — [clear] / [clear] для типов — для полного сброса (при перезагрузке мира и т.п.).
 *
 * ## Особенности
 * — **Thread-safe**: `ConcurrentHashMap` для реестра, единый `lock` для упорядочивания
 *   handler'ов по приоритету. Snapshot на время `post` — безопасно для unsubscribe в handler'е.
 * — **Cancellable**: если событие implements [Cancellable] и `cancelled = true`,
 *   дальнейшие обработчики не вызываются.
 * — **Приоритеты**: меньше значение = выше приоритет (как в Orbit). Default = 0.
 *   Обработчики с меньшим priority вызываются раньше.
 *
 * ## Пример
 * ```kotlin
 * // Подписка
 * EventBus.subscribe<LocationChangedEvent> { event ->
 *     logger.info("Локация: ${event.location}")
 * }
 *
 * // Подписка с высоким приоритетом (сработает первой)
 * EventBus.subscribe<LocationChangedEvent>(priority = -100) { event ->
 *     // Может отменить, если нужно
 *     if (событие_не_важно) event.cancel()
 * }
 *
 * // Публикация
 * EventBus.post(LocationChangedEvent("Dwarven Mines"))
 * ```
 *
 * ## Когда НЕ использовать
 * — Для событий Minecraft есть Fabric API: `ClientTickEvents`, `ClientReceiveMessageEvents` и т.д.
 *   Они уже оптимизированы для частоты и потокобезопасности.
 * — EventBus — для **внутренних** событий мода (между фичами), не для Minecraft событий.
 */
object EventBus {
    private val LOGGER = LoggerFactory.getLogger("StarredHeltix")

    /** Handler с ID и приоритетом. Меньше priority = раньше вызывается. */
    private data class Handler<T>(val id: Long, val priority: Int, val handler: (T) -> Unit)

    /** Реестр подписок: Class события → отсортированный список handler'ов. */
    private val handlers = ConcurrentHashMap<Class<*>, MutableList<Handler<*>>>()

    /** Один lock для всех мутаций — простой и надёжный. */
    private val lock = Any()

    /** Счётчик ID для создания уникальных [Subscription]. */
    private val idCounter = AtomicLong(0)

    /**
     * Подписывает [handler] на события типа [T].
     *
     * ВАЖНО: всегда используйте явный `<T>` при вызове, иначе Kotlin не выведет тип.
     *
     * @param priority меньше = выше приоритет (раньше вызывается). Default = 0.
     * @param handler лямбда-обработчик. Должна быть stable-ссылкой для [unsubscribe].
     *
     * Примеры:
     * ```
     * EventBus.subscribe<MyEvent> { event -> ... }
     * EventBus.subscribe<MyEvent>(priority = -10) { event -> ... }
     * ```
     */
    inline fun <reified T : Any> subscribe(priority: Int = 0, noinline handler: (T) -> Unit): Subscription {
        return subscribe(T::class.java, priority, handler)
    }

    /**
     * Не-reified версия [subscribe] (для случаев, когда тип известен только как Class).
     * Возвращает [Subscription], который можно использовать для отписки.
     */
    fun <T : Any> subscribe(eventClass: Class<T>, priority: Int = 0, handler: (T) -> Unit): Subscription {
        val id = idCounter.incrementAndGet()
        synchronized(lock) {
            val list = handlers.computeIfAbsent(eventClass) { mutableListOf() }
            val newHandler = Handler(id, priority, handler)
            // Вставка в порядке возрастания priority.
            var inserted = false
            for (i in list.indices) {
                if (list[i].priority > priority) {
                    list.add(i, newHandler)
                    inserted = true
                    break
                }
            }
            if (!inserted) list.add(newHandler)
        }
        return Subscription(id, eventClass)
    }

    /**
     * Отписывается от события по ID подписки.
     */
    fun unsubscribeById(eventClass: Class<*>, id: Long) {
        synchronized(lock) {
            handlers[eventClass]?.removeAll { it.id == id }
        }
    }

    /** @deprecated Используйте [subscribe] возвращающий [Subscription]; unsubscribe не работает с лямбдами по ссылке === */
    @Deprecated("Используйте subscribe() возвращающий Subscription; unsubscribe не работает с лямбдами по ссылке ===")
    inline fun <reified T : Any> unsubscribe(noinline handler: (T) -> Unit) {
        unsubscribe(T::class.java, handler)
    }

    /** @deprecated Используйте [subscribe] возвращающий [Subscription]; unsubscribe не работает с лямбдами по ссылке === */
    @Deprecated("Используйте subscribe() возвращающий Subscription; unsubscribe не работает с лямбдами по ссылке ===")
    fun <T : Any> unsubscribe(eventClass: Class<T>, handler: (T) -> Unit) {
        synchronized(lock) {
            handlers[eventClass]?.removeAll { it.handler === handler }
        }
    }

    /**
     * Публикует [event]. Все подписанные handler'ы вызываются по порядку приоритета.
     *
     * — Snapshot списка handler'ов берётся один раз: unsubscribe внутри handler'а безопасен.
     * — Fast path: если подписчик один, snapshot не создаётся (меньше аллокаций).
     * — Если [event] implements [Cancellable] и `cancelled = true` после handler'а, дальнейшие
     *   обработчики не вызываются.
     * — Ошибка в одном handler'е не прерывает остальные (логируется).
     */
    fun post(event: Any) {
        val list = handlers[event.javaClass] ?: return

        // Snapshot под синхронизацией — безопасно для unsubscribe внутри handler'а
        val snapshot: List<Handler<*>>
        synchronized(lock) {
            snapshot = list.toList()
        }

        for (h in snapshot) {
            try {
                @Suppress("UNCHECKED_CAST")
                (h as Handler<Any>).handler.invoke(event)
                if (event is Cancellable && event.cancelled) break
            } catch (e: Exception) {
                LOGGER.error("[EventBus] Ошибка в обработчике события ${event.javaClass.simpleName}", e)
            }
        }
    }

    /**
     * Удаляет все подписки. Используется при полном сбросе мода (например, перезагрузке мира).
     */
    fun clear() {
        synchronized(lock) {
            handlers.clear()
        }
    }

    /**
     * Удаляет все подписки на конкретный тип события.
     */
    fun clear(eventClass: Class<*>) {
        synchronized(lock) {
            handlers.remove(eventClass)
        }
    }

    /**
     * Возвращает количество подписок на [eventClass] (для отладки и статистики).
     */
    fun subscriberCount(eventClass: Class<*>): Int {
        return handlers[eventClass]?.size ?: 0
    }

    /**
     * Возвращает все классы событий, на которые есть подписки (для отладки).
     */
    fun subscribedEventTypes(): Set<Class<*>> {
        return handlers.keys.toSet()
    }
}

class Subscription(private val id: Long, private val eventClass: Class<*>) {
    fun unsubscribe() {
        EventBus.unsubscribeById(eventClass, id)
    }
}