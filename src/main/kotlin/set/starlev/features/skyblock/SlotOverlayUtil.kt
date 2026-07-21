package set.starlev.features.skyblock

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import set.starlev.StarredHeltix

/**
 * Утилита для отрисовки подсветки слотов по цвету редкости предмета.
 * Портировано из HELTIX.
 *
 * — Квадратная форма: [drawSlotSquare]
 * — Круглая форма: [drawSlotCircle]
 */
object SlotOverlayUtil {


    fun isEnabled(): Boolean {
        val config = StarredHeltix.feature.skyblock.slotOverlay
        return config.enabled
    }

    /** Возвращает режим формы: "Квадратная" или "Круглая". */
    private fun getShape(): String {
        return StarredHeltix.feature.skyblock.slotOverlay.shape
    }

    /** Возвращает прозрачность (0-255). */
    private fun getAlpha(): Int {
        return StarredHeltix.feature.skyblock.slotOverlay.alpha.toInt()
            .coerceIn(0, 255)
    }

    /** Возвращает, рисовать ли в хотбаре. */
    private fun showInHotbar(): Boolean {
        return StarredHeltix.feature.skyblock.slotOverlay.showInHotbar
    }

    /**
     * Извлекает цвет редкости из предмета (аналог HELTIX Utils.getRgbFromItemStack).
     * @return ARGB int (альфа будет заменена), или null если цвет не найден / белый.
     */
    fun getRgbColor(stack: ItemStack): Int? {
        if (stack.isEmpty) return null

        // Проверяем CUSTOM_NAME компонент
        val customName = stack.get(DataComponents.CUSTOM_NAME) ?: return null
        val style = customName.style
        val textColor = style.color ?: return null
        val rgb = textColor.value

        // Пропускаем белый цвет (обычные предметы)
        if (rgb == 0xFFFFFF) return null

        return rgb
    }

    /**
     * Рисует квадратную подсветку под предметом.
     * Аналог HELTIX Utils.drawBackground.
     */
    fun drawSlotSquare(graphics: GuiGraphics, x: Int, y: Int, size: Int, rgb: Int) {
        val alpha = getAlpha()
        if (alpha <= 0) return
        graphics.fill(x, y, x + size, y + size, (alpha shl 24) or (rgb and 0xFFFFFF))
    }

    /**
     * Рисует круглую подсветку под предметом.
     * Использует построчный рендеринг круга (сканирование по Y).
     * Улучшенная версия: полный охват -radius..radius и ровные края.
     */
    fun drawSlotCircle(graphics: GuiGraphics, x: Int, y: Int, size: Int, rgb: Int) {
        val alpha = getAlpha()
        if (alpha <= 0) return
        val centerX = x + size / 2
        val centerY = y + size / 2
        val radius = size / 2
        val color = (alpha shl 24) or (rgb and 0xFFFFFF)

        // Строчный рендеринг круга (алгоритм заполнения окружности)
        for (dy in -radius..radius) {
            val dx = Math.sqrt(((radius * radius - dy * dy).toDouble()))
            val lineHalf = dx.toInt()
            if (lineHalf < 0) continue
            graphics.fill(centerX - lineHalf, centerY + dy, centerX + lineHalf + 1, centerY + dy + 1, color)
        }
    }

    /**
     * Основная точка входа: рисует подсветку для предмета.
     */
    fun drawOverlay(graphics: GuiGraphics, x: Int, y: Int, stack: ItemStack, isHotbar: Boolean = false) {
        if (!isEnabled()) return
        if (isHotbar && !showInHotbar()) return

        val rgb = getRgbColor(stack) ?: return
        val shape = getShape()
        if (shape == "Круглая") {
            drawSlotCircle(graphics, x, y, 16, rgb)
        } else {
            drawSlotSquare(graphics, x, y, 16, rgb)
        }
    }
}
