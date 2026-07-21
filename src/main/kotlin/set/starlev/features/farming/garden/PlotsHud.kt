package set.starlev.features.farming.garden

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import set.starlev.StarredHeltix
import set.starlev.events.GuiEvents
import set.starlev.features.Category
import set.starlev.features.Feature
import set.starlev.injections.accessors.ContainerScreenAccessor
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.ContainerDetector
import set.starlev.utils.detectors.ItemLoreDetector

object GardenPlotsOverlay : Feature(
    name = "Plots HUD",
    category = Category.FARMING,
    description = "HUD участков Garden"
) {

    /** 25 слотов (5×5), null = нет плота */
    private val cachedPlots = arrayOfNulls<PlotSlot>(25).apply {
        val saved = StarredHeltix.feature.farming.garden.plots.cachedPlotNames
        for (i in 0 until 25) {
            val name = saved.getOrElse(i) { "" }
            if (name.isNotEmpty()) {
                this[i] = PlotSlot(i, ItemStack.EMPTY, name)
            }
        }
        if (this[12] == null) {
            this[12] = PlotSlot(12, ItemStack.EMPTY, "Сарай")
        }
    }

    private const val GRID_COLS = 5
    private const val GRID_ROWS = 5
    private const val SLOT_SIZE = 16
    private const val SLOT_GAP = 1
    private const val PADDING = 6
    private const val HEADER_HEIGHT = 10
    private const val EMPTY_AREA_WIDTH = 80
    private const val EMPTY_AREA_HEIGHT = 20

    private data class PlotSlot(
        val slotIndex: Int,   // 0..24 в сетке 5×5
        val item: ItemStack,
        val plotName: String,
    )

    override fun init() {
        GuiEvents.registerForeground { graphics, mouseX, mouseY, screen ->
            detectAndCachePlots(screen)
            render(graphics, screen, mouseX, mouseY)
        }
        GuiEvents.registerClick { mouseX, mouseY, button, screen ->
            if (button != 0) return@registerClick false
            handleClick(screen, mouseX.toInt(), mouseY.toInt())
        }
    }

    private fun detectAndCachePlots(screen: net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<*>) {
        val config = StarredHeltix.feature.farming.garden
        if (!config.plotsEnabled || !GardenApi.inGarden()) return

        val info = ContainerDetector.getCurrentContainerInfo() ?: return
        val title = info.normalizedTitle.lowercase()

        if (title.contains("управление участками") || title.contains("manage plots")) {
            val menu = screen.menu
            val newPlots = arrayOfNulls<PlotSlot>(25)
            for (i in 0 until menu.slots.size) {
                val stack = menu.slots[i].item
                if (stack.isEmpty) continue
                val lore = ItemLoreDetector.getLore(stack)
                if (lore.any { it.contains("телепорта") || it.contains("телепортации") || it.contains("teleport") }) {
                    val name = ColorUtils.stripColor(stack.hoverName.string).trim()
                    // Извлекаем номер участка из имени: "Участок 1" → 1, "Plot 1" → 1
                    val number = extractPlotNumber(name)
                    if (number in 1..25) {
                        newPlots[number - 1] = PlotSlot(number - 1, stack.copy(), name)
                    }
                }
            }
            // Копируем в основной массив (только если нашли хоть что-то)
            if (newPlots.any { it != null }) {
                newPlots.copyInto(cachedPlots)
                val configList = StarredHeltix.feature.farming.garden.plots.cachedPlotNames
                for (i in 0 until 25) {
                    val plot = cachedPlots[i]
                    configList[i] = plot?.plotName ?: ""
                }
                configList[12] = "Сарай"
                StarredHeltix.configManager.saveConfig("plots-update")
            }
            if (cachedPlots[12] == null) {
                cachedPlots[12] = PlotSlot(12, ItemStack.EMPTY, "Сарай")
            }
        }
    }

    /**
     * Извлекает номер участка из названия.
     * "Участок 1" → 1, "Участок 12" → 12, "Plot 5" → 5
     */
    private fun extractPlotNumber(rawName: String): Int {
        val digits = rawName.replace(Regex("[^0-9]"), "")
        return digits.toIntOrNull() ?: -1
    }

    private fun render(graphics: net.minecraft.client.gui.GuiGraphics, screen: net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<*>, mouseX: Int, mouseY: Int) {
        val config = StarredHeltix.feature.farming.garden
        if (!config.plotsEnabled || !GardenApi.inGarden()) return

        val accessor = screen as ContainerScreenAccessor
        val startX = accessor.leftPos + accessor.imageWidth + 4
        val startY = accessor.topPos

        if (cachedPlots.all { it == null }) {
            renderEmptyArea(graphics, startX, startY, mouseX, mouseY)
        } else {
            renderGrid(graphics, startX, startY, mouseX, mouseY)
        }
    }

    private fun renderEmptyArea(graphics: net.minecraft.client.gui.GuiGraphics, startX: Int, startY: Int, mouseX: Int, mouseY: Int) {
        val totalWidth = EMPTY_AREA_WIDTH + PADDING * 2
        val totalHeight = EMPTY_AREA_HEIGHT + PADDING * 2

        val hovered = mouseX in startX until startX + totalWidth &&
                      mouseY in startY until startY + totalHeight

        val bgColor = if (hovered) 0xAA333333.toInt() else 0x801D1D2B.toInt()
        val borderColor = if (hovered) 0xFF3AAFD9.toInt() else 0x803AAFD9.toInt()

        graphics.fill(startX, startY, startX + totalWidth, startY + totalHeight, bgColor)
        graphics.fill(startX, startY, startX + totalWidth, startY + 1, borderColor)
        graphics.fill(startX, startY + totalHeight - 1, startX + totalWidth, startY + totalHeight, borderColor)
        graphics.fill(startX, startY, startX + 1, startY + totalHeight, borderColor)
        graphics.fill(startX + totalWidth - 1, startY, startX + totalWidth, startY + totalHeight, borderColor)

        val text = Component.literal("§7§l/plots")
        val textX = startX + (totalWidth - mc.font.width(text)) / 2
        val textY = startY + (totalHeight - mc.font.lineHeight) / 2
        graphics.drawString(mc.font, text, textX, textY, 0xFFFFFFFF.toInt(), true)
    }

    private fun renderGrid(graphics: net.minecraft.client.gui.GuiGraphics, startX: Int, startY: Int, mouseX: Int, mouseY: Int) {
        val gridPixelWidth = GRID_COLS * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP
        val gridPixelHeight = GRID_ROWS * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP
        val totalWidth = gridPixelWidth + PADDING * 2
        val totalHeight = HEADER_HEIGHT + gridPixelHeight + PADDING * 2

        val bgColor = 0xE01D1D2B.toInt()
        val accentColor = 0xFF3AAFD9.toInt()
        val slotColor = 0x60FFFFFF
        val slotHoverColor = 0xAA3AAFD9.toInt()
        val emptySlotColor = 0x301D1D2B

        // Background
        graphics.fill(startX, startY, startX + totalWidth, startY + totalHeight, bgColor)
        // Top accent
        graphics.fill(startX, startY, startX + totalWidth, startY + 2, accentColor)
        // Border
        graphics.fill(startX, startY + totalHeight - 1, startX + totalWidth, startY + totalHeight, accentColor)
        graphics.fill(startX, startY, startX + 1, startY + totalHeight, accentColor)
        graphics.fill(startX + totalWidth - 1, startY, startX + totalWidth, startY + totalHeight, accentColor)

        // Title
        graphics.drawString(mc.font, Component.literal("§b§lУчастки"), startX + PADDING, startY + 3, 0xFFFFFFFF.toInt(), false)

        val gridStartY = startY + PADDING + HEADER_HEIGHT

        for (gridIndex in 0 until 25) {
            val col = gridIndex % GRID_COLS
            val row = gridIndex / GRID_COLS
            val slotX = startX + PADDING + col * (SLOT_SIZE + SLOT_GAP)
            val slotY = gridStartY + row * (SLOT_SIZE + SLOT_GAP)
            val hovered = mouseX in slotX until slotX + SLOT_SIZE &&
                          mouseY in slotY until slotY + SLOT_SIZE

            val plot = cachedPlots[gridIndex]
            if (plot != null) {
                // Слот с участком
                graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, if (hovered) slotHoverColor else slotColor)
                if (!plot.item.isEmpty) {
                    graphics.renderItem(plot.item, slotX, slotY)
                }
                if (gridIndex == 12) {
                    graphics.drawString(mc.font, Component.literal("§eB"), slotX + 4, slotY + 4, 0xFFFFFFFF.toInt(), false)
                }
            } else {
                // Пустой слот
                graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, emptySlotColor)
                // Рисуем тонкую рамку для пустого слота
                graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + 1, 0x30333333)
                graphics.fill(slotX, slotY + SLOT_SIZE - 1, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x30333333)
                graphics.fill(slotX, slotY, slotX + 1, slotY + SLOT_SIZE, 0x30333333)
                graphics.fill(slotX + SLOT_SIZE - 1, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x30333333)
            }
        }

        // Tooltip при наведении
        val hoveredIndex = getHoveredGridIndex(mouseX, mouseY, startX, gridStartY)
        if (hoveredIndex in 0..24) {
            val plot = cachedPlots[hoveredIndex]
            if (plot != null && !plot.item.isEmpty) {
                graphics.setTooltipForNextFrame(mc.font, plot.item, mouseX, mouseY)
            }
        }
    }

    private fun handleClick(screen: net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<*>, mouseX: Int, mouseY: Int): Boolean {
        val config = StarredHeltix.feature.farming.garden
        if (!config.plotsEnabled || !GardenApi.inGarden()) return false

        val accessor = screen as ContainerScreenAccessor
        val startX = accessor.leftPos + accessor.imageWidth + 4
        val startY = accessor.topPos

        if (cachedPlots.all { it == null }) {
            val totalWidth = EMPTY_AREA_WIDTH + PADDING * 2
            val totalHeight = EMPTY_AREA_HEIGHT + PADDING * 2
            if (mouseX in startX until startX + totalWidth &&
                mouseY in startY until startY + totalHeight) {
                mc.player?.connection?.sendCommand("plots")
                return true
            }
        } else {
            val gridPixelWidth = GRID_COLS * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP
            val gridPixelHeight = GRID_ROWS * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP
            val totalWidth = gridPixelWidth + PADDING * 2
            val totalHeight = HEADER_HEIGHT + gridPixelHeight + PADDING * 2
            val gridStartY = startY + PADDING + HEADER_HEIGHT

            val hoveredIndex = getHoveredGridIndex(mouseX, mouseY, startX, gridStartY)
            if (hoveredIndex in 0..24 && cachedPlots[hoveredIndex] != null) {
                mc.player?.connection?.sendCommand("plots")
                return true
            }
        }
        return false
    }

    private fun getHoveredGridIndex(mouseX: Int, mouseY: Int, startX: Int, gridStartY: Int): Int {
        for (gridIndex in 0 until 25) {
            val col = gridIndex % GRID_COLS
            val row = gridIndex / GRID_COLS
            val slotX = startX + PADDING + col * (SLOT_SIZE + SLOT_GAP)
            val slotY = gridStartY + row * (SLOT_SIZE + SLOT_GAP)
            if (mouseX in slotX until slotX + SLOT_SIZE &&
                mouseY in slotY until slotY + SLOT_SIZE) {
                return gridIndex
            }
        }
        return -1
    }
}
