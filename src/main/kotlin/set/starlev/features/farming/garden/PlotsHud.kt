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
import set.starlev.utils.stripColor
import set.starlev.utils.detectors.ContainerDetector
import set.starlev.utils.detectors.ItemLoreDetector
import set.starlev.render.drawString
import set.starlev.render.drawCenteredString

object GardenPlotsOverlay : Feature(
    name = "Plots HUD",
    category = Category.FARMING,
    description = "HUD участков Garden"
) {

    private const val GRID_COLS = 5
    private const val GRID_ROWS = 5
    // Геометрия панели сада (26.2):
    // панель 104x114 (без нижних кнопок desk/warp), сетка с шагом 18, origin (7,17),
    // отступ от инвентаря 4px.
    private const val SLOT_SIZE = 16
    private const val SLOT_PITCH = 18
    private const val PANEL_WIDTH = 104
    private const val PANEL_HEIGHT = 114
    private const val GRID_ORIGIN_X = 7
    private const val GRID_ORIGIN_Y = 17
    private const val INVENTORY_GAP = 4

    // Номер плота (Hypixel/Heltix-раскладка) -> ячейка 5x5; плот 13 над сараем и т.д.
    // Индекс = номер плота 1..24; сарай = центр (ячейка 12).
    // Объявлены ДО cachedPlots — object инициализирует поля по порядку.
    private val PLOT_TO_CELL = intArrayOf(
        -1, 7, 11, 13, 17, 6, 8, 16, 18, 2, 10, 14, 22, 1, 3, 5, 9, 15, 19, 21, 23, 0, 4, 20, 24,
    )
    private val CELL_TO_PLOT = IntArray(25) { -1 }.also { inv ->
        for (plot in 1..24) inv[PLOT_TO_CELL[plot]] = plot
    }

    /** 25 слотов (5×5), null = нет плота */
    private val cachedPlots = arrayOfNulls<PlotSlot>(25).apply {
        val saved = StarredHeltix.feature.farming.garden.plots.cachedPlotNames
        val savedItems = try {
            StarredHeltix.feature.farming.garden.plots.cachedPlotItems
        } catch (_: Exception) {
            emptyList<String>()
        }
        for (i in 0 until 25) {
            val name = saved.getOrElse(i) { "" }
            if (name.isEmpty()) continue
            // Старые сейвы хранят плоты в линейной раскладке (N-1); имя несёт номер —
            // переставляем в «ромб» (PLOT_TO_CELL) при загрузке, без переоткрытия меню.
            val digits = name.replace(Regex("[^0-9]"), "").toIntOrNull() ?: -1
            val cell = when {
                name.contains("сарай", true) || name.contains("barn", true) || digits == 0 -> 12
                digits in 1..24 -> PLOT_TO_CELL[digits]
                else -> i
            }
            // NBT-кэш иконки (переживает рестарт); при битом NBT — пустой стек, имя остаётся
            val stack = decodeStack(savedItems.getOrElse(cell) { "" })
            this[cell] = PlotSlot(cell, stack, name)
        }
        if (this[12] == null) {
            this[12] = PlotSlot(12, ItemStack.EMPTY, "Сарай")
        }
    }

    private val SLOT_HIGHLIGHT_BACK = net.minecraft.resources.Identifier.withDefaultNamespace("container/slot_highlight_back")
    private val SLOT_HIGHLIGHT_FRONT = net.minecraft.resources.Identifier.withDefaultNamespace("container/slot_highlight_front")
    // ItemStack нельзя создавать в <clinit> (RULES: компонентные реестры ещё не связаны) — только lazily.
    private val barnIcon: ItemStack by lazy {
        ItemStack(net.minecraft.world.item.Items.LODESTONE).also {
            it.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("§a§lСарай"))
        }
    }

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
            handleClick(screen, mouseX.toInt(), mouseY.toInt(), button)
        }
    }

    private fun isPlayerInventory(screen: net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<*>): Boolean {
        // Инвентарь игрока — только InventoryMenu (не сундуки/меню). Проверяем тип меню вместо класса экрана для совместимости с Creative.
        return screen.menu is net.minecraft.world.inventory.InventoryMenu
    }

    private fun detectAndCachePlots(screen: net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<*>) {
        val config = StarredHeltix.feature.farming.garden
        if (!config.plotsEnabled) return
        // Проверка сада — расширена внутри GardenDetector, но здесь делаем мягкую проверку:
        // даже если inGarden() вернул false из-за кэша, всё равно пробуем детектить по заголовку
        val inGardenCached = GardenApi.inGarden()

        val info = ContainerDetector.getCurrentContainerInfo() ?: return
        val title = info.normalizedTitle.lowercase()
        val decodedTitle = info.decodedTitle.lowercase()
        val isPlotMenu = title.contains("управление участками") ||
                decodedTitle.contains("управление участками") ||
                title.contains("управление") && title.contains("участ") ||
                decodedTitle.contains("управление") && decodedTitle.contains("участ")

        if (!isPlotMenu) return
        // Если это меню участков, игнорируем проверку сада (игрок точно в саду, если открыл его)
        // Но если не в саду — всё равно позволим кэшировать (на случай бага GardenDetector)
        val menu = screen.menu
        val newPlots = arrayOfNulls<PlotSlot>(25)
        val newPestCells = mutableSetOf<Int>()
        var foundAny = false
        for (i in 0 until menu.slots.size) {
            val stack = menu.slots[i].item
            if (stack.isEmpty) continue
            val rawName = stripColor(stack.hoverName.string).trim()
            if (rawName.isEmpty()) continue
            // Должно содержать цифру и указание на участок / сарай / plot / barn
            val hasDigit = rawName.any { it.isDigit() } || rawName.contains("сарай", ignoreCase = true) || rawName.contains("barn", ignoreCase = true)
            if (!hasDigit) continue
            val isPlotName = rawName.contains("участ", ignoreCase = true) ||
                    rawName.contains("plot", ignoreCase = true) ||
                    rawName.contains("сарай", ignoreCase = true) ||
                    rawName.contains("barn", ignoreCase = true) ||
                    rawName.contains("сада", ignoreCase = true)
            if (!isPlotName) continue

            val lore = ItemLoreDetector.getLore(stack)
            val loreText = lore.joinToString(" ").lowercase()
            val hasPlotLore = loreText.contains("телепорта") ||
                    loreText.contains("телепортации") ||
                    loreText.contains("teleport") ||
                    loreText.contains("нажми") ||
                    loreText.contains("click") ||
                    lore.any { it.isNotBlank() } // любая непустая лора уже признак участка

            // Принимаем слот как плот, если имя подходит и есть лора (или даже без лоры, если имя явно "участок N")
            if (!hasPlotLore && !rawName.contains("участ", ignoreCase = true) && !rawName.contains("plot", ignoreCase = true)) continue

            // Раскладка как в реальных садах: не линейный порядок,
            // а «ромб» вокруг сарая — иначе номера плотов плетутся не на своих местах.
            val number = extractPlotNumber(rawName)
            if (rawName.contains("сарай", ignoreCase = true) || rawName.contains("barn", ignoreCase = true) || number == 0) {
                newPlots[12] = PlotSlot(12, stack.copy(), rawName)
                foundAny = true
                continue
            }
            if (number !in 1..24) continue
            val cell = PLOT_TO_CELL[number]
            newPlots[cell] = PlotSlot(cell, stack.copy(), rawName)
            foundAny = true

            // Вредители: Heltix пишет это в лору слота — «На этом участке N ⚠ Вредитель»
            // (вредитель/вредители/вредителя — общая основа «вредител»). Помечаем только
            // те плоты, у которых это реально написано в описании слота.
            if (loreText.contains("вредител") || loreText.contains("pest")) {
                newPestCells.add(cell)
            }
        }
        // Копируем в основной массив (только если нашли хоть что-то)
        if (foundAny) {
            for (i in 0 until 25) {
                if (newPlots[i] != null) cachedPlots[i] = newPlots[i]
            }
            // Гарантируем сарай в центре
            if (cachedPlots[12] == null) {
                cachedPlots[12] = PlotSlot(12, ItemStack.EMPTY, "Сарай")
            }
            // Заражённость = ровно те плоты, в чьей лоре написано «Вредитель»
            infectedCells = newPestCells
            // Имена + NBT иконок (25 ячеек, нормализуем длину — старые сейвы могут быть короче)
            val plotsCfg = StarredHeltix.feature.farming.garden.plots
            plotsCfg.cachedPlotNames = MutableList(25) { i -> cachedPlots[i]?.plotName ?: "" }
            plotsCfg.cachedPlotNames[12] = "Сарай"
            plotsCfg.cachedPlotItems = MutableList(25) { i ->
                val stack = cachedPlots[i]?.item
                if (stack != null && !stack.isEmpty) encodeStack(stack) else ""
            }
            StarredHeltix.configManager.saveConfig("plots-update")
        } else {
            // Фоллбэк: если ничего не нашли, но меню точно плоты — логируем для дебага без спама
        }
        if (cachedPlots[12] == null) {
            cachedPlots[12] = PlotSlot(12, ItemStack.EMPTY, "Сарай")
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

    /** NBT-кэш иконки плота (как cachedEquipment в EquipmentOverlay): переживает рестарт. */
    private fun encodeStack(stack: ItemStack): String {
        return try {
            val result = ItemStack.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, stack)
            result.result().map { it.toString() }.orElse("")
        } catch (_: Exception) {
            ""
        }
    }

    private fun decodeStack(nbtString: String): ItemStack {
        if (nbtString.isEmpty()) return ItemStack.EMPTY
        return try {
            val nbt = net.minecraft.nbt.TagParser.parseCompoundFully(nbtString)
            val result = ItemStack.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, nbt)
            result.result().orElse(ItemStack.EMPTY)
        } catch (_: Exception) {
            ItemStack.EMPTY
        }
    }

    private fun render(graphics: net.minecraft.client.gui.GuiGraphicsExtractor, screen: net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<*>, mouseX: Int, mouseY: Int) {
        val config = StarredHeltix.feature.farming.garden
        if (!config.plotsEnabled || !GardenApi.inGarden()) return
        // ТЗ: плот худ работает только в инвентаре игрока, а не в сундуках/меню
        if (!isPlayerInventory(screen)) return

        val accessor = screen as ContainerScreenAccessor
        val startX = accessor.leftPos + accessor.imageWidth + INVENTORY_GAP
        val startY = accessor.topPos

        renderGrid(graphics, startX, startY, mouseX, mouseY)
    }

    private var infectedCells: Set<Int> = emptySet()

    /** Сброс заражённых грядок при смене мира/сервера. Кэш плотов (cachedPlots) не трогаем. */
    fun onWorldChange() {
        infectedCells = emptySet()
    }

    /** Есть ли заражённые плоты (для тултипа редактора и др.). */
    fun hasInfectedPlots(): Boolean = infectedCells.isNotEmpty()

    private fun renderGrid(graphics: net.minecraft.client.gui.GuiGraphicsExtractor, startX: Int, startY: Int, mouseX: Int, mouseY: Int) {
        // Заражённость берём из detectAndCachePlots (лора слотов меню «Управление участками»):
        // «На этом участке N ⚠ Вредитель». Без открытого меню — последнее известное состояние.

        val gardenPlotsCfg = try { StarredHeltix.feature.farming.garden.plots } catch (_: Exception) { null }
        val showBg = gardenPlotsCfg?.showBackground ?: true
        val showPest = gardenPlotsCfg?.showPests ?: true
        val pestColor = try { ColorUtils.parseColor(gardenPlotsCfg?.pestColorV2 ?: "0:255:255:0:0", 0xFFFF0000.toInt()) } catch (_: Exception) { 0xFFFF0000.toInt() }

        val bgColor = 0xE01D1D2B.toInt()
        val accentColor = 0xFF3AAFD9.toInt()
        val time = System.currentTimeMillis()
        val blink = (time and 512L) != 0L

        // Панель: плоский фон 104x132 минус ряд кнопок
        if (showBg) {
            graphics.fill(startX, startY, startX + PANEL_WIDTH, startY + PANEL_HEIGHT, bgColor)
            graphics.fill(startX, startY, startX + PANEL_WIDTH, startY + 2, accentColor)
            graphics.fill(startX, startY + PANEL_HEIGHT - 1, startX + PANEL_WIDTH, startY + PANEL_HEIGHT, accentColor)
            graphics.fill(startX, startY, startX + 1, startY + PANEL_HEIGHT, accentColor)
            graphics.fill(startX + PANEL_WIDTH - 1, startY, startX + PANEL_WIDTH, startY + PANEL_HEIGHT, accentColor)
        }

        graphics.drawString(mc.font, Component.literal("§b§lУчастки"), startX + 8, startY + 6, 0xFFFFFFFF.toInt(), false)

        var hoveredCell = -1
        for (cell in 0 until 25) {
            val col = cell % GRID_COLS
            val row = cell / GRID_COLS
            val slotX = startX + GRID_ORIGIN_X + col * SLOT_PITCH
            val slotY = startY + GRID_ORIGIN_Y + row * SLOT_PITCH
            // Клик/ховер по всей ячейке 18x18
            val hovered = mouseX in slotX until slotX + SLOT_PITCH &&
                          mouseY in slotY until slotY + SLOT_PITCH
            val isPest = showPest && infectedCells.contains(cell) && blink

            if (hovered) {
                graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK, slotX - 3, slotY - 3, 24, 24)
            }

            val plot = cachedPlots[cell]
            val stack = when {
                plot != null && !plot.item.isEmpty -> plot.item
                cell == 12 -> barnIcon // сарай в центре всегда виден (The Barn)
                else -> null
            }
            if (stack != null) {
                graphics.item(stack, slotX + 1, slotY + 1)
                graphics.itemDecorations(mc.font, stack, slotX + 1, slotY + 1)
            }
            if (hovered) {
                graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT, slotX - 3, slotY - 3, 24, 24)
                hoveredCell = cell
            }

            // Рамка вредителей 16x16 вокруг предмета
            if (isPest) {
                graphics.fill(slotX, slotY, slotX + SLOT_SIZE + 2, slotY + 1, pestColor)
                graphics.fill(slotX, slotY + SLOT_SIZE + 1, slotX + SLOT_SIZE + 2, slotY + SLOT_SIZE + 2, pestColor)
                graphics.fill(slotX, slotY, slotX + 1, slotY + SLOT_SIZE + 2, pestColor)
                graphics.fill(slotX + SLOT_SIZE + 1, slotY, slotX + SLOT_SIZE + 2, slotY + SLOT_SIZE + 2, pestColor)
            }
        }

        if (hoveredCell in 0..24) {
            if (hoveredCell == 12) {
                graphics.setTooltipForNextFrame(mc.font, Component.literal("§eСарай §7ПКМ → §f/plottp 0 §8| §7ЛКМ — /plots"), mouseX, mouseY)
            } else {
                val plot = cachedPlots[hoveredCell]
                if (plot != null && !plot.item.isEmpty) {
                    graphics.setTooltipForNextFrame(mc.font, plot.item, mouseX, mouseY)
                } else if (infectedCells.contains(hoveredCell) && showPest) {
                    graphics.setTooltipForNextFrame(mc.font, Component.literal("§c§lВредители! §7→ §f/plottp ${CELL_TO_PLOT[hoveredCell]}"), mouseX, mouseY)
                }
            }
        }
    }

    private fun handleClick(screen: net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<*>, mouseX: Int, mouseY: Int, button: Int): Boolean {
        val config = StarredHeltix.feature.farming.garden
        if (!config.plotsEnabled || !GardenApi.inGarden()) return false
        if (!isPlayerInventory(screen)) return false

        val accessor = screen as ContainerScreenAccessor
        val startX = accessor.leftPos + accessor.imageWidth + INVENTORY_GAP
        val startY = accessor.topPos
        val onPanel = mouseX in startX until startX + PANEL_WIDTH && mouseY in startY until startY + PANEL_HEIGHT

        // ЛКМ по панели — открыть управление участками
        if (button == 0) {
            if (onPanel) {
                mc.player?.connection?.sendCommand("plots")
                return true
            }
            return false
        }
        // ПКМ по ячейке — телепорт к плоту по его номеру
        if (button != 1) return false
        if (!onPanel) return false

        val cell = getHoveredGridIndex(mouseX, mouseY, startX, startY)
        if (cell !in 0..24) return false
        // Сарай — телепорт 0
        if (cell == 12) {
            mc.player?.connection?.sendCommand("plottp 0")
            return true
        }
        val plot = cachedPlots[cell] ?: return false
        val num = extractPlotNumber(plot.plotName).takeIf { it in 1..24 }
            ?: CELL_TO_PLOT[cell].takeIf { it > 0 }
            ?: return false
        mc.player?.connection?.sendCommand("plottp $num")
        return true
    }

    private fun getHoveredGridIndex(mouseX: Int, mouseY: Int, startX: Int, startY: Int): Int {
        // Ячейки 18x18 с origin (7,17)
        val gx = mouseX - (startX + GRID_ORIGIN_X)
        val gy = mouseY - (startY + GRID_ORIGIN_Y)
        if (gx < 0 || gy < 0) return -1
        val col = gx / SLOT_PITCH
        val row = gy / SLOT_PITCH
        if (col !in 0 until GRID_COLS || row !in 0 until GRID_ROWS) return -1
        return row * GRID_COLS + col
    }
}
