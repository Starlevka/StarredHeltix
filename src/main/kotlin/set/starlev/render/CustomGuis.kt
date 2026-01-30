package set.starlev.render

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.CharacterEvent
import org.lwjgl.glfw.GLFW
import set.starlev.StarredHeltix
import set.starlev.features.chat.MessageFilterManager
import set.starlev.features.chat.CustomBindManager

class FilterGui(private val parent: Screen? = null) : Screen(Component.literal("Фильтры сообщений")) {
    private var filters = StarredHeltix.feature.chat.general.messageFilter.filters
    private var isAdding = false
    private var newFilterText = ""

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        
        filters = StarredHeltix.feature.chat.general.messageFilter.filters

        guiGraphics.drawCenteredString(font, title, width / 2, 10, -1) // -1 это белый (0xFFFFFFFF)
        guiGraphics.drawCenteredString(font, "§7Всего фильтров: ${filters.size}", width / 2, 25, 0xFFAAAAAA.toInt())
        guiGraphics.drawCenteredString(font, "§8Нажмите 'x' для удаления", width / 2, 35, 0xFF555555.toInt())

        val centerX = width / 2
        var startY = 60
        val itemHeight = 25

        // Кнопка добавления
        val addY = startY
        guiGraphics.fill(centerX - 120, addY, centerX + 120, addY + 20, if (isAdding) 0x90000000.toInt() else 0x70000000)
        
        if (isAdding) {
            val displayText = if (newFilterText.isEmpty()) "§8Введите текст..." else "§f$newFilterText§b_"
            guiGraphics.drawString(font, displayText, centerX - 110, addY + 6, -1)
            guiGraphics.drawString(font, "§a[OK]", centerX + 85, addY + 6, -1)
        } else {
            guiGraphics.drawCenteredString(font, "§a+ Добавить новый фильтр", centerX, addY + 6, -1)
        }
        
        startY += 30

        for ((index, filter) in filters.withIndex()) {
            val y = startY + index * itemHeight
            if (y + itemHeight < height - 40) {
                // Отрисовка строки фильтра в стиле SkyHanni
                guiGraphics.fill(centerX - 120, y, centerX + 120, y + 20, 0x70000000)

                guiGraphics.drawString(font, "§f$filter", centerX - 110, y + 6, -1)
                
                // Кнопка удаления
                if (mouseX.toDouble() in (centerX + 90).toDouble()..(centerX + 110).toDouble() && 
                    mouseY.toDouble() in y.toDouble()..(y + 20).toDouble()) {
                    guiGraphics.drawString(font, "§cX", centerX + 95, y + 6, -1)
                } else {
                    guiGraphics.drawString(font, "§7x", centerX + 95, y + 6, -1)
                }
            }
        }

        guiGraphics.drawCenteredString(font, "§7ESC - Назад", width / 2, height - 30, 0xFFAAAAAA.toInt())
        
        super.render(guiGraphics, mouseX, mouseY, partialTick)
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        val mouseX = event.x()
        val mouseY = event.y()
        val button = event.button()
        
        val centerX = width / 2
        var startY = 60
        val itemHeight = 25

        if (button == 0) {
            // Клик по кнопке добавления
            if (mouseX in (centerX - 120).toDouble()..(centerX + 120).toDouble() && 
                mouseY in startY.toDouble()..(startY + 20).toDouble()) {
                if (isAdding && newFilterText.isNotEmpty()) {
                    MessageFilterManager.addFilter(newFilterText)
                    newFilterText = ""
                    isAdding = false
                } else {
                    isAdding = !isAdding
                }
                return true
            }
            
            startY += 30

            for ((index, filter) in filters.withIndex()) {
                val y = startY + index * itemHeight
                if (mouseX in (centerX + 90).toDouble()..(centerX + 110).toDouble() && 
                    mouseY in y.toDouble()..(y + 20).toDouble()) {
                    MessageFilterManager.removeFilter(filter)
                    return true
                }
            }
        }
        return super.mouseClicked(event, isDoubleClick)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val keyCode = event.input()
        
        if (isAdding) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (newFilterText.isNotEmpty()) {
                    MessageFilterManager.addFilter(newFilterText)
                    newFilterText = ""
                    isAdding = false
                }
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                isAdding = false
                newFilterText = ""
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (newFilterText.isNotEmpty()) {
                    newFilterText = newFilterText.substring(0, newFilterText.length - 1)
                }
                return true
            }
        }
        
        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (isAdding) {
            newFilterText += event.codepoint().toChar()
            return true
        }
        return super.charTyped(event)
    }

    override fun onClose() {
        if (parent != null) {
            minecraft?.setScreen(parent)
        } else {
            super.onClose()
        }
    }
}

class BindsGui(private val parent: Screen? = null) : Screen(Component.literal("Бинды клавиш")) {
    private var listeningForBind: String? = null
    private var isAdding = false
    private var newBindName = ""
    private var newBindCommand = ""
    private var inputStage = 0 // 0 - name, 1 - command

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        
        guiGraphics.drawCenteredString(font, title, width / 2, 10, -1)
        
        val centerX = width / 2
        var startY = 40
        val itemHeight = 30

        // Кнопка добавления
        val addY = startY
        guiGraphics.fill(centerX - 130, addY, centerX + 130, addY + 25, if (isAdding) 0x90000000.toInt() else 0x70000000)
        
