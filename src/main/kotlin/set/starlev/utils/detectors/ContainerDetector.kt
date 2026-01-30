package set.starlev.utils.detectors

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import set.starlev.events.GuiEvents

/**
 * Детектор для обнаружения и идентификации контейнеров (меню).
 */
object ContainerDetector {
    private val openCallbacks = mutableListOf<(AbstractContainerScreen<*>) -> Unit>()
    private val closeCallbacks = mutableListOf<(AbstractContainerScreen<*>) -> Unit>()

    fun init() {
        GuiEvents.registerOpen { screen ->
            openCallbacks.forEach { it(screen) }
        }

        GuiEvents.registerClose { screen ->
            closeCallbacks.forEach { it(screen) }
        }
    }

    /**
     * Регистрирует колбэк, который вызывается при открытии любого контейнера.
     */
    fun registerOpen(callback: (AbstractContainerScreen<*>) -> Unit) {
        openCallbacks.add(callback)
    }

    /**
     * Регистрирует колбэк, который вызывается при закрытии любого контейнера.
     */
    fun registerClose(callback: (AbstractContainerScreen<*>) -> Unit) {
        closeCallbacks.add(callback)
    }
}
