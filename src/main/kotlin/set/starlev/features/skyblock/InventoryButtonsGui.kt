package set.starlev.features.skyblock

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.CharacterEvent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW
import set.starlev.render.SHBackdrop
import set.starlev.render.SHFooter
import set.starlev.render.SHHeader
import set.starlev.render.SHListHelper
import set.starlev.render.SHRow
import set.starlev.render.SHTokens
import set.starlev.render.drawString
import set.starlev.render.drawCenteredString

/**
 * Меню настройки кнопок инвентаря — улучшенный UI в стиле NEU / Inventory Buttons (Fabric Port) и Firmament.
 * Фичи как в других модах:
 * - Поиск иконки с превью и подсказками (как NEU item picker)
 * - Единая форма для icon/command/tooltip (не стадийный ввод)
 * - Пресеты быстрых команд
 * - Перемещение кнопок ↑/↓
 * - Предпросмотр иконки в списке
 */
class InventoryButtonsGui(private val parent: Screen? = null) : Screen(Component.literal("Кнопки инвентаря")) {
    private var isAdding = false
    private var editingIndex: Int? = null

    private var formIcon = ""
    private var formCommand = ""
    private var formTooltip = ""
    private var focusedField = 0 // 0=icon,1=command,2=tooltip
    private var scrollOffset = 0

    private val buttons get() = InventoryButtonsManager.getButtons()

    // Пресеты как в NEU / Firmament
    private val quickPresets = listOf(
        Triple("oak_door", "/hub", "Хаб"),
        Triple("grass_block", "/warp hub", "Хаб варп"),
        Triple("ender_pearl", "/warp dungeon_hub", "Данжи"),
        Triple("iron_sword", "/sbmenu", "Скайблок меню"),
        Triple("chest", "/storage", "Хранилище"),
        Triple("ender_chest", "/ec", "Эндер сундук"),
    )

    private fun getIconSuggestions(query: String): List<Identifier> {
        if (query.length < 2) return emptyList()
        val q = query.lowercase().trim().removePrefix("minecraft:")
        return BuiltInRegistries.ITEM.keySet().filter { it.path.contains(q) }.take(8)
    }

    private fun getPreviewItem(iconStr: String): ItemStack {
        if (iconStr.isBlank()) return ItemStack.EMPTY
        return try {
            val id = if (iconStr.contains(":")) Identifier.tryParse(iconStr) else Identifier.tryParse("minecraft:$iconStr")
            val item = id?.let { BuiltInRegistries.ITEM.getOptional(it).orElse(null) }
            item?.defaultInstance ?: ItemStack.EMPTY
        } catch (_: Exception) { ItemStack.EMPTY }
    }

