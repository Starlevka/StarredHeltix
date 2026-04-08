package set.starlev.render

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.CharacterEvent
import net.minecraft.core.BlockPos
import org.lwjgl.glfw.GLFW
import set.starlev.StarredHeltix
import set.starlev.features.chat.MessageFilterManager
import set.starlev.features.chat.CustomBindManager
import set.starlev.features.misc.Waypoints

class FilterGui(private val parent: Screen? = null) : Screen(Component.literal("Фильтры сообщений")) {
    private var filters = StarredHeltix.feature.chat.general.messageFilter.filters
    private var isAdding = false
    private var newFilterText = ""

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)

        filters = StarredHeltix.feature.chat.general.messageFilter.filters

        guiGraphics.drawCenteredString(font, title, width / 2, 10, -1)
        guiGraphics.drawCenteredString(font, "§7Всего фильтров: ${filters.size}", width / 2, 25, 0xFFAAAAAA.toInt())
        guiGraphics.drawCenteredString(font, "§8Нажмите 'x' для удаления", width / 2, 35, 0xFF555555.toInt())

        val centerX = width / 2
        var startY = 60
        val itemHeight = 25

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
                guiGraphics.fill(centerX - 120, y, centerX + 120, y + 20, 0x70000000)
                guiGraphics.drawString(font, "§f$filter", centerX - 110, y + 6, -1)

                if (mouseX.toDouble() in (centerX + 90).toDouble()..(centerX + 110).toDouble() &&
                    mouseY.toDouble() in y.toDouble()..(y + 20).toDouble()) {
                    guiGraphics.drawString(font, "§cX", centerX + 95, y + 6, -1)
                } else {
                    guiGraphics.drawString(font, "§7x", centerX + 95, y + 6, -1)
                }
            }
        }

        guiGraphics.drawCenteredString(font, "§7ESC - Назад", width / 2, height - 30, 0xFFAAAAAA.toInt())
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        val mouseX = event.x()
        val mouseY = event.y()
        val button = event.button()

        val centerX = width / 2
        var startY = 60
        val itemHeight = 25

        if (button == 0) {
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

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        guiGraphics.fill(0, 0, width, height, 0x80000000.toInt())
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
    private var inputStage = 0

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)

        guiGraphics.drawCenteredString(font, title, width / 2, 10, -1)

        val centerX = width / 2
        var startY = 40
        val itemHeight = 30

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
                guiGraphics.fill(centerX - 130, y, centerX + 130, y + 25, 0x70000000)

                guiGraphics.drawString(font, "§e$name", centerX - 120, y + 8, -1)
                val displayCmd = if (cmd.length > 20) cmd.substring(0, 17) + "..." else cmd
                guiGraphics.drawString(font, "§f$displayCmd", centerX - 40, y + 8, -1)

                val keyName = if (listeningForBind == name) "§b???" else "§7[§f${CustomBindManager.getKeyName(key)}§7]"
                val keyWidth = font.width(keyName)
                guiGraphics.drawString(font, keyName, centerX + 80 - keyWidth / 2, y + 8, -1)

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

                if (mouseX in (centerX + 60).toDouble()..(centerX + 100).toDouble() &&
                    mouseY in y.toDouble()..(y + 25).toDouble()) {
                    listeningForBind = name
                    return true
                }

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

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        guiGraphics.fill(0, 0, width, height, 0x80000000.toInt())
    }

    override fun onClose() {
        if (parent != null) {
            minecraft?.setScreen(parent)
        } else {
            super.onClose()
        }
    }
}

class WaypointsGui(private val parent: Screen? = null) : Screen(Component.literal("Waypoints")) {
    private var isAdding = false
    private var newName = ""
    private var addAsTemporary = false
    private var editingColorId: String? = null
    private var colorInput = ""
    private var colorError: String? = null
    private var colorCursorTicks = 0

