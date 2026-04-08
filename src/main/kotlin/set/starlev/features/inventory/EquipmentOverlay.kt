package set.starlev.features.inventory

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.TagParser

import net.minecraft.world.item.ItemStack

import set.starlev.StarredHeltix
import set.starlev.events.GuiEvents
import set.starlev.injections.accessors.ContainerScreenAccessor
import set.starlev.utils.detectors.ContainerDetector

object EquipmentOverlay {
    private val mc = Minecraft.getInstance()
    private const val SLOT_SIZE = 16
    private const val SLOT_GAP = 2
    private const val PADDING = 6
    private val EQUIPMENT_CONTAINER_SLOTS = listOf(10, 19, 28, 37)
    private val SLOT_LABELS = listOf("Ожерелье", "Плащ", "Пояс", "Перчатки")
    private val rememberedItems = arrayOfNulls<ItemStack>(4)
    private val wardrobePresets = mutableMapOf<Int, Array<ItemStack>>()
    private var currentWardrobeIndex = -1

    init {
        loadCachedEquipment()
    }

    private fun loadCachedEquipment() {
        val config = StarredHeltix.feature.skyblock.equipmentOverlay
        val cached = config.cachedEquipment
        if (cached.size == 4) {
            for (i in 0 until 4) {
                val nbtString = cached[i]
                if (nbtString.isNotEmpty()) {
                    try {
                        val nbt = TagParser.parseCompoundFully(nbtString)
                        val result = ItemStack.CODEC.parse(NbtOps.INSTANCE, nbt)
                        result.result().ifPresent { stack -> rememberedItems[i] = stack }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun saveCachedEquipment() {
        val config = StarredHeltix.feature.skyblock.equipmentOverlay
        val serialized = rememberedItems.map { stack ->
            if (stack != null && !stack.isEmpty) {
                val result = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack)
                result.result().map { it.toString() }.orElse("")
            } else {
                ""
            }
        }
        config.cachedEquipment = serialized
        StarredHeltix.configManager.saveConfig("equipment-cache")
    }

    fun init() {
        GuiEvents.registerForeground { graphics, mouseX, mouseY, screen ->
            val info = ContainerDetector.getCurrentContainerInfo()
            if (info != null) {
                val normalized = info.normalizedTitle
                if (normalized.contains("экипировка") ||
                    normalized.contains("equipment") ||
                    normalized.contains("аксессуары") ||
                    normalized.contains("прочая статистика") ||
                    normalized.contains("снаряжение")) {
                    rememberEquipment(screen)
                }
                if (normalized.contains("гардероб экипировки")) {
                    processWardrobe(screen)
                }
            }
            render(graphics, screen, mouseX, mouseY)
        }
        GuiEvents.registerClick { mouseX, mouseY, button, screen ->
            if (button != 0) return@registerClick false
            handleClick(screen, mouseX, mouseY)
        }
    }

    private fun rememberEquipment(screen: AbstractContainerScreen<*>) {
        val menu = screen.menu
        rememberedItems.fill(null)
        for ((i, slotIndex) in EQUIPMENT_CONTAINER_SLOTS.withIndex()) {
            if (slotIndex < menu.slots.size) {
                val stack = menu.slots[slotIndex].item
                if (!stack.isEmpty) rememberedItems[i] = stack.copy()
            }
        }
        saveCachedEquipment()
    }

    private fun processWardrobe(screen: AbstractContainerScreen<*>) {
        val menu = screen.menu
        // Ищем лаймовый краситель по имени или ID
        var dyeSlotIndex = -1
        for (i in 0 until menu.slots.size) {
            val stack = menu.slots[i].item
            if (stack.isEmpty) continue
            val itemName = stack.hoverName.string.lowercase()
            val itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.item).toString()
            if (itemName.contains("лайм") && itemName.contains("красител") ||
                itemName.contains("lime") && itemName.contains("dye") ||
                itemId.contains("lime_dye")) {
                dyeSlotIndex = i
                break
            }
        }
        if (dyeSlotIndex == -1) return
        // Слоты экипировки — 4 слота над красителем в той же колонке
        val col = dyeSlotIndex % 9
        val equipmentSlots = mutableListOf<Int>()
        for (row in 0 until 5) { // проверяем до 5 строк выше
            val slotIdx = dyeSlotIndex - (row + 1) * 9
            if (slotIdx >= 0 && slotIdx % 9 == col) {
                equipmentSlots.add(slotIdx)
            }
        }
        // Берём первые 4 слота сверху вниз
        val sortedSlots = equipmentSlots.sorted()
        if (sortedSlots.isNotEmpty()) {
            val items = arrayOfNulls<ItemStack>(4)
            for ((i, slotIdx) in sortedSlots.withIndex()) {
                if (i >= 4) break
                val stack = menu.slots[slotIdx].item
                if (!stack.isEmpty) items[i] = stack.copy()
            }
            val presetIndex = currentWardrobeIndex + 1
            wardrobePresets[presetIndex] = items.filterNotNull().toTypedArray()
            currentWardrobeIndex = presetIndex
            for ((i, item) in items.withIndex()) {
                if (item != null) rememberedItems[i] = item
            }
        }
    }

    private fun render(graphics: GuiGraphics, screen: AbstractContainerScreen<*>, mouseX: Int, mouseY: Int) {
        val config = StarredHeltix.feature.skyblock.equipmentOverlay
        if (!config.enabled) return
        if (screen !is InventoryScreen) return
        val hasAny = rememberedItems.any { it != null && !it.isEmpty }
        if (!hasAny) return

        val accessor = screen as ContainerScreenAccessor
        // Позиция слева от инвентаря (стиль Firmament)
        val startX = accessor.leftPos - SLOT_SIZE - SLOT_GAP * 2 - PADDING * 2 - 4
        val startY = accessor.topPos

        val totalWidth = SLOT_SIZE + PADDING * 2
        val totalHeight = SLOT_SIZE * 4 + SLOT_GAP * 3 + PADDING * 2 + 10 // +10 для заголовка

        // Фон в стиле Firmament — тёмный полупрозрачный
        graphics.fill(startX, startY, startX + totalWidth, startY + totalHeight, 0xE01D1D2B.toInt())
        // Акцентная полоска сверху (голубая как в Firmament)
        graphics.fill(startX, startY, startX + totalWidth, startY + 2, 0xFF3AAFD9.toInt())
        // Тонкая рамка
        val borderColor = 0xFF3AAFD9.toInt()
        graphics.fill(startX, startY + totalHeight - 1, startX + totalWidth, startY + totalHeight, borderColor) // низ
        graphics.fill(startX, startY, startX + 1, startY + totalHeight, borderColor) // лево
        graphics.fill(startX + totalWidth - 1, startY, startX + totalWidth, startY + totalHeight, borderColor) // право

        // Заголовок
        graphics.drawString(mc.font, "§b✦", startX + totalWidth / 2 - 4, startY + 4, 0xFFFFFFFF.toInt(), false)

        // Слоты
        for (i in 0 until 4) {
            val item = rememberedItems[i]
            val slotX = startX + PADDING
            val slotY = startY + PADDING + 12 + i * (SLOT_SIZE + SLOT_GAP)

            // Фон слота (чуть светлее основного)
            graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x60FFFFFF)

            if (item != null && !item.isEmpty) {
                graphics.renderItem(item, slotX, slotY)
                graphics.renderItemDecorations(mc.font, item, slotX, slotY)
            }
        }

        // Tooltip при наведении
        val hoveredSlot = getHoveredSlotIndex(mouseX, mouseY, startX, startY)
        if (hoveredSlot in 0..3) {
            val item = rememberedItems[hoveredSlot]
            if (item != null && !item.isEmpty) {
                graphics.setTooltipForNextFrame(mc.font, item, mouseX, mouseY)
            }
        }
    }

    private fun handleClick(screen: AbstractContainerScreen<*>, mouseX: Double, mouseY: Double): Boolean {
        if (screen !is InventoryScreen) return false
        val config = StarredHeltix.feature.skyblock.equipmentOverlay
        if (!config.enabled) return false
        val accessor = screen as ContainerScreenAccessor
        val startX = accessor.leftPos - SLOT_SIZE - SLOT_GAP * 2 - PADDING * 2 - 4
        val startY = accessor.topPos
        if (getHoveredSlotIndex(mouseX.toInt(), mouseY.toInt(), startX, startY) in 0..3) {
            mc.player?.connection?.sendCommand("equipment")
            return true
        }
        return false
    }

    private fun getHoveredSlotIndex(mouseX: Int, mouseY: Int, startX: Int, startY: Int): Int {
        for (i in 0 until 4) {
            val slotX = startX + PADDING
            val slotY = startY + PADDING + 12 + i * (SLOT_SIZE + SLOT_GAP)
            if (mouseX in slotX until slotX + SLOT_SIZE && mouseY in slotY until slotY + SLOT_SIZE) {
                return i
            }
        }
        return -1
    }
}