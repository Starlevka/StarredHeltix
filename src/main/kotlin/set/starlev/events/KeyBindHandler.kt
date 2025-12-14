package set.starlev.events

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.resources.ResourceLocation
import org.lwjgl.glfw.GLFW
import set.starlev.StarredHeltix
import set.starlev.config.ConfigGuiManager
import set.starlev.features.misc.CustomBindManager

object KeyBindHandler {
    private val configKey = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.starredheltix.config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KeyMapping.Category(ResourceLocation.fromNamespaceAndPath("key", "categories.misc"))
        )
    )

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            // Обработка отложенного открытия экрана
            StarredHeltix.screenToOpen?.let { screen ->
                client.setScreen(screen)
                StarredHeltix.screenToOpen = null
            }

            while (configKey.consumeClick()) {
                if (client.screen == null) {
                    ConfigGuiManager.openConfigGui()
                }
            }

            // Обработка кастомных биндов
            CustomBindManager.tick()
        }
    }
}
