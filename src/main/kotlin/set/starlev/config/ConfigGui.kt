package set.starlev.config

import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import net.minecraft.client.gui.screens.Screen
import set.starlev.StarredHeltix
import set.starlev.utils.ConfigUtils.openEditor

object ConfigGuiManager {

    private var configEditor: MoulConfigEditor<Features>? = null

    var currentScreenInstance: Screen? = null

    fun getConfigEditorInstance(): MoulConfigEditor<Features> {
        if (configEditor == null) {
            val editor = MoulConfigEditor(StarredHeltix.configManager.processor)
            // Обычный размер окна: широкий режим (wide = true) растягивал меню
            // почти на весь экран — выглядит непривычно.
            editor.wide = false
            configEditor = editor
        }
        return configEditor!!
    }

    fun openConfigGui(search: String? = null) {
        val currentEditor = getConfigEditorInstance()
        if (search != null) {
            currentEditor.search(search)
        }
        openEditor(currentEditor)
    }
}
