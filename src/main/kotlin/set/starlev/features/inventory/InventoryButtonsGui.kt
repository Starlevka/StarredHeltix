package set.starlev.features.inventory

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.CharacterEvent
import org.lwjgl.glfw.GLFW

/**
 * Меню настройки кнопок инвентаря.
 * Полностью переписано в стиле /sh filters и /sh binds: ручной рендеринг, пошаговый ввод, минимализм.
 */
class InventoryButtonsGui(private val parent: Screen? = null) : Screen(Component.literal("Кнопки инвентаря")) {
    private var isAdding = false
    private var inputStage = 0 // 0: иконка, 1: команда, 2: подсказка
    private var newIcon = ""
    private var newCommand = ""
    private var newTooltip = ""
    private var editingIndex: Int? = null

    private val buttons get() = InventoryButtonsManager.getButtons()

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)

        guiGraphics.drawCenteredString(font, title, width / 2, 10, -1)
        guiGraphics.drawCenteredString(font, "§7Всего кнопок: ${buttons.size} / ${InventoryButton.MAX_TABS}", width / 2, 25, 0xFFAAAAAA.toInt())

        val centerX = width / 2
        val boxW = 400
        val left = centerX - boxW / 2
        val right = centerX + boxW / 2
        var startY = 40
        val itemHeight = 30

        // Поле добавления/редактирования
        val addY = startY
        guiGraphics.fill(left, addY, right, addY + 25, if (isAdding) 0x90000000.toInt() else 0x70000000)

        if (isAdding) {
            val stageName = when (inputStage) {
                0 -> "§7Иконка: "
                1 -> "§7Команда: "
                2 -> "§7Подсказка: "
                else -> ""
            }
            val inputText = when (inputStage) {
                0 -> newIcon
                1 -> newCommand
                2 -> newTooltip
                else -> ""
            }
            val displayText = if (inputText.isEmpty()) "§8Введите..." else "§f$inputText§b_"
            guiGraphics.drawString(font, stageName + displayText, centerX - 160, addY + 8, -1)
            guiGraphics.drawString(font, "§a[OK]", centerX + 160, addY + 8, -1)
        } else {
            guiGraphics.drawCenteredString(font, "§a+ Добавить кнопку", centerX, addY + 8, -1)
        }

        // Заголовок списка
        startY += 40
        guiGraphics.drawString(font, "§7Иконка", centerX - 160, startY - 15, 0xFFAAAAAA.toInt())
        guiGraphics.drawString(font, "§7Команда", centerX - 60, startY - 15, 0xFFAAAAAA.toInt())
        guiGraphics.drawString(font, "§7Подсказка", centerX + 40, startY - 15, 0xFFAAAAAA.toInt())
        guiGraphics.drawString(font, "§7Редакт", centerX + 110, startY - 15, 0xFFAAAAAA.toInt())
        guiGraphics.drawString(font, "§7Закрыть", centerX + 165, startY - 15, 0xFFAAAAAA.toInt())
        
        // Список кнопок
        for ((index, btn) in buttons.withIndex()) {
            val y = startY + index * itemHeight
            if (y + itemHeight > height - 40) break

            guiGraphics.fill(left, y, right, y + 25, 0x70000000)
            val iconDisplay = btn.icon?.take(12) ?: "§8-"
            val cmdDisplay = (btn.command ?: "").take(15).let { if (it.length == 15 && (btn.command?.length ?: 0) > 15) "$it..." else it }
            val tipDisplay = (btn.tooltip ?: "").take(12).let { if (it.length == 12 && (btn.tooltip?.length ?: 0) > 12) "$it..." else it }

            guiGraphics.drawString(font, "§e$iconDisplay", centerX - 160, y + 8, -1)
            guiGraphics.drawString(font, "§f$cmdDisplay", centerX - 60, y + 8, -1)
            guiGraphics.drawString(font, "§8$tipDisplay", centerX + 40, y + 8, -1)
            
            // Кнопка редактирования
            val isHoverEdit = mouseX.toDouble() in (centerX + 100).toDouble()..(centerX + 120).toDouble() && mouseY.toDouble() in y.toDouble()..(y + 25).toDouble()
            guiGraphics.drawString(font, if (isHoverEdit) "§e✎" else "§7✎", centerX + 105, y + 8, -1)
            
            // Кнопка удаления
            val isHoverDelete = mouseX.toDouble() in (centerX + 160).toDouble()..(centerX + 180).toDouble() && mouseY.toDouble() in y.toDouble()..(y + 25).toDouble()
            guiGraphics.drawString(font, if (isHoverDelete) "§cX" else "§7x", centerX + 165, y + 8, -1)
        }

        guiGraphics.drawCenteredString(font, "§7ESC - Назад", width / 2, height - 30, 0xFFAAAAAA.toInt())
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        val mouseX = event.x()
        val mouseY = event.y()
        val button = event.button()

        val centerX = width / 2
        val boxW = 400
        val left = centerX - boxW / 2
        val right = centerX + boxW / 2
        var startY = 40
        val itemHeight = 30

        if (button == 0) {
            // Клик по полю добавления
            if (mouseX in left.toDouble()..right.toDouble() && mouseY in startY.toDouble()..(startY + 25).toDouble()) {
                if (isAdding) {
                    advanceStage()
                } else {
                    isAdding = true
                    inputStage = 0
                    editingIndex = null
                    newIcon = ""
                    newCommand = ""
                    newTooltip = ""
                }
                return true
            }

            // Клик по элементам списка
            startY += 40
            for ((index, btn) in buttons.withIndex()) {
                val y = startY + index * itemHeight
                if (y + itemHeight > height - 40) break
                
                // Редактирование
                if (mouseX in (centerX + 100).toDouble()..(centerX + 120).toDouble() && mouseY in y.toDouble()..(y + 25).toDouble()) {
                    isAdding = true
                    inputStage = 0
                    editingIndex = index
                    newIcon = btn.icon ?: ""
                    newCommand = btn.command ?: ""
                    newTooltip = btn.tooltip ?: ""
                    return true
                }
                
                // Удаление
                if (mouseX in (centerX + 160).toDouble()..(centerX + 180).toDouble() && mouseY in y.toDouble()..(y + 25).toDouble()) {
                    removeButton(index)
                    return true
                }
            }
        }
        return super.mouseClicked(event, isDoubleClick)
    }

    private fun advanceStage() {
        when (inputStage) {
            0 -> {
                if (newIcon.isNotBlank()) inputStage = 1
            }
            1 -> {
                if (newCommand.isNotBlank()) inputStage = 2
            }
            2 -> {
                val list = buttons.toMutableList()
                if (editingIndex != null && editingIndex!! in list.indices) {
                    list[editingIndex!!] = list[editingIndex!!].copy(
                        icon = newIcon.trim(),
                        command = newCommand.trim(),
                        tooltip = newTooltip.trim()
                    )
                } else if (list.size < InventoryButton.MAX_TABS) {
                    val nextIndex = (list.maxOfOrNull { it.index } ?: -1) + 1
                    list.add(InventoryButton(nextIndex, newIcon.trim(), newCommand.trim(), newTooltip.trim()))
                }
                InventoryButtonsManager.setButtons(list)
                isAdding = false
                inputStage = 0
                editingIndex = null
                newIcon = ""
                newCommand = ""
                newTooltip = ""
            }
        }
    }

    private fun removeButton(index: Int) {
        val list = buttons.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            list.forEachIndexed { i, b -> b.index = i }
            InventoryButtonsManager.setButtons(list)
        }
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val keyCode = event.input()

        if (isAdding) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                advanceStage()
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (inputStage > 0) {
                    inputStage--
                } else {
                    isAdding = false
                    editingIndex = null
                    newIcon = ""
                    newCommand = ""
                    newTooltip = ""
                }
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                when (inputStage) {
                    0 -> if (newIcon.isNotEmpty()) newIcon = newIcon.dropLast(1)
                    1 -> if (newCommand.isNotEmpty()) newCommand = newCommand.dropLast(1)
                    2 -> if (newTooltip.isNotEmpty()) newTooltip = newTooltip.dropLast(1)
                }
                return true
            }
        }

        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (isAdding) {
            val chr = event.codepoint().toChar()
            when (inputStage) {
                0 -> if (newIcon.length < 40) newIcon += chr
                1 -> if (newCommand.length < 60) newCommand += chr
                2 -> if (newTooltip.length < 40) newTooltip += chr
            }
            return true
        }
        return super.charTyped(event)
    }

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        guiGraphics.fill(0, 0, width, height, 0x80000000.toInt())
    }

    override fun onClose() {
        InventoryButtonsManager.save()
        if (parent != null) {
            minecraft?.setScreen(parent)
        } else {
            super.onClose()
        }
    }
}