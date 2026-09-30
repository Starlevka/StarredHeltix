package set.starlev.render

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence

/**
 * Совместимость с GuiGraphics API старых версий (1.21.x) поверх GuiGraphicsExtractor 26.2.
 * drawString/drawCenteredString сохраняют старую семантику теней:
 * String — с тенью, Component/FormattedCharSequence — без.
 */
fun GuiGraphicsExtractor.drawString(font: Font, text: String, x: Int, y: Int, color: Int, shadow: Boolean = true) {
    this.text(font, text, x, y, color, shadow)
}

fun GuiGraphicsExtractor.drawString(font: Font, text: Component, x: Int, y: Int, color: Int, shadow: Boolean = false) {
    this.text(font, text, x, y, color, shadow)
}

fun GuiGraphicsExtractor.drawString(font: Font, text: FormattedCharSequence, x: Int, y: Int, color: Int, shadow: Boolean = false) {
    this.text(font, text, x, y, color, shadow)
}

fun GuiGraphicsExtractor.drawCenteredString(font: Font, text: String, x: Int, y: Int, color: Int, shadow: Boolean = true) {
    this.centeredText(font, text, x, y, color)
}

fun GuiGraphicsExtractor.drawCenteredString(font: Font, text: Component, x: Int, y: Int, color: Int, shadow: Boolean = true) {
    this.centeredText(font, text, x, y, color)
}

fun GuiGraphicsExtractor.drawCenteredString(font: Font, text: FormattedCharSequence, x: Int, y: Int, color: Int, shadow: Boolean = true) {
    this.centeredText(font, text, x, y, color)
}

/**
 * Скруглённый прямоугольник («лестницей», как RoundedCornerRectRenderer в SkyHanni):
 * первые/последние `radius` рядов отступают по дуге окружности, середина — один fill.
 * ~2*radius+1 вызовов fill на панель. Цвет ARGB.
 */
fun GuiGraphicsExtractor.drawRoundedPanel(x: Int, y: Int, width: Int, height: Int, color: Int, radius: Int = 4) {
    if (width <= 0 || height <= 0) return
    val r = radius.coerceAtMost(minOf(width, height) / 2)
    if (r <= 0) {
        fill(x, y, x + width, y + height, color)
        return
    }
    for (i in 0 until r) {
        val dy = r - 0.5f - i
        val hw = kotlin.math.sqrt((r * r - dy * dy).coerceAtLeast(0f))
        val inset = kotlin.math.ceil(r - hw).toInt().coerceIn(0, r)
        fill(x + inset, y + i, x + width - inset, y + i + 1, color)
        fill(x + inset, y + height - 1 - i, x + width - inset, y + height - i, color)
    }
    fill(x, y + r, x + width, y + height - r, color)
}

/**
 * Гладкая скруглённая панель с software-сглаживанием углов:
 * ядро — крупные fill, дуга — попиксельный альфа-бленд (2x2 субпикселы),
 * маска покрытия кэшируется на радиус. Выглядит как шейдерный rounded rect
 * (Skysoft/CustomScoreboard), но только на ванильных fill — без пайплайнов.
 * Цвет ARGB.
 */
fun GuiGraphicsExtractor.drawSmoothRoundedPanel(x: Int, y: Int, width: Int, height: Int, color: Int, radius: Int = 4) {
    if (width <= 0 || height <= 0) return
    val baseA = (color ushr 24) and 0xFF
    if (baseA <= 0) return
    val r = radius.coerceAtMost(minOf(width, height) / 2)
    if (r <= 0) {
        fill(x, y, x + width, y + height, color)
        return
    }
    val rgb = color and 0x00FFFFFF
    // Ядро — три крупных fill на полной альфе
    fill(x, y + r, x + width, y + height - r, color)
    fill(x + r, y, x + width - r, y + r, color)
    fill(x + r, y + height - r, x + width - r, y + height, color)
    // Углы — попиксельно по кэшированной маске (TL, TR, BL, BR одним проходом)
    val mask = cornerMask(r)
    for (i in 0 until r) {
        for (j in 0 until r) {
            val cov = mask[i * r + j]
            if (cov <= 0f) continue
            val px = x + j
            val qx = x + width - 1 - j
            val pyTop = y + i
            val pyBot = y + height - 1 - i
            if (cov >= 1f) {
                fill(px, pyTop, px + 1, pyTop + 1, color)
                if (qx != px) fill(qx, pyTop, qx + 1, pyTop + 1, color)
                fill(px, pyBot, px + 1, pyBot + 1, color)
                if (qx != px) fill(qx, pyBot, qx + 1, pyBot + 1, color)
            } else {
                val a = (baseA * cov + 0.5f).toInt().coerceIn(1, baseA)
                val c = (a shl 24) or rgb
                fill(px, pyTop, px + 1, pyTop + 1, c)
                if (qx != px) fill(qx, pyTop, qx + 1, pyTop + 1, c)
                fill(px, pyBot, px + 1, pyBot + 1, c)
                if (qx != px) fill(qx, pyBot, qx + 1, pyBot + 1, c)
            }
        }
    }
}

