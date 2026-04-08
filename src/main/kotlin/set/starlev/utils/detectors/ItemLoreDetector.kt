package set.starlev.utils.detectors

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.client.Minecraft
import net.minecraft.world.item.Item
import set.starlev.utils.CacheManager

object ItemLoreDetector {

    /**
     * Returns the lore (tooltip) of the given item stack as a list of strings.
     * Excludes the item name (first line).
     */
    fun getLore(stack: ItemStack): List<String> {
        if (stack.isEmpty) return emptyList()
        val hash = getItemStackHash(stack)
        val cached = CacheManager.getCachedLore(hash)
        if (cached != null) return cached

        val lore = generateLore(stack)
        CacheManager.cacheLore(hash, lore)
        return lore
    }

    private fun generateLore(stack: ItemStack): List<String> {
        val client = Minecraft.getInstance()
        val player = client.player ?: return emptyList()
        
        val tooltipComponents = stack.getTooltipLines(
            Item.TooltipContext.of(client.level),
            player,
            TooltipFlag.NORMAL
        )

        return if (tooltipComponents.isNotEmpty()) {
            tooltipComponents.drop(1).map { CacheManager.dedupString(it.string) }
        } else {
            emptyList()
        }
    }

    private fun getItemStackHash(stack: ItemStack): Int {
        var result = stack.item.hashCode()
        result = 31 * result + CacheManager.getComponentHash(stack.components)
        result = 31 * result + stack.count
        return result
    }

    /**
     * Returns the full tooltip (including name) of the given item stack.
     */
    fun getFullTooltip(stack: ItemStack): List<String> {
        if (stack.isEmpty) return emptyList()
        
        val client = Minecraft.getInstance()
        val player = client.player ?: return emptyList()
        
        val tooltipComponents = stack.getTooltipLines(
            Item.TooltipContext.of(client.level),
            player,
            TooltipFlag.NORMAL
        )

        return tooltipComponents.map { CacheManager.dedupString(it.string) }
    }
}
