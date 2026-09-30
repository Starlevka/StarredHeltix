package set.starlev.features.skyblock

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import set.starlev.StarredHeltix
import set.starlev.render.HudElement
import set.starlev.registry.ItemRegistry
import set.starlev.render.drawString
import kotlin.math.min

/**
 * Лог предметов по образцу SkyCubed PickUpLog (MIT):
 * - Трекинг изменений инвентаря через tick polling (как PickUpLog.onTick)
 * - Ключ — skyblockId (ExtraAttributes.id) или fallback к stripped displayName
 * - Мерж одинаковых предметов в течение lifetime (как в Skysoft linkedMap)
 * - Фейд-ин/аут через smoothStep, максимум линий, длительность
 * - Рендер: [иконка предмета] [полное имя] [+/-кол-во] — как компоненты PickUpLog (ITEM_STACK, NAME, DIFFERENCE)
 */
object InventoryHistoryLog : HudElement("InventoryHistoryLog") {
    private val mc = Minecraft.getInstance()
    private val state = ItemChangeLogState()
    private var lastInventory = mutableMapOf<String, Int>()
    private var lastItemStacks = mutableMapOf<String, ItemStack>()
    private var isInitialized = false
    private var wasEnabled = false

    private var lastWorldSwap = System.currentTimeMillis()

