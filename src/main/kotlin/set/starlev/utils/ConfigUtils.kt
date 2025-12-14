package set.starlev.utils

import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.hud.HudEditorScreen

object ConfigUtils {

    fun openEditor(editor: MoulConfigEditor<*>) {
        StarredHeltix.screenToOpen = MoulConfigScreenComponent(Component.empty(), GuiContext(GuiElementComponent(editor)), null)
    }

    /**
     * Открыть редактор HUD элементов
     */
    fun openHudEditor() {
        StarredHeltix.screenToOpen = HudEditorScreen()
    }

    fun String.asStructuredText() = StructuredText.of(this)
}
