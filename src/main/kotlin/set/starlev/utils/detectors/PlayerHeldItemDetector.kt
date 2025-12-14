package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack

object PlayerHeldItemDetector {

    /**
     * Returns the item stack in the player's main hand.
     */
    fun getMainHandItem(): ItemStack {
        return Minecraft.getInstance().player?.getItemInHand(InteractionHand.MAIN_HAND) ?: ItemStack.EMPTY
    }

    /**
     * Returns the item stack in the player's off hand.
     */
    fun getOffHandItem(): ItemStack {
        return Minecraft.getInstance().player?.getItemInHand(InteractionHand.OFF_HAND) ?: ItemStack.EMPTY
    }

    /**
     * Returns the display name of the item in the main hand.
     */
    fun getMainHandItemName(): String {
        return getMainHandItem().hoverName.string
    }

    /**
     * Returns the display name of the item in the off hand.
     */
    fun getOffHandItemName(): String {
        return getOffHandItem().hoverName.string
    }
}
