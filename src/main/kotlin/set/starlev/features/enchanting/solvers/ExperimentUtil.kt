package set.starlev.features.enchanting.solvers

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import set.starlev.utils.detectors.ContainerDetector

/**
 * Общие эвристики сканирования стола экспериментов — порт логики определения
 * предметов/цветов из эталонного Heltix-мода `heltix-superpairs`
 * (`scripts/SOURCES/SOLVERS EXPERIMENTAL`).
 *
 * Отличия Heltix-механик от Hypixel:
 * — Ритмотрон — не «позиции», а ЦВЕТОВАЯ последовательность на стеклянном поле;
 * — Секвенсор — порядок кликов задаётся ЦВЕТОМ (спектр), а не числом на блоке;
 * — Суперпары — карточки видны по имени, пары ищутся по hoverName.
 *
 * Имена предметов декодируются через [ContainerDetector.decodeTitle] на случай
 * Hfont-кодирования (для обычного текста — no-op).
 */
internal object ExperimentUtil {

    fun clean(text: String): String {
        val decoded = try { ContainerDetector.decodeTitle(text) } catch (_: Exception) { text }
        return decoded.replace(Regex("(?i)§[0-9a-fk-or]"), "").trim()
    }

    fun path(stack: ItemStack): String {
        if (stack.isEmpty) return ""
        return try {
            BuiltInRegistries.ITEM.getKey(stack.item)?.path?.lowercase() ?: stack.item.toString().lowercase()
        } catch (_: Exception) {
            stack.item.toString().lowercase()
        }
    }

    fun hover(stack: ItemStack): String =
        if (stack.isEmpty) "" else clean(stack.hoverName.string).lowercase()

    /** «registry-path + hoverName» одной строкой — как rawItem+hoverName в эталоне. */
    fun blob(stack: ItemStack): String = path(stack) + " " + hover(stack)

    // ——— индикаторы фаз/состояний ———

    /** Фаза «запомните узор» (в эталоне — glowstone или «запомните»). */
    fun isRememberFlag(stack: ItemStack): Boolean {
        val b = blob(stack)
        return b.contains("glowstone") || hover(stack).contains("запомн") || hover(stack).contains("remember")
    }

    /** Фаза ожидания клика (в эталоне — clock/«часы»/«повторите»). */
    fun isWaitFlag(stack: ItemStack): Boolean {
        val b = blob(stack)
        return b.contains("clock") || hover(stack).contains("часы") ||
            hover(stack).contains("повтор") || hover(stack).contains("таймер") ||
            hover(stack).contains("оставш")
    }

    /** Статус «Раунд N» — смена текста = новый раунд (сброс солвера). */
    fun isRoundFlag(stack: ItemStack): Boolean {
        val h = hover(stack)
        return h.contains("раунд") || h.contains("round")
    }

    fun isSystemItem(hoverLower: String): Boolean =
        hoverLower.contains("стойка") || hoverLower.contains("раунд") || hoverLower.contains("полка") ||
            hoverLower.contains("часы") || hoverLower.contains("запомн") || hoverLower.contains("повтор") ||
            hoverLower.contains("нажм") || hoverLower.contains("клик")

    // ——— ячейки поля ———

    fun isGlass(stack: ItemStack): Boolean {
        val b = blob(stack)
        return b.contains("glass") || b.contains("стекло")
    }

    fun isPane(stack: ItemStack): Boolean {
        val b = blob(stack)
        return b.contains("pane") || b.contains("панель")
    }

    /** Закрытая/некликабельная карта (эталон superpairs: «закрыто/нажми/клик/…»). */
    fun isClosedCard(stack: ItemStack): Boolean {
        val h = hover(stack)
        return isGlass(stack) || isPane(stack) || h.contains("закрыто") ||
            h.contains("нажми") || h.contains("клик") || h.contains("полка") || h.isEmpty()
    }

    fun isCeramic(stack: ItemStack): Boolean {
        val b = blob(stack)
        return b.contains("terracotta") || b.contains("clay") || b.contains("concrete") ||
            b.contains("керамика") || b.contains("терракота") || b.contains("глина") || b.contains("бетон")
    }

    // ——— цвета ———

    private val SPECTRUM = listOf(
        "white", "light_blue", "blue", "purple", "magenta", "pink",
        "red", "orange", "yellow", "lime", "green", "cyan"
    )

    /** Индекс в спектре Секвенсора (0 white … 11 cyan), -1 — не цветная ячейка. */
    fun spectrumIndex(stack: ItemStack): Int {
        val b = blob(stack)
        // Эталон: костная мука = white(0), лазурит = blue(2)
        if (b.contains("bone") || b.contains("meal") || b.contains("кост") ||
            b.contains("мука") || b.contains("пыль") || b.contains("бел")
        ) return 0
        if (b.contains("lapis") || b.contains("лазурит") || b.contains("син")) return 2
        // ВАЖНО: light_blue(1) идёт раньше blue(2) в SPECTRUM, "голуб" раньше "син" в RU-фолбэке
        for ((i, needle) in SPECTRUM.withIndex()) {
            if (b.contains(needle)) return i
        }
        return when {
            b.contains("голуб") -> 1
            b.contains("фиолет") -> 3
            b.contains("пурпур") -> 4
            b.contains("розов") -> 5
            b.contains("красн") -> 6
            b.contains("оранж") -> 7
            b.contains("желт") -> 8
            b.contains("салат") -> 9
            b.contains("зелен") || b.contains("зелён") -> 10
            b.contains("бирюз") -> 11
            else -> -1
        }
    }

    /** Название цвета по индексу спектра (для логов). */
    fun spectrumName(index: Int): String = when (index) {
        0 -> "Белый"; 1 -> "Голубой"; 2 -> "Синий"; 3 -> "Фиолетовый"
        4 -> "Пурпурный"; 5 -> "Розовый"; 6 -> "Красный"; 7 -> "Оранжевый"
        8 -> "Жёлтый"; 9 -> "Салатовый"; 10 -> "Зелёный"; 11 -> "Бирюзовый"
        else -> "?$index"
    }
    fun parseColor(stack: ItemStack): String? {
        val i = spectrumIndex(stack)
        return when (i) {
            0 -> "Белый"; 1 -> "Голубой"; 2 -> "Синий"; 3 -> "Фиолетовый"
            4 -> "Пурпурный"; 5 -> "Розовый"; 6 -> "Красный"; 7 -> "Оранжевый"
            8 -> "Жёлтый"; 9 -> "Салатовый"; 10 -> "Зелёный"; 11 -> "Бирюзовый"
            else -> {
                // Коричневый/серый вне спектра — для Ритмотрона важны как классы
                val b = blob(stack)
                when {
                    b.contains("brown") || b.contains("коричн") -> "Коричневый"
                    b.contains("gray") || b.contains("grey") || b.contains("сер") -> "Серый"
                    b.contains("black") || b.contains("чёрн") || b.contains("черн") -> "Чёрный"
                    else -> null
                }
            }
        }
    }
}
