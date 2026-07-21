package set.starlev.features.enchanting

/**
 * Класс для подсветки слотов в контейнерах.
 * @param slot Индекс слота
 * @param color Цвет в формате ARGB
 */
data class ColorHighlight(val slot: Int, val color: Int) {
    companion object {
        private const val RED_HIGHLIGHT = 0x66FF0000
        private const val YELLOW_HIGHLIGHT = 0x66FFFF00
        private const val GREEN_HIGHLIGHT = 0x6600FF00
        private const val GRAY_HIGHLIGHT = 0x66808080

        @JvmStatic
        fun red(slot: Int): ColorHighlight = ColorHighlight(slot, RED_HIGHLIGHT)

        @JvmStatic
        fun yellow(slot: Int): ColorHighlight = ColorHighlight(slot, YELLOW_HIGHLIGHT)

        @JvmStatic
        fun green(slot: Int): ColorHighlight = ColorHighlight(slot, GREEN_HIGHLIGHT)

        @JvmStatic
        fun gray(slot: Int): ColorHighlight = ColorHighlight(slot, GRAY_HIGHLIGHT)
    }
}
