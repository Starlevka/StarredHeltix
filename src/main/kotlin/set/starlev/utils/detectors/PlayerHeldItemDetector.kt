package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack

object PlayerHeldItemDetector {

    /**
     * Возвращает стак предмета из главной руки игрока.
     */
    fun getMainHandItem(): ItemStack {
        return Minecraft.getInstance().player?.getItemInHand(InteractionHand.MAIN_HAND) ?: ItemStack.EMPTY
    }

    /**
     * Возвращает стак предмета из второй руки игрока.
     */
    fun getOffHandItem(): ItemStack {
        return Minecraft.getInstance().player?.getItemInHand(InteractionHand.OFF_HAND) ?: ItemStack.EMPTY
    }

    /**
     * Возвращает отображаемое имя предмета в главной руке.
     */
    fun getMainHandItemName(): String {
        return getMainHandItem().hoverName.string
    }

    /**
     * Возвращает отображаемое имя предмета во второй руке.
     */
    fun getOffHandItemName(): String {
        return getOffHandItem().hoverName.string
    }
}
