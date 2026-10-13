package set.starlev.features.enchanting.solvers

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import set.starlev.StarredHeltix
import set.starlev.features.enchanting.ColorHighlight
import set.starlev.features.enchanting.ExperimentSolver

/**
 * Секвенсор (Heltix Sequencer) — решатель мини-игры «Секвенсор».
 *
 * Порт `heltix-superpairs/SequencerHandler`: порядок кликов задаётся не числами,
 * а ЦВЕТОМ блоков — по фиксированному спектру
 * white → light_blue → blue → purple → magenta → pink → red → orange → yellow → lime → green → cyan.
 *
 * Механика Heltix (в этом и суть солвера): красители МИГАЮТ в ячейках во время фазы
 * запоминания, а потом ячейки ЗАКРЫВАЮТСЯ панелями. Поэтому каждый кадр копим
 * ПАМЯТЬ slot → индекс спектра (union, ничего не забываем), а шаги строим из памяти
 * сортировкой по спектру. Без этого солвер «видит» только текущий миг — ячейки,
 * вспыхнувшие и закрывшиеся раньше, терялись.
 *
 * Каждый кадр в [tick]:
 *  - смена текста «Раунд N» → сброс;
 *  - все видимые сейчас цветные ячейки добавляются в память (isPane/isGlass/системные — пропуск);
 *  - шаги = память, отсортированная по спектру (дубли — по номеру слота);
 *  - фаза игры (gamePhase): все запомненные ячейки скрыты хотя бы раз (латч);
 *  - авто-продвижение: целевая ячейка снова открыла цвет (сервер принял клик);
 *    клик по цели тоже продвигает (как handleClick в эталоне).
 */
