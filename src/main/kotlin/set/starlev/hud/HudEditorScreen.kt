package set.starlev.hud

import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import net.minecraft.client.Minecraft
import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW
import kotlin.math.max
import kotlin.math.min
import set.starlev.features.skyblock.HudScoreboard

/**
 * Экран редактора HUD для изменения позиций элементов
 */
class HudEditorScreen : Screen(Component.literal("HUD Editor")) {
    private var draggingElement: HudElement? = null
    private var dragOffsetX = 0
    private var dragOffsetY = 0

    override fun init() {
        super.init()
        // Добавить кнопку сброса
        val resetButton = Button.builder(Component.literal("§cСброс позиций"))
            { onResetClicked() }
            .pos(this.width - 150, 10)
            .size(140, 20)
            .build()
        this.addRenderableWidget(resetButton)

        val scoreboardEditorButton = Button.builder(Component.literal("§eСтроки scoreboard")) {
            HudManager.isEditMode = false
            HudManager.saveAllLayouts()
            minecraft?.setScreen(ScoreboardEditorScreen(this))
        }
            .pos(this.width - 150, 35)
            .size(140, 20)
            .build()
        this.addRenderableWidget(scoreboardEditorButton)
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // Включить режим редактирования
        HudManager.isEditMode = true

        // Отрисовать справку
        guiGraphics.drawCenteredString(
            this.font,
            Component.literal("§7ЛКМ: Перемещение | ПКМ: Фон | Скролл: Масштаб"),
            this.width / 2,
            10,
            0xFFFFFF
        )
        guiGraphics.drawCenteredString(
            this.font,
            Component.literal("§7Shift+Скролл: Ширина | Ctrl+Скролл: Высота"),
            this.width / 2,
            22,
            0xAAAAAA
        )
        
        // Показать масштаб наведённого элемента
        for ((_, element) in HudManager.getAllElements()) {
            if (element.isHovered(mouseX, mouseY)) {
                val scalePercent = (element.scale * 100).toInt()
                guiGraphics.drawString(
                    this.font,
                    Component.literal("§eМасштаб: ${scalePercent}%"),
                    element.x,
                    element.y - 12,
                    0xFFFF55
                )
                break
            }
        }

        // Вызвать родительский render для кнопок
        super.render(guiGraphics, mouseX, mouseY, partialTick)
    }

    /**
     * Сброс позиций HUD элементов
     */
    private fun onResetClicked() {
        HudManager.resetAllPositions()
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        if (event.button() == 0) { // ЛКМ - Перетаскивание
            for ((_, element) in HudManager.getAllElements()) {
                if (element.isHovered(event.x().toInt(), event.y().toInt())) {
                    draggingElement = element
                    dragOffsetX = event.x().toInt() - element.x
                    dragOffsetY = event.y().toInt() - element.y
                    return true
                }
            }
        } else if (event.button() == 1) { // ПКМ - Вкл/Выкл фона
            for ((_, element) in HudManager.getAllElements()) {
                if (element.isHovered(event.x().toInt(), event.y().toInt())) {
                    element.showBackground = !element.showBackground
                    return true
                }
            }
        }
        return super.mouseClicked(event, isDoubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        draggingElement?.let { element ->
            val newX = event.x().toInt() - dragOffsetX
            val newY = event.y().toInt() - dragOffsetY
            element.x = newX.coerceAtLeast(0).coerceAtMost((this.width - element.getScaledWidth()).coerceAtLeast(0))
            element.y = newY.coerceAtLeast(0).coerceAtMost((this.height - element.getScaledHeight()).coerceAtLeast(0))
            return true
        }
        return super.mouseDragged(event, offsetX, offsetY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        // Отпустить левую кнопку мыши
        if (event.button() == 0 && draggingElement != null) {
            draggingElement = null
            return true
        }
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        // Найти элемент под курсором и изменить его параметры
        for ((_, element) in HudManager.getAllElements()) {
            if (element.isHovered(mouseX.toInt(), mouseY.toInt())) {
                val delta = if (verticalAmount > 0) 1 else -1
                
                if (isShiftDown()) {
                    // Ширина (минимум 50 или исходная ширина)
                    val change = delta * 10
                    val currentW = if (element.customWidth > 0) element.customWidth else element.getWidth()
                    element.customWidth = (currentW + change).coerceAtLeast(50)
                } else if (isControlDown()) {
                    // Высота (минимум 20 или исходная высота)
                    val change = delta * 10
                    val currentH = if (element.customHeight > 0) element.customHeight else element.getHeight()
                    element.customHeight = (currentH + change).coerceAtLeast(20)
                } else {
                    // Масштаб
                    if (delta > 0) element.increaseScale() else element.decreaseScale()
                }
                return true
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    private fun isShiftDown(): Boolean {
        val window = Minecraft.getInstance().window
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT)
    }

    private fun isControlDown(): Boolean {
        val window = Minecraft.getInstance().window
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)
    }

    override fun onClose() {
        HudManager.isEditMode = false
        HudManager.saveAllLayouts()
        super.onClose()
    }

    override fun isPauseScreen(): Boolean = false
}

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

    override fun onClose() {
        if (parent != null) {
            minecraft?.setScreen(parent)
        } else {
            super.onClose()
        }
    }

    override fun isPauseScreen(): Boolean = false
}
