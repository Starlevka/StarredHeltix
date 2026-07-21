package set.starlev.hud

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import set.starlev.features.skyblock.HudScoreboard
import net.minecraft.client.input.MouseButtonEvent

class ScoreboardEditorScreen(private val parent: Screen? = null) : Screen(Component.literal("Scoreboard Editor")) {
    private data class LineBox(
        val key: String,
        val x1: Int,
        val y1: Int,
        val x2: Int,
        val y2: Int,
        val index: Int
    )

    private var draggingKey: String? = null
    private var dragInsertIndex = -1

    private var lastBaseX = 0
    private var lastBaseY = 0
    private var lastScale = 1f
    private var lastPadding = 4
    private var lastRowH = 9
    private var lastBoxes: List<LineBox> = emptyList()
    private var lastKeys: List<String> = emptyList()

    override fun init() {
        super.init()

        val resetButton = Button.builder(Component.literal("§cСброс")) {
            HudScoreboard.resetEditorOrder()
        }
            .pos(this.width - 120, 10)
            .size(110, 20)
            .build()
        this.addRenderableWidget(resetButton)
    }

    private fun toLocalX(mx: Int): Int {
        val s = lastScale.toDouble().coerceAtLeast(0.0001)
        return (lastBaseX + ((mx - lastBaseX) / s)).toInt()
    }

    private fun toLocalY(my: Int): Int {
        val s = lastScale.toDouble().coerceAtLeast(0.0001)
        return (lastBaseY + ((my - lastBaseY) / s)).toInt()
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.fill(0, 0, width, height, 0xC0101010.toInt())

        guiGraphics.drawCenteredString(
            this.font,
            Component.literal("§7Перетаскивай любые строки прямо на скорборде, ESC для выхода"),
            this.width / 2,
            10,
            0xFFFFFF
        )

        val lines = HudScoreboard.getEditorLines()
        if (lines.isEmpty()) {
            guiGraphics.drawCenteredString(font, Component.literal("§cScoreboard не найден"), width / 2, height / 2, 0xFFFFFF)
            super.render(guiGraphics, mouseX, mouseY, partialTick)
            return
        }

        val baseX = HudScoreboard.getAdjustedX()
        val baseY = HudScoreboard.getAdjustedY()
        val scale = HudScoreboard.scale
        val padding = 4
        val rowH = 9

        val maxWidth = lines.maxOf { font.width(it.component) }
        val sbWidth = maxWidth + padding * 2
        val sbHeight = lines.size * rowH + padding * 2

        lastBaseX = baseX
        lastBaseY = baseY
        lastScale = scale
        lastPadding = padding
        lastRowH = rowH
        lastKeys = lines.map { it.key }

        val boxes = ArrayList<LineBox>(lines.size)

        val pose = guiGraphics.pose()
        pose.pushMatrix()
        pose.translate(baseX.toFloat(), baseY.toFloat())
        pose.scale(scale, scale)
        pose.translate(-baseX.toFloat(), -baseY.toFloat())

        guiGraphics.fill(baseX, baseY, baseX + sbWidth, baseY + sbHeight, 0x70000000)

        var y = baseY + padding
        for (i in lines.indices) {
            val line = lines[i]
            val isDragging = draggingKey == line.key
            val bg = if (isDragging) 0x80FFFFFF.toInt() else 0x40101010
            guiGraphics.fill(baseX + 2, y, baseX + sbWidth - 2, y + rowH, bg)

            val drawX = if (line.centered) {
                baseX + (sbWidth - font.width(line.component)) / 2
            } else {
                baseX + sbWidth - padding - font.width(line.component)
            }
            guiGraphics.drawString(font, line.component, drawX, y, 0xFFFFFFFF.toInt())
            boxes.add(LineBox(line.key, baseX + 2, y, baseX + sbWidth - 2, y + rowH, i))
            y += rowH
        }

        if (draggingKey != null && dragInsertIndex >= 0) {
            val lineY = baseY + padding + dragInsertIndex.coerceIn(0, lines.size) * rowH
            guiGraphics.fill(baseX + 2, lineY - 1, baseX + sbWidth - 2, lineY + 1, 0xFF55FF55.toInt())
        }

        pose.popMatrix()

        lastBoxes = boxes

        super.render(guiGraphics, mouseX, mouseY, partialTick)
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        if (event.button() != 0) return super.mouseClicked(event, isDoubleClick)
        val mx = event.x().toInt()
        val my = event.y().toInt()
        val lx = toLocalX(mx)
        val ly = toLocalY(my)

        for (b in lastBoxes) {
            if (lx in b.x1..b.x2 && ly in b.y1..b.y2) {
                draggingKey = b.key
                dragInsertIndex = b.index
                return true
            }
        }
        return super.mouseClicked(event, isDoubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        draggingKey ?: return super.mouseDragged(event, offsetX, offsetY)
        val my = event.y().toInt()
        val ly = toLocalY(my)

        val startY = lastBaseY + lastPadding
        val relativeY = ly - startY
        val rawIndex = relativeY / lastRowH
        dragInsertIndex = rawIndex.coerceIn(0, lastKeys.size)
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (event.button() != 0) return super.mouseReleased(event)
        val key = draggingKey ?: return super.mouseReleased(event)

        val keys = lastKeys.toMutableList()
        val from = keys.indexOf(key)
        if (from >= 0) {
            keys.removeAt(from)
            var insertAt = dragInsertIndex.coerceIn(0, keys.size)
            if (from < insertAt) insertAt = (insertAt - 1).coerceAtLeast(0)
            keys.add(insertAt, key)
            HudScoreboard.setEditorOrder(keys)
        }

        draggingKey = null
        dragInsertIndex = -1
        return true
    }

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
    }

    override fun onClose() {
        if (parent != null) {
            minecraft?.setScreen(parent)
        } else {
            super.onClose()
        }
    }

    override fun isPauseScreen(): Boolean = false
}
