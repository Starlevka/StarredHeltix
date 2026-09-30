package set.starlev.features.visual.swing

import kotlin.math.roundToInt
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText

// ---- Цветовые расширения ----

const val ARGB_ALPHA_SHIFT = 24
const val RGB_MASK = 0x00FFFFFF

fun Int.withAlpha(alpha: Int): Int =
    (alpha.coerceIn(0, 255) shl ARGB_ALPHA_SHIFT) or (this and RGB_MASK)

fun Int.withScaledAlpha(alphaScale: Double): Int {
    val scaledAlpha = ((this ushr ARGB_ALPHA_SHIFT) * alphaScale.coerceIn(0.0, 1.0))
        .roundToInt()
        .coerceIn(0, 255)
    return withAlpha(scaledAlpha)
}

// ---- Геометрия ----

data class Rect(val x: Int, val y: Int, val width: Int, val height: Int) {
    fun contains(mouseX: Int, mouseY: Int): Boolean =
        mouseX in x until x + width && mouseY in y until y + height

    fun intersects(other: Rect): Boolean =
        x < other.x + other.width &&
            x + width > other.x &&
            y < other.y + other.height &&
            y + height > other.y
}

data class Point(val x: Int, val y: Int)

// ---- Обрезка текста ----

fun Font.elide(text: String, maximumWidth: Int): String {
    if (width(text) <= maximumWidth) return text
    return plainSubstrByWidth(text, (maximumWidth - width(ELLIPSIS)).coerceAtLeast(0)) + ELLIPSIS
}

fun Font.elideComponent(text: Component, maximumWidth: Int): net.minecraft.util.FormattedCharSequence {
    if (width(text) <= maximumWidth) return text.visualOrderText
    val suffix = FormattedText.of(ELLIPSIS, text.style)
    val head = substrByWidth(text, (maximumWidth - width(suffix)).coerceAtLeast(0))
    return Language.getInstance().getVisualOrder(FormattedText.composite(head, suffix))
}

private const val ELLIPSIS = "…"

// ---- Стиль панели ----

object OverlayPanelStyle {
    const val BACKGROUND = 0xB0101010.toInt()
    const val OUTLINE = 0x80505050.toInt()
    const val PADDING = 5

    fun draw(context: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int) {
        context.fill(x, y, x + width, y + height, BACKGROUND)
        context.outline(x, y, width, height, OUTLINE)
    }
}

// ---- Цвета контролов ----

object PixelControlColors {
    val HEADER = 0x801B2530.toInt()
    val TEXT = 0xFFFFFFFF.toInt()
    val MUTED_TEXT = 0xFF9AA4AE.toInt()
    val SLIDER_TRACK = 0xFF30363B.toInt()
    val SLIDER_KNOB = 0xFFB9C2CA.toInt()
    val ACCENT = 0xFF45A3FF.toInt()
}

object PixelControlPanelRenderer {
    fun draw(context: GuiGraphicsExtractor, bounds: Rect, headerHeight: Int) {
        OverlayPanelStyle.draw(context, bounds.x, bounds.y, bounds.width, bounds.height)
        context.fill(
            bounds.x + BORDER,
            bounds.y + BORDER,
            bounds.x + bounds.width - BORDER,
            bounds.y + headerHeight,
            PixelControlColors.HEADER,
        )
    }

    private const val BORDER = 1
}


object PixelSliderRenderer {
    fun draw(context: GuiGraphicsExtractor, track: Rect, progress: Float, isHovered: Boolean) {
        val fillWidth = (track.width * progress.coerceIn(0f, 1f)).roundToInt()
        context.fill(
            track.x,
            track.y,
            track.x + track.width,
            track.y + track.height,
            PixelControlColors.SLIDER_TRACK,
        )
        context.fill(
            track.x,
            track.y,
            track.x + fillWidth,
            track.y + track.height,
            PixelControlColors.ACCENT,
        )
        val knobX = (track.x + fillWidth).coerceIn(track.x, track.x + track.width)
        context.fill(
            knobX - KNOB_HALF_WIDTH,
            track.y - KNOB_OVERHANG,
            knobX + KNOB_HALF_WIDTH,
            track.y + track.height + KNOB_OVERHANG,
            if (isHovered) PixelControlColors.TEXT else PixelControlColors.SLIDER_KNOB,
        )
    }

    private const val KNOB_HALF_WIDTH = 2
    private const val KNOB_OVERHANG = 2
}

// ---- Кнопки ----

enum class PixelButtonTone {
    NORMAL,
    DANGER,
    CONFIRM,
}

