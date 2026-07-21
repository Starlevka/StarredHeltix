package set.starlev.features.enchanting.solvers

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.IntArraySet
import it.unimi.dsi.fastutil.ints.IntSet
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import set.starlev.StarredHeltix
import set.starlev.features.enchanting.ColorHighlight
import set.starlev.features.enchanting.ExperimentSolver

/**
 * Решатель для мини-игры Суперпары (Superpairs).
 * Запоминает уже открытые предметы и показывает дубликаты и совпадения.
 */
class SuperpairsSolver : ExperimentSolver(Regex("Суперпары")) {

    private var prevClickedSlot: Int = 0
    private var currentSlotItem: ItemStack = ItemStack.EMPTY
    private val duplicatedSlots: IntSet = IntArraySet(28)

    override fun isEnabled(): Boolean {
        return StarredHeltix.feature.enchanting.tableSolvers.superpairs
    }

    override fun start(screen: ContainerScreen) {
        super.start(screen)
        setState(State.SHOW)
    }

    override fun tick(screen: ContainerScreen) {
        if (getState() == State.SHOW) {
            val menu = screen.menu
            val itemStack = menu.getSlot(prevClickedSlot).item

            if (!itemStack.isEmpty) {
                if (itemStack.`is`(Items.CYAN_STAINED_GLASS) || itemStack.`is`(Items.BLACK_STAINED_GLASS_PANE)) return

                for (entry in slots.int2ObjectEntrySet()) {
                    val entryItem = entry.value
                    if (ItemStack.matches(entryItem, itemStack)) {
                        duplicatedSlots.add(entry.intKey)
                        duplicatedSlots.add(prevClickedSlot)
                        currentSlotItem = itemStack
                        return
                    }
                }

                if (slots.get(prevClickedSlot) == null) {
                    slots.put(prevClickedSlot, itemStack)
                }

                currentSlotItem = itemStack
            }
        }
    }

    override fun getReplacementItems(displaySlots: Int2ObjectMap<ItemStack>): Int2ObjectMap<ItemStack> {
        val replacements = it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<ItemStack>()
        if (!StarredHeltix.feature.enchanting.tableSolvers.superpairsShowItems) return replacements

        for (entry in displaySlots.int2ObjectEntrySet()) {
            val index = entry.intKey
            val displayStack = entry.value
            val storedStack = slots.get(index)

            if (storedStack != null && !ItemStack.matches(storedStack, displayStack)) {
                // Если в слоте сейчас стекло, заменяем на то что было
                if (displayStack.`is`(Items.CYAN_STAINED_GLASS) || displayStack.`is`(Items.BLACK_STAINED_GLASS_PANE)) {
                    replacements.put(index, storedStack)
                }
            }
        }
        return replacements
    }

    override fun getColors(displaySlots: Int2ObjectMap<ItemStack>): List<ColorHighlight> {
        val highlights = mutableListOf<ColorHighlight>()
        if (getState() != State.SHOW) return highlights

        for (entry in displaySlots.int2ObjectEntrySet()) {
            val index = entry.intKey
            val displayStack = entry.value
            val storedStack = slots.get(index)

            if (storedStack != null) {
                if (duplicatedSlots.contains(index)) {
                    // Сервер уже подтвердил пару — не подсвечиваем
                    if (ItemStack.matches(storedStack, displayStack)) continue
                    // Мод знает о паре, но сервер ещё не раскрыл — полупрозрачный зелёный
                    if (StarredHeltix.feature.enchanting.tableSolvers.superpairsShowCollected) {
                        highlights.add(ColorHighlight(index, 0x3300AA00))
                    }
                } else if (!ItemStack.matches(storedStack, displayStack)) {
                    if (ItemStack.matches(currentSlotItem, storedStack) &&
                        displayStack.hoverName.string.contains("вторую")) {
                        highlights.add(ColorHighlight.green(index))
                    } else {
                        // Серый цвет для просто запомненных
                        highlights.add(ColorHighlight(index, 0x88808080.toInt()))
                    }
                }
            }
        }
        return highlights
    }

    override fun onClickSlot(slot: Int, stack: ItemStack, button: Int): Boolean {
        if (getState() == State.SHOW) {
            // button -2 is from key press, others are mouse buttons
            this.prevClickedSlot = slot
            this.currentSlotItem = ItemStack.EMPTY
        }
        return false
    }

    override fun reset() {
        prevClickedSlot = 0
        currentSlotItem = ItemStack.EMPTY
        duplicatedSlots.clear()
        super.reset()
    }
}