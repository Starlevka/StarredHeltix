package set.starlev.events

import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerListener
import net.minecraft.world.item.ItemStack
import org.slf4j.LoggerFactory

/**
 * Обёртка вокруг ванильного [ContainerListener] для публикации обновлений слотов в [EventBus].
 *
 * Зачем:
 * — Заменяет прямой `ContainerListener` в солверах на подписку на [SlotUpdateEvent] через EventBus.
 * — Decoupled: солвер подписывается на EventBus, не знает про [ContainerListener].
 * — Можно подписаться из любого места (другие фичи, HUD и т.д.).
 *
 * Использование:
 * ```
 * // При открытии контейнера:
 * ContainerEventWire.attach(menu)
 *
 * // При закрытии:
 * ContainerEventWire.detach(menu)
 * ```
 *
 * После `attach()` все обновления слотов будут опубликованы как [SlotUpdateEvent] в [EventBus].
 * Каждый listener хранится в map — можно аттачить несколько menu одновременно (вложенные GUI).
 */
object ContainerEventWire {
    private val LOGGER = LoggerFactory.getLogger("StarredHeltix")

    /** Активные форвардеры по меню. */
    private val forwarders = mutableMapOf<AbstractContainerMenu, SlotUpdateForwarder>()

    fun init() {
        LOGGER.info("[ContainerEventWire] Готов к подключению контейнеров")
    }

    /**
     * Подключает форвардер к [menu] — каждое обновление слота будет опубликовано в [EventBus].
     * Если меню уже подключено — ничего не делает (идемпотентно).
     */
    fun attach(menu: AbstractContainerMenu) {
        if (forwarders.containsKey(menu)) return
        val forwarder = SlotUpdateForwarder()
        menu.addSlotListener(forwarder)
        forwarders[menu] = forwarder
        LOGGER.info(
            "[ContainerEventWire] attach: containerId=${menu.containerId}, " +
                "class=${menu.javaClass.simpleName}"
        )
    }

    /**
     * Отключает форвардер от [menu].
     */
    fun detach(menu: AbstractContainerMenu) {
        val forwarder = forwarders.remove(menu) ?: return
        try {
            menu.removeSlotListener(forwarder)
        } catch (_: Exception) {
            // Меню уже закрыто — не страшно.
        }
    }

    /** Отключает все форвардеры. */
    fun detachAll() {
        for ((menu, forwarder) in forwarders) {
            try {
                menu.removeSlotListener(forwarder)
            } catch (_: Exception) {}
        }
        forwarders.clear()
    }
}

/**
 * [ContainerListener], который публикует каждое обновление слота как [SlotUpdateEvent] в [EventBus].
 * Внутренний класс [ContainerEventWire].
 */
private class SlotUpdateForwarder : ContainerListener {
    override fun slotChanged(menu: AbstractContainerMenu, slotId: Int, stack: ItemStack) {
        EventBus.post(SlotUpdateEvent(menu, slotId, stack))
    }

    override fun dataChanged(menu: AbstractContainerMenu, dataSlotIndex: Int, value: Int) {
        // Не используется солверами; игнорируем.
    }
}