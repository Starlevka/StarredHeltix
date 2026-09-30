package set.starlev.events

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.AbstractContainerMenu

class GuiEvents private constructor() {
    companion object {
        private val openCallbacks = mutableListOf<(AbstractContainerScreen<*>) -> Unit>()
        private val closeCallbacks = mutableListOf<(AbstractContainerScreen<*>) -> Unit>()
        private val renderCallbacks = mutableListOf<(net.minecraft.client.gui.GuiGraphicsExtractor) -> Unit>()
        private val foregroundCallbacks = mutableListOf<(net.minecraft.client.gui.GuiGraphicsExtractor, Int, Int, AbstractContainerScreen<*>) -> Unit>()
        private val clickCallbacks = mutableListOf<(Double, Double, Int, AbstractContainerScreen<*>) -> Boolean>()
        private val keyCallbacks = mutableListOf<(net.minecraft.client.input.KeyEvent, AbstractContainerScreen<*>) -> Boolean>()

        @JvmStatic
        fun registerOpen(callback: (AbstractContainerScreen<*>) -> Unit) {
            openCallbacks.add(callback)
        }

        @JvmStatic
        fun registerRender(callback: (net.minecraft.client.gui.GuiGraphicsExtractor) -> Unit) {
            renderCallbacks.add(callback)
        }

        @JvmStatic
        fun registerForeground(callback: (net.minecraft.client.gui.GuiGraphicsExtractor, Int, Int, AbstractContainerScreen<*>) -> Unit) {
            foregroundCallbacks.add(callback)
        }

        @JvmStatic
        fun registerClick(callback: (Double, Double, Int, AbstractContainerScreen<*>) -> Boolean) {
            clickCallbacks.add(callback)
        }

        /** Хоткеи внутри контейнерных экранов (из AbstractContainerScreenMixin.keyPressed). */
        @JvmStatic
        fun registerKey(callback: (net.minecraft.client.input.KeyEvent, AbstractContainerScreen<*>) -> Boolean) {
            keyCallbacks.add(callback)
        }

        @JvmStatic
        fun fireKey(event: net.minecraft.client.input.KeyEvent, screen: AbstractContainerScreen<*>): Boolean {
            return keyCallbacks.any { it(event, screen) }
        }

        @JvmStatic
        fun registerClose(callback: (AbstractContainerScreen<*>) -> Unit) {
            closeCallbacks.add(callback)
        }

        @JvmStatic
        fun fireRender(graphics: net.minecraft.client.gui.GuiGraphicsExtractor) {
            renderCallbacks.forEach { it(graphics) }
            set.starlev.events.StarredEvents.guiRender.post(set.starlev.events.GuiRenderEvent(graphics))
        }

        @JvmStatic
        fun fireForeground(graphics: net.minecraft.client.gui.GuiGraphicsExtractor, mouseX: Int, mouseY: Int, screen: AbstractContainerScreen<*>) {
            foregroundCallbacks.forEach { it(graphics, mouseX, mouseY, screen) }
        }

        @JvmStatic
        fun fireClick(mouseX: Double, mouseY: Double, button: Int, screen: AbstractContainerScreen<*>): Boolean {
            return clickCallbacks.any { it(mouseX, mouseY, button, screen) }
        }

        @JvmStatic
        fun fireOpen(screen: AbstractContainerScreen<*>) {
            openCallbacks.forEach { it(screen) }
            set.starlev.events.StarredEvents.guiOpen.post(set.starlev.events.GuiOpenEvent(screen))
        }

        @JvmStatic
        fun fireClose(screen: AbstractContainerScreen<*>) {
            closeCallbacks.forEach { it(screen) }
            set.starlev.events.StarredEvents.guiClose.post(set.starlev.events.GuiCloseEvent(screen))
        }
    }
}
