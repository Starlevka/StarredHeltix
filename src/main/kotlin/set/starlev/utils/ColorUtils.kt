package set.starlev.utils

import java.awt.Color

object ColorUtils {

    /**
     * Парсит цветовую строку из MoulConfig в ARGB.
     * Поддерживает эффект радуги (chroma).
     */
    fun parseColor(colorStr: String, default: Int = 0xFFFFFFFF.toInt()): Int {
        return try {
            if (colorStr.startsWith("#")) {
                val hex = colorStr.removePrefix("#")
                val longVal = hex.toLongOrNull(16) ?: return default
                return when (hex.length) {
                    6 -> (0xFF shl 24) or longVal.toInt() // RRGGBB -> Alpha 255
                    8 -> longVal.toInt() // AARRGGBB
                    else -> default
                }
            }

            val parts = colorStr.split(":")
            if (parts.size < 4) return default

            val chroma: Int
            val a: Int
            val r: Int
            val g: Int
            val b: Int

            when (parts.size) {
                4 -> {
                    chroma = 0
                    r = parts[0].toInt().coerceIn(0, 255)
                    g = parts[1].toInt().coerceIn(0, 255)
                    b = parts[2].toInt().coerceIn(0, 255)
                    a = parts[3].toInt().coerceIn(0, 255)
                }
                5 -> {
                    val chromaPart = parts[0]
                    chroma = when {
                        chromaPart.equals("chroma", ignoreCase = true) -> 255
                        chromaPart.equals("rainbow", ignoreCase = true) -> 255
                        else -> chromaPart.toIntOrNull() ?: 0
                    }
                    a = parts[1].toInt().coerceIn(0, 255)
                    r = parts[2].toInt().coerceIn(0, 255)
                    g = parts[3].toInt().coerceIn(0, 255)
                    b = parts[4].toInt().coerceIn(0, 255)
                }
                else -> return default
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
                
                hsv[0] = ((System.currentTimeMillis() % 1000000L) / 1000.0f / seconds) % 1f
                if (hsv[0] < 0) hsv[0] += 1f
                
                // Если включена радуга, принудительно устанавливаем насыщенность и яркость,
                // иначе белый цвет (S=0) останется белым даже при смене оттенка.
                if (hsv[1] < 0.5f) hsv[1] = 1.0f
                if (hsv[2] < 0.5f) hsv[2] = 1.0f
                
                return (finalAlpha shl 24) or (Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]) and 0x00FFFFFF)
            } else {
                return (finalAlpha shl 24) or (r shl 16) or (g shl 8) or b
            }
        } catch (e: Exception) {
            default
        }
    }
}
