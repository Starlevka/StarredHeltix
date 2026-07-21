package set.starlev.events

import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack

/**
 * Fired when a slot in the open container changes (from server packet).
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