object PixelButtonRenderer {
    fun draw(
        context: GuiGraphicsExtractor,
        font: Font,
        bounds: Rect,
        label: String,
        selected: Boolean,
        hovered: Boolean,
        enabled: Boolean,
        tone: PixelButtonTone = PixelButtonTone.NORMAL,
        alpha: Double = 1.0,
    ) {
        val palette = when (tone) {
            PixelButtonTone.NORMAL -> PixelButtonColors.NORMAL
            PixelButtonTone.DANGER -> PixelButtonColors.DANGER
            PixelButtonTone.CONFIRM -> PixelButtonColors.CONFIRM
        }
        val fill = when {
            !enabled -> PixelButtonColors.DISABLED
            selected -> palette.selected
            hovered -> palette.hovered
            else -> palette.normal
        }

        drawChamferedLayer(context, bounds, PixelButtonColors.BORDER.withScaledAlpha(alpha))
        val inner = Rect(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - 2)
        drawChamferedLayer(context, inner, fill.withScaledAlpha(alpha))
        drawBevel(context, inner, selected, enabled, alpha)
        if (selected && enabled) {
            context.fill(
                inner.x + 1,
                inner.y + inner.height - 2,
                inner.x + inner.width - 1,
                inner.y + inner.height - 1,
                palette.accent.withScaledAlpha(alpha),
            )
        }
        val textPosition = labelPosition(bounds, font.width(label), font.lineHeight)
        context.text(
            font,
            label,
            textPosition.x,
            textPosition.y,
            (if (enabled) PixelButtonColors.TEXT else PixelButtonColors.DISABLED_TEXT).withScaledAlpha(alpha),
            false,
        )
    }

    fun labelPosition(bounds: Rect, textWidth: Int, lineHeight: Int): Point = Point(
        x = bounds.x + (bounds.width - textWidth + 1) / 2,
        y = bounds.y + (bounds.height - lineHeight + 2) / 2,
    )

    fun drawIcon(
        context: GuiGraphicsExtractor,
        bounds: Rect,
        icon: List<String>,
        scale: Int,
        enabled: Boolean,
    ) {
        val iconWidth = icon.maxOf(String::length) * scale
        val iconHeight = icon.size * scale
        val startX = bounds.x + (bounds.width - iconWidth) / 2
        val startY = bounds.y + (bounds.height - iconHeight) / 2
        for ((row, pixels) in icon.withIndex()) {
            for ((column, pixel) in pixels.withIndex()) {
                if (pixel != ICON_PIXEL) continue
                val x = startX + column * scale
                val y = startY + row * scale
                context.fill(
                    x,
                    y,
                    x + scale,
                    y + scale,
                    if (enabled) PixelButtonColors.TEXT else PixelButtonColors.DISABLED_TEXT,
                )
            }
        }
    }

    private fun drawChamferedLayer(context: GuiGraphicsExtractor, bounds: Rect, color: Int) {
        context.fill(bounds.x + 1, bounds.y, bounds.x + bounds.width - 1, bounds.y + bounds.height, color)
        context.fill(bounds.x, bounds.y + 1, bounds.x + bounds.width, bounds.y + bounds.height - 1, color)
    }

    private fun drawBevel(
        context: GuiGraphicsExtractor,
        bounds: Rect,
        selected: Boolean,
        enabled: Boolean,
        alpha: Double,
    ) {
        if (!enabled) return
        val topLeft = if (selected) PixelButtonColors.INSET else PixelButtonColors.HIGHLIGHT
        val bottomRight = if (selected) PixelButtonColors.HIGHLIGHT else PixelButtonColors.INSET
        context.fill(bounds.x + 1, bounds.y, bounds.x + bounds.width - 1, bounds.y + 1, topLeft.withScaledAlpha(alpha))
        context.fill(bounds.x, bounds.y + 1, bounds.x + 1, bounds.y + bounds.height - 1, topLeft.withScaledAlpha(alpha))
        context.fill(
            bounds.x + 1,
            bounds.y + bounds.height - 1,
            bounds.x + bounds.width - 1,
            bounds.y + bounds.height,
            bottomRight.withScaledAlpha(alpha),
        )
        context.fill(
            bounds.x + bounds.width - 1,
            bounds.y + 1,
            bounds.x + bounds.width,
            bounds.y + bounds.height - 1,
            bottomRight.withScaledAlpha(alpha),
        )
    }

    private const val ICON_PIXEL = 'X'
}

private data class PixelButtonPalette(
    val normal: Int,
    val hovered: Int,
    val selected: Int,
    val accent: Int,
)

private object PixelButtonColors {
    val NORMAL = PixelButtonPalette(
        normal = 0xFF3B4147.toInt(),
        hovered = 0xFF4B5963.toInt(),
        selected = 0xFF286B98.toInt(),
        accent = 0xFF65C2FF.toInt(),
    )
    val DANGER = PixelButtonPalette(
        normal = 0xFF493638.toInt(),
        hovered = 0xFF6C3C40.toInt(),
        selected = 0xFF7A343A.toInt(),
        accent = 0xFFFF7379.toInt(),
    )
    val CONFIRM = PixelButtonPalette(
        normal = 0xFF35483B.toInt(),
        hovered = 0xFF45634D.toInt(),
        selected = 0xFF3D6B49.toInt(),
        accent = 0xFF70D98A.toInt(),
    )
    val BORDER = 0xFF111315.toInt()
    val DISABLED = 0xFF24272A.toInt()
    val HIGHLIGHT = 0xFF6F7880.toInt()
    val INSET = 0xFF1D2226.toInt()
    val TEXT = 0xFFFFFFFF.toInt()
    val DISABLED_TEXT = 0xFF606870.toInt()
}

