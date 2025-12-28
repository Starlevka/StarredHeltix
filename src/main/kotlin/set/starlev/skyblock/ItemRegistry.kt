package set.starlev.skyblock

import net.minecraft.world.item.ItemStack
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData
import net.minecraft.nbt.CompoundTag
import java.util.Optional

/**
 * Реестр предметов для идентификации кастомных предметов Skyblock
 */
object ItemRegistry {

    enum class SkyblockItem(val id: String, val displayName: String) {
        JUNGLE_AXE("JUNGLE_AXE", "Джунглевый топор"),
        TREECAPITATOR("TREECAPITATOR", "Древоточец"),
        // Можно добавлять другие предметы по мере необходимости
        UNKNOWN("UNKNOWN", "Неизвестно")
    }

    /**
     * Получить Skyblock ID предмета из его NBT данных.
     * В современных версиях Minecraft (1.20.5+) NBT хранится в компонентах.
     */
    fun getSkyblockId(stack: ItemStack): String? {
        if (stack.isEmpty) return null

        // 1. Попытка получить через ExtraAttributes (стандарт Hypixel/Heltix)
        try {
            val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return null
            val nbt = customData.copyTag()
            val extraAttributes = nbt.get("ExtraAttributes") as? CompoundTag ?: return null
            val idTag = extraAttributes.get("id") ?: return null
            
            val idResult = idTag.asString()
            return (idResult as? Optional<*>)?.orElse(null) as? String
        } catch (e: Exception) {
            // Игнорируем ошибки доступа к NBT
        }

        return null
    }

    /**
     * Проверить, является ли предмет определенным Skyblock предметом
     */
    fun isItem(stack: ItemStack, item: SkyblockItem): Boolean {
        val id = getSkyblockId(stack)
        if (id != null && id == item.id) return true
        
        // Резервный метод: проверка по названию (менее надежно, но полезно)
        val name = stack.hoverName.string
        return name.contains(item.displayName, ignoreCase = true)
    }

    /**
     * Определить тип топора для лесорубства
     */
    fun getAxeType(stack: ItemStack): SkyblockItem {
        return when {
            isItem(stack, SkyblockItem.TREECAPITATOR) -> SkyblockItem.TREECAPITATOR
            isItem(stack, SkyblockItem.JUNGLE_AXE) -> SkyblockItem.JUNGLE_AXE
            else -> SkyblockItem.UNKNOWN
        }
    }
}
