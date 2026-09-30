package set.starlev.features.enchanting

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import set.starlev.utils.detectors.SlotPhaseDetector

/**
 * Базовый класс для всех решателей экспериментов стола чародейства.
 * Использует конечный автомат (state machine) для управления состоянием.
 */
abstract class ExperimentSolver(protected val titlePattern: Regex) {
    
    enum class State {
        /** Состояние запоминания паттерна */
        REMEMBER,
        /** Состояние ожидания начала показа */
        WAIT,
        /** Состояние показа решения игроку */
        SHOW,
        /** Состояние завершения раунда */
        END
    }

    protected var stateField: State = State.REMEMBER
    protected val slots: Int2ObjectMap<ItemStack> = Int2ObjectOpenHashMap()
    
    /** Текущий экран контейнера */
    var currentScreen: ContainerScreen? = null
        protected set

    /** Последний виденный сырой заголовок (для детекта смены уровня внутри открытого стола). */
    protected var lastTitleRaw: String = ""

    /**
     * Проверяет, подходит ли данный экран для этого солвера.
     * Заголовок Heltix может быть в Hfont (PUA-глифы) — сначала декодируем.
     */
    fun matches(screen: ContainerScreen): Boolean {
        val raw = screen.title.string
        val title = try {
            set.starlev.utils.detectors.ContainerDetector.decodeTitle(raw)
        } catch (_: Exception) {
            raw
        }
        // Ищем паттерн как подстроку, так как title может содержать символы форматирования
        return titlePattern.containsMatchIn(title)
    }

    /**
     * Проверяет, включен ли солвер в конфиге.
     */
    abstract fun isEnabled(): Boolean

    /**
     * Вызывается при открытии подходящего контейнера.
     */
    open fun start(screen: ContainerScreen) {
        this.currentScreen = screen
        this.stateField = State.REMEMBER
        this.lastTitleRaw = screen.title.string
        this.slots.clear()
    }

    /**
     * Детект смены заголовка внутри одного открытого стола (порт HeltixSuperpairs:
     * при `title != activeTitle` вызывается resetAllHandlers). Прогресс уровня меняет
     * заголовок окна, не закрывая контейнер — в этот момент солвер должен забыть прошлый раунд.
     * @return true, если заголовок изменился с прошлого кадра (состояние уже не актуально).
     */
    protected fun checkTitleChanged(screen: ContainerScreen): Boolean {
        val raw = screen.title.string
        if (raw == lastTitleRaw) return false
        lastTitleRaw = raw
        return true
    }

    /**
     * Вызывается при закрытии контейнера или сбросе.
     */
    open fun reset() {
        this.stateField = State.REMEMBER
        this.slots.clear()
        this.currentScreen = null
    }

    /**
     * Вызывается каждый тик для обновления состояния.
     */
    abstract fun tick(screen: ContainerScreen)

    /**
     * Возвращает список подсветок для слотов.
     */
    abstract fun getColors(displaySlots: Int2ObjectMap<ItemStack>): List<ColorHighlight>

    /**
     * Возвращает карту предметов для визуальной замены в контейнере.
     */
    open fun getReplacementItems(displaySlots: Int2ObjectMap<ItemStack>): Int2ObjectMap<ItemStack> {
        return Int2ObjectOpenHashMap()
    }

    /**
     * Возвращает заменённый стек для рендера конкретного слота (вызывается из миксина рендера).
     * По умолчанию — без замены. Солверы могут переопределить для отображения запомненных предметов
     * (Superpairs) или нумерации шагов (Chronomatron/Ultrasequencer).
     * @return null если замены нет, иначе стек для рендера/тултипа
     */
    open fun getDisplayStack(slotId: Int, original: ItemStack): ItemStack? = null

    /**
     * Вызывается при клике по слоту.
     * @return true если клик должен быть отменен
     */
    open fun onClickSlot(slot: Int, stack: ItemStack, button: Int): Boolean {
        return false
    }

    /**
     * Вызывается при обновлении содержимого слота в открытом контейнере.
     *
     * Слоты вне диапазона 9..44 и инструкционный слот 49 — на совести солвера
     * (фильтровать или обрабатывать по необходимости).
     *
     * Поставляется через [set.starlev.events.EventBus] (см. [set.starlev.events.ContainerEventWire]),
     * не через ванильный [net.minecraft.world.inventory.ContainerListener].
     *
     * @param menu контейнер, в котором обновился слот
     * @param slotId индекс слота
     * @param stack новый стек в слоте (ItemStack.EMPTY если пусто)
     */
    open fun onSlotUpdate(menu: AbstractContainerMenu, slotId: Int, stack: ItemStack) {}

    /**
     * Вызывается при обновлении содержимого слотов.
     */
    open fun markDirty() {}

    /**
     * Активен ли сейчас «таймер» — индикатор фазы WAIT (сервер ждёт клика игрока).
     * Ищем любой слот меню, который SlotPhaseDetector определяет как WAIT.
     * В фазе REMEMBER («запомните узор», GLOWSTONE) таймера нет → false,
     * поэтому блокировка неправильных кликов не должна срабатывать.
     */
    protected fun isTimerActive(): Boolean {
        val menu = currentScreen?.menu ?: return false
        return menu.slots.any { SlotPhaseDetector.detect(it.item) == SlotPhaseDetector.Phase.WAIT }
    }

    /**
     * Нужно ли блокировать неправильные клики прямо сейчас:
     * включённая настройка + активный таймер (фаза WAIT) в открытом меню.
     */
    protected fun shouldBlockIncorrectClicks(): Boolean {
        return set.starlev.StarredHeltix.feature.enchanting.tableSolvers.blockIncorrectClicks && isTimerActive()
    }

    /**
     * Получает текущее состояние.
     */
    fun getState(): State = stateField

    /**
     * Устанавливает состояние.
     */
    protected fun setState(newState: State) {
        this.stateField = newState
    }
}
