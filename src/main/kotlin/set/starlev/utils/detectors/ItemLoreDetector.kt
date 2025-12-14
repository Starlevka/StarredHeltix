package set.starlev.utils.detectors

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.client.Minecraft
import net.minecraft.world.item.Item

object ItemLoreDetector {

    /**
     * Returns the lore (tooltip) of the given item stack as a list of strings.
     * Excludes the item name (first line).
     */
    fun getLore(stack: ItemStack): List<String> {
        if (stack.isEmpty) return emptyList()
        
        val client = Minecraft.getInstance()
        val player = client.player ?: return emptyList()
        
        // Get tooltip components
        val tooltipComponents = stack.getTooltipLines(
            Item.TooltipContext.of(client.level),
            player,
            TooltipFlag.NORMAL
        )

        // Drop the first line (item name) and convert rest to string
        return if (tooltipComponents.isNotEmpty()) {
            tooltipComponents.drop(1).map { it.string }
        } else {
            emptyList()
        }
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

        return tooltipComponents.map { it.string }
    }
}
