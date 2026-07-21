package set.starlev.features.enchanting.solvers

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.core.component.DataComponents
import set.starlev.StarredHeltix
import set.starlev.features.enchanting.ColorHighlight
import set.starlev.features.enchanting.ExperimentSolver
import set.starlev.utils.detectors.SlotPhaseDetector

/**
 * Chronomatron (Ритмотрон) — решатель мини-игры «Хронометрон».
 *
 * Архитектура:
 * — Event-driven: получает обновления слотов через [onSlotUpdate] (от EventBus).
 * — Не подписывается на [net.minecraft.world.inventory.ContainerListener] напрямую.
 * — Детект фазы через [SlotPhaseDetector] (Item enum + текст displayName).
 * — Хранит **слоты**, а не предметы. Не нужен TERRACOTTA_TO_GLASS Map на 16 пар.
 *
 * Что исправлено по сравнению со старой реализацией:
 * — Баг #1: после записи первого highlight переходил в WAIT — остальные highlight'ы фазы
 *   REMEMBER терялись. Теперь состояние остаётся REMEMBER до прихода CLOCK/GLOWSTONE-text.
 * — Баг #2: хранил только ОДИН слот за раз (`chronomatronCurrentSlot`). Теперь накапливаем
 *   ВСЕ подсвеченные слоты текущего шага через `pendingSlots: MutableSet<Int>`. Это критично,
 *   потому что Chronomatron подсвечивает все слоты одного цвета разом.
 * — Баг #3: убрана мёртвая ветка `chronomatronChainLengthCount++` с непонятной логикой.
 * — Баг #4: детект фазы через [SlotPhaseDetector] — Item enum ИЛИ текст displayName
 *   (для Heltix, где предмет может быть не GLOWSTONE/CLOCK).
 * — Баг #5: убран TERRACOTTA_TO_GLASS Map (16 пар) — мы работаем со слотами, цвета не нужны.
 *
 * Состояния:
 * 1. [State.REMEMBER] — сервер показывает последовательность.
 *    Накапливаем подсвеченные слоты в `pendingSlots`.
 * 2. [State.SHOW] — сервер ждёт клика. Подсвечиваем слоты текущего шага.
 * 3. [State.END] — все шаги пройдены.
 */
class ChronomatronSolver : ExperimentSolver(Regex("Ритмотрон|Chronomatron")) {

    /**
     * Последовательность шагов. Каждый шаг — список slot-индексов одного цвета.
     * Например: [[11,14,22,31], [12,23,33], [15,25]] — 3 шага по 4/3/2 слота.
     */
    private val chronoSequence: MutableList<List<Int>> = mutableListOf()

    /** Текущий шаг, на котором находится игрок (0-indexed). */
    private var currentStep = 0

    /** Слоты, подсвеченные в текущем шаге (накапливаются во время REMEMBER, сохраняя порядок). */
    private val pendingSlots: MutableList<Int> = mutableListOf()

    /** Индекс следующего клика внутри текущего шага */
    private var stepClickIndex = 0

    /** Текущая фаза по последнему обновлению слота 49 (инструкция). */
    @Volatile private var lastPhase: SlotPhaseDetector.Phase = SlotPhaseDetector.Phase.NONE

    override fun isEnabled(): Boolean {
        return StarredHeltix.feature.enchanting.tableSolvers.chronomatron
    }

    override fun start(screen: ContainerScreen) {
        super.start(screen)
        chronoSequence.clear()
        currentStep = 0
        stepClickIndex = 0
        pendingSlots.clear()
        lastPhase = SlotPhaseDetector.Phase.NONE
        StarredHeltix.LOGGER.info("[Chronomatron] Started, title=${screen.title.string}")
    }

    /**
     * tick() не используется — вся логика работает на событиях [onSlotUpdate].
     */
    override fun tick(screen: ContainerScreen) {}

    /**
     * Обрабатывает обновление одного слота.
     * Поставляется через [set.starlev.events.EventBus] (см. [set.starlev.events.SlotUpdateEvent]).
     *
     * Логика фазы:
     * — Сканируем ВСЕ слоты на предмет phase-индикатора (GLOWSTONE / CLOCK / текст).
     *   Это нужно потому, что Heltix может класть инструкцию в другой слот, не 49.
     * — Игровое поле (slots 9..44) обрабатываем только в фазе REMEMBER.
     */
    override fun onSlotUpdate(menu: AbstractContainerMenu, slotId: Int, stack: ItemStack) {
        // 1. Фазовая инструкция — только слот 49 (или нижний ряд 45-53).
        //    НЕ проверяем слоты игрового поля (9-44), чтобы случайно не
        //    сдетектить GLOWSTONE/CLOCK из сетки как фазу.
        if (slotId in 45..53) {
            val newPhase = SlotPhaseDetector.detect(stack)
            if (newPhase != SlotPhaseDetector.Phase.NONE) {
                if (newPhase != lastPhase) {
                    onPhaseChanged(newPhase)
                    lastPhase = newPhase
                }
                return
            }
        }

        // 2. Игровое поле Chronomatron (slots 9..44).
        if (slotId !in 9..44) return

        // В фазе REMEMBER записываем подсвеченные слоты.
        // Проверяем hasFoil() ИЛИ тип предмета (терракотта/стекло/шерсть).
        if (lastPhase == SlotPhaseDetector.Phase.REMEMBER && isMarkedChronomatronSlot(stack)) {
            pendingSlots.add(slotId)
        }
    }

