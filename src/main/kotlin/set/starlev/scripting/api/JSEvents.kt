package set.starlev.scripting.api

import org.mozilla.javascript.Callable
import set.starlev.scripting.SHEventBus
import set.starlev.scripting.ScriptConsole
import set.starlev.scripting.ScriptChatHandler
import set.starlev.scripting.SHPriority
import set.starlev.scripting.SHScriptEngine
import set.starlev.scripting.SHTrigger

/**
 * JS API: Объект `events` для подписки на игровые события
 */
class JSEvents(private val moduleName: String) {

    fun onTick(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "tick", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    /** Каждый кадр (FPS, ~60-240 Гц) — для плавной камеры/рендера. Алиасы: onFrame/onRender/onFps */
    fun onFps(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "fps", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }
    fun onFrame(callable: Callable): SHTrigger = onFps(callable)
    fun onRender(callable: Callable): SHTrigger = onFps(callable)

    fun onChat(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "chat", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    /**
     * Отменяемый чат: вызывается ДО показа сообщения (может быть на сетевом потоке).
     * Колбэк получает `(message, event)` — `event.cancel()` скрывает сообщение.
     */
    fun onChatCancellable(callable: Callable) {
        ScriptChatHandler.addIncoming(moduleName, callable)
    }

    /**
     * Отменяемый исходящий чат: вызывается ДО отправки на сервер.
     * Колбэк получает `(message, event)` — `event.cancel()` отменяет отправку.
     */
    fun onChatOutgoingCancellable(callable: Callable) {
        ScriptChatHandler.addOutgoing(moduleName, callable)
    }

    fun onChatOutgoing(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "chatOutgoing", callable, SHPriority.HIGH)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    fun onWorldRender(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "worldRender", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    fun onOverlay(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "overlay", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    fun onGuiOpen(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "guiOpen", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    fun onGuiClose(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "guiClose", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    fun onGuiClick(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "guiClick", callable, SHPriority.HIGH)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    fun onEntityDeath(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "entityDeath", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    fun onSlotUpdate(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "slotUpdate", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    fun onSkillXp(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "skillXp", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    fun onKey(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "key", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    /**
     * Звук: обычная подписка — триггер "soundPlay", вызывается для ЛЮБОГО звука (без критерия/отмены).
     * Источник: SoundManagerMixin.
     */
    fun onSoundPlay(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "soundPlay", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    /**
     * Звук с критерием + отменой (CTJS-паритет). Слушатель живёт в ScriptSoundHandler,
     * вызывается из SoundEngineMixin с фильтрацией по [criteria] (подстрока или /regex/)
     * и может отменить проигрывание через `event.cancel()`.
     * Возвращаемый хэндл: `trigger.unregister()` снимает именно эту подписку.
     */
    fun onSoundPlayWithCriteria(criteria: String, callable: Callable): SHTrigger {
        val entry = set.starlev.scripting.ScriptSoundHandler.add(moduleName, callable, criteria)
        val trigger = SHTrigger(moduleName, "soundPlay", callable) // только как хэндл, в движок не регистрируем
        trigger.onUnregisterHook = { set.starlev.scripting.ScriptSoundHandler.remove(entry) }
        return trigger
    }

    fun onPacketReceive(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "packetReceive", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    fun onPacketSend(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "packetSend", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    /** Пометка «пока нет источника-события»: не тихо-нооп, а явный warning, хэндл инертен. */
    private fun unsupported(name: String, callable: Callable): SHTrigger {
        ScriptConsole.warn(moduleName, "events.$name пока не поддерживается в этой сборке (нет источника-события). Подписка вызываться НЕ будет.")
        return SHTrigger(moduleName, "__unsupported__", callable)
    }

    fun onSpawnParticle(callable: Callable): SHTrigger = unsupported("onSpawnParticle", callable)
    fun onRenderEntity(callable: Callable): SHTrigger = unsupported("onRenderEntity", callable)
    fun onRenderBlockEntity(callable: Callable): SHTrigger = unsupported("onRenderBlockEntity", callable)
    fun onDrawBlockHighlight(callable: Callable): SHTrigger = unsupported("onDrawBlockHighlight", callable)
    fun onGuiKey(callable: Callable): SHTrigger = unsupported("onGuiKey", callable)
    fun onGuiMouseClick(callable: Callable): SHTrigger = unsupported("onGuiMouseClick", callable)
    fun onGuiMouseDrag(callable: Callable): SHTrigger = unsupported("onGuiMouseDrag", callable)
    fun onPlayerInteract(callable: Callable): SHTrigger = unsupported("onPlayerInteract", callable)
    fun onItemTooltip(callable: Callable): SHTrigger = unsupported("onItemTooltip", callable)
    fun onDropItem(callable: Callable): SHTrigger = unsupported("onDropItem", callable)
    fun onMessageSent(callable: Callable): SHTrigger = unsupported("onMessageSent", callable)

    // ==================== Редкие события (SHGameEvents) ====================

    /** Игрок поднял предмет: (itemId: String, amount: Int). */
    fun onPickup(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "pickup", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    /** Урон: (entityName: String, damageTypeId: String, isSelf: Boolean). */
    fun onDamage(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "damage", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    /** Вход на сервер. */
    fun onConnect(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "connect", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    /** Выход с сервера. */
    fun onDisconnect(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "disconnect", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    /** Сущность загрузилась в мир: (name, typeId, uuid). */
    fun onEntityLoad(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "entityLoad", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    /** Сущность выгрузилась из мира: (name, typeId, uuid). */
    fun onEntityUnload(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "entityUnload", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    /** Начало дня (рассвет). */
    fun onDayStart(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "dayStart", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    /** Начало ночи. */
    fun onNightStart(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "nightStart", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    /** Открыт любой экран (не только контейнер): (screenClassName). */
    fun onScreenOpen(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "screenOpen", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    /** Закрыт любой экран: (screenClassName). */
    fun onScreenClose(callable: Callable): SHTrigger {
        val trigger = SHTrigger(moduleName, "screenClose", callable)
        SHScriptEngine.registerTrigger(trigger)
        return trigger
    }

    // ==================== Межмодульные события (SHEventBus) ====================

    /**
     * Подписка на межмодульное событие: `events.on("мойка-событие", fn)`.
     * Данные придут от чужого `events.emit(...)`. Системные имена зарезервированы: "sh.*".
     */
    fun on(event: String, callable: Callable) {
        SHEventBus.on(moduleName, event, callable)
    }

    /** Одноразовая подписка: снимается после первого срабатывания. */
    fun once(event: String, callable: Callable) {
        val wrapper = object : Callable {
            var listener: SHEventBus.BusListener? = null
            override fun call(cx: org.mozilla.javascript.Context, scope: org.mozilla.javascript.Scriptable, thisObj: org.mozilla.javascript.Scriptable, args: Array<out Any>?): Any? {
                listener?.let { SHEventBus.removeListener(event, it) }
                return callable.call(cx, scope, thisObj, args)
            }
        }
        val listener = SHEventBus.BusListener(moduleName, wrapper)
        wrapper.listener = listener
        SHEventBus.addListener(event, listener)
    }

    /** Публикация межмодульного события. @return количество получателей. */
    fun emit(event: String, vararg data: Any?): Int {
        if (event.lowercase().startsWith("sh.")) {
            ScriptConsole.warn(moduleName, "Префикс 'sh.' зарезервирован: событие '$event' не отправлено")
            return 0
        }
        return SHEventBus.emit(event, *data)
    }
}