    private fun parseWaypointColor(input: String): Int? {
        val raw = input.trim()
        if (raw.isBlank()) return null

        if (raw.contains(":")) {
            val parts = raw.split(":").map { it.trim() }.filter { it.isNotEmpty() }
            val nums = parts.mapNotNull { it.toIntOrNull() }
            val rgb = when (nums.size) {
                3 -> nums
                4 -> nums
                5 -> nums.drop(1)
                else -> return null
            }
            val r = rgb[0].coerceIn(0, 255)
            val g = rgb[1].coerceIn(0, 255)
            val b = rgb[2].coerceIn(0, 255)
            val a = if (rgb.size >= 4) rgb[3].coerceIn(0, 255) else 255
            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        val hex = raw.removePrefix("#").removePrefix("0x").removePrefix("0X")
        if (hex.length != 6 && hex.length != 8) return null
        val value = hex.toLongOrNull(16) ?: return null
        return if (hex.length == 6) {
            (0xFF000000L or value).toInt()
        } else {
            value.toInt()
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)

        guiGraphics.drawCenteredString(font, title, width / 2, 10, -1)

        val list = Waypoints.getAll()
        guiGraphics.drawCenteredString(font, "§7Всего меток: ${list.size}", width / 2, 25, 0xFFAAAAAA.toInt())
        guiGraphics.drawCenteredString(font, "§8ЛКМ: локация/цвет • ПКМ по цвету: свой • 'x': удалить", width / 2, 35, 0xFF555555.toInt())

        val centerX = width / 2
        var startY = 55
        val itemHeight = 25

        val addY = startY
        guiGraphics.fill(centerX - 140, addY, centerX + 140, addY + 20, if (isAdding) 0x90000000.toInt() else 0x70000000)

        if (isAdding) {
            val displayText = if (newName.isEmpty()) "§8Название..." else "§f$newName§b_"
            guiGraphics.drawString(font, displayText, centerX - 130, addY + 6, -1)
            if (addAsTemporary) {
                guiGraphics.drawString(font, "§b[TEMP]", centerX + 40, addY + 6, -1)
            }
            guiGraphics.drawString(font, "§a[OK]", centerX + 105, addY + 6, -1)
        } else {
            guiGraphics.drawCenteredString(font, "§a+ ЛКМ: обычная • ПКМ: ВРЕМ(30с)", centerX, addY + 6, -1)
        }

        startY += 30

        val now = System.currentTimeMillis()
        for ((index, triple) in list.withIndex()) {
            val id = triple.id
            val name = triple.name
            val pos = triple.pos
            val y = startY + index * itemHeight
            if (y + itemHeight >= height - 40) break

            guiGraphics.fill(centerX - 140, y, centerX + 140, y + 20, 0x70000000)

            val tag = if (triple.expiresAt != null) {
                val leftSec = ((triple.expiresAt - now) / 1000.0).coerceAtLeast(0.0)
                " §bTEMP§7(${String.format(java.util.Locale.ROOT, "%.0f", leftSec)}s)"
            } else ""

            val visibilityLabel = Waypoints.visibilityLabel(triple.visibility)
            val visShort = if (visibilityLabel.length > 10) visibilityLabel.take(10) + "…" else visibilityLabel
            val line = "§e${name.take(22)}§7: §f${pos.x} ${pos.y} ${pos.z}$tag"
            guiGraphics.drawString(font, line, centerX - 130, y + 6, -1)

            val rowRight = centerX + 140

            val visLeft = rowRight - 105
            val visRight = rowRight - 45
            val colorLeft = rowRight - 40
            val colorRight = rowRight - 20
            val delLeft = rowRight - 17
            val delRight = rowRight - 7

            guiGraphics.fill(visLeft, y + 3, visRight, y + 17, 0x55000000)
            val visTextX = visLeft + (visRight - visLeft) / 2
            guiGraphics.drawCenteredString(font, "§7$visShort", visTextX, y + 6, -1)

            val colorFill = triple.color or 0xFF000000.toInt()
            guiGraphics.fill(colorLeft, y + 3, colorRight, y + 17, 0xFF000000.toInt())
            guiGraphics.fill(colorLeft + 1, y + 4, colorRight - 1, y + 16, colorFill)

            val isHoverDelete = mouseX.toDouble() in delLeft.toDouble()..delRight.toDouble() &&
                mouseY.toDouble() in y.toDouble()..(y + 20).toDouble()
            guiGraphics.drawString(font, if (isHoverDelete) "§cX" else "§7x", delLeft + 2, y + 6, -1)

            val isHoverVis = mouseX.toDouble() in visLeft.toDouble()..visRight.toDouble() &&
                mouseY.toDouble() in y.toDouble()..(y + 20).toDouble()
            if (isHoverVis) {
                guiGraphics.fill(visLeft, y + 3, visRight, y + 17, 0x3300FFFF)
            }

            val isHoverColor = mouseX.toDouble() in colorLeft.toDouble()..colorRight.toDouble() &&
                mouseY.toDouble() in y.toDouble()..(y + 20).toDouble()
            if (isHoverColor) {
                guiGraphics.fill(colorLeft - 1, y + 2, colorRight + 1, y + 18, 0x3300FFFF)
            } else {
                guiGraphics.fill(colorLeft - 1, y + 2, colorRight + 1, y + 18, 0x22000000)
            }
        }

        val colorId = editingColorId
        if (colorId != null) {
            colorCursorTicks++
            guiGraphics.fill(0, 0, width, height, 0xA0000000.toInt())

            val boxW = 280
            val boxH = 110
            val bx = width / 2 - boxW / 2
            val by = height / 2 - boxH / 2
            guiGraphics.fill(bx, by, bx + boxW, by + boxH, 0xD0101010.toInt())

            guiGraphics.drawCenteredString(font, "§eЦвет метки", width / 2, by + 10, -1)
            guiGraphics.drawCenteredString(font, "§7#RRGGBB / #AARRGGBB / R:G:B(:A) / 0:R:G:B:A", width / 2, by + 25, 0xFFAAAAAA.toInt())

            val cursor = (colorCursorTicks / 6) % 2 == 0
            val inputText = if (cursor) "§f$colorInput§b_" else "§f$colorInput"
            guiGraphics.drawString(font, inputText, bx + 20, by + 45, -1)

            val parsed = parseWaypointColor(colorInput)
            val current = list.firstOrNull { it.id == colorId }?.color ?: 0xFFFFFFFF.toInt()
            val preview = (parsed ?: current) or 0xFF000000.toInt()
            guiGraphics.fill(bx + 20, by + 65, bx + 50, by + 95, 0xFF000000.toInt())
            guiGraphics.fill(bx + 21, by + 66, bx + 49, by + 94, preview)

            val msg = colorError ?: "Enter: применить • Esc: отмена"
            val msgColor = if (colorError != null) 0xFFFF5555.toInt() else 0xFFAAAAAA.toInt()
            guiGraphics.drawString(font, msg, bx + 60, by + 72, msgColor)
        }

        guiGraphics.drawCenteredString(font, "§7ESC - Назад", width / 2, height - 30, 0xFFAAAAAA.toInt())
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        val mouseX = event.x()
        val mouseY = event.y()
        val button = event.button()

        if (editingColorId != null) return true

        val centerX = width / 2
        var startY = 55
        val itemHeight = 25

        if (button == 0 || button == 1) {
            if (mouseX in (centerX - 140).toDouble()..(centerX + 140).toDouble() &&
                mouseY in startY.toDouble()..(startY + 20).toDouble()
            ) {
                if (isAdding) {
                    val player = minecraft?.player
                    if (player != null && newName.isNotEmpty()) {
                        val pos = BlockPos.containing(player.position())
                        if (addAsTemporary) {
                            val seconds = set.starlev.StarredHeltix.feature.misc.waypoints.defaultTemporarySeconds
                            Waypoints.addTemporary(newName, pos, (seconds * 1000).toLong())
                        } else {
                            Waypoints.addRegular(newName, pos)
                        }
                        newName = ""
                        isAdding = false
                        addAsTemporary = false
                    }
                } else {
                    isAdding = true
                    addAsTemporary = button == 1
                }
                return true
            }

            startY += 30
            val list = Waypoints.getAll()
            for ((index, triple) in list.withIndex()) {
                val id = triple.id
                val y = startY + index * itemHeight
                if (y + itemHeight >= height - 40) break

                val rowRight = centerX + 140
                val visLeft = rowRight - 105
                val visRight = rowRight - 45
                val colorLeft = rowRight - 40
                val colorRight = rowRight - 20
                val delLeft = rowRight - 17
                val delRight = rowRight - 7

                if (mouseX in delLeft.toDouble()..delRight.toDouble() &&
                    mouseY in y.toDouble()..(y + 20).toDouble()
                ) {
                    Waypoints.remove(id)
                    return true
                }

                if (mouseX in colorLeft.toDouble()..colorRight.toDouble() &&
                    mouseY in y.toDouble()..(y + 20).toDouble()
                ) {
                    if (button == 0) {
                        Waypoints.cycleColor(id)
                    } else {
                        editingColorId = id
                        colorInput = String.format(java.util.Locale.ROOT, "#%06X", triple.color and 0x00FFFFFF)
                        colorError = null
                        colorCursorTicks = 0
                    }
                    return true
                }

                if (mouseX in visLeft.toDouble()..visRight.toDouble() &&
                    mouseY in y.toDouble()..(y + 20).toDouble()
                ) {
                    Waypoints.cycleVisibility(id)
                    return true
                }
            }
        }

        return super.mouseClicked(event, isDoubleClick)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val keyCode = event.input()

        val colorId = editingColorId
        if (colorId != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingColorId = null
                colorError = null
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (colorInput.isNotEmpty()) colorInput = colorInput.substring(0, colorInput.length - 1)
                colorError = null
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                val parsed = parseWaypointColor(colorInput)
                if (parsed != null) {
                    Waypoints.setColor(colorId, parsed)
                    editingColorId = null
                    colorError = null
                } else {
                    colorError = "Неверный формат цвета"
                }
                return true
            }
            return true
        }

        if (isAdding) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                val player = minecraft?.player
                if (player != null && newName.isNotEmpty()) {
                    val pos = BlockPos.containing(player.position())
                    if (addAsTemporary) {
                        val seconds = set.starlev.StarredHeltix.feature.misc.waypoints.defaultTemporarySeconds
                        Waypoints.addTemporary(newName, pos, (seconds * 1000).toLong())
                    } else {
                        Waypoints.addRegular(newName, pos)
                    }
                    newName = ""
                    isAdding = false
                    addAsTemporary = false
                }
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                isAdding = false
                newName = ""
                addAsTemporary = false
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (newName.isNotEmpty()) newName = newName.substring(0, newName.length - 1)
                return true
            }
        }

        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (editingColorId != null) {
            val chr = event.codepoint().toChar()
            val ok = chr == '#' || chr == ':' ||
                (chr in '0'..'9') ||
                (chr in 'a'..'f') ||
                (chr in 'A'..'F') ||
                chr == 'x' || chr == 'X'
            if (ok && colorInput.length < 32) {
                colorInput += chr
                colorError = null
            }
            return true
        }
        if (isAdding) {
            newName += event.codepoint().toChar()
            if (newName.length > 40) newName = newName.take(40)
            return true
        }
        return super.charTyped(event)
    }

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        guiGraphics.fill(0, 0, width, height, 0x80000000.toInt())
    }

    override fun onClose() {
        if (parent != null) {
            minecraft?.setScreen(parent)
        } else {
            super.onClose()
        }
    }
}