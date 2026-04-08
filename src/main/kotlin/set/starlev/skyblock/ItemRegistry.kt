package set.starlev.skyblock

import net.minecraft.world.item.ItemStack
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData
import net.minecraft.nbt.CompoundTag
import set.starlev.utils.*
import java.util.Optional

/**
 * Реестр предметов для идентификации кастомных предметов Skyblock
 */
object ItemRegistry {

    enum class SkyblockItem(val id: String, val displayName: String) {
        JUNGLE_AXE("JUNGLE_AXE", "Джунглевый топор"),
        TREECAPITATOR("TREECAPITATOR", "Древоточец"),
        ASPECT_OF_THE_END("ASPECT_OF_THE_END", "Аспект Энда"),
        ASPECT_OF_THE_VOID("ASPECT_OF_THE_VOID", "Аспект Бездны"),
        HYPERION("HYPERION", "Гиперион"),
        RANCHERS_BOOTS("RANCHERS_BOOTS", "Ботинки Ранчера"),
        UNKNOWN("UNKNOWN", "Неизвестно")
    }

    /**
     * Получить Skyblock ID предмета из его NBT данных.
     * В современных версиях Minecraft (1.20.5+) NBT хранится в компонентах.
     */
    fun getSkyblockId(stack: ItemStack): String? {
        if (stack.isEmpty) return null
        val components = stack.components
        val cached = set.starlev.utils.CacheManager.getCachedSkyblockId(components) {
            // 1. Попытка получить через ExtraAttributes (стандарт Hypixel/Heltix)
            try {
                val customData: CustomData = stack.get(DataComponents.CUSTOM_DATA) ?: return@getCachedSkyblockId null
                val nbt: CompoundTag = customData.copyTag()
                if (nbt.contains("ExtraAttributes")) {
                    val extraAttributesOpt: Optional<CompoundTag> = nbt.getCompound("ExtraAttributes")
                    if (extraAttributesOpt.isPresent) {
                        val extraAttributes = extraAttributesOpt.get()
                        if (extraAttributes.contains("id")) {
                            val idOpt: Optional<String> = extraAttributes.getString("id")
                            if (idOpt.isPresent) {
                                return@getCachedSkyblockId idOpt.get()
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
            null
        }
        if (cached != null) return cached

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
