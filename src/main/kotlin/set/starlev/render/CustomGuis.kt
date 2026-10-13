package set.starlev.render

import net.minecraft.client.gui.GuiGraphicsExtractor
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
import set.starlev.features.chat.CommandAliases
import set.starlev.features.skyblock.Waypoints
import set.starlev.render.drawString
import set.starlev.render.drawCenteredString

// Unified SH-style: 280px card, tokens, scissor + scrollbar, single backdrop/header/footer palette
// See SHDesign.kt — SHTokens / SHBackdrop / SHHeader etc.

class FilterGui(private val parent: Screen? = null) : Screen(Component.literal("Фильтры сообщений")) {
    private var filters = StarredHeltix.feature.chat.messageFilter.filters
    private var isAdding = false
    private var newFilterText = ""
    private var scrollOffset = 0

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        filters = StarredHeltix.feature.chat.messageFilter.filters

        SHHeader.draw(graphics, font, title, "Всего фильтров: ${filters.size}", "Нажмите 'x' для удаления", width)

        val centerX = width / 2
        val cardW = SHTokens.CONTENT_WIDTH
        val half = cardW / 2
        var startY = 60
        val addY = startY
        SHRow.draw(graphics, set.starlev.features.visual.swing.Rect(centerX - half, addY, cardW, 20), false, isAdding)
        if (isAdding) {
            val displayText = if (newFilterText.isEmpty()) "§8Введите текст..." else "§f$newFilterText§b_"
            graphics.drawString(font, displayText, centerX - half + 10, addY + 6, -1)
            graphics.drawString(font, "§a[OK]", centerX + half - 35, addY + 6, -1)
        } else {
            graphics.drawCenteredString(font, Component.literal("§a+ Добавить новый фильтр"), centerX, addY + 6, -1)
        }

        // list area with scissor
        val listTop = 95
        val listBottom = height - 40
        val listHeight = listBottom - listTop
        val itemHeight = SHTokens.ROW_HEIGHT
        val totalHeight = filters.size * itemHeight

        scrollOffset = SHListHelper.clampScroll(scrollOffset, totalHeight, listHeight)

        graphics.enableScissor(centerX - half, listTop, centerX + half, listBottom)
        var y = listTop - scrollOffset
        for ((index, filter) in filters.withIndex()) {
            if (y + itemHeight < listTop) { y += itemHeight; continue }
            if (y > listBottom) break
            SHRow.draw(graphics, set.starlev.features.visual.swing.Rect(centerX - half, y, cardW, 20), false)
            graphics.drawString(font, "§f$filter", centerX - half + 10, y + 6, -1)
            val delHover = mouseX in (centerX + half - 30)..(centerX + half - 10) && mouseY in y..(y + 20)
            graphics.drawString(font, if (delHover) "§cX" else "§7x", centerX + half - 20, y + 6, -1)
            y += itemHeight
        }
        graphics.disableScissor()
        SHListHelper.drawScrollbar(graphics, set.starlev.features.visual.swing.Rect(centerX - half, listTop, cardW, listHeight), totalHeight, scrollOffset, listHeight)
        SHFooter.draw(graphics, font, width, height)
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()
        val button = event.button()
        val centerX = width / 2
        val cardW = SHTokens.CONTENT_WIDTH
        val half = cardW / 2
        var startY = 60
        if (button == 0) {
            if (mouseX in (centerX - half)..(centerX + half) && mouseY in startY..(startY + 20)) {
                if (isAdding && newFilterText.isNotEmpty()) {
                    MessageFilterManager.addFilter(newFilterText)
                    newFilterText = ""
                    isAdding = false
                } else {
                    isAdding = !isAdding
                }
                return true
            }
            val listTop = 95
            val listBottom = height - 40
            if (mouseY in listTop..listBottom) {
                val itemHeight = SHTokens.ROW_HEIGHT
                val idx = (mouseY - listTop + scrollOffset) / itemHeight
                if (idx in filters.indices) {
                    val y = listTop + idx * itemHeight - scrollOffset
                    if (mouseX in (centerX + half - 30)..(centerX + half - 10) && mouseY in y..(y + 20)) {
                        MessageFilterManager.removeFilter(filters[idx])
                        return true
                    }
                }
            }
        }
        return super.mouseClicked(event, isDoubleClick)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val listTop = 95
        val listBottom = height - 40
        if (mouseY in listTop.toDouble()..listBottom.toDouble()) {
            val total = filters.size * SHTokens.ROW_HEIGHT
            val visible = listBottom - listTop
            scrollOffset = SHListHelper.clampScroll((scrollOffset - verticalAmount * 20).toInt(), total, visible)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
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
                if (newFilterText.isNotEmpty()) newFilterText = newFilterText.substring(0, newFilterText.length - 1)
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

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        SHBackdrop.render(graphics, width, height)
    }

