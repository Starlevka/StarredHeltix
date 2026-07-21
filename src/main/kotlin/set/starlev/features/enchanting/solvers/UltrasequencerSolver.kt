package set.starlev.features.enchanting.solvers

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.DyeItem
import net.minecraft.world.item.Item
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import set.starlev.StarredHeltix
import set.starlev.features.enchanting.ColorHighlight
import set.starlev.features.enchanting.ExperimentSolver
import set.starlev.utils.detectors.SlotPhaseDetector

/**
 * Ultrasequencer (Секвенсор) — решатель мини-игры «Ультрасеквенсор».
 *
 * Архитектура:
 * — Event-driven: получает обновления слотов через [onSlotUpdate] (от EventBus).
 * — Не подписывается на [net.minecraft.world.inventory.ContainerListener] напрямую —
 *   это делает [set.starlev.events.ContainerEventWire] и пробрасывает через
 *   [set.starlev.features.enchanting.ExperimentSolverManager].
 * — Детект фазы по Item enum + тексту displayName (см. [SlotPhaseDetector]) —
 *   работает и для Hypixel-стиля (GLOWSTONE/CLOCK), и для Heltix (BOOK с кастомным именем).
 * — Детект игровых слотов через [DyeItem] и особые предметы (INK_SAC и пр.).
 * — Сортировка по `stack.count` — первым идёт «1», потом «2» и т.д.
 *
 * Состояния:
 * 1. [State.REMEMBER] — сервер показывает последовательность.
 *    Накапливаем dye items, сортируем по возрастанию числа.
 * 2. [State.SHOW] — сервер ждёт клика. Подсвечиваем слот с наименьшим числом.
 * 3. [State.END] — все клики сделаны.
 */
class UltrasequencerSolver private constructor() : ExperimentSolver(Regex("Секвенсор|Ultrasequencer")) {

    companion object {
        @JvmField
        val INSTANCE = UltrasequencerSolver()

        /**
         * Особые предметы, не наследники [DyeItem], но используемые на панели Ultrasequencer.
         * В Minecraft 1.21 [DyeItem] покрывает все `*_DYE`, но INK_SAC, BONE_MEAL и пр. — отдельные Item.
         * Подход NoFrills: явно перечисляем такие случаи.
         */
        private val DYE_LIKE_ITEMS: Set<Item> = setOf(
            Items.INK_SAC,
            Items.BONE_MEAL,
            Items.LAPIS_LAZULI,
            Items.COCOA_BEANS,
        )
    }

    /**
     * Очередь ожидаемых кликов в правильном порядке.
     * Первый элемент — следующий слот для клика. Сортируется по возрастанию числа.
     */
    private val expectedQueue: ArrayDeque<Pair<Int, ItemStack>> = ArrayDeque()

    /** Текущая фаза по последнему обновлению слота 49 (инструкция). */
    @Volatile private var lastPhase: SlotPhaseDetector.Phase = SlotPhaseDetector.Phase.NONE

    /** Ссылка на активное меню (нужна для [rebuildQueue] вне start()). */
    @Volatile private var activeMenu: AbstractContainerMenu? = null

    override fun isEnabled(): Boolean {
        return StarredHeltix.feature.enchanting.tableSolvers.ultrasequencer
    }

    override fun start(screen: ContainerScreen) {
        super.start(screen)
        expectedQueue.clear()
        lastPhase = SlotPhaseDetector.Phase.NONE
        activeMenu = screen.menu
        StarredHeltix.LOGGER.info("[Ultrasequencer] Started, title=${screen.title.string}")
    }

    /**
     * tick() не используется — вся логика работает на событиях [onSlotUpdate].
     * Оставлен пустым для совместимости с [set.starlev.features.enchanting.ExperimentSolverManager].
     */
    override fun tick(screen: ContainerScreen) {}

    /**
     * Обрабатывает обновление одного слота.
     * Поставляется через [set.starlev.events.EventBus] (см. [set.starlev.events.SlotUpdateEvent]).
     *
     * Оптимизация: `rebuildQueue` НЕ вызывается здесь, чтобы не тратить CPU
     * на каждом из 36 слотов. Вместо этого очередь собирается в [onPhaseChanged]
     * при переходе REMEMBER → WAIT (к этому моменту все dye items уже в меню).
     */
    override fun onSlotUpdate(menu: AbstractContainerMenu, slotId: Int, stack: ItemStack) {
        activeMenu = menu

        // 1. Проверяем слот на phase-индикатор (независимо от slotId —
        // Heltix может класть инструкцию в другой слот).
        val newPhase = SlotPhaseDetector.detect(stack)
        if (newPhase != SlotPhaseDetector.Phase.NONE) {
            if (newPhase != lastPhase) {
                onPhaseChanged(newPhase, menu, stack)
                lastPhase = newPhase
            }
            return
        }

        // 2. Игровое поле Ultrasequencer (slots 9..44).
        // В фазе REMEMBER ничего не делаем — очередь будет построена при переходе в WAIT.
        if (slotId !in 9..44) return
    }