    fun init() {
        // Сброс при смене мира — как в SkyCubed PickUpLog (5с защита от ложных срабатываний)
        try {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
                state.clear()
                lastInventory.clear()
                lastItemStacks.clear()
                isInitialized = false
                lastWorldSwap = System.currentTimeMillis()
            }
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
                state.clear()
                lastInventory.clear()
                lastItemStacks.clear()
                isInitialized = false
                lastWorldSwap = System.currentTimeMillis()
            }
        } catch (_: Exception) {}
    }

    private fun getUniqueId(stack: ItemStack): String {
        // Как в SkyCubed PickUpLog: UUID > ENCHANTED_BOOK с зачарами > POTION > skyblockId
        try {
            val customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA)
            if (customData != null) {
                val tag = customData.copyTag()
                if (tag.contains("ExtraAttributes")) {
                    val extraOpt = tag.getCompound("ExtraAttributes")
                    if (extraOpt.isPresent) {
                        val extra = extraOpt.get()
                        // UUID для уникальных предметов
                        if (extra.contains("uuid")) {
                            val uuidOpt = extra.getString("uuid")
                            if (uuidOpt.isPresent && uuidOpt.get().isNotBlank()) return uuidOpt.get()
                        }
                        if (extra.contains("id")) {
                            val idOpt = extra.getString("id")
                            if (idOpt.isPresent) {
                                val id = idOpt.get()
                                if (id == "ENCHANTED_BOOK") {
                                    val ench = stack.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS)
                                    return "$id:$ench"
                                }
                                if (id == "POTION") {
                                    val potion = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS)
                                    return "$id:$potion"
                                }
                                if (id.isNotBlank()) return id
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        // Фолбэк к skyblockId или имени
        ItemRegistry.getSkyblockId(stack)?.let { return it }
        val stripped = net.minecraft.ChatFormatting.stripFormatting(stack.hoverName.string)?.trim()
        if (!stripped.isNullOrBlank()) return stripped
        return stack.item.toString()
    }

    private fun getItemKey(stack: ItemStack): String = getUniqueId(stack)

    override fun render() {
        val config = StarredHeltix.feature.skyblock.inventoryHistory
        if (!config.enabled && !isEditing && !wasEnabled) return
        if (config.enabled != wasEnabled) {
            if (!config.enabled) state.clear()
            wasEnabled = config.enabled
        }
        if (!config.enabled && !isEditing) return

        val graphics = cachedGraphics ?: return
        val now = System.currentTimeMillis()
        val durationMs = (config.duration * 1000).toInt()
        val maxEntries = config.maxEntries.toInt()

        val entries = state.visibleEntries(
            lifetimeMillis = durationMs,
            maximumLines = maxEntries,
            newestFirst = false,
            now = now
        )

        val entriesToShow: List<ItemChangeVisual> = if (isEditing && entries.isEmpty()) {
            // Пример для редактора
            listOf(
                ItemChangeVisual("DIAMOND", 1, 1f, ItemStack(net.minecraft.world.item.Items.DIAMOND)),
                ItemChangeVisual("COBBLESTONE", -64, 1f, ItemStack(net.minecraft.world.item.Items.COBBLESTONE))
            )
        } else {
            entries
        }

        if (entriesToShow.isEmpty() && !isEditing) return

        // Компоненты строки как в SkyCubed PickUpLog: [иконка 16x16] [полное имя] [+/-кол-во]
        val rowHeight = 18
        val font = mc.font
        data class Row(val stack: ItemStack, val name: Component, val diff: Component, val opacity: Float)
        val rows = entriesToShow.map { visual ->
            val stack = visual.stack ?: lastItemStacks[visual.itemId] ?: ItemStack.EMPTY
            val name = if (stack.isEmpty) Component.literal(visual.itemId) else stack.hoverName.copy()
            val amount = visual.amount
            val diffText = if (amount > 0) "+$amount" else amount.toString()
            val diffColor = if (amount > 0) 0xFF55FF55.toInt() else 0xFFFF5555.toInt()
            val diff = Component.literal(diffText).withStyle { it.withColor(diffColor and 0xFFFFFF) }
            Row(stack, name, diff, visual.opacity)
        }

        var maxWidth = 0
        for (row in rows) {
            val w = 16 + 4 + font.width(row.name) + 4 + font.width(row.diff)
            if (w > maxWidth) maxWidth = w
        }

        val finalWidth = maxOf(maxWidth + 8, 100)
        val finalHeight = rows.size * rowHeight + 4

        this.showBackground = config.showBackground
        drawBackground(finalWidth, finalHeight, 0, shadow = false)

        var currentY = y + 2
        rows.forEachIndexed { idx, row ->
            val alpha = (row.opacity * 255).toInt().coerceIn(0, 255)
            val withAlpha = alpha shl 24
            if (idx < entriesToShow.size) {
                if (!row.stack.isEmpty) {
                    val iconY = currentY + 1
                    graphics.item(row.stack, x + 4, iconY, idx + 1)
                    graphics.itemDecorations(font, row.stack, x + 4, iconY)
                }
                val textY = currentY + (rowHeight - font.lineHeight) / 2
                val nameX = x + 4 + 16 + 4
                graphics.drawString(font, row.name, nameX, textY, withAlpha or 0xFFFFFF, true)
                graphics.drawString(font, row.diff, nameX + font.width(row.name) + 4, textY, withAlpha or 0xFFFFFF, true)
            }
            currentY += rowHeight
        }
    }

    fun tick() {
        val player = mc.player ?: return
        val config = StarredHeltix.feature.skyblock.inventoryHistory
        if (!config.enabled) return

        val currentInventory = mutableMapOf<String, Int>()
        val itemMap = mutableMapOf<String, ItemStack>()

        val maxSlot = if (config.ignoreEquipped) 36 else player.inventory.containerSize

        for (i in 0 until maxSlot) {
            val stack = try { player.inventory.getItem(i) } catch (_: Exception) { continue }
            if (stack.isEmpty) continue

            val key = getItemKey(stack)
            val count = currentInventory.getOrDefault(key, 0)
            currentInventory[key] = count + stack.count
            if (!itemMap.containsKey(key)) {
                itemMap[key] = stack.copy()
            }
        }

        if (!isInitialized) {
            lastInventory.putAll(currentInventory)
            lastItemStacks.putAll(itemMap)
            isInitialized = true
            return
        }

        // Защита от ложных срабатываний при смене мира как в SkyCubed (5с)
        val isSwappingWorlds = System.currentTimeMillis() - lastWorldSwap < 5000
        if (isSwappingWorlds) {
            lastInventory.clear()
            lastInventory.putAll(currentInventory)
            lastItemStacks.clear()
            lastItemStacks.putAll(itemMap)
            return
        }

        val allKeys = (lastInventory.keys + currentInventory.keys).distinct()
        for (key in allKeys) {
            val lastCount = lastInventory.getOrDefault(key, 0)
            val currentCount = currentInventory.getOrDefault(key, 0)

            if (lastCount != currentCount) {
                val diff = currentCount - lastCount
                val stack = itemMap[key] ?: lastItemStacks[key] ?: continue
                if (stack.isEmpty) continue
                // Добавляем через state (как Skysoft ItemChangeLogState.add)
                state.add(key, diff, config.maxEntries.toInt(), stack)
            }
        }

        lastInventory.clear()
        lastInventory.putAll(currentInventory)
        // Обновляем кэш стаков — храним только актуальные + те что только что исчезли (но state уже содержит копию)
        lastItemStacks.clear()
        lastItemStacks.putAll(itemMap)
        // Для удалённых предметов сохраняем последнюю копию в state, но не в lastItemStacks — не нужно
        // Чтобы при удалении показать имя, state хранит stack
    }

    override fun getWidth(): Int = 120
    override fun getHeight(): Int {
        val config = StarredHeltix.feature.skyblock.inventoryHistory
        return (config.maxEntries.toInt().coerceAtLeast(1) * 18 + 4)
    }
    override fun getDefaultScale(): Float = 1.0f
    override fun getDefaultX(): Int = 283
    override fun getDefaultY(): Int = 3
}

/**
 * Состояние лога 1:1 с Skysoft ItemChangeLogState
 */
internal class ItemChangeLogState(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val entries = linkedMapOf<String, ItemChangeEntry>()

    fun add(itemId: String, change: Int, maximumLines: Int = Int.MAX_VALUE, stack: ItemStack = ItemStack.EMPTY) {
        if (change == 0) return
        val now = currentTimeMillis()
        val entry = entries[itemId]
        if (entry == null) {
            entries[itemId] = ItemChangeEntry(itemId, change.toLong(), now, now, stack.copy())
        } else {
            entry.amount += change
            entry.updatedAtMillis = now
            entry.stack = stack.copy()
            if (entry.amount == 0L) entries.remove(itemId)
        }
        while (entries.size > maximumLines.coerceAtLeast(1)) entries.remove(entries.keys.first())
    }

    fun clear() = entries.clear()

    fun visibleEntries(
        lifetimeMillis: Int,
        maximumLines: Int,
        newestFirst: Boolean = false,
        now: Long = currentTimeMillis(),
    ): List<ItemChangeVisual> {
        val lifetime = lifetimeMillis.coerceAtLeast(1).toLong()
        // Удаляем протухшие
        entries.values.removeIf { entry -> now - entry.updatedAtMillis >= lifetime }
        val visibleEntries = entries.values.toList().takeLast(maximumLines.coerceAtLeast(1))
            .let { visible -> if (newestFirst) visible.asReversed() else visible }
        return visibleEntries.map { entry ->
            val age = (now - entry.createdAtMillis).coerceAtLeast(0L)
            val remaining = (lifetime - (now - entry.updatedAtMillis)).coerceAtLeast(0L)
            val fadeIn = smoothStep(
                (age.toFloat() / min(FADE_IN_MILLIS, lifetime).coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f),
            )
            val fadeOut = smoothStep(
                (remaining.toFloat() / min(FADE_OUT_MILLIS, lifetime).coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f),
            )
            ItemChangeVisual(entry.itemId, entry.amount, min(fadeIn, fadeOut), entry.stack)
        }
    }

    private fun smoothStep(t: Float): Float {
        // 3t^2 - 2t^3 как в Skysoft EasingUtilities
        return t * t * (3f - 2f * t)
    }

    private data class ItemChangeEntry(
        val itemId: String,
        var amount: Long,
        val createdAtMillis: Long,
        var updatedAtMillis: Long,
        var stack: ItemStack,
    )
}

internal data class ItemChangeVisual(
    val itemId: String,
    val amount: Long,
    val opacity: Float,
    val stack: ItemStack? = null,
)

private const val FADE_IN_MILLIS = 200L
private const val FADE_OUT_MILLIS = 500L
