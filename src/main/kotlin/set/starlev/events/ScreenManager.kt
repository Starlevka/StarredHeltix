package set.starlev.events

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

object ScreenManager {
    private var pendingScreen: Screen? = null

    fun openDeferred(screen: Screen) {
        pendingScreen = screen
    }

    fun processPending(client: Minecraft) {
        pendingScreen?.let {
            client.setScreen(it)
            pendingScreen = null
        }
    }

    fun getCurrentScreen(): Screen? = pendingScreen

    fun clearPending() {
        pendingScreen = null
    }
}