    private fun getFormBounds(centerX: Int, half: Int): Triple<Int, Int, Int> {
        val left = centerX - half
        val formY = 40
        val formH = 92 // 3 поля + пресеты + кнопки
        return Triple(left, formY, formH)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        SHHeader.draw(graphics, font, title, "Всего кнопок: ${buttons.size} / ${InventoryButton.MAX_TABS}", "Клик по иконке — превью • Tab — переключение полей", width)
        val centerX = width / 2
        val cardW = SHTokens.CONTENT_WIDTH_WIDE
        val half = cardW / 2
        val left = centerX - half
        val right = centerX + half

        // Верхняя карточка — форма добавления/редактирования
        val (formLeft, formY, formH) = getFormBounds(centerX, half)
        if (isAdding) {
            SHRow.draw(graphics, set.starlev.features.visual.swing.Rect(formLeft, formY, cardW, formH), false, true)
            val fieldW = cardW - 70
            val fieldH = 18
            val startX = formLeft + 10
            var y = formY + 8
            val labels = listOf("Иконка", "Команда", "Подсказка")
            val values = listOf(formIcon, formCommand, formTooltip)
            for (i in 0..2) {
                val isFocused = focusedField == i
                val fieldY = y
                graphics.drawString(font, "${labels[i]}:", startX, fieldY + 4, if (isFocused) SHTokens.ACCENT.toInt() else SHTokens.TEXT_MUTED.toInt())
                val fx = startX + 65
                val bg = if (isFocused) 0xFF2A2A2A.toInt() else 0xFF1A1A1A.toInt()
                val border = if (isFocused) SHTokens.ACCENT.toInt() else 0xFF3A3A3A.toInt()
                graphics.fill(fx, fieldY, fx + fieldW, fieldY + fieldH, bg)
                graphics.fill(fx, fieldY + fieldH - 1, fx + fieldW, fieldY + fieldH, border)
                val text = values[i]
                val display = if (text.isEmpty()) "§8..." else if (isFocused) "§f$text§b_" else "§f$text"
                // Обрезаем отображение если слишком длинное
                val shown = if (font.width(display) > fieldW - 10) font.plainSubstrByWidth(display, fieldW - 16) else display
                graphics.drawString(font, shown, fx + 4, fieldY + 5, -1)
                // Превью иконки для поля иконки
                if (i == 0) {
                    val preview = getPreviewItem(formIcon)
                    if (!preview.isEmpty) {
                        graphics.item(preview, fx + fieldW + 8, fieldY + 1)
                    } else if (formIcon.isNotBlank()) {
                        graphics.drawString(font, "§c?", fx + fieldW + 12, fieldY + 5, -1)
                    }
                }
                y += 20
            }
            // Пресеты быстрых иконок
            y += 2
            graphics.drawString(font, "§7Пресеты:", startX, y, SHTokens.TEXT_DIM.toInt())
            var px = startX + 55
            for ((icon, _, label) in quickPresets.take(6)) {
                val isHover = mouseX in px..(px + 16) && mouseY in y..(y + 16)
                if (isHover) graphics.fill(px - 2, y - 2, px + 18, y + 18, 0x33FFFFFF)
                val it = getPreviewItem(icon)
                if (!it.isEmpty) graphics.item(it, px, y)
                px += 20
            }
            // Кнопки Сохранить / Отмена
            val btnSaveX = formLeft + cardW - 140
            val btnCancelX = formLeft + cardW - 70
            val btnY = formY + formH - 22
            val saveHover = mouseX in btnSaveX..(btnSaveX + 60) && mouseY in btnY..(btnY + 16)
            val cancelHover = mouseX in btnCancelX..(btnCancelX + 60) && mouseY in btnY..(btnY + 16)
            val saveEnabled = formIcon.isNotBlank() && formCommand.isNotBlank() && !getPreviewItem(formIcon).isEmpty
            SHRow.drawButton(graphics, font, set.starlev.features.visual.swing.Rect(btnSaveX, btnY, 60, 16), "§aСохранить", false, saveHover, saveEnabled, set.starlev.features.visual.swing.PixelButtonTone.CONFIRM)
            SHRow.drawButton(graphics, font, set.starlev.features.visual.swing.Rect(btnCancelX, btnY, 60, 16), "Отмена", false, cancelHover, true, set.starlev.features.visual.swing.PixelButtonTone.NORMAL)

            // Подсказки для иконки — дропдаун
            if (focusedField == 0 && formIcon.length >= 2) {
                val suggestions = getIconSuggestions(formIcon)
                if (suggestions.isNotEmpty()) {
                    val dropY = formY + 8 + 20 // под первым полем
                    val dropH = suggestions.size * 18 + 4
                    val dropX = startX + 65
                    graphics.fill(dropX, dropY, dropX + fieldW, dropY + dropH, 0xFF0F0F0F.toInt())
                    graphics.fill(dropX, dropY, dropX + fieldW, dropY + 1, SHTokens.ACCENT.toInt())
                    var sy = dropY + 2
                    for (id in suggestions) {
                        val isHover = mouseX in dropX..(dropX + fieldW) && mouseY in sy..(sy + 18)
                        if (isHover) graphics.fill(dropX + 1, sy, dropX + fieldW - 1, sy + 18, 0x33FFFFFF)
                        val item = BuiltInRegistries.ITEM.getOptional(id).orElse(null)?.defaultInstance ?: ItemStack.EMPTY
                        if (!item.isEmpty) graphics.item(item, dropX + 4, sy + 1)
                        graphics.drawString(font, "§7${id.path}", dropX + 24, sy + 5, -1)
                        sy += 18
                    }
                }
            }
        } else {
            // Кнопка добавления
            SHRow.draw(graphics, set.starlev.features.visual.swing.Rect(left, formY, cardW, 26), false, false)
            graphics.drawCenteredString(font, Component.literal("§a+ Добавить кнопку  §7(как в NEU / Firmament)"), centerX, formY + 9, -1)
        }

        // Заголовки списка
        val listTop = if (isAdding) formY + formH + 12 else 78
        graphics.drawString(font, "§7Иконка", left + 10, listTop - 12, SHTokens.TEXT_MUTED.toInt())
        graphics.drawString(font, "§7Команда", left + 46, listTop - 12, SHTokens.TEXT_MUTED.toInt())
        graphics.drawString(font, "§7Подсказка", left + 190, listTop - 12, SHTokens.TEXT_MUTED.toInt())

        val listBottom = height - 40
        val itemHeight = 28
        val totalHeight = buttons.size * itemHeight
        scrollOffset = SHListHelper.clampScroll(scrollOffset, totalHeight, listBottom - listTop)
        graphics.enableScissor(left, listTop, right, listBottom)
        var y = listTop - scrollOffset
        for ((index, btn) in buttons.withIndex()) {
            if (y + itemHeight < listTop) { y += itemHeight; continue }
            if (y > listBottom) break
            SHRow.draw(graphics, set.starlev.features.visual.swing.Rect(left, y, cardW, itemHeight - 2), false)
            val preview = btn.getItem()
            if (!preview.isEmpty) graphics.item(preview, left + 10, y + 5) else graphics.drawString(font, "§8?", left + 14, y + 8, -1)
            val cmdDisplay = (btn.command ?: "").let { if (it.length > 18) it.take(15) + "..." else it }
            val tipDisplay = (btn.tooltip ?: "").let { if (it.length > 14) it.take(12) + "..." else it.ifEmpty { "§8-" } }
            graphics.drawString(font, "§f$cmdDisplay", left + 46, y + 9, -1)
            graphics.drawString(font, "§8$tipDisplay", left + 190, y + 9, -1)
            // Кнопки управления справа: ↑ ↓ ✎ X
            val btnUpX = right - 78
            val btnDownX = right - 60
            val btnEditX = right - 42
            val btnDelX = right - 24
            val isHoverUp = mouseX in btnUpX..(btnUpX + 14) && mouseY in y..(y + itemHeight)
            val isHoverDown = mouseX in btnDownX..(btnDownX + 14) && mouseY in y..(y + itemHeight)
            val isHoverEdit = mouseX in btnEditX..(btnEditX + 14) && mouseY in y..(y + itemHeight)
            val isHoverDel = mouseX in btnDelX..(btnDelX + 14) && mouseY in y..(y + itemHeight)
            graphics.drawString(font, if (isHoverUp) "§e↑" else "§7↑", btnUpX, y + 9, -1)
            graphics.drawString(font, if (isHoverDown) "§e↓" else "§7↓", btnDownX, y + 9, -1)
            graphics.drawString(font, if (isHoverEdit) "§e✎" else "§7✎", btnEditX, y + 9, -1)
            graphics.drawString(font, if (isHoverDel) "§c✕" else "§7x", btnDelX, y + 9, -1)
            y += itemHeight
        }
        graphics.disableScissor()
        SHListHelper.drawScrollbar(graphics, set.starlev.features.visual.swing.Rect(left, listTop, cardW, listBottom - listTop), totalHeight, scrollOffset, listBottom - listTop)
        SHFooter.draw(graphics, font, width, height)
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()
        val button = event.button()
        val centerX = width / 2
        val cardW = SHTokens.CONTENT_WIDTH_WIDE
        val half = cardW / 2
        val left = centerX - half
        val right = centerX + half
        val (formLeft, formY, formH) = getFormBounds(centerX, half)

        if (button == 0 && isAdding) {
            // Фокус полей
            val fieldW = cardW - 70
            val startX = formLeft + 10
            var y = formY + 8
            for (i in 0..2) {
                val fx = startX + 65
                if (mouseX in fx..(fx + fieldW) && mouseY in y..(y + 18)) {
                    focusedField = i
                    return true
                }
                y += 20
            }
            // Выбор пресета
            var px = startX + 55
            val presetY = formY + 8 + 60
            for ((icon, cmd, tip) in quickPresets.take(6)) {
                if (mouseX in px..(px + 16) && mouseY in presetY..(presetY + 16)) {
                    formIcon = icon
                    if (formCommand.isEmpty()) formCommand = cmd
                    if (formTooltip.isEmpty()) formTooltip = tip
                    focusedField = 0
                    return true
                }
                px += 20
            }
            // Дропдаун подсказок
            if (focusedField == 0 && formIcon.length >= 2) {
                val suggestions = getIconSuggestions(formIcon)
                if (suggestions.isNotEmpty()) {
                    val dropY = formY + 8 + 20
                    val dropX = startX + 65
                    var sy = dropY + 2
                    for (id in suggestions) {
                        if (mouseX in dropX..(dropX + fieldW) && mouseY in sy..(sy + 18)) {
                            formIcon = id.toString()
                            focusedField = 1
                            return true
                        }
                        sy += 18
                    }
                }
            }
            // Кнопки сохранения/отмены
            val btnSaveX = formLeft + cardW - 140
            val btnCancelX = formLeft + cardW - 70
            val btnY = formY + formH - 22
            if (mouseX in btnSaveX..(btnSaveX + 60) && mouseY in btnY..(btnY + 16)) {
                saveForm()
                return true
            }
            if (mouseX in btnCancelX..(btnCancelX + 60) && mouseY in btnY..(btnY + 16)) {
                isAdding = false
                editingIndex = null
                return true
            }
        }

        if (button == 0 && !isAdding) {
            if (mouseX in left..right && mouseY in formY..(formY + 26)) {
                isAdding = true
                editingIndex = null
                formIcon = ""
                formCommand = ""
                formTooltip = ""
                focusedField = 0
                return true
            }
        }

        // Список кнопок — управление
        val listTop = if (isAdding) formY + formH + 12 else 78
        val listBottom = height - 40
        if (button == 0 && mouseY in listTop..listBottom) {
            val idx = (mouseY - listTop + scrollOffset) / 28
            if (idx in buttons.indices) {
                val y = listTop + idx * 28 - scrollOffset
                val btnUpX = right - 78
                val btnDownX = right - 60
                val btnEditX = right - 42
                val btnDelX = right - 24
                when {
                    mouseX in btnUpX..(btnUpX + 14) && mouseY in y..(y + 28) -> { moveButton(idx, -1); return true }
                    mouseX in btnDownX..(btnDownX + 14) && mouseY in y..(y + 28) -> { moveButton(idx, 1); return true }
                    mouseX in btnEditX..(btnEditX + 14) && mouseY in y..(y + 28) -> {
                        isAdding = true
                        editingIndex = idx
                        val btn = buttons[idx]
                        formIcon = btn.icon ?: ""
                        formCommand = btn.command ?: ""
                        formTooltip = btn.tooltip ?: ""
                        focusedField = 0
                        return true
                    }
                    mouseX in btnDelX..(btnDelX + 14) && mouseY in y..(y + 28) -> { removeButton(idx); return true }
                }
            }
        }

        return super.mouseClicked(event, isDoubleClick)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val centerX = width / 2
        val cardW = SHTokens.CONTENT_WIDTH_WIDE
        val half = cardW / 2
        val (formLeft, formY, formH) = getFormBounds(centerX, half)
        val listTop = if (isAdding) formY + formH + 12 else 78
        val listBottom = height - 40
        if (mouseY in listTop.toDouble()..listBottom.toDouble()) {
            val total = buttons.size * 28
            scrollOffset = SHListHelper.clampScroll((scrollOffset - verticalAmount * 20).toInt(), total, listBottom - listTop)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    private fun saveForm() {
        if (formIcon.isBlank() || formCommand.isBlank()) return
        if (getPreviewItem(formIcon).isEmpty) return
        val list = buttons.toMutableList()
        if (editingIndex != null && editingIndex!! in list.indices) {
            list[editingIndex!!] = list[editingIndex!!].copy(icon = formIcon.trim(), command = formCommand.trim(), tooltip = formTooltip.trim())
        } else if (list.size < InventoryButton.MAX_TABS) {
            val nextIndex = (list.maxOfOrNull { it.index } ?: -1) + 1
            list.add(InventoryButton(nextIndex, formIcon.trim(), formCommand.trim(), formTooltip.trim()))
        }
        InventoryButtonsManager.setButtons(list)
        isAdding = false
        editingIndex = null
        formIcon = ""
        formCommand = ""
        formTooltip = ""
    }

    private fun moveButton(index: Int, delta: Int) {
        val list = buttons.toMutableList()
        val newIndex = (index + delta).coerceIn(0, list.size - 1)
        if (newIndex == index) return
        val tmp = list[index]
        list[index] = list[newIndex]
        list[newIndex] = tmp
        list.forEachIndexed { i, b -> b.index = i }
        InventoryButtonsManager.setButtons(list)
    }

    private fun removeButton(index: Int) {
        val list = buttons.toMutableList()
        if (index in list.indices) { list.removeAt(index); list.forEachIndexed { i, b -> b.index = i }; InventoryButtonsManager.setButtons(list) }
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val keyCode = event.input()
        if (isAdding) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (focusedField < 2) focusedField++ else saveForm()
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                isAdding = false
                editingIndex = null
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                focusedField = (focusedField + 1) % 3
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                when (focusedField) {
                    0 -> if (formIcon.isNotEmpty()) formIcon = formIcon.dropLast(1)
                    1 -> if (formCommand.isNotEmpty()) formCommand = formCommand.dropLast(1)
                    2 -> if (formTooltip.isNotEmpty()) formTooltip = formTooltip.dropLast(1)
                }
                return true
            }
        }
        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (isAdding) {
            val chr = event.codepoint().toChar()
            when (focusedField) {
                0 -> if (formIcon.length < 40) formIcon += chr
                1 -> if (formCommand.length < 60) formCommand += chr
                2 -> if (formTooltip.length < 40) formTooltip += chr
            }
            return true
        }
        return super.charTyped(event)
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) { SHBackdrop.render(graphics, width, height) }
    override fun onClose() { InventoryButtonsManager.save(); if (parent != null) minecraft.gui.setScreen(parent) else super.onClose() }
}
