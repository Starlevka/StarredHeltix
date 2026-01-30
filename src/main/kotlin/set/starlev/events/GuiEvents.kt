package set.starlev.events

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu

class GuiEvents private constructor() {
    companion object {
        private val openCallbacks = mutableListOf<(AbstractContainerScreen<*>) -> Unit>()
        private val closeCallbacks = mutableListOf<(AbstractContainerScreen<*>) -> Unit>()
        private val renderCallbacks = mutableListOf<(net.minecraft.client.gui.GuiGraphics) -> Unit>()

        @JvmStatic
        fun registerOpen(callback: (AbstractContainerScreen<*>) -> Unit) {
            openCallbacks.add(callback)
        }
        
        @JvmStatic
        fun registerRender(callback: (net.minecraft.client.gui.GuiGraphics) -> Unit) {
            renderCallbacks.add(callback)
        }

        @JvmStatic
        fun registerClose(callback: (AbstractContainerScreen<*>) -> Unit) {
            closeCallbacks.add(callback)
        }

        @JvmStatic
        fun fireRender(graphics: net.minecraft.client.gui.GuiGraphics) {
            renderCallbacks.forEach { it(graphics) }
        }

        @JvmStatic
        fun fireOpen(screen: AbstractContainerScreen<*>) {
            openCallbacks.forEach { it(screen) }
        }

        @JvmStatic
        fun fireClose(screen: AbstractContainerScreen<*>) {
            closeCallbacks.forEach { it(screen) }
        }
    }
}
