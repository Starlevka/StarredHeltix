package set.starlev.features.enchanting.solvers

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import set.starlev.StarredHeltix
import set.starlev.features.enchanting.ColorHighlight
import set.starlev.features.enchanting.ExperimentSolver

/**
 * Решатель мини-игры Суперпары (Heltix).
 *
 * Порт `heltix-superpairs/SuperpairsHandler`: накопление памяти — каждый кадр
 * запоминаем ВСЕ открытые (не закрытые стеклом/панелью) ячейки slot -> имя+предмет;
 * подсвечиваем зелёным любые две+ ячейки с одинаковым ключом (тот же предмет = пара).
 * Картинки не блокируются и не кликаются — клик просто проходит к серверу.
 *
 * Улучшение поверх эталона: getDisplayStack показывает запомненный предмет сквозь
 * закрытое стекло (тумблер «Показывать предметы» в конфиге, проверка в менеджере).
 */
class SuperpairsSolver : ExperimentSolver(Regex("Суперпары|Superpairs")) {

    /** slot -> «path|имя» — ключ пары. */
    private val pairKey = HashMap<Int, String>()

    /** slot -> запомненный стек (для визуальной замены под стеклом). */
    private val rememberedStacks = HashMap<Int, ItemStack>()

    private var lastRoundText = ""

    override fun isEnabled(): Boolean = StarredHeltix.feature.enchanting.tableSolvers.superpairs

    override fun start(screen: ContainerScreen) {
        super.start(screen)
        setState(State.SHOW)
    }

    override fun tick(screen: ContainerScreen) {
        if (checkTitleChanged(screen)) {
            pairKey.clear()
            rememberedStacks.clear()
            lastRoundText = ""
        }
        val slots = screen.menu.slots
        val containerSlots = slots.size - 36
        if (containerSlots <= 0) return

        // Чистим фантомы: слоты за пределами текущего контейнера (размер меню сменился)
        if (pairKey.keys.any { it >= containerSlots }) {
            val it = pairKey.keys.iterator()
            while (it.hasNext()) {
                val s = it.next()
                if (s >= containerSlots) {
                    it.remove()
                    rememberedStacks.remove(s)
                }
            }
        }

        // Новый раунд по тексту «Раунд N» — забываем прошлое поле
        for (i in 0 until slots.size) {
            val stack = slots[i].item
            if (stack.isEmpty || !ExperimentUtil.isRoundFlag(stack)) continue
            val hover = ExperimentUtil.hover(stack)
            if (hover != lastRoundText) {
                lastRoundText = hover
                pairKey.clear()
                rememberedStacks.clear()
            }
            break
        }

        for (i in 0 until containerSlots) {
            val stack = slots[i].item
            if (stack.isEmpty) continue
            if (ExperimentUtil.isClosedCard(stack)) continue
            if (ExperimentUtil.isSystemItem(ExperimentUtil.hover(stack))) continue
            val name = ExperimentUtil.clean(stack.hoverName.string)
            if (name.isBlank()) continue
            pairKey[i] = ExperimentUtil.path(stack) + "|" + name
            rememberedStacks[i] = stack.copy()
        }
    }

    override fun getColors(displaySlots: Int2ObjectMap<ItemStack>): List<ColorHighlight> {
        // Отдельный тумблер подсветки пар — при выключении только показ предметов (superpairsShowItems), без заливок
        if (!StarredHeltix.feature.enchanting.tableSolvers.superpairsShowCollected) return emptyList()
        if (pairKey.isEmpty()) return emptyList()
        // Группировка по ключу: подсвечиваем только точные пары (ровно 2 совпадения).
        // Группы из 3+ — неоднозначные (ложные коллизии имён/фантомы прошлых раундов),
        // их подсветка давала 8-9 заливок вместо 4 — такие группы игнорируем.
        val byKey = HashMap<String, MutableList<Int>>()
        for ((slot, key) in pairKey) byKey.getOrPut(key) { mutableListOf() }.add(slot)
        val result = ArrayList<ColorHighlight>()
        for ((_, group) in byKey) {
            if (group.size != 2) continue
            for (slot in group) result.add(ColorHighlight.green(slot))
        }
        return result
    }

    override fun getDisplayStack(slotId: Int, original: ItemStack): ItemStack? {
        if (slotId !in 0 until 54) return null
        if (!ExperimentUtil.isClosedCard(original)) return null
        val stored = rememberedStacks[slotId] ?: return null
        if (stored.isEmpty) return null
        return stored.copy()
    }

    override fun getReplacementItems(displaySlots: Int2ObjectMap<ItemStack>): Int2ObjectMap<ItemStack> {
        val replacements = it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<ItemStack>()
        if (!StarredHeltix.feature.enchanting.tableSolvers.superpairsShowItems) return replacements
        for (entry in displaySlots.int2ObjectEntrySet()) {
            val index = entry.intKey
            val displayStack = entry.value
            val stored = rememberedStacks[index] ?: continue
            if (ExperimentUtil.isClosedCard(displayStack) && !ItemStack.matches(stored, displayStack)) {
                replacements.put(index, stored.copy())
            }
        }
        return replacements
    }

    override fun onSlotUpdate(menu: AbstractContainerMenu, slotId: Int, stack: ItemStack) {
        // Вся логика в tick(screen) — покадровый скан, как в эталоне.
    }

    override fun onClickSlot(slot: Int, stack: ItemStack, button: Int): Boolean {
        // Клик всегда проходит: в Суперпарах нужно фlip'ать карты, блокировать нечего.
        return false
    }

    override fun reset() {
        pairKey.clear()
        rememberedStacks.clear()
        lastRoundText = ""
        super.reset()
    }
}