class UltrasequencerSolver private constructor() : ExperimentSolver(
    Regex("Секвенсор|Секуенсор|Ultrasequencer|Sequencer|Высший|Запредельный|Метафизический")
) {

    companion object {
        @JvmField
        val INSTANCE = UltrasequencerSolver()
    }

    /** slot -> индекс спектра. Память с начала раунда: вспышки закрываются, память остаётся. */
    private val colorMemory = HashMap<Int, Int>()

    /** slot -> шаг (1-базный). Порядок = сортировка памяти по индексу спектра. */
    private val sequenceSlots = HashMap<Int, Int>()
    private var currentStep = 1
    private var lastRoundText = ""

    /** Латч: все запомненные ячейки хотя бы раз скрыты → пошла фаза игры. */
    private var gamePhase = false

    /** Последний момент роста памяти — авто-продвижение разрешаем только на стабильной памяти. */
    private var lastMemoryChangeMs = 0L

    /** Троттлинг периодической сводки в лог. */
    private var lastSummaryMs = 0L

    @Volatile private var lastAdvanceMs = 0L

    override fun isEnabled(): Boolean = StarredHeltix.feature.enchanting.tableSolvers.ultrasequencer

    override fun start(screen: ContainerScreen) {
        super.start(screen)
        hardReset()
        setState(State.SHOW)
    }

    private fun hardReset() {
        colorMemory.clear()
        sequenceSlots.clear()
        currentStep = 1
        lastRoundText = ""
        gamePhase = false
        lastMemoryChangeMs = 0L
    }

    override fun tick(screen: ContainerScreen) {
        if (checkTitleChanged(screen)) {
            // МЯГКИЙ ресинк: память НЕ трогаем. Настоящая смена игры всегда идёт через
            // close/open контейнера → start() → hardReset(). А заголовок может дёргаться
            // (таймер/прогресс) внутри одной игры — сносить память по нему нельзя.
        }
        val slots = screen.menu.slots
        val containerSlots = slots.size - 36
        if (containerSlots <= 0) return
        val nowMs = System.currentTimeMillis()

        // 1. Новый раунд?
        for (i in 0 until slots.size) {
            val stack = slots[i].item
            if (stack.isEmpty || !ExperimentUtil.isRoundFlag(stack)) continue
            val hover = ExperimentUtil.hover(stack)
            if (hover != lastRoundText) {
                lastRoundText = hover
                colorMemory.clear()
                sequenceSlots.clear()
                currentStep = 1
                gamePhase = false
                setState(State.SHOW)
            }
            break
        }

        // 2. Память: UNION всех видимых сейчас цветных ячеек. Закрывшиеся не забываем!
        for (i in 0 until containerSlots) {
            val stack = slots[i].item
            if (stack.isEmpty) continue
            val hover = ExperimentUtil.hover(stack)
            if (ExperimentUtil.isPane(stack) || ExperimentUtil.isGlass(stack) ||
                ExperimentUtil.isSystemItem(hover)
            ) continue
            val ci = ExperimentUtil.spectrumIndex(stack)
            if (ci != -1 && colorMemory[i] != ci) {
                colorMemory[i] = ci
                lastMemoryChangeMs = nowMs
            }
        }

        // 3. Шаги = память, отсортированная по спектру (white→…→cyan), как COLOR_PATTERN в эталоне
        if (colorMemory.isNotEmpty()) {
            val sorted = colorMemory.entries.sortedWith(compareBy({ it.value }, { it.key }))
            sequenceSlots.clear()
            sorted.forEachIndexed { idx, e -> sequenceSlots[e.key] = idx + 1 }
            if (getState() != State.END) setState(State.SHOW)
        }

        // 4. Фаза игры: все запомненные ячейки скрыты (мигание закончилось)
        if (!gamePhase && colorMemory.isNotEmpty()) {
            gamePhase = colorMemory.keys.all { s -> s >= slots.size || isCellHidden(slots[s].item) }
            if (gamePhase) {
                val order = sequenceSlots.entries.sortedBy { it.value }
                    .joinToString { "${it.key}=${ExperimentUtil.spectrumName(colorMemory[it.key] ?: -1)}" }
            }
        }

        // 5. Авто-продвижение: в фазе игры целевая ячейка снова открыла цвет (сервер принял клик).
        //    ВАЖНО: проверяем ОТКРЫТИЕ, а не закрытие — в начале игры всё закрыто,
        //    проверка «закрылось» мгновенно «прошла» бы весь раунд.
        //    И только на стабильной памяти — вспышки во время запоминания шаги не трогают.
        if (gamePhase && sequenceSlots.isNotEmpty() && getState() != State.END &&
            currentStep <= sequenceSlots.size &&
            nowMs - lastMemoryChangeMs > 1000
        ) {
            val targetSlot = sequenceSlots.entries.firstOrNull { it.value == currentStep }?.key
            if (targetSlot != null && targetSlot < slots.size &&
                isCellOpen(slots[targetSlot].item) &&
                nowMs - lastAdvanceMs > 150
            ) {
                lastAdvanceMs = nowMs
                advanceStep()
            }
        }

        // 6. Периодическая сводка (раз в 5с) — для диагностики «почему пусто»
        if (nowMs - lastSummaryMs > 5000) {
            lastSummaryMs = nowMs
        }
    }

    private fun isCellHidden(stack: ItemStack): Boolean =
        stack.isEmpty || ExperimentUtil.isPane(stack) || ExperimentUtil.isGlass(stack) ||
            ExperimentUtil.isClosedCard(stack) || ExperimentUtil.spectrumIndex(stack) == -1

    private fun isCellOpen(stack: ItemStack): Boolean =
        !stack.isEmpty && !ExperimentUtil.isPane(stack) && !ExperimentUtil.isGlass(stack) &&
            !ExperimentUtil.isClosedCard(stack) && ExperimentUtil.spectrumIndex(stack) != -1

    private fun advanceStep() {
        currentStep++
        if (currentStep > sequenceSlots.size) {
            setState(State.END)
        }
    }

    override fun getColors(displaySlots: Int2ObjectMap<ItemStack>): List<ColorHighlight> {
        if (sequenceSlots.isEmpty() || getState() == State.END) return emptyList()
        val result = ArrayList<ColorHighlight>()
        for ((slot, step) in sequenceSlots) {
            when {
                step == currentStep -> result.add(ColorHighlight.green(slot))
                step > currentStep -> result.add(ColorHighlight(slot, 0x3300FF00)) // остальные — бледно-зелёные
            }
        }
        return result
    }

    override fun getDisplayStack(slotId: Int, original: ItemStack): ItemStack? = null

    override fun getReplacementItems(displaySlots: Int2ObjectMap<ItemStack>): Int2ObjectMap<ItemStack> =
        it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap()

    override fun onClickSlot(slot: Int, stack: ItemStack, button: Int): Boolean {
        if (sequenceSlots.isEmpty() || getState() == State.END) return false
        val expected = sequenceSlots[slot]
        return if (expected != null && expected == currentStep) {
            // Правильный клик — продвигаем сразу, а не ждём подтверждение сервера
            // (как handleClick в эталоне). Дабл-прогресса нет: авто-продвижение в tick
            // смотрит на СЛЕДУЮЩУЮ цель, а она пока закрыта.
            lastAdvanceMs = System.currentTimeMillis()
            advanceStep()
            false
        } else {
            StarredHeltix.feature.enchanting.tableSolvers.blockIncorrectClicks
        }
    }

    override fun onSlotUpdate(menu: AbstractContainerMenu, slotId: Int, stack: ItemStack) {
        // Вся логика в tick(screen).
    }

    override fun reset() {
        hardReset()
        super.reset()
    }
}