/** Маска покрытия пикселей углового квадрата r×r (центр дуги в (r, r), 2x2 субпикселы). */
private val cornerMaskCache = HashMap<Int, FloatArray>()

private fun cornerMask(r: Int): FloatArray {
    synchronized(cornerMaskCache) {
        return cornerMaskCache.getOrPut(r) {
            val out = FloatArray(r * r)
            val offs = floatArrayOf(-0.25f, 0.25f)
            for (i in 0 until r) {
                for (j in 0 until r) {
                    var inside = 0
                    for (oy in offs) {
                        for (ox in offs) {
                            val dx = (j + 0.5f + ox) - r
                            val dy = (i + 0.5f + oy) - r
                            if (dx * dx + dy * dy <= r * r) inside++
                        }
                    }
                    out[i * r + j] = inside / 4f
                }
            }
            out
        }
    }
}

/** Отступ дуги для строки i (0 = крайняя) при радиусе r — для построчной обрезки текстур. */
private fun arcInset(i: Int, r: Int): Int {
    if (r <= 0) return 0
    val dy = r - 0.5f - i
    val hw = kotlin.math.sqrt((r * r - dy * dy).coerceAtLeast(0f))
    return kotlin.math.ceil(r - hw).toInt().coerceIn(0, r)
}

/**
 * Текстура фоном, растянутая под структуру width x height:
 * картинка ЛЮБОГО разрешения (imgW x imgH) полностью натягивается на цель —
 * как CSS background-size: 100% 100%. UV всегда 0..1 по всему изображению,
 * поэтому произвольные размеры файла безопасны.
 */
fun GuiGraphicsExtractor.blitStretched(
    id: net.minecraft.resources.Identifier,
    x: Int, y: Int, width: Int, height: Int,
    imgW: Int, imgH: Int, color: Int = -1
) {
    if (width <= 0 || height <= 0 || imgW <= 0 || imgH <= 0) return
    blit(
        net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
        id, x, y, 0.0f, 0.0f,
        width, height, imgW, imgH, imgW, imgH, color
    )
}

/**
 * Та же растяжка текстуры, но со скруглёнными углами (как RoundedTexture
 * в CustomScoreboard): середина — одним blit, верхние/нижние `radius` рядов —
 * построчно с дуговым отступом и пропорциональной нарезкой UV.
 */
fun GuiGraphicsExtractor.blitRoundedStretched(
    id: net.minecraft.resources.Identifier,
    x: Int, y: Int, width: Int, height: Int,
    imgW: Int, imgH: Int, radius: Int = 4, color: Int = -1
) {
    if (width <= 0 || height <= 0 || imgW <= 0 || imgH <= 0) return
    val r = radius.coerceAtMost(minOf(width, height) / 2)
    if (r <= 0) {
        blitStretched(id, x, y, width, height, imgW, imgH, color)
        return
    }
    val pipe = net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED
    fun rowBlit(dx: Int, dy: Int, dw: Int) {
        if (dw <= 0) return
        val u = dx * imgW.toFloat() / width
        val v = dy * imgH.toFloat() / height
        val sw = (dw * imgW.toFloat() / width + 0.5f).toInt().coerceAtLeast(1)
        val sh = (imgH.toFloat() / height + 0.5f).toInt().coerceAtLeast(1)
        blit(pipe, id, x + dx, y + dy, u, v, dw, 1, sw, sh, imgW, imgH, color)
    }
    // Середина одним вызовом
    if (height - 2 * r > 0) {
        val v = r * imgH.toFloat() / height
        val sh = ((height - 2 * r) * imgH.toFloat() / height + 0.5f).toInt().coerceAtLeast(1)
        blit(pipe, id, x, y + r, 0f, v, width, height - 2 * r, imgW, sh, imgW, imgH, color)
    }
    // Верх/низ построчно по дуге
    for (i in 0 until r) {
        val inset = arcInset(i, r)
        rowBlit(inset, i, width - inset * 2)
        rowBlit(inset, height - 1 - i, width - inset * 2)
    }
}
