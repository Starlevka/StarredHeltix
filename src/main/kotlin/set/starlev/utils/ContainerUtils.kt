package set.starlev.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

object ContainerUtils {
    private val mc = Minecraft.getInstance()

    /**
     * Возвращает список всех слотов контейнера (без учета инвентаря игрока)
     */
    fun getContainerSlots(screen: AbstractContainerScreen<*>): List<Slot> {
        val menu = screen.menu
        val totalSlots = menu.slots.size
        // Обычно последние 36 слотов - это инвентарь игрока
        val containerSlotCount = totalSlots - 36
        if (containerSlotCount <= 0) return emptyList()
        
        return menu.slots.subList(0, containerSlotCount)
    }

    /**
     * Возвращает список предметов в контейнере (без учета инвентаря игрока)
     */
    fun getContainerItems(screen: AbstractContainerScreen<*>): List<ItemStack> {
        return getContainerSlots(screen).map { it.item }
    }

    /**
     * Проверяет, содержит ли контейнер определенный набор предметов в определенных слотах.
     * Полезно для идентификации меню без использования названия.
     */
    fun matchContainerStructure(screen: AbstractContainerScreen<*>, requiredMatches: Map<Int, (ItemStack) -> Boolean>): Boolean {
        val slots = screen.menu.slots
        for ((index, predicate) in requiredMatches) {
            if (index >= slots.size) return false
            if (!predicate(slots[index].item)) return false
        }
        return true
    }

    /**
     * Получает "отпечаток" контейнера для отладки.
     */
    fun getContainerFingerprint(screen: AbstractContainerScreen<*>): String {
        val items = getContainerItems(screen)
        val size = items.size
        val itemNames = items.mapIndexed { index, stack -> 
            if (stack.isEmpty) "Empty" else stack.item.toString()
        }.joinToString(", ")
        
        return "Size: $size, Items: [$itemNames]"
    }
}