        if (isAdding) {
            val displayTitle = if (inputStage == 0) "§7Название: " else "§7Команда: "
            val inputText = if (inputStage == 0) newBindName else newBindCommand
            val displayText = if (inputText.isEmpty()) "§8Введите..." else "§f$inputText§b_"
            
            guiGraphics.drawString(font, displayTitle + displayText, centerX - 120, addY + 8, -1)
            guiGraphics.drawString(font, "§a[OK]", centerX + 100, addY + 8, -1)
        } else {
            guiGraphics.drawCenteredString(font, "§a+ Добавить новый бинд", centerX, addY + 8, -1)
        }
        
        startY += 40
        guiGraphics.drawCenteredString(font, "§7Название           Команда", width / 2, startY - 15, 0xFFAAAAAA.toInt())
        
        CustomBindManager.binds.entries.forEachIndexed { index, entry ->
            val name = entry.key
            val (cmd, key) = entry.value
            val y = startY + index * itemHeight
            
            if (y + itemHeight < height - 40) {
                // Отрисовка бинда в стиле SkyHanni
                guiGraphics.fill(centerX - 130, y, centerX + 130, y + 25, 0x70000000)

                guiGraphics.drawString(font, "§e$name", centerX - 120, y + 8, -1)
                val displayCmd = if (cmd.length > 20) cmd.substring(0, 17) + "..." else cmd
                guiGraphics.drawString(font, "§f$displayCmd", centerX - 40, y + 8, -1)

                // Кнопка назначения клавиши
                val keyName = if (listeningForBind == name) "§b???" else "§7[§f${CustomBindManager.getKeyName(key)}§7]"
                val keyWidth = font.width(keyName)
                guiGraphics.drawString(font, keyName, centerX + 80 - keyWidth / 2, y + 8, -1)

                // Кнопка удаления
                if (mouseX.toDouble() in (centerX + 110).toDouble()..(centerX + 125).toDouble() && 
                    mouseY.toDouble() in y.toDouble()..(y + 25).toDouble()) {
                    guiGraphics.drawString(font, "§cX", centerX + 115, y + 8, -1)
                } else {
                    guiGraphics.drawString(font, "§7x", centerX + 115, y + 8, -1)
                }
            }
        }

        if (listeningForBind != null) {
            guiGraphics.fill(0, 0, width, height, 0x80000000.toInt())
            guiGraphics.drawCenteredString(font, "Нажмите любую клавишу для бинда §e$listeningForBind", width / 2, height / 2, -1)
            guiGraphics.drawCenteredString(font, "§7ESC для отмены", width / 2, height / 2 + 15, 0xFFAAAAAA.toInt())
        }

        guiGraphics.drawCenteredString(font, "§7ESC - Назад", width / 2, height - 30, 0xFFAAAAAA.toInt())
        
        super.render(guiGraphics, mouseX, mouseY, partialTick)
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        if (listeningForBind != null) return true

        val mouseX = event.x()
        val mouseY = event.y()
        val button = event.button()

        val centerX = width / 2
        var startY = 40
        val itemHeight = 30

        if (button == 0) {
            // Клик по кнопке добавления
            if (mouseX in (centerX - 130).toDouble()..(centerX + 130).toDouble() && 
                mouseY in startY.toDouble()..(startY + 25).toDouble()) {
                if (isAdding) {
                    if (inputStage == 0 && newBindName.isNotEmpty()) {
                        inputStage = 1
                    } else if (inputStage == 1 && newBindCommand.isNotEmpty()) {
                        CustomBindManager.create(newBindName, newBindCommand)
                        newBindName = ""
                        newBindCommand = ""
                        isAdding = false
                        inputStage = 0
                    }
                } else {
                    isAdding = true
                    inputStage = 0
                }
                return true
            }

            startY += 40

            val bindsList = CustomBindManager.binds.entries.toList()
            for ((index, entry) in bindsList.withIndex()) {
                val name = entry.key
                val y = startY + index * itemHeight
                
                // Нажатие на выбор клавиши
                if (mouseX in (centerX + 60).toDouble()..(centerX + 100).toDouble() && 
                    mouseY in y.toDouble()..(y + 25).toDouble()) {
                    listeningForBind = name
                    return true
                }
                
                // Нажатие на удаление
                if (mouseX in (centerX + 110).toDouble()..(centerX + 130).toDouble() && 
                    mouseY in y.toDouble()..(y + 25).toDouble()) {
                    CustomBindManager.delete(name)
                    return true
                }
            }
        }
        return super.mouseClicked(event, isDoubleClick)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val keyCode = event.input()
        
        if (listeningForBind != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listeningForBind = null
            } else {
                val name = listeningForBind!!
                CustomBindManager.setKey(name, keyCode)
                listeningForBind = null
            }
            return true
        }

        if (isAdding) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (inputStage == 0 && newBindName.isNotEmpty()) {
                    inputStage = 1
                } else if (inputStage == 1 && newBindCommand.isNotEmpty()) {
                    CustomBindManager.create(newBindName, newBindCommand)
                    newBindName = ""
                    newBindCommand = ""
                    isAdding = false
                    inputStage = 0
                }
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (inputStage == 1) {
                    inputStage = 0
                } else {
                    isAdding = false
                    newBindName = ""
                    newBindCommand = ""
                }
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (inputStage == 0) {
                    if (newBindName.isNotEmpty()) newBindName = newBindName.substring(0, newBindName.length - 1)
                } else {
                    if (newBindCommand.isNotEmpty()) newBindCommand = newBindCommand.substring(0, newBindCommand.length - 1)
                }
                return true
            }
        }

        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (isAdding) {
            val chr = event.codepoint().toChar()
            if (inputStage == 0) newBindName += chr else newBindCommand += chr
            return true
        }
        return super.charTyped(event)
    }

    override fun onClose() {
        if (parent != null) {
            minecraft?.setScreen(parent)
        } else {
            super.onClose()
        }
    }
}