    private fun onPhaseChanged(
        newPhase: SlotPhaseDetector.Phase,
        menu: AbstractContainerMenu,
        instructionStack: ItemStack,
    ) {
        when (newPhase) {
            SlotPhaseDetector.Phase.REMEMBER -> {
                // Новый раунд. Очередь пока не строим — соберём при переходе в WAIT,
                // когда все dye items гарантированно пришли в меню.
                expectedQueue.clear()
                setState(State.REMEMBER)
                StarredHeltix.LOGGER.info("[Ultrasequencer] → REMEMBER")
            }
            SlotPhaseDetector.Phase.WAIT -> {
                // CLOCK = сервер ждёт клика.
                // Откладываем rebuildQueue на следующий тик — при ContentPacket
                // слот инструкции приходит раньше слотов с dye items, и меню
                // ещё не заполнено на момент вызова onPhaseChanged.
                Minecraft.getInstance().execute {
                    rebuildQueue(menu)
                    if (expectedQueue.isNotEmpty()) {
                        setState(State.SHOW)
                        StarredHeltix.LOGGER.info(
                            "[Ultrasequencer] → SHOW. ${expectedQueue.size} dye items в очереди. " +
                                "Следующий слот: ${expectedQueue.first().first}."
                        )
                    } else {
                        StarredHeltix.LOGGER.warn(
                            "[Ultrasequencer] WAIT, но dye items не найдены. " +
                                "Возможно, Heltix использует неожиданные предметы."
                        )
                    }
                }
            }
            SlotPhaseDetector.Phase.NONE -> {
                // Неизвестный предмет в инструкции. Не критично, просто ждём.
                if (!instructionStack.isEmpty) {
                    StarredHeltix.LOGGER.debug(
                        "[Ultrasequencer] Неизвестный предмет в инструкции: ${instructionStack.item}"
                    )
                }
            }
        }
    }

    /**
     * Собирает все dye items из слотов 9..44 и сортирует по возрастанию числа.
     *
     * Критерии сортировки:
     * 1. `stack.count` — основной (как у NoFrills: `Comparator.comparingInt(s -> s.stack.getCount())`).
     * 2. Число из `hoverName` — запасной, если count не совпадает с номером (на случай странностей Heltix).
     */
    private fun rebuildQueue(menu: AbstractContainerMenu) {
        expectedQueue.clear()
        val slots = mutableListOf<Pair<Int, ItemStack>>()
        for (index in 9..44) {
            val stack = menu.getSlot(index).item
            if (isDye(stack)) {
                slots.add(Pair(index, stack))
            }
        }
        slots.sortWith(
            compareBy<Pair<Int, ItemStack>> { it.second.count }
                .thenBy { it.second.hoverName.string.toIntOrNull() ?: Int.MAX_VALUE }
        )
        expectedQueue.addAll(slots)
    }

    /**
     * Проверяет, является ли предмет ячейкой Ultrasequencer.
     * — Наследник [DyeItem] (RED_DYE, BLUE_DYE и т.д.).
     * — Или один из специальных предметов ([DYE_LIKE_ITEMS]).
     */
    private fun isDye(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val item = stack.item
        return item is DyeItem || item in DYE_LIKE_ITEMS
    }

    override fun getColors(displaySlots: Int2ObjectMap<ItemStack>): List<ColorHighlight> {
        if (getState() != State.SHOW || expectedQueue.isEmpty()) return emptyList()
        val next = expectedQueue.first()
        return listOf(ColorHighlight.green(next.first))
    }

    override fun getReplacementItems(displaySlots: Int2ObjectMap<ItemStack>): Int2ObjectMap<ItemStack> {
        val replacements = it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<ItemStack>()
        if (expectedQueue.isEmpty()) return replacements

        for ((order, pair) in expectedQueue.withIndex()) {
            val (slotIndex, stack) = pair
            val display = displaySlots.get(slotIndex) ?: continue
            if (display.isEmpty) continue
            if (!isDye(display)) continue
            val copy = display.copy()
            val isNext = (order == 0)
            val color = if (isNext) "§a§l" else "§7"
            val newName = "${color}${order + 1}. §f${copy.hoverName.string}"
            copy.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(newName))
            replacements.put(slotIndex, copy)
        }
        return replacements
    }

    override fun onClickSlot(slot: Int, stack: ItemStack, button: Int): Boolean {
        if (getState() != State.SHOW) return shouldBlockIncorrectClicks()
        if (expectedQueue.isEmpty()) return false

        val next = expectedQueue.first()
        if (slot != next.first) return shouldBlockIncorrectClicks()

        expectedQueue.removeFirst()
        if (expectedQueue.isEmpty()) {
            setState(State.END)
            StarredHeltix.LOGGER.info("[Ultrasequencer] Все клики сделаны → END.")
        }
        return false
    }

    override fun reset() {
        expectedQueue.clear()
        lastPhase = SlotPhaseDetector.Phase.NONE
        activeMenu = null
        super.reset()
    }
}