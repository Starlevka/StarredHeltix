package set.starlev.utils

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import set.starlev.config.ConfigGuiManager

class ModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory<Screen> {
            MoulConfigScreenComponent(
                Component.empty(),
                GuiContext(GuiElementComponent(ConfigGuiManager.getConfigEditorInstance())),
                null
            )
        }
    }
}
