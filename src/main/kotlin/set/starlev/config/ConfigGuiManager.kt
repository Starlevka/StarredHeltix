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
            configEditor = MoulConfigEditor(StarredHeltix.configManager.processor)
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
