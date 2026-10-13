package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import set.starlev.utils.detectors.ItemLoreDetector

/**
 * JS API: Обёртка предмета.
 */
class JSItem(val mcValue: ItemStack) {
    /** Отображаемое имя (без кодов цвета). */
    fun getName(): String = mcValue.hoverName.string

    /** Идентификатор предмета: "minecraft:diamond_sword". */
    fun getID(): String =
        net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(mcValue.item).toString()

    fun getCount(): Int = mcValue.count

    fun getMaxStackSize(): Int = mcValue.maxStackSize

    fun isEmpty(): Boolean = mcValue.isEmpty

    /** Лор предмета (без имени). */
    fun getLore(): List<String> = ItemLoreDetector.getLore(mcValue)

    /** Полный тултип (с именем). */
    fun getTooltip(): List<String> = ItemLoreDetector.getFullTooltip(mcValue)

    /**
     * Кастомные данные предмета: ключи custom_data (NBT), кастомное имя, глинт.
     */
    fun getNBT(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        val customData = mcValue.get(DataComponents.CUSTOM_DATA)
        if (customData != null) {
            val tag = customData.copyTag()
            for (key in tag.keySet()) {
                result[key] = tag.get(key)?.toString()
            }
        }
        val customName = mcValue.get(DataComponents.CUSTOM_NAME)
        if (customName != null) result["custom_name"] = customName.string
        val glint = mcValue.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)
        if (glint != null) result["glint"] = glint
        return result
    }

    override fun toString(): String = "${getCount()}x ${getName()} [${getID()}]"
}

/**
 * JS API: Объект `inventory` — доступ к инвентарю игрока и открытому контейнеру,
 * с возможностью кликов по слотам.
 */
class JSInventory {
    private val client: Minecraft get() = Minecraft.getInstance()

    // ==================== Инвентарь игрока ====================

    /** Все предметы инвентаря игрока (0-8: хотбар, 9-35: основной). */
    fun getItems(): List<JSItem> {
        val inv = client.player?.inventory ?: return emptyList()
        return inv.nonEquipmentItems.map { JSItem(it) }
    }

    fun getItemInSlot(slot: Int): JSItem? {
        val inv = client.player?.inventory ?: return null
        val items = inv.nonEquipmentItems
        if (slot < 0 || slot >= items.size) return null
        return JSItem(items[slot])
    }
    fun getItemInHotbar(slot: Int): JSItem? = getItemInSlot(slot)

    /** Предмет в левой руке. */
    fun getHeldItem(): JSItem? = client.player?.mainHandItem?.takeIf { !it.isEmpty }?.let { JSItem(it) }

    /** Предмет в правой руке. */
    fun getOffHandItem(): JSItem? = client.player?.offhandItem?.takeIf { !it.isEmpty }?.let { JSItem(it) }

    /** Броня по EquipmentSlot: "feet", "legs", "chest", "head". */
    fun getArmorItem(part: String): JSItem? {
        val slot = when (part.lowercase()) {
            "feet", "boots" -> net.minecraft.world.entity.EquipmentSlot.FEET
            "legs", "leggings" -> net.minecraft.world.entity.EquipmentSlot.LEGS
            "chest", "chestplate" -> net.minecraft.world.entity.EquipmentSlot.CHEST
            "head", "helmet" -> net.minecraft.world.entity.EquipmentSlot.HEAD
            "offhand" -> net.minecraft.world.entity.EquipmentSlot.OFFHAND
            else -> return null
        }
        val stack = client.player?.getItemBySlot(slot) ?: return null
        return stack.takeIf { !it.isEmpty }?.let { JSItem(it) }
    }

    /** Индекс выбранного слота хотбара (0-8). */
    fun getSelectedHotbarSlot(): Int = client.player?.inventory?.selectedSlot ?: 0

    fun setSelectedHotbarSlot(slot: Int) {
        if (slot in 0..8) client.player?.inventory?.selectedSlot = slot
    }

    /** Найти слот предмета по имени (частичное совпадение, без регистра). */
    fun findSlotByName(name: String): Int? {
        val inv = client.player?.inventory ?: return null
        val items = inv.nonEquipmentItems
        return items.indexOfFirst { !it.isEmpty && it.hoverName.string.contains(name, ignoreCase = true) }
            .takeIf { it >= 0 }
    }
    // ==================== Открытый контейнер (GUI) ====================

    /** true, если открыт любой контейнер (сундук, GUI Skyblock и т.п.). */
    fun isInContainer(): Boolean =
        client.gui.screen() is net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<*>

    /**
     * Предметы открытого контейнера. Для Skyblock GUI это все видимые слоты
     * (включая "кнопки" — конфирмы, апгрейды и т.д.).
     */
    fun getContainerItems(): List<JSItem> {
        val player = client.player ?: return emptyList()
        val menu = player.containerMenu
        return menu.slots.map { JSItem(it.item) }
    }

    fun getContainerItemInSlot(slot: Int): JSItem? {
        val player = client.player ?: return null
        val menu = player.containerMenu
        if (slot < 0 || slot >= menu.slots.size) return null
        return JSItem(menu.slots[slot].item)
    }

    /** Заголовок открытого контейнера. */
    fun getContainerTitle(): String? =
        (client.gui.screen() as? net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<*>)?.title?.string

    /**
     * Клик по слоту открытого контейнера (PICKUP / SHIFT / CLONE).
     * @param slot номер слота в меню
     * @param clickType "left", "right", "shift" или "middle"
     */
    fun clickContainerSlot(slot: Int, clickType: String = "left") {
        val player = client.player ?: return
        val menu = player.containerMenu
        if (slot < 0 || slot >= menu.slots.size) return
        val input = when (clickType.lowercase()) {
            "shift" -> net.minecraft.world.inventory.ContainerInput.QUICK_MOVE
            "middle" -> net.minecraft.world.inventory.ContainerInput.CLONE
            else -> net.minecraft.world.inventory.ContainerInput.PICKUP
        }
        val button = if (clickType.equals("right", ignoreCase = true)) 1 else 0
        client.execute {
            client.gameMode?.handleContainerInput(menu.containerId, slot, button, input, player)
        }
    }

    /**
     * Drag-распределение (зажать ЛКМ и растащить по слотам).
     * Эквивалент: клик по startSlot (взять стак) → клики по слотам (положить по одному) → клик по endSlot (оставить остаток).
     *
     * @param slots последовательность слотов; первый — откуда берём, остальные — куда раскладываем
     */
    fun drag(vararg slots: Int) {
        if (slots.size < 2) return
        val player = client.player ?: return
        val menu = player.containerMenu
        val valid = slots.filter { it in menu.slots.indices }
        if (valid.size < 2) return
        client.execute {
            // Взять весь стак из первого слота
            client.gameMode?.handleContainerInput(menu.containerId, valid[0], 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player)
            // Разложить по одному предмету в каждый следующий слот
            for (i in 1 until valid.size) {
                client.gameMode?.handleContainerInput(menu.containerId, valid[i], 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player)
            }
            // Вернуть остаток в исходный слот
            client.gameMode?.handleContainerInput(menu.containerId, valid[0], 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player)
        }
    }

    /** Закрыть открытый контейнер. */
    fun closeContainer() {
        client.execute {
            client.player?.closeContainer()
        }
    }
}
