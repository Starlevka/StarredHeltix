package set.starlev.render

import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.client.Minecraft
import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW
import set.starlev.features.visual.swing.Rect
import set.starlev.render.RenderEngine
import set.starlev.render.SHChipRenderer
import set.starlev.render.SHTokens
import set.starlev.render.drawString
import set.starlev.render.drawCenteredString

/**
 * Экран редактора HUD — теперь с фильтром по категориям (пилот единого стиля SH-kit).
 * Чипы рендерятся как Pixel-кнопки, фильтр сохраняется в HudManager.editorFilter.
 */
class HudEditorScreen : Screen(Component.literal("HUD Editor")) {
    private var draggingElement: HudElement? = null
    private var dragOffsetX = 0
    private var dragOffsetY = 0
    private var guideX: Int? = null
    private var guideY: Int? = null
    private var chipBounds: List<Pair<Rect, HudManager.HudCategory>> = emptyList()

    companion object {
        private const val SNAP_DISTANCE = 6
    }

    private var actionChipBounds: List<Pair<Rect, String>> = emptyList()

    override fun init() {
        super.init()
        // Кнопки теперь — чипы того же стиля что и категории (SHChipRenderer), без ванильных Button
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        HudManager.isEditMode = true
        // Рендерим HUD нижним слоем, чтобы кнопки редактора были поверх
        RenderEngine.renderHud(graphics, Minecraft.getInstance().getDeltaTracker())
        graphics.nextStratum()

        graphics.drawCenteredString(
            this.font,
            Component.literal("§7ЛКМ: Перемещение | ПКМ: Фон | Скролл: Масштаб | Притягивание включено"),
            this.width / 2,
            10,
            0xFFFFFF
        )
        graphics.drawCenteredString(
            this.font,
            Component.literal("§7Shift+Скролл: Ширина | Ctrl+Скролл: Высота"),
            this.width / 2,
            22,
            0xAAAAAA
        )

        // Категорийные чипы (единый стиль — Pixel-чипы)
        drawCategoryChips(graphics, mouseX, mouseY)
        drawActionChips(graphics, mouseX, mouseY)

        // подсказка по фильтру
        val filteredCount = HudManager.getFilteredEditorElements().size
        val total = HudManager.getEditorElements().size
        if (HudManager.editorFilter != HudManager.HudCategory.ALL) {
            graphics.drawCenteredString(font, Component.literal("§8Показано $filteredCount/$total • §7ESC или чип «Все» чтобы сбросить"), width / 2, 80, SHTokens.TEXT_DIM.toInt())
        }

        guideX?.let { x -> graphics.fill(x, 0, x + 1, height, 0xAA55FFFF.toInt()) }
        guideY?.let { y -> graphics.fill(0, y, width, y + 1, 0xAA55FFFF.toInt()) }

        for (element in HudManager.getFilteredEditorElements()) {
            if (element.isHovered(mouseX, mouseY)) {
                val scalePercent = (element.scale * 100).toInt()
                graphics.drawString(
                    this.font,
                    Component.literal("§eМасштаб: ${scalePercent}% • ${HudManager.getHudCategory(element.id).label}"),
                    element.x,
                    element.y - 12,
                    0xFFFF55
                )
                break
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
    }

    private fun drawCategoryChips(g: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val cats = HudManager.HudCategory.entries
        var x = width / 2
        // сначала считаем общую ширину для центрирования
        var totalW = -4
        val widths = cats.map { SHChipRenderer.measureWidth(font, it.label) }
        for (w in widths) totalW += w + 4
        x -= totalW / 2
        val y = 38
        val h = 14
        val list = mutableListOf<Pair<Rect, HudManager.HudCategory>>()
        for ((i, cat) in cats.withIndex()) {
            val w = widths[i]
            val bounds = Rect(x, y, w, h)
            val hovered = bounds.contains(mouseX, mouseY)
            val selected = HudManager.editorFilter == cat
            SHChipRenderer.draw(g, font, bounds, cat.label, selected, hovered)
            list.add(bounds to cat)
            x += w + 4
        }
        chipBounds = list
    }

    private fun drawActionChips(g: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        // Подписи до 7 символов — иначе пиксельный чип слипается (SHChipRenderer.fit)
        val actions = listOf("Сброс" to "reset", "Строки" to "scoreboard")
        var totalW = -4
        val widths = actions.map { SHChipRenderer.measureWidth(font, it.first) }
        for (w in widths) totalW += w + 4
        var x = width / 2 - totalW / 2
        val y = 56
        val h = 14
        val list = mutableListOf<Pair<Rect, String>>()
        for ((i, pair) in actions.withIndex()) {
            val label = pair.first
            val id = pair.second
            val w = widths[i]
            val bounds = Rect(x, y, w, h)
            val hovered = bounds.contains(mouseX, mouseY)
            // Сброс — тон DANGER при ховере, scoreboard — обычный
            SHChipRenderer.draw(g, font, bounds, label, false, hovered)
            list.add(bounds to id)
            x += w + 4
        }
        actionChipBounds = list
    }

    private fun onResetClicked() {
        HudManager.resetAllPositions()
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        // чипы имеют приоритет — категории + действия (сброс/scoreboard)
        val mx = event.x().toInt()
        val my = event.y().toInt()
        for ((bounds, cat) in chipBounds) {
            if (bounds.contains(mx, my) && event.button() == 0) {
                HudManager.editorFilter = cat
                return true
            }
        }
        for ((bounds, id) in actionChipBounds) {
            if (bounds.contains(mx, my) && event.button() == 0) {
                when (id) {
                    "reset" -> onResetClicked()
                    "scoreboard" -> {
                        HudManager.isEditMode = false
                        HudManager.saveAllLayouts()
                        minecraft.gui.setScreen(ScoreboardEditorScreen(this))
                    }
                }
                return true
            }
        }
        if (event.button() == 0) {
            for (element in HudManager.getFilteredEditorElements()) {
                if (element.isHovered(mx, my)) {
                    draggingElement = element
                    dragOffsetX = mx - element.x
                    dragOffsetY = my - element.y
                    return true
                }
            }
        } else if (event.button() == 1) {
            for (element in HudManager.getFilteredEditorElements()) {
                if (element.isHovered(mx, my)) {
                    element.showBackground = !element.showBackground
                    return true
                }
            }
        }
        return super.mouseClicked(event, isDoubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        draggingElement?.let { element ->
            val rawX = event.x().toInt() - dragOffsetX
            val rawY = event.y().toInt() - dragOffsetY
            val snapped = applySnap(element, rawX, rawY)
            element.x = snapped.first.coerceAtLeast(0).coerceAtMost((this.width - element.getScaledWidth()).coerceAtLeast(0))
            element.y = snapped.second.coerceAtLeast(0).coerceAtMost((this.height - element.getScaledHeight()).coerceAtLeast(0))
            return true
        }
        return super.mouseDragged(event, offsetX, offsetY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (event.button() == 0 && draggingElement != null) {
            draggingElement = null
            guideX = null
            guideY = null
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
        for (element in HudManager.getFilteredEditorElements()) {
            if (element.isHovered(mouseX.toInt(), mouseY.toInt())) {
                val delta = if (verticalAmount > 0) 1 else -1
                if (isShiftDown()) {
                    val change = delta * 10
                    val currentW = if (element.customWidth > 0) element.customWidth else element.getWidth()
                    element.customWidth = (currentW + change).coerceAtLeast(50)
                } else if (isControlDown()) {
                    val change = delta * 10
                    val currentH = if (element.customHeight > 0) element.customHeight else element.getHeight()
                    element.customHeight = (currentH + change).coerceAtLeast(20)
                } else {
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

    private fun applySnap(element: HudElement, rawX: Int, rawY: Int): Pair<Int, Int> {
        val elementW = element.getScaledWidth().coerceAtLeast(1)
        val elementH = element.getScaledHeight().coerceAtLeast(1)
        var bestX = rawX
        var bestY = rawY
        var bestDX = SNAP_DISTANCE + 1
        var bestDY = SNAP_DISTANCE + 1
        guideX = null
        guideY = null

        fun trySnapX(targetX: Int, snappedX: Int) {
            val delta = kotlin.math.abs(rawX - snappedX)
            if (delta < bestDX && delta <= SNAP_DISTANCE) { bestDX = delta; bestX = snappedX; guideX = targetX.coerceIn(0, width) }
        }
        fun trySnapY(targetY: Int, snappedY: Int) {
            val delta = kotlin.math.abs(rawY - snappedY)
            if (delta < bestDY && delta <= SNAP_DISTANCE) { bestDY = delta; bestY = snappedY; guideY = targetY.coerceIn(0, height) }
        }
        val selfLeft = rawX; val selfCenterX = rawX + elementW / 2; val selfTop = rawY; val selfCenterY = rawY + elementH / 2
        trySnapX(0, 0); trySnapX(width / 2, width / 2 - elementW / 2); trySnapX(width, width - elementW)
        trySnapY(0, 0); trySnapY(height / 2, height / 2 - elementH / 2); trySnapY(height, height - elementH)
        for (other in HudManager.getFilteredEditorElements()) {
            if (other === element) continue
            val left = other.x; val centerX = other.x + other.getScaledWidth() / 2; val right = other.x + other.getScaledWidth()
            val top = other.y; val centerY = other.y + other.getScaledHeight() / 2; val bottom = other.y + other.getScaledHeight()
            trySnapX(left, left); trySnapX(centerX, centerX - elementW / 2); trySnapX(right, right - elementW); trySnapX(left, left - elementW); trySnapX(right, right); trySnapX(centerX, centerX - (selfCenterX - selfLeft))
            trySnapY(top, top); trySnapY(centerY, centerY - elementH / 2); trySnapY(bottom, bottom - elementH); trySnapY(top, top - elementH); trySnapY(bottom, bottom); trySnapY(centerY, centerY - (selfCenterY - selfTop))
        }
        return bestX to bestY
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {}
    override fun onClose() { HudManager.isEditMode = false; HudManager.saveAllLayouts(); super.onClose() }
    override fun isPauseScreen(): Boolean = false
}
