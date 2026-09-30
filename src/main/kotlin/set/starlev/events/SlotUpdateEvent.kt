package set.starlev.events

import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack

/**
 * Вызывается, когда слот открытого контейнера меняется (пакетом с сервера).
 *
 * Источник:
 * — `ClientboundContainerSetSlotPacket` — обновление одного слота.
 * — `ClientboundContainerSetContentPacket` — полная синхронизация (при открытии).
 *
 * Публикуется в [EventBus] через [ContainerEventWire].
 * Солверы подписываются на это событие вместо ванильного [net.minecraft.world.inventory.ContainerListener].
 */
data class SlotUpdateEvent(
    val menu: AbstractContainerMenu,
    val slotId: Int,
    val stack: ItemStack,
)