    private fun onPhaseChanged(newPhase: SlotPhaseDetector.Phase) {
        when (newPhase) {
            SlotPhaseDetector.Phase.REMEMBER -> {
                // Новый раунд (или первый) — сбрасываем последовательность.
                chronoSequence.clear()
                currentStep = 0
                stepClickIndex = 0
                pendingSlots.clear()
                setState(State.REMEMBER)
                StarredHeltix.LOGGER.info("[Chronomatron] → REMEMBER")
            }
            SlotPhaseDetector.Phase.WAIT -> {
                // CLOCK / "Оставшееся" = конец показа. Закрываем текущий шаг (если есть открытые слоты)
                // и переходим к фазе клика.
                if (pendingSlots.isNotEmpty()) {
                    chronoSequence.add(pendingSlots.toList())
                    pendingSlots.clear()
                }
                if (chronoSequence.isNotEmpty()) {
                    setState(State.SHOW)
                    StarredHeltix.LOGGER.info(
                        "[Chronomatron] → SHOW. ${chronoSequence.size} шагов: " +
                            chronoSequence.joinToString(" → ") { "${it.size} сл." }
                    )
                } else {
                    StarredHeltix.LOGGER.debug(
                        "[Chronomatron] WAIT без последовательности — ничего не записали."
                    )
                }
            }
            SlotPhaseDetector.Phase.NONE -> {
                // Неизвестный предмет в инструкции. Не критично.
            }
        }
    }

    private fun isMarkedChronomatronSlot(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        if (stack.hasFoil()) return true
        val item = stack.item
        val name = item.toString().lowercase()
        return name.contains("terracotta") ||
               name.contains("stained_glass") ||
               name.contains("wool") ||
               name.contains("concrete_powder") ||
               name.contains("glazed_terracotta")
    }

    override fun getColors(displaySlots: Int2ObjectMap<ItemStack>): List<ColorHighlight> {
        if (getState() != State.SHOW) return emptyList()
        if (currentStep >= chronoSequence.size) return emptyList()
        val step = chronoSequence[currentStep]
        if (stepClickIndex >= step.size) return emptyList()
        return listOf(ColorHighlight.green(step[stepClickIndex]))
    }

    override fun getReplacementItems(displaySlots: Int2ObjectMap<ItemStack>): Int2ObjectMap<ItemStack> {
        val replacements = it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<ItemStack>()
        if (getState() != State.SHOW) return replacements

        for ((stepIndex, step) in chronoSequence.withIndex()) {
            for (slotIndex in step) {
                val display = displaySlots.get(slotIndex) ?: continue
                if (display.isEmpty) continue
                val copy = display.copy()
                val stepColor = when (stepIndex) {
                    currentStep -> "§a"
                    in 0 until currentStep -> "§7"
                    else -> "§8"
                }
                val newName = "${stepColor}[${stepIndex + 1}] §f${copy.hoverName.string}"
                copy.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(newName))
                replacements.put(slotIndex, copy)
            }
        }
        return replacements
    }

    override fun onClickSlot(slot: Int, stack: ItemStack, button: Int): Boolean {
        if (getState() != State.SHOW) return shouldBlockIncorrectClicks()
        if (currentStep >= chronoSequence.size) return false

        val step = chronoSequence[currentStep]
        if (stepClickIndex >= step.size) return false
        if (slot != step[stepClickIndex]) return shouldBlockIncorrectClicks()

        stepClickIndex++
        if (stepClickIndex >= step.size) {
            currentStep++
            stepClickIndex = 0
            if (currentStep >= chronoSequence.size) {
                setState(State.END)
                StarredHeltix.LOGGER.info("[Chronomatron] Все шаги пройдены → END.")
            }
        }
        return false
    }

    override fun reset() {
        chronoSequence.clear()
        currentStep = 0
        stepClickIndex = 0
        pendingSlots.clear()
        lastPhase = SlotPhaseDetector.Phase.NONE
        super.reset()
    }
}