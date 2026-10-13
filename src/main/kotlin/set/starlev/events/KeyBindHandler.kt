package set.starlev.events

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW
import set.starlev.StarredHeltix
import set.starlev.config.ConfigGuiManager
import set.starlev.features.chat.CustomBindManager
import set.starlev.features.visual.GhostNPCHandler

object KeyBindHandler {
    private val configKey = KeyMappingHelper.registerKeyMapping(
        KeyMapping(
            "key.starredheltix.config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            ModKeybinds.CATEGORY
        )
    )

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            // Обработка отложенного открытия экрана
            StarredHeltix.screenToOpen?.let { screen ->
                client.setScreenAndShow(screen)
                StarredHeltix.screenToOpen = null
            }

            while (configKey.consumeClick()) {
                if (client.gui.screen() == null) {
                    ConfigGuiManager.openConfigGui()
                }
            }

            // Проверка ЛКМ для GhostNPC и скриптовых NPC
            if (client.options.keyAttack.isDown) {
                val handledByScriptNpc = set.starlev.render.ScriptNPCManager.handleAttack()
                if (GhostNPCHandler.handleAttack() || handledByScriptNpc) {
                    // Обработано
                }
            }

            // Обработка кастомных биндов
            CustomBindManager.tick()

            // See Through Farming: клавиша прозрачности окна
            set.starlev.features.visual.SeeThroughWindow.tick()
        }
    }
}
