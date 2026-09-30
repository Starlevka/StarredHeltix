package set.starlev.utils.detectors

import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.slf4j.LoggerFactory

/**
 * Детектор фазы эксперимента стола чародейства по предмету в инструкции.
 *
 * Двухуровневая проверка (для совместимости с разными серверами):
 *
 * 1. **По Item enum** (Hypixel-стиль, быстрая):
 *    — `Items.GLOWSTONE` = фаза REMEMBER.
 *    — `Items.CLOCK` = фаза WAIT.
 *
 * 2. **По подстроке в displayName** (Heltix и кастомные серверы):
 *    — REMEMBER: «запомн», «remember», «sequence», «pattern».
 *    — WAIT: «оставш», «нажм», «click», «timer».
 *
 * Использует подстроки, а не полные слова — устойчиво к разным формулировкам.
 * Если предмет неизвестен и текст не подходит — NONE.
 */
object SlotPhaseDetector {

    private val LOGGER = LoggerFactory.getLogger("StarredHeltix")

    enum class Phase {
        /** Сервер показывает последовательность. */
        REMEMBER,

        /** Сервер ждёт клика игрока. */
        WAIT,

        /** Нет активной фазы. */
        NONE,
    }

    /** Определяет фазу по [ItemStack]. Пустой стек → [Phase.NONE]. */
    fun detect(stack: ItemStack): Phase {
        if (stack.isEmpty) return Phase.NONE
        return detect(stack.item, stack.hoverName.string)
    }

    /**
     * Определяет фазу по [Item] и его display name.
     *
     * Сначала проверяется Item enum, затем текст в displayName (подстроки).
     * Расширен для Heltix: также учитывает BOOKSHELF/CAULDRON/BOOK как статус,
     * но фазу определяем преимущественно по GLOWSTONE/CLOCK + тексту.
     */
    fun detect(item: Item, displayName: String): Phase {
        // 1. Item enum — Hypixel-стиль.
        when (item) {
            Items.GLOWSTONE -> return Phase.REMEMBER
            Items.CLOCK -> return Phase.WAIT
            // BOOKSHELF и CAULDRON — часто статус-индикаторы, но у Heltix могут быть инструкцией.
            // Проверяем их отдельно через текст, чтобы не пропустить фазу если Heltix использует их.
            else -> {}
        }

        // 2. Подстроки в displayName — Heltix и кастомные серверы.
        // Используем короткие подстроки для устойчивости к разным формулировкам.
        // Русские варианты от Heltix: "Запомните узор!", "Нажмите по порядку", "Оставшееся время: X", "Таймер: X"
        val name = displayName.lowercase()
        return when {
            "запомн" in name || "remember" in name || "sequence" in name || "pattern" in name || "узо" in name -> Phase.REMEMBER
            "оставш" in name || "нажм" in name || "click" in name || "timer" in name || "таймер" in name || "поряд" in name || "время" in name || "вторую" in name || "секунд" in name -> Phase.WAIT
            else -> Phase.NONE
        }
    }

    /** Проверяет, является ли предмет статус-индикатором (не игровым полем). */
    fun isStatus(stack: ItemStack): Boolean {
        if (stack.isEmpty) return true
        val item = stack.item
        if (item == Items.CLOCK || item == Items.BOOKSHELF || item == Items.CAULDRON) return true
        if (item == Items.GLOWSTONE) {
            val lore = stack.hoverName.string.lowercase()
            // Enchanted Book — не статус, а игровая ячейка суперпар
            if (lore.contains("enchanted book") || lore.contains("зачар")) return false
            return true
        }
        // Книги, часы, котлы — часто статус.
        val name = stack.hoverName.string.lowercase()
        return "запомн" in name || "remember" in name || "нажм" in name || "click" in name || "timer" in name || "время" in name
    }
}