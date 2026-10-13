package set.starlev.render

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import set.starlev.features.visual.swing.OverlayPanelStyle
import set.starlev.features.visual.swing.PixelButtonRenderer
import set.starlev.features.visual.swing.PixelButtonTone
import set.starlev.features.visual.swing.PixelControlColors
import set.starlev.features.visual.swing.Rect
import set.starlev.features.visual.swing.withAlpha

/**
 * Единый дизайн-кит мода (SH = StarredHeltix).
 * Базируется на отполированной системе SwingEditor (PixelControl) — самый совершенный стиль в моде.
 * Пилот: токены + SHPanel/SHHeader/SHRow/SHFooter/SHBackdrop для Filter/Binds/Waypoints/InventoryButtons.
 */
object SHTokens {
    const val BACKDROP = 0x80000000 // общий скрим (50% чёрный)
    const val PANEL_BG = 0xB0101010 // OverlayPanelStyle.BACKGROUND
    const val PANEL_OUTLINE = 0x80505050
    const val ROW_BG = 0x70000000
    const val ROW_HOVER = 0x90000000
    const val ROW_ACTIVE = 0x90FFFFFF
    const val MODAL_SHADE = 0xA0000000.toInt()
    const val TEXT_PRIMARY = 0xFFFFFFFF // белый
    const val TEXT_MUTED = 0xFF9AA4AE // из PixelControlColors.MUTED
    const val TEXT_DIM = 0xFF606870
    const val ACCENT = 0xFF45A3FF // электрик-синий
    const val ACCENT_SOFT = 0x803AAFD9
    const val DANGER = 0xFFFF5555
    const val SUCCESS = 0xFF55FF55

    // Отступы / размеры (4pt сетка)
    const val CARD_INSET = 9
    const val CONTENT_WIDTH = 280
    const val CONTENT_WIDTH_WIDE = 400
    const val ROW_HEIGHT = 25
    const val HEADER_HEIGHT = 24
    const val PADDING = 4
    const val FOOTER_OFFSET = 30
    const val SCROLLBAR_WIDTH = 2
}

// ---- Backdrop / Panel ----

object SHBackdrop {
    fun render(g: GuiGraphicsExtractor, w: Int, h: Int) {
        g.fill(0, 0, w, h, SHTokens.BACKDROP.toInt())
    }
}

object SHPanel {
    fun draw(g: GuiGraphicsExtractor, bounds: Rect) {
        g.fill(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, SHTokens.PANEL_BG.toInt())
        g.outline(bounds.x, bounds.y, bounds.width, bounds.height, SHTokens.PANEL_OUTLINE.toInt())
    }

    fun drawWithHeader(g: GuiGraphicsExtractor, bounds: Rect, headerH: Int = SHTokens.HEADER_HEIGHT) {
        draw(g, bounds)
        g.fill(bounds.x + 1, bounds.y + 1, bounds.x + bounds.width - 1, bounds.y + headerH, PixelControlColors.HEADER)
    }
}

object SHHeader {
    fun draw(g: GuiGraphicsExtractor, font: Font, title: Component, subtitle: String?, hint: String?, width: Int) {
        g.drawCenteredString(font, title, width / 2, 10, SHTokens.TEXT_PRIMARY.toInt())
        if (subtitle != null) {
            g.drawCenteredString(font, Component.literal(subtitle), width / 2, 25, SHTokens.TEXT_MUTED.toInt())
        }
        if (hint != null) {
            g.drawCenteredString(font, Component.literal(hint), width / 2, 35, SHTokens.TEXT_DIM.toInt())
        }
    }
}

object SHFooter {
    fun draw(g: GuiGraphicsExtractor, font: Font, width: Int, height: Int, text: String = "§7ESC — Назад") {
        g.drawCenteredString(font, Component.literal(text), width / 2, height - SHTokens.FOOTER_OFFSET, SHTokens.TEXT_MUTED.toInt())
    }
}

object SHRow {
    fun draw(g: GuiGraphicsExtractor, bounds: Rect, hovered: Boolean, active: Boolean = false) {
        val bg = when {
            active -> SHTokens.ROW_ACTIVE.toInt()
            hovered -> SHTokens.ROW_HOVER.toInt()
            else -> SHTokens.ROW_BG
        }
        g.fill(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, bg)
    }

    fun drawButton(
        g: GuiGraphicsExtractor,
        font: Font,
        bounds: Rect,
        label: String,
        selected: Boolean = false,
        hovered: Boolean = false,
        enabled: Boolean = true,
        tone: PixelButtonTone = PixelButtonTone.NORMAL
    ) {
        PixelButtonRenderer.draw(g, font, bounds, label, selected, hovered, enabled, tone)
    }
}

// ---- Scroll helper (scissor + scrollbar) ----

object SHListHelper {
    /**
     * Рисует вертикальный скролл-бар справа от области.
     */
    fun drawScrollbar(
        g: GuiGraphicsExtractor,
        area: Rect,
        totalContentHeight: Int,
        scrollOffset: Int,
        visibleHeight: Int
    ) {
        if (totalContentHeight <= visibleHeight) return
        val barH = (visibleHeight.toFloat() / totalContentHeight * visibleHeight).toInt().coerceAtLeast(12)
        val maxScroll = (totalContentHeight - visibleHeight).coerceAtLeast(1)
        val barY = area.y + (scrollOffset.toFloat() / maxScroll * (visibleHeight - barH)).toInt()
        val barX = area.x + area.width - SHTokens.SCROLLBAR_WIDTH
        // track
        g.fill(barX, area.y, barX + SHTokens.SCROLLBAR_WIDTH, area.y + visibleHeight, 0x22000000)
        // thumb
        g.fill(barX, barY, barX + SHTokens.SCROLLBAR_WIDTH, barY + barH, SHTokens.ACCENT.toInt())
    }

    fun clampScroll(offset: Int, totalHeight: Int, visibleHeight: Int): Int {
        val max = (totalHeight - visibleHeight).coerceAtLeast(0)
        return offset.coerceIn(0, max)
    }
}

// ---- Chip / Category filter (для HUD редактора) ----

data class SHChip(val id: String, val label: String, val color: Int = SHTokens.ACCENT.toInt())

object SHChipRenderer {
    /**
     * Больше 7 символов пиксельный чип не переваривает — подпись «Сброс позиций»
     * или «Строки scoreboard» превращалась в кашу из слипшихся пикселей.
     */
    private const val MAX_LABEL_LENGTH = 7

    fun fit(label: String): String =
        if (label.length <= MAX_LABEL_LENGTH) label else label.take(MAX_LABEL_LENGTH)

    fun draw(
        g: GuiGraphicsExtractor,
        font: Font,
        bounds: Rect,
        label: String,
        selected: Boolean,
        hovered: Boolean
    ) {
        // Используем PixelButtonRenderer для единообразия — маленький чип как кнопка
        PixelButtonRenderer.draw(
            g, font, bounds, fit(label), selected, hovered, true,
            if (selected) PixelButtonTone.CONFIRM else PixelButtonTone.NORMAL
        )
    }

    fun measureWidth(font: Font, label: String): Int {
        return font.width(fit(label)) + 16 // padding 8+8
    }
}