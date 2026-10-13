package set.starlev.features.skyblock

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import set.starlev.StarredHeltix

/**
 * Утилита для отрисовки подсветки слотов по цвету редкости предмета.
 * 3 режима:
 *  1) Простой квадрат (без текстуры) — заливка через fill
 *  2) Квадрат с обводкой — бекграунд цветом + квадратная рамка через fill
 *  3) Круг — спрайт кружка через blitSprite (NativeImage-текстура)
 */
object SlotOverlayUtil {

    private val CIRCLE_SPRITE = Identifier.fromNamespaceAndPath("starredheltix", "item_background_circular")

    fun isEnabled(): Boolean {
        val config = StarredHeltix.feature.skyblock.slotOverlay
        return config.enabled
    }

    private fun getStyle(): set.starlev.config.categories.SkyblockConfig.SlotOverlayConfig.SlotStyle {
        return StarredHeltix.feature.skyblock.slotOverlay.getEffectiveStyle()
    }

    /** Возвращает прозрачность (0-255). */
    private fun getAlpha(): Int {
        return StarredHeltix.feature.skyblock.slotOverlay.alpha
            .coerceIn(0, 255)
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
     * 1) Простой квадрат (без текстуры) — обычная заливка.
     */
    fun drawSlotSimple(graphics: GuiGraphicsExtractor, x: Int, y: Int, size: Int, rgb: Int) {
        val alpha = getAlpha()
        if (alpha <= 0) return
        val color = (alpha shl 24) or (rgb and 0xFFFFFF)
        graphics.fill(x, y, x + size, y + size, color)
    }

    /**
     * 2) Квадрат с обводкой — бекграунд цветом редкости + квадратная рамка
     * поверх (чистый fill-рендер, без спрайтов).
     */
    fun drawSlotSquare(graphics: GuiGraphicsExtractor, x: Int, y: Int, size: Int, rgb: Int) {
        val alpha = getAlpha()
        if (alpha <= 0) return
        // Бекграунд
        val bgColor = (alpha shl 24) or (rgb and 0xFFFFFF)
        graphics.fill(x, y, x + size, y + size, bgColor)
        // Квадратная обводка более непрозрачным цветом для акцента
        val borderAlpha = (alpha + 80).coerceIn(0, 255)
        val borderColor = (borderAlpha shl 24) or (rgb and 0xFFFFFF)
        graphics.fill(x, y, x + size, y + 1, borderColor)
        graphics.fill(x, y + size - 1, x + size, y + size, borderColor)
        graphics.fill(x, y, x + 1, y + size, borderColor)
        graphics.fill(x + size - 1, y, x + size, y + size, borderColor)
    }

    /**
     * 3) Круг — спрайт кружка через blitSprite (текстура загружается через NativeImage).
     */
    fun drawSlotCircle(graphics: GuiGraphicsExtractor, x: Int, y: Int, size: Int, rgb: Int) {
        val alpha = getAlpha()
        if (alpha <= 0) return
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED, CIRCLE_SPRITE,
            x, y, size, size,
            (alpha shl 24) or (rgb and 0xFFFFFF)
        )
    }

    /**
     * Основная точка входа: рисует подсветку для предмета.
     * Рисуется везде, включая хотбар (отдельной опции больше нет).
     */
    fun drawOverlay(graphics: GuiGraphicsExtractor, x: Int, y: Int, stack: ItemStack) {
        if (!isEnabled()) return

        val rgb = getRgbColor(stack) ?: return
        when (getStyle()) {
            set.starlev.config.categories.SkyblockConfig.SlotOverlayConfig.SlotStyle.SIMPLE_SQUARE -> drawSlotSimple(graphics, x, y, 16, rgb)
            set.starlev.config.categories.SkyblockConfig.SlotOverlayConfig.SlotStyle.BORDERED_SQUARE -> drawSlotSquare(graphics, x, y, 16, rgb)
            set.starlev.config.categories.SkyblockConfig.SlotOverlayConfig.SlotStyle.CIRCLE -> drawSlotCircle(graphics, x, y, 16, rgb)
        }
    }

    /**
     * Фон слота в предпросмотре хранилищ (StoragePreview) — наследует стиль из «Слот-подсветки».
     * Если подсветка выключена — старое поведение: полупрозрачный квадрат (alpha 128).
     */
    fun drawPreviewBackground(graphics: GuiGraphicsExtractor, x: Int, y: Int, size: Int, rgb: Int) {
        if (!isEnabled()) {
            graphics.fill(x, y, x + size, y + size, (128 shl 24) or (rgb and 0xFFFFFF))
            return
        }
        when (getStyle()) {
            set.starlev.config.categories.SkyblockConfig.SlotOverlayConfig.SlotStyle.SIMPLE_SQUARE -> drawSlotSimple(graphics, x, y, size, rgb)
            set.starlev.config.categories.SkyblockConfig.SlotOverlayConfig.SlotStyle.BORDERED_SQUARE -> drawSlotSquare(graphics, x, y, size, rgb)
            set.starlev.config.categories.SkyblockConfig.SlotOverlayConfig.SlotStyle.CIRCLE -> drawSlotCircle(graphics, x, y, size, rgb)
        }
    }
}
