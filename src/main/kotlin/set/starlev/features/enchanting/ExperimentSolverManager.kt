package set.starlev.features.enchanting

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import org.slf4j.LoggerFactory
import set.starlev.StarredHeltix
import set.starlev.events.ContainerEventWire
import set.starlev.events.EventBus
import set.starlev.events.GuiEvents
import set.starlev.events.SlotUpdateEvent
import set.starlev.features.Category
import set.starlev.features.Feature
import set.starlev.injections.accessors.ContainerScreenAccessor
import set.starlev.features.enchanting.solvers.ChronomatronSolver
import set.starlev.features.enchanting.solvers.SuperpairsSolver
import set.starlev.features.enchanting.solvers.UltrasequencerSolver
import set.starlev.utils.detectors.LocationDetector

/**
 * Менеджер для управления решателями стола экспериментов.
 * Обрабатывает открытие контейнеров, рендеринг подсветок и клики.
 */
object ExperimentSolverManager : Feature(
    name = "Experiment Solver",
    category = Category.ENCHANTING,
    description = "Солвер экспериментов"
) {
    private val LOGGER = LoggerFactory.getLogger(ExperimentSolverManager::class.java)
    
    private val superpairs = SuperpairsSolver()
    private val chronomatron = ChronomatronSolver()
    private val ultrasequencer = UltrasequencerSolver.INSTANCE

    /** Текущий активный решатель */
    private var currentSolver: ExperimentSolver? = null
    
    /** Кэшированные подсветки */
    private var highlights: List<ColorHighlight>? = null
    
    /** ID экрана для отслеживания */
    private var screenId: Int = 0

    /** Счетчик тиков для проверки состояния */
    private var lastTickTime: Long = 0

    /**
     * Инициализирует менеджер и регистрирует обработчики событий.
     */
    override fun init() {
        // Подписка на SlotUpdateEvent (event-driven slot updates через EventBus).
        // Заменяет ContainerListener из солверов — теперь обновления приходят
        // через Fabric packet events → ContainerEventWire → EventBus → сюда.
        EventBus.subscribe<SlotUpdateEvent>(priority = -50) { event ->
            val solver = currentSolver ?: return@subscribe
            solver.onSlotUpdate(event.menu, event.slotId, event.stack)
        }

        // Регистрируем обработчик открытия контейнера
        GuiEvents.registerOpen { screen ->
            if (screen is ContainerScreen) {
                onScreenOpened(screen)
            }
        }
        
        // Регистрируем обработчик закрытия контейнера
        GuiEvents.registerClose { screen ->
            if (currentSolver != null) {
                LOGGER.info("[ExperimentSolverManager] Closing solver: ${currentSolver?.javaClass?.simpleName}")
                clearScreen()
            }
        }
        
        // Регистрируем обработчик рендера
        GuiEvents.registerForeground { graphics, mouseX, mouseY, screen ->
            val solver = currentSolver
            if (solver != null) {
                val containerScreen = screen as? ContainerScreen
                if (containerScreen != null) {
                    solver.tick(containerScreen)
                }
                onDraw(graphics, mouseX, mouseY, screen)
            }
        }
        
        // Регистрируем обработчик кликов
        GuiEvents.registerClick { mouseX, mouseY, button, screen ->
            val solver = currentSolver
            if (screen is ContainerScreen && solver != null) {
                val slot = getSlotAt(screen, mouseX.toInt(), mouseY.toInt())
                if (slot != null) {
                    val blocked = solver.onClickSlot(slot.index, slot.item, button)
                    markHighlightsDirty()
                    return@registerClick blocked
                }
            }
            false
        }
    }
    
    /**
     * Вызывается при открытии экрана контейнера.
     */
    private fun onScreenOpened(screen: ContainerScreen) {
        val title = screen.title.string
        
        val newSolver = when {
            superpairs.matches(screen) && superpairs.isEnabled() -> superpairs
            chronomatron.matches(screen) && chronomatron.isEnabled() -> chronomatron
            ultrasequencer.matches(screen) && ultrasequencer.isEnabled() -> ultrasequencer
            else -> null
        }

        if (newSolver != null) {
            clearScreen()
            currentSolver = newSolver
            LOGGER.info("[ExperimentSolverManager] Started solver: ${newSolver.javaClass.simpleName} for screen: $title")
            newSolver.start(screen)
            // Подключаем форвардер для публикации SlotUpdateEvent в EventBus.
            // После этого обновления слотов приходят в солвер через onSlotUpdate().
            ContainerEventWire.attach(screen.menu)
            markHighlightsDirty()
        }
    }
    
    /**
     * Очищает текущий экран и сбрасывает решатель.
     */
    private fun clearScreen() {
        // Отключаем форвардер, если он был подключён.
        currentSolver?.currentScreen?.menu?.let { ContainerEventWire.detach(it) }
        currentSolver?.reset()
        currentSolver = null
        highlights = null
    }
    
    /**
     * Возвращает карту предметов для замены от активного решателя.
     */
    fun getReplacementItems(displaySlots: Int2ObjectMap<ItemStack>): Int2ObjectMap<ItemStack> {
        return currentSolver?.getReplacementItems(displaySlots) ?: it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap()
    }

    /**
     * Применяет замены предметов к предоставленной карте.
     */
    fun replaceItems(originalMap: Int2ObjectMap<ItemStack>) {
        val replacements = getReplacementItems(originalMap)
        if (replacements.isEmpty()) return
        for (entry in replacements.int2ObjectEntrySet()) {
            originalMap.put(entry.intKey, entry.value)
        }
    }

    /**
     * Помечает подсветки как устаревшие.
     */
    fun markHighlightsDirty() {
        highlights = null
    }
    
    /**
     * Обрабатывает клик по слоту.
     * @return true если клик должен быть отменен
     */
    private fun onSlotClick(slot: Int, stack: ItemStack, button: Int): Boolean {
        val result = currentSolver?.onClickSlot(slot, stack, button) ?: false
        markHighlightsDirty()
        return result
    }
    
    /**
     * Получает слот по координатам мыши (абсолютные координаты экрана).
     * Корректирует позицию слота вычитанием leftPos/topPos.
     */
    private fun getSlotAt(screen: ContainerScreen, mouseX: Int, mouseY: Int): Slot? {
        val accessor = screen as ContainerScreenAccessor
        val guiLeft = accessor.leftPos
        val guiTop = accessor.topPos
        for (slot in screen.menu.slots) {
            val slotScreenX = guiLeft + slot.x
            val slotScreenY = guiTop + slot.y
            if (mouseX >= slotScreenX && mouseX < slotScreenX + 16 && 
                mouseY >= slotScreenY && mouseY < slotScreenY + 16) {
                return slot
            }
        }
        return null
    }
    
    /**
     * Рендерит подсветки слотов.
     */
    private fun onDraw(graphics: GuiGraphics, mouseX: Int, mouseY: Int, screen: AbstractContainerScreen<*>) {
        val containerScreen = screen as? ContainerScreen ?: return
        val solver = currentSolver ?: return
        
        val rows = containerScreen.menu.rowCount
        val containerSlots = containerScreen.menu.slots.subList(0, rows * 9)
        
        val now = System.currentTimeMillis()
        if (now - lastTickTime > 50) {
            val slotMap = createSlotMap(containerSlots)
            highlights = solver.getColors(slotMap)
            lastTickTime = now
        }
        
        highlights?.forEach { highlight ->
            if (highlight.slot < containerSlots.size) {
                val slot = containerSlots[highlight.slot]
                val accessor = containerScreen as ContainerScreenAccessor
                
                // В AbstractContainerScreenMixin мы вызываем fireForeground в конце render.
                // В ваниле к этому моменту матрица НЕ сдвинута на leftPos/topPos.
                // Значит нам НУЖНО добавлять guiLeft/guiTop.
                // Если рендерит "где попало", проверим правильность leftPos.
                val screenX = accessor.leftPos + slot.x
                val screenY = accessor.topPos + slot.y
                
                graphics.fill(screenX, screenY, screenX + 16, screenY + 16, highlight.color)
            }
        }
    }
    
    /**
     * Создает карту слотов для передачи решателю.
     */
    fun createSlotMap(slots: List<Slot>): Int2ObjectMap<ItemStack> {
        val map = Int2ObjectRBTreeMap<ItemStack>()
        for (i in slots.indices) {
            map.put(i, slots[i].item)
        }
        return map
    }
    
    /**
     * Тикер для вызова tick() у активного решателя.
     * Должен вызываться каждый кадр.
     */
    fun tick() {
        currentSolver?.let { solver ->
            val mc = net.minecraft.client.Minecraft.getInstance()
            val screen = mc.screen
            if (screen is ContainerScreen && mc.player != null) {
                solver.tick(screen)
            }
        }
    }
}
