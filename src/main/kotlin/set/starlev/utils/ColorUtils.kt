package set.starlev.utils

import java.awt.Color

object ColorUtils {

    /**
     * Парсит цветовую строку из MoulConfig (chroma:alpha:r:g:b) в ARGB.
     * Поддерживает эффект радуги (chroma).
     */
    fun parseColor(colorStr: String, default: Int = 0xFFFFFFFF.toInt()): Int {
        return try {
            val parts = colorStr.split(":")
            if (parts.size < 4) return default

            val chroma: Int
            val a: Int
            val r: Int
            val g: Int
            val b: Int

            if (parts.size == 4) {
                // Формат alpha:r:g:b
                chroma = 0
                a = parts[0].toInt().coerceIn(0, 255)
                r = parts[1].toInt().coerceIn(0, 255)
                g = parts[2].toInt().coerceIn(0, 255)
                b = parts[3].toInt().coerceIn(0, 255)
            } else {
                // Формат chroma:alpha:r:g:b
                chroma = parts[0].toInt()
                a = parts[1].toInt().coerceIn(0, 255)
                r = parts[2].toInt().coerceIn(0, 255)
                g = parts[3].toInt().coerceIn(0, 255)
                b = parts[4].toInt().coerceIn(0, 255)
            }

            // Защита от коллизий с шейдерными эффектами текста.
            // Шейдеры используют альфу 242-254 для активации эффектов.
            // Если chroma == 0 (обычный цвет), мы сдвигаем альфу из этого диапазона.
            val finalAlpha = if (chroma == 0 && a in 242..254) {
                if (a >= 248) 255 else 241
            } else {
                a
            }

            if (chroma > 0) {
                // Логика радуги из MoulConfig
                val hsv = Color.RGBtoHSB(r, g, b, null)
                val speed = chroma.coerceIn(1, 255)
                val seconds = (255 - speed) / 254f * 59f + 1f
                
                hsv[0] = (hsv[0] + (System.currentTimeMillis() / 1000.0 / seconds).toFloat()) % 1f
                if (hsv[0] < 0) hsv[0] += 1f
                
                return (finalAlpha shl 24) or (Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]) and 0x00FFFFFF)
            } else {
                return (finalAlpha shl 24) or (r shl 16) or (g shl 8) or b
            }
        } catch (e: Exception) {
            default
        }
    }
}
