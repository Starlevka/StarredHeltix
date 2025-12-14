package set.starlev.hud

import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component

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
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // Включить режим редактирования
        HudManager.isEditMode = true

        // Отрисовать все HUD элементы
        HudManager.renderAll(guiGraphics)

        // Отрисовать справку
        guiGraphics.drawCenteredString(
            this.font,
            Component.literal("§7Перетаскивайте элементы, скролл для масштаба, ESC для выхода"),
            this.width / 2,
            10,
            0xFFFFFF
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
        if (event.button() == 0) {
            for ((_, element) in HudManager.getAllElements()) {
                if (element.isHovered(event.x().toInt(), event.y().toInt())) {
                    draggingElement = element
                    dragOffsetX = event.x().toInt() - element.x
                    dragOffsetY = event.y().toInt() - element.y
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
        // Найти элемент под курсором и изменить его масштаб
        for ((_, element) in HudManager.getAllElements()) {
            if (element.isHovered(mouseX.toInt(), mouseY.toInt())) {
                if (verticalAmount > 0) {
                    // Скролл вверх - увеличить масштаб
                    element.increaseScale()
                } else if (verticalAmount < 0) {
                    // Скролл вниз - уменьшить масштаб
                    element.decreaseScale()
                }
                return true
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun onClose() {
        HudManager.isEditMode = false
        HudManager.saveAllLayouts()
        super.onClose()
    }

    override fun isPauseScreen(): Boolean = false
}