    override fun onClose() {
        if (parent != null) minecraft.gui.setScreen(parent) else super.onClose()
    }
}

class BindsGui(private val parent: Screen? = null) : Screen(Component.literal("Бинды клавиш")) {
    private var listeningForBind: String? = null
    private var isAdding = false
    private var newBindName = ""
    private var newBindCommand = ""
    private var inputStage = 0
    private var scrollOffset = 0

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        SHHeader.draw(graphics, font, title, null, null, width)
        val centerX = width / 2
        val cardW = SHTokens.CONTENT_WIDTH
        val half = cardW / 2
        var startY = 40
        val addY = startY
        SHRow.draw(graphics, set.starlev.features.visual.swing.Rect(centerX - half, addY, cardW, 25), false, isAdding)
        if (isAdding) {
            val displayTitle = if (inputStage == 0) "§7Название: " else "§7Команда: "
            val inputText = if (inputStage == 0) newBindName else newBindCommand
            val displayText = if (inputText.isEmpty()) "§8Введите..." else "§f$inputText§b_"
            graphics.drawString(font, displayTitle + displayText, centerX - half + 10, addY + 8, -1)
            graphics.drawString(font, "§a[OK]", centerX + half - 30, addY + 8, -1)
        } else {
            graphics.drawCenteredString(font, Component.literal("§a+ Добавить новый бинд"), centerX, addY + 8, -1)
        }
        startY += 40
        graphics.drawCenteredString(font, Component.literal("§7Название           Команда"), width / 2, startY - 15, SHTokens.TEXT_MUTED.toInt())
        val listTop = startY
        val listBottom = height - 40
        val itemHeight = 30
        val entries = CustomBindManager.binds.entries.toList()
        val totalHeight = entries.size * itemHeight
        scrollOffset = SHListHelper.clampScroll(scrollOffset, totalHeight, listBottom - listTop)
        graphics.enableScissor(centerX - half, listTop, centerX + half, listBottom)
        var y = listTop - scrollOffset
        for ((index, entry) in entries.withIndex()) {
            if (y + itemHeight < listTop) { y += itemHeight; continue }
            if (y > listBottom) break
            SHRow.draw(graphics, set.starlev.features.visual.swing.Rect(centerX - half, y, cardW, 25), false)
            val name = entry.key
            val (cmd, key) = entry.value
            graphics.drawString(font, "§e$name", centerX - half + 10, y + 8, -1)
            val displayCmd = if (cmd.length > 18) cmd.substring(0, 15) + "..." else cmd
            graphics.drawString(font, "§f$displayCmd", centerX - 30, y + 8, -1)
            val keyName = if (listeningForBind == name) "§b???" else "§7[§f${CustomBindManager.getKeyName(key)}§7]"
            val keyWidth = font.width(keyName)
            graphics.drawString(font, keyName, centerX + 70 - keyWidth / 2, y + 8, -1)
            val delHover = mouseX in (centerX + half - 25)..(centerX + half - 5) && mouseY in y..(y + 25)
            graphics.drawString(font, if (delHover) "§cX" else "§7x", centerX + half - 15, y + 8, -1)
            y += itemHeight
        }
        graphics.disableScissor()
        SHListHelper.drawScrollbar(graphics, set.starlev.features.visual.swing.Rect(centerX - half, listTop, cardW, listBottom - listTop), totalHeight, scrollOffset, listBottom - listTop)
        if (listeningForBind != null) {
            graphics.fill(0, 0, width, height, 0x80000000.toInt())
            graphics.drawCenteredString(font, Component.literal("Нажмите любую клавишу для бинда §e$listeningForBind"), width / 2, height / 2, -1)
            graphics.drawCenteredString(font, Component.literal("§7ESC для отмены"), width / 2, height / 2 + 15, SHTokens.TEXT_MUTED.toInt())
        }
        SHFooter.draw(graphics, font, width, height)
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        if (listeningForBind != null) return true
        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()
        val button = event.button()
        val centerX = width / 2
        val cardW = SHTokens.CONTENT_WIDTH
        val half = cardW / 2
        var startY = 40
        if (button == 0) {
            if (mouseX in (centerX - half)..(centerX + half) && mouseY in startY..(startY + 25)) {
                if (isAdding) {
                    if (inputStage == 0 && newBindName.isNotEmpty()) inputStage = 1
                    else if (inputStage == 1 && newBindCommand.isNotEmpty()) {
                        CustomBindManager.create(newBindName, newBindCommand)
                        newBindName = ""; newBindCommand = ""; isAdding = false; inputStage = 0
                    }
                } else { isAdding = true; inputStage = 0 }
                return true
            }
            startY += 40
            val listTop = startY
            val listBottom = height - 40
            if (mouseY in listTop..listBottom) {
                val idx = (mouseY - listTop + scrollOffset) / 30
                val list = CustomBindManager.binds.entries.toList()
                if (idx in list.indices) {
                    val name = list[idx].key
                    val y = listTop + idx * 30 - scrollOffset
                    if (mouseX in (centerX + 45)..(centerX + 95) && mouseY in y..(y + 25)) { listeningForBind = name; return true }
                    if (mouseX in (centerX + half - 25)..(centerX + half - 5) && mouseY in y..(y + 25)) { CustomBindManager.delete(name); return true }
                }
            }
        }
        return super.mouseClicked(event, isDoubleClick)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val listTop = 80
        val listBottom = height - 40
        if (mouseY in listTop.toDouble()..listBottom.toDouble()) {
            val total = CustomBindManager.binds.size * 30
            scrollOffset = SHListHelper.clampScroll((scrollOffset - verticalAmount * 20).toInt(), total, listBottom - listTop)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val keyCode = event.input()
        if (listeningForBind != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) listeningForBind = null else { val name = listeningForBind!!; CustomBindManager.setKey(name, keyCode); listeningForBind = null }
            return true
        }
        if (isAdding) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (inputStage == 0 && newBindName.isNotEmpty()) inputStage = 1
                else if (inputStage == 1 && newBindCommand.isNotEmpty()) { CustomBindManager.create(newBindName, newBindCommand); newBindName = ""; newBindCommand = ""; isAdding = false; inputStage = 0 }
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { if (inputStage == 1) inputStage = 0 else { isAdding = false; newBindName = ""; newBindCommand = "" }; return true }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) { if (inputStage == 0) { if (newBindName.isNotEmpty()) newBindName = newBindName.dropLast(1) } else if (newBindCommand.isNotEmpty()) newBindCommand = newBindCommand.dropLast(1); return true }
        }
        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (isAdding) { val chr = event.codepoint().toChar(); if (inputStage == 0) newBindName += chr else newBindCommand += chr; return true }
        return super.charTyped(event)
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) { SHBackdrop.render(graphics, width, height) }
    override fun onClose() { if (parent != null) minecraft.gui.setScreen(parent) else super.onClose() }
}

