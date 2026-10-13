package set.starlev.features.enchanting.solvers

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import set.starlev.StarredHeltix
import set.starlev.features.enchanting.ColorHighlight
import set.starlev.features.enchanting.ExperimentSolver

/**
 * Ритмотрон (Rhythmatron) — решатель мини-игры Heltix «Ритмотрон».
 *
 * Порт логики `heltix-superpairs/RhythmatronHandler` (scripts/SOURCES/SOLVERS EXPERIMENTAL):
 * на Heltix это НЕ Hypixel-хрономатрон (позиции), а ЦВЕТОВАЯ последовательность на
 * фиксированном стеклянном поле:
 *  - поле — «стеклянные» слоты, каждый имеет свой цвет (boardColors);
 *  - фаза «запомните» (GLOWSTONE/«запомн») — по полю бегут вспышки из керамики/терракоты/бетона,
 *    их цвета по порядку = искомая последовательность (sequenceColors);
 *  - фаза «повторите» (CLOCK/«часы») — подсветка всех ячеек текущего шага-цвета, клик
 *    по любой из них переводит к следующему шагу.
 *
 * Шаг запоминается по ФРОНТУ «ячейка загорелась», а не по смене цвета: сервер вполне
 * может повторить один и тот же шаг два раза подряд (2-3-3), и сравнение с предыдущим
 * цветом схлопывало такой повтор в один шаг.
 *
 * Сканирование меню — каждый кадр в [tick] (как в эталоне), а не по событиям слотов:
 * Heltix шлёт вспышки быстро и событийно их легко пропустить.
 */
class ChronomatronSolver : ExperimentSolver(Regex("Ритмотрон|Rhythmatron|Chronomatron|Хронометрон")) {

    /** slot -> цвет стеклянной ячейки поля (фиксированная доска). */
    private val boardColors = HashMap<Int, String>()

    /** Запомненная последовательность цветов. */
    private val sequenceColors = ArrayList<String>()

    /** Текущий шаг (1-базный). */
    @Volatile private var currentStep: Int = 1

    /** true = фаза «повторите» (ждём кликов); false = фаза «запомните»/NONE. */
    @Volatile private var isGamePhase: Boolean = false

    private var lastRoundText: String = ""

    /**
     * Слоты, которые светились на прошлом кадре — фронт детекции вспышки.
     *
     * Раньше шаг запоминался только если цвет отличался от предыдущего, и повтор подряд
     * (сервер: 2-3-3) схлопывался в один шаг. Теперь шаг — это ФРОНТ «ячейка загорелась»:
     * между двумя одинаковыми вспышками ячейка успевает вернуться в стекло, и второй
     * такой же цвет снова даёт фронт.
     */
    private val flashingSlots = HashSet<Int>()

    override fun isEnabled(): Boolean = StarredHeltix.feature.enchanting.tableSolvers.chronomatron

    override fun start(screen: ContainerScreen) {
        super.start(screen)
        hardReset()
    }

    private fun hardReset() {
        boardColors.clear()
        sequenceColors.clear()
        currentStep = 1
        isGamePhase = false
        lastRoundText = ""
        flashingSlots.clear()
    }

