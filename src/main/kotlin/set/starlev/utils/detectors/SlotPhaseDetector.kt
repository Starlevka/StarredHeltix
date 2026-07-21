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
     */
    fun detect(item: Item, displayName: String): Phase {
        // 1. Item enum — Hypixel-стиль.
        when (item) {
            Items.GLOWSTONE -> return Phase.REMEMBER
            Items.CLOCK -> return Phase.WAIT
            else -> {}
        }

        // 2. Подстроки в displayName — Heltix и кастомные серверы.
        // Используем короткие подстроки для устойчивости к разным формулировкам.
        val name = displayName.lowercase()
        return when {
            "запомн" in name || "remember" in name || "sequence" in name -> Phase.REMEMBER
            "оставш" in name || "нажм" in name || "click" in name || "timer" in name -> Phase.WAIT
            else -> Phase.NONE
        }
    }
}