/**
 * Алиасы команд (/dh → /warp dungeon_hub) — по образцу BindsGui: добавить/удалить.
 * Ввод: сначала алиас, затем команда; Enter — следующий шаг, ESC — назад/отмена.
 */
class AliasesGui(private val parent: Screen? = null) : Screen(Component.literal("Алиасы команд")) {
    private var isAdding = false
    private var newAlias = ""
    private var newCommand = ""
    private var inputStage = 0
    private var scrollOffset = 0

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        SHHeader.draw(graphics, font, title, null, null, width)
        val centerX = width / 2
        val cardW = SHTokens.CONTENT_WIDTH
        val half = cardW / 2
        var startY = 40
        SHRow.draw(graphics, set.starlev.features.visual.swing.Rect(centerX - half, startY, cardW, 25), false, isAdding)
        if (isAdding) {
            val prompt = if (inputStage == 0) "§7Алиас: " else "§7Команда: "
            val input = if (inputStage == 0) newAlias else newCommand
            val shown = if (input.isEmpty()) "§8Введите..." else "§f$input§b_"
            graphics.drawString(font, prompt + shown, centerX - half + 10, startY + 8, -1)
            graphics.drawString(font, "§a[OK]", centerX + half - 30, startY + 8, -1)
        } else {
            graphics.drawCenteredString(font, Component.literal("§a+ Добавить алиас"), centerX, startY + 8, -1)
        }
        startY += 40
        graphics.drawCenteredString(
            font,
            Component.literal("§7Алиас                      Команда"),
            centerX, startY - 15, SHTokens.TEXT_MUTED.toInt()
        )
        val listTop = startY
        val listBottom = height - 40
        val itemHeight = 30
        val entries = CommandAliases.snapshot().entries.toList()
        val totalHeight = entries.size * itemHeight
        scrollOffset = SHListHelper.clampScroll(scrollOffset, totalHeight, listBottom - listTop)
        graphics.enableScissor(centerX - half, listTop, centerX + half, listBottom)
        var y = listTop - scrollOffset
        for (entry in entries) {
            if (y + itemHeight < listTop) { y += itemHeight; continue }
            if (y > listBottom) break
            SHRow.draw(graphics, set.starlev.features.visual.swing.Rect(centerX - half, y, cardW, 25), false)
            graphics.drawString(font, "§e/${entry.key}", centerX - half + 10, y + 8, -1)
            val cmd = entry.value
            val shown = if (cmd.length > 22) "/" + cmd.take(19) + "..." else "/$cmd"
            graphics.drawString(font, "§f$shown", centerX - 30, y + 8, -1)
            val delHover = mouseX in (centerX + half - 25)..(centerX + half - 5) && mouseY in y..(y + 25)
            graphics.drawString(font, if (delHover) "§cX" else "§7x", centerX + half - 15, y + 8, -1)
            y += itemHeight
        }
        graphics.disableScissor()
        SHListHelper.drawScrollbar(
            graphics,
            set.starlev.features.visual.swing.Rect(centerX - half, listTop, cardW, listBottom - listTop),
            totalHeight, scrollOffset, listBottom - listTop
        )
        SHFooter.draw(graphics, font, width, height)
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()
        if (event.button() != 0) return super.mouseClicked(event, isDoubleClick)
        val centerX = width / 2
        val cardW = SHTokens.CONTENT_WIDTH
        val half = cardW / 2
        var startY = 40
        if (mouseX in (centerX - half)..(centerX + half) && mouseY in startY..(startY + 25)) {
            if (isAdding) {
                if (inputStage == 0 && newAlias.isNotEmpty()) inputStage = 1
                else if (inputStage == 1 && newCommand.isNotEmpty()) {
                    CommandAliases.create(newAlias, newCommand)
                    newAlias = ""; newCommand = ""; isAdding = false; inputStage = 0
                }
            } else {
                isAdding = true; inputStage = 0
            }
            return true
        }
        startY += 40
        val listTop = startY
        val listBottom = height - 40
        if (mouseY in listTop..listBottom) {
            val idx = (mouseY - listTop + scrollOffset) / 30
            val list = CommandAliases.snapshot().keys.toList()
            if (idx in list.indices) {
                val y = listTop + idx * 30 - scrollOffset
                if (mouseX in (centerX + half - 25)..(centerX + half - 5) && mouseY in y..(y + 25)) {
                    CommandAliases.delete(list[idx])
                    return true
                }
            }
        }
        return super.mouseClicked(event, isDoubleClick)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val listTop = 80
        val listBottom = height - 40
        if (mouseY in listTop.toDouble()..listBottom.toDouble()) {
            val total = CommandAliases.snapshot().size * 30
            scrollOffset = SHListHelper.clampScroll(
                (scrollOffset - verticalAmount * 20).toInt(), total, listBottom - listTop
            )
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val keyCode = event.input()
        if (isAdding) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (inputStage == 0 && newAlias.isNotEmpty()) inputStage = 1
                else if (inputStage == 1 && newCommand.isNotEmpty()) {
                    CommandAliases.create(newAlias, newCommand)
                    newAlias = ""; newCommand = ""; isAdding = false; inputStage = 0
                }
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (inputStage == 1) inputStage = 0 else { isAdding = false; newAlias = ""; newCommand = "" }
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (inputStage == 0) { if (newAlias.isNotEmpty()) newAlias = newAlias.dropLast(1) }
                else if (newCommand.isNotEmpty()) newCommand = newCommand.dropLast(1)
                return true
            }
        }
        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (isAdding) {
            val chr = event.codepoint().toChar()
            if (inputStage == 0) newAlias += chr else newCommand += chr
            return true
        }
        return super.charTyped(event)
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        SHBackdrop.render(graphics, width, height)
    }

    override fun onClose() {
        if (parent != null) minecraft.gui.setScreen(parent) else super.onClose()
    }
}

