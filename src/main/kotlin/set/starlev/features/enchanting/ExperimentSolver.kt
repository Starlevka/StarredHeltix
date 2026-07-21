package set.starlev.features.enchanting

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack

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

    /**
     * Проверяет, подходит ли данный экран для этого солвера.
     */
    fun matches(screen: ContainerScreen): Boolean {
        val title = screen.title.string
        // Ищем паттерн как подстроку, так как title содержит юникод символы форматирования
        return titlePattern.find(title) != null
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
        this.slots.clear()
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
     * Проверяет, нужно ли блокировать неправильные клики.
     */
    protected fun shouldBlockIncorrectClicks(): Boolean {
        return set.starlev.StarredHeltix.feature.enchanting.tableSolvers.blockIncorrectClicks
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
