package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

/**
 * JS API: Объект `item` — подробная информация о предметах.
 *
 * Дополняет inventory.getItemInSlot(): урон, чары, редкость, полные чары с id.
 *
 * ```js
 * var it = item.getHeld();
 * if (it) {
 *     chat.log(it.getId() + " урон " + it.getDamage() + "/" + it.getMaxDamage());
 *     it.getEnchantments().forEach(function(e) { chat.log(e.id + " " + e.level); });
 * }
 * ```
 */
class JSItemApi {

    /** Предмет в главной руке (или null). */
    fun getHeld(): JSItemDetails? = wrap(Minecraft.getInstance().player?.mainHandItem)

    /** Предмет во второй руке (или null). */
    fun getOffHand(): JSItemDetails? = wrap(Minecraft.getInstance().player?.offhandItem)

    /** Предмет в слоте инвентаря 0-35 (0-8 — хотбар). */
    fun getSlot(slot: Int): JSItemDetails? {
        val inv = Minecraft.getInstance().player?.inventory ?: return null
        val items = inv.nonEquipmentItems
        if (slot !in items.indices) return null
        return wrap(items[slot])
    }

    private fun wrap(stack: ItemStack?): JSItemDetails? {
        if (stack == null || stack.isEmpty) return null
        return JSItemDetails(stack)
    }
}

/**
 * JS API: Подробная обёртка предмета (доверенный класс пакета scripting.api).
 */
class JSItemDetails internal constructor(private val stack: ItemStack) {

    /** Идентификатор предмета: "minecraft:diamond_sword". */
    fun getId(): String = BuiltInRegistries.ITEM.getKey(stack.item).toString()

    fun getName(): String = stack.hoverName.string

    fun getCount(): Int = stack.count

    fun getDamage(): Int = stack.getDamageValue()

    fun getMaxDamage(): Int = stack.getMaxDamage()

    /** Редкость: "common" | "uncommon" | "rare" | "epic". */
    fun getRarity(): String = stack.rarity.name.lowercase()

    /** Чары: [{id: "minecraft:sharpness", level: 5}]. */
    fun getEnchantments(): List<Map<String, Any?>> {
        return try {
            val ench = stack.getEnchantments()
            ench.entrySet().map { entry ->
                val holder = entry.key
                val loc = holder.unwrapKey().orElse(null)?.identifier()?.toString() ?: ""
                mapOf("id" to loc, "level" to entry.intValue)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Уровень конкретных чар по id (0 — нет). */
    fun getEnchantmentLevel(id: String): Int {
        return try {
            val loc = net.minecraft.resources.Identifier.tryParse(id) ?: return 0
            val ench = stack.getEnchantments()
            ench.keySet().firstOrNull { it.unwrapKey().orElse(null)?.identifier() == loc }
                ?.let { ench.getLevel(it) } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /** Лор предмета (серые строки под именем). */
    fun getLore(): List<String> = set.starlev.utils.detectors.ItemLoreDetector.getLore(stack)

    /** Полный тултип как в инвентаре (имя + лор + чары + ...). */
    fun getFullTooltip(): List<String> = set.starlev.utils.detectors.ItemLoreDetector.getFullTooltip(stack)

    /** Есть ли кастомное имя (наковальня/анил). */
    fun hasCustomName(): Boolean = stack.get(DataComponents.CUSTOM_NAME) != null

    fun isEnchanted(): Boolean = stack.isEnchanted

    fun isDamageable(): Boolean = stack.isDamageableItem

    /** Прочность: maxDamage - damage. */
    fun getDurability(): Int = stack.maxDamage - stack.damageValue

    /**
     * Клиентская правка имени предмета (тултип). Видна только вам,
     * сбрасывается при следующем синке инвентаря сервером.
     * Принимает строку (§-коды) или JSTextComponent.
     */
    fun setName(name: Any?): JSItemDetails {
        when (name) {
            null -> stack.remove(DataComponents.CUSTOM_NAME)
            is JSTextComponent -> stack.set(DataComponents.CUSTOM_NAME, name.mcComponent)
            is String -> stack.set(DataComponents.CUSTOM_NAME, Component.literal(name.replace('&', '§')))
        }
        return this
    }

    /**
     * Клиентская правка лора (строки под именем). Строки с §-кодами или JSTextComponent.
     * Максимум 256 строк (лимит ванилы). null — сбросить лор.
     */
    fun setLore(lore: List<Any?>?): JSItemDetails {
        if (lore == null) {
            stack.remove(DataComponents.LORE)
            return this
        }
        val lines = lore.take(256).map { entry ->
            when (entry) {
                is JSTextComponent -> entry.mcComponent
                is String -> Component.literal(entry.replace('&', '§'))
                else -> Component.literal(entry?.toString() ?: "")
            }
        }
        // Второй список — "additional lines" (серые), основной список достаточно
        stack.set(DataComponents.LORE, net.minecraft.world.item.component.ItemLore(lines))
        return this
    }

    /** Текущий лор как компоненты (после setLore). */
    fun getLoreComponents(): List<JSTextComponent> =
        stack.get(DataComponents.LORE)?.lines?.map { JSTextComponent.wrap(it) } ?: emptyList()

    override fun toString(): String = "Item{id=${getId()}, name=${getName()}, count=${getCount()}}"
}