class WaypointsGui(private val parent: Screen? = null) : Screen(Component.literal("Waypoints")) {    private var isAdding = false
    private var newName = ""
    private var addAsTemporary = false
    private var editingColorId: String? = null
    private var colorInput = ""
    private var colorError: String? = null
    private var colorCursorTicks = 0
    private var scrollOffset = 0

    private fun parseWaypointColor(input: String): Int? {
        val raw = input.trim()
        if (raw.isBlank()) return null
        if (raw.contains(":")) {
            val parts = raw.split(":").map { it.trim() }.filter { it.isNotEmpty() }
            val nums = parts.mapNotNull { it.toIntOrNull() }
            val rgb = when (nums.size) { 3 -> nums; 4 -> nums; 5 -> nums.drop(1); else -> return null }
            val r = rgb[0].coerceIn(0, 255); val g = rgb[1].coerceIn(0, 255); val b = rgb[2].coerceIn(0, 255); val a = if (rgb.size >= 4) rgb[3].coerceIn(0, 255) else 255
            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        val hex = raw.removePrefix("#").removePrefix("0x").removePrefix("0X")
        if (hex.length != 6 && hex.length != 8) return null
        val value = hex.toLongOrNull(16) ?: return null
        return if (hex.length == 6) (0xFF000000L or value).toInt() else value.toInt()
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        SHHeader.draw(graphics, font, title, "Всего меток: ${Waypoints.getAll().size}", "ЛКМ: локация/цвет • ПКМ по цвету: свой • 'x': удалить", width)
        val centerX = width / 2
        val cardW = SHTokens.CONTENT_WIDTH
        val half = cardW / 2
        var startY = 55
        val addY = startY
        SHRow.draw(graphics, set.starlev.features.visual.swing.Rect(centerX - half, addY, cardW, 20), false, isAdding)
        if (isAdding) {
            val displayText = if (newName.isEmpty()) "§8Название..." else "§f$newName§b_"
            graphics.drawString(font, displayText, centerX - half + 10, addY + 6, -1)
            if (addAsTemporary) graphics.drawString(font, "§b[TEMP]", centerX + 40, addY + 6, -1)
            graphics.drawString(font, "§a[OK]", centerX + half - 35, addY + 6, -1)
        } else {
            graphics.drawCenteredString(font, Component.literal("§a+ ЛКМ: обычная • ПКМ: ВРЕМ(30с)"), centerX, addY + 6, -1)
        }
        val listTop = 90
        val listBottom = height - 40
        val itemHeight = 25
        val list = Waypoints.getAll()
        val totalHeight = list.size * itemHeight
        scrollOffset = SHListHelper.clampScroll(scrollOffset, totalHeight, listBottom - listTop)
        graphics.enableScissor(centerX - half, listTop, centerX + half, listBottom)
        var idx = 0
        var y = listTop - scrollOffset
        val now = System.currentTimeMillis()
        for (triple in list) {
            if (y + itemHeight < listTop) { y += itemHeight; idx++; continue }
            if (y > listBottom) break
            SHRow.draw(graphics, set.starlev.features.visual.swing.Rect(centerX - half, y, cardW, 20), false)
            val tag = if (triple.expiresAt != null) { val leftSec = ((triple.expiresAt - now) / 1000.0).coerceAtLeast(0.0); " §bTEMP§7(${String.format(java.util.Locale.ROOT, "%.0f", leftSec)}s)" } else ""
            val visibilityLabel = Waypoints.visibilityLabel(triple.visibility)
            val visShort = if (visibilityLabel.length > 10) visibilityLabel.take(10) + "…" else visibilityLabel
            val line = "§e${triple.name.take(18)}§7: §f${triple.pos.x} ${triple.pos.y} ${triple.pos.z}$tag"
            // truncate if too long (card 280, left 10, right slots 105+)
            graphics.drawString(font, line, centerX - half + 6, y + 6, -1)
            val rowRight = centerX + half
            val visLeft = rowRight - 105
            val visRight = rowRight - 45
            val colorLeft = rowRight - 40
            val colorRight = rowRight - 20
            val delLeft = rowRight - 17
            graphics.fill(visLeft, y + 3, visRight, y + 17, 0x55000000)
            graphics.drawCenteredString(font, Component.literal("§7$visShort"), visLeft + (visRight - visLeft) / 2, y + 6, SHTokens.TEXT_MUTED.toInt())
            val colorFill = triple.color or 0xFF000000.toInt()
            graphics.fill(colorLeft, y + 3, colorRight, y + 17, 0xFF000000.toInt())
            graphics.fill(colorLeft + 1, y + 4, colorRight - 1, y + 16, colorFill)
            val isHoverDelete = mouseX in (delLeft)..(rowRight - 7) && mouseY in y..(y + 20)
            graphics.drawString(font, if (isHoverDelete) "§cX" else "§7x", delLeft + 2, y + 6, -1)
            val isHoverVis = mouseX in visLeft..visRight && mouseY in y..(y + 20)
            if (isHoverVis) graphics.fill(visLeft, y + 3, visRight, y + 17, 0x3300FFFF)
            val isHoverColor = mouseX in colorLeft..colorRight && mouseY in y..(y + 20)
            if (isHoverColor) graphics.fill(colorLeft - 1, y + 2, colorRight + 1, y + 18, 0x3300FFFF) else graphics.fill(colorLeft - 1, y + 2, colorRight + 1, y + 18, 0x22000000)
            y += itemHeight; idx++
        }
        graphics.disableScissor()
        SHListHelper.drawScrollbar(graphics, set.starlev.features.visual.swing.Rect(centerX - half, listTop, cardW, listBottom - listTop), totalHeight, scrollOffset, listBottom - listTop)
        val colorId = editingColorId
        if (colorId != null) {
            colorCursorTicks++
            graphics.fill(0, 0, width, height, SHTokens.MODAL_SHADE)
            val boxW = 280; val boxH = 110; val bx = width / 2 - boxW / 2; val by = height / 2 - boxH / 2
            SHPanel.draw(graphics, set.starlev.features.visual.swing.Rect(bx, by, boxW, boxH))
            graphics.drawCenteredString(font, Component.literal("§eЦвет метки"), width / 2, by + 10, -1)
            graphics.drawCenteredString(font, Component.literal("§7#RRGGBB / #AARRGGBB / R:G:B(:A) / 0:R:G:B:A"), width / 2, by + 25, SHTokens.TEXT_MUTED.toInt())
            val cursor = (colorCursorTicks / 6) % 2 == 0
            val inputText = if (cursor) "§f$colorInput§b_" else "§f$colorInput"
            graphics.drawString(font, inputText, bx + 20, by + 45, -1)
            val parsed = parseWaypointColor(colorInput)
            val current = list.firstOrNull { it.id == colorId }?.color ?: 0xFFFFFFFF.toInt()
            val preview = (parsed ?: current) or 0xFF000000.toInt()
            graphics.fill(bx + 20, by + 65, bx + 50, by + 95, 0xFF000000.toInt())
            graphics.fill(bx + 21, by + 66, bx + 49, by + 94, preview)
            val msg = colorError ?: "Enter: применить • Esc: отмена"
            val msgColor = if (colorError != null) SHTokens.DANGER.toInt() else SHTokens.TEXT_MUTED.toInt()
            graphics.drawString(font, Component.literal(msg), bx + 60, by + 72, msgColor)
        }
        SHFooter.draw(graphics, font, width, height)
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()
        val button = event.button()
        if (editingColorId != null) return true
        val centerX = width / 2
        val cardW = SHTokens.CONTENT_WIDTH
        val half = cardW / 2
        var startY = 55
        if (button == 0 || button == 1) {
            if (mouseX in (centerX - half)..(centerX + half) && mouseY in startY..(startY + 20)) {
                if (isAdding) {
                    val player = minecraft.player
                    if (player != null && newName.isNotEmpty()) {
                        val pos = BlockPos.containing(player.position())
                        if (addAsTemporary) Waypoints.addTemporary(newName, pos, (set.starlev.StarredHeltix.feature.skyblock.waypoints.defaultTemporarySeconds * 1000).toLong())
                        else Waypoints.addRegular(newName, pos)
                        newName = ""; isAdding = false; addAsTemporary = false
                    }
                } else { isAdding = true; addAsTemporary = button == 1 }
                return true
            }
            val listTop = 90
            val listBottom = height - 40
            if (mouseY in listTop..listBottom) {
                val idx = (mouseY - listTop + scrollOffset) / 25
                val list = Waypoints.getAll()
                if (idx in list.indices) {
                    val triple = list[idx]
                    val y = listTop + idx * 25 - scrollOffset
                    val rowRight = centerX + half
                    val visLeft = rowRight - 105; val visRight = rowRight - 45; val colorLeft = rowRight - 40; val colorRight = rowRight - 20; val delLeft = rowRight - 17
                    if (mouseX in delLeft..(rowRight - 7) && mouseY in y..(y + 20)) { Waypoints.remove(triple.id); return true }
                    if (mouseX in colorLeft..colorRight && mouseY in y..(y + 20)) { if (button == 0) Waypoints.cycleColor(triple.id) else { editingColorId = triple.id; colorInput = String.format(java.util.Locale.ROOT, "#%06X", triple.color and 0x00FFFFFF); colorError = null; colorCursorTicks = 0 }; return true }
                    if (mouseX in visLeft..visRight && mouseY in y..(y + 20)) { Waypoints.cycleVisibility(triple.id); return true }
                }
            }
        }
        return super.mouseClicked(event, isDoubleClick)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (editingColorId != null) return true
        val listTop = 90; val listBottom = height - 40
        if (mouseY in listTop.toDouble()..listBottom.toDouble()) {
            val total = Waypoints.getAll().size * 25
            scrollOffset = SHListHelper.clampScroll((scrollOffset - verticalAmount * 20).toInt(), total, listBottom - listTop)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val keyCode = event.input()
        val colorId = editingColorId
        if (colorId != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { editingColorId = null; colorError = null; return true }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) { if (colorInput.isNotEmpty()) colorInput = colorInput.dropLast(1); colorError = null; return true }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { val parsed = parseWaypointColor(colorInput); if (parsed != null) { Waypoints.setColor(colorId, parsed); editingColorId = null; colorError = null } else colorError = "Неверный формат цвета"; return true }
            return true
        }
        if (isAdding) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { val player = minecraft.player; if (player != null && newName.isNotEmpty()) { val pos = BlockPos.containing(player.position()); if (addAsTemporary) Waypoints.addTemporary(newName, pos, (set.starlev.StarredHeltix.feature.skyblock.waypoints.defaultTemporarySeconds * 1000).toLong()) else Waypoints.addRegular(newName, pos); newName = ""; isAdding = false; addAsTemporary = false }; return true }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { isAdding = false; newName = ""; addAsTemporary = false; return true }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) { if (newName.isNotEmpty()) newName = newName.dropLast(1); return true }
        }
        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (editingColorId != null) { val chr = event.codepoint().toChar(); val ok = chr == '#' || chr == ':' || (chr in '0'..'9') || (chr in 'a'..'f') || (chr in 'A'..'F') || chr == 'x' || chr == 'X'; if (ok && colorInput.length < 32) { colorInput += chr; colorError = null }; return true }
        if (isAdding) { newName += event.codepoint().toChar(); if (newName.length > 40) newName = newName.take(40); return true }
        return super.charTyped(event)
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) { SHBackdrop.render(graphics, width, height) }
    override fun onClose() { if (parent != null) minecraft.gui.setScreen(parent) else super.onClose() }
}