    override fun tick(screen: ContainerScreen) {
        // Смена заголовка (переход на следующий уровень) — полный сброс, как resetAllHandlers в эталоне
        if (checkTitleChanged(screen)) hardReset()
        val slots = screen.menu.slots
        val containerSlots = slots.size - 36
        if (containerSlots <= 0) return

        var foundGlowstone = false
        var foundClock = false

        // 1. Раунд-текст, индикаторы фазы
        for (i in 0 until slots.size) {
            val stack = slots[i].item
            if (stack.isEmpty) continue
            val hover = ExperimentUtil.hover(stack)
            if (ExperimentUtil.isRoundFlag(stack) && hover != lastRoundText) {
                // Новый раунд/уровень — полный сброс доски и последовательности
                lastRoundText = hover
                boardColors.clear()
                sequenceColors.clear()
                currentStep = 1
                isGamePhase = false
                flashingSlots.clear()
            }
            if (ExperimentUtil.isRememberFlag(stack)) foundGlowstone = true
            if (ExperimentUtil.isWaitFlag(stack)) foundClock = true
        }

        if (foundGlowstone) {
            if (isGamePhase) { isGamePhase = false; currentStep = 1; sequenceColors.clear() }
            flashingSlots.clear()
        } else if (foundClock) {
            isGamePhase = true
        }

        // 2. Доска: стеклянные ячейки и их цвета (постоянно обновляем)
        for (i in 0 until containerSlots) {
            val stack = slots[i].item
            if (stack.isEmpty) continue
            if (!ExperimentUtil.isGlass(stack)) continue
            ExperimentUtil.parseColor(stack)?.let { boardColors[i] = it }
        }

        // 3. Фаза «запомните»: ловим вспышки (не-стеклянные цветные блоки) по фронту «загорелась».
        // Порядок обхода — по индексу слота, он же порядок вспышек на сервере.
        if (!isGamePhase) {
            val lit = HashSet<Int>(containerSlots)
            for (i in 0 until containerSlots) {
                val stack = slots[i].item
                if (stack.isEmpty) continue
                val hover = ExperimentUtil.hover(stack)
                val isGlassOrSystem = ExperimentUtil.isGlass(stack) || ExperimentUtil.isPane(stack) ||
                    ExperimentUtil.isSystemItem(hover)
                if (isGlassOrSystem) continue
                if (!ExperimentUtil.isCeramic(stack)) continue
                // Ячейка горит — с этого момента она «светится», даже если цвет не разобрали:
                // иначе фронт мигнёт и шаг продублируется на следующем кадре.
                lit.add(i)
                val color = ExperimentUtil.parseColor(stack) ?: boardColors[i] ?: continue
                // Вспышка = ячейка только что загорелась. Повтор того же цвета подряд
                // (2-3-3) — новая вспышка, потому что между ними ячейка гасла.
                if (flashingSlots.contains(i)) continue
                sequenceColors.add(color)
                flashingSlots.add(i)
                setState(State.REMEMBER)
            }
            flashingSlots.retainAll(lit)
        }

        // 4. Авто-продвижение: если ожидаемую ячейку уже «закрыли» (перестала быть стеклом)
        if (isGamePhase && sequenceColors.isNotEmpty()) {
            val target = currentStep - 1
            if (target < sequenceColors.size) {
                val expected = sequenceColors[target]
                for ((slotIdx, color) in boardColors) {
                    if (color != expected) continue
                    if (slotIdx >= slots.size) continue
                    val stack = slots[slotIdx].item
                    if (stack.isEmpty) continue
                    val hover = ExperimentUtil.hover(stack)
                    val stillGlass = ExperimentUtil.isGlass(stack) || hover.contains("закрыто")
                    if (!stillGlass) {
                        advanceStep()
                        break
                    }
                }
            }
            if (currentStep > sequenceColors.size) setState(State.END)
            else setState(State.SHOW)
        }
    }

    private fun advanceStep() {
        if (currentStep <= sequenceColors.size) currentStep++
    }

    override fun getColors(displaySlots: Int2ObjectMap<ItemStack>): List<ColorHighlight> {
        if (!isGamePhase || sequenceColors.isEmpty()) return emptyList()
        val target = currentStep - 1
        if (target >= sequenceColors.size) return emptyList()
        val expected = sequenceColors[target]
        val result = ArrayList<ColorHighlight>()
        for ((slotIdx, color) in boardColors) {
            if (color == expected) result.add(ColorHighlight.green(slotIdx))
        }
        return result
    }

    override fun getDisplayStack(slotId: Int, original: ItemStack): ItemStack? = null

    override fun getReplacementItems(displaySlots: Int2ObjectMap<ItemStack>): Int2ObjectMap<ItemStack> =
        it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap()

    override fun onClickSlot(slot: Int, stack: ItemStack, button: Int): Boolean {
        if (!isGamePhase || sequenceColors.isEmpty()) return false
        val target = currentStep - 1
        if (target >= sequenceColors.size) return false
        val expected = sequenceColors[target]
        val clickedColor = boardColors[slot]
        return if (clickedColor == expected) {
            advanceStep()
            false // клик правильный — пропускаем, сервер сам закроет ячейку
        } else {
            StarredHeltix.feature.enchanting.tableSolvers.blockIncorrectClicks
        }
    }

    override fun onSlotUpdate(menu: AbstractContainerMenu, slotId: Int, stack: ItemStack) {
        // Вся логика в tick(screen) — событийная модель тут ненадёжна (Heltix шлёт быстро).
    }

    override fun reset() {
        hardReset()
        super.reset()
    }
}
