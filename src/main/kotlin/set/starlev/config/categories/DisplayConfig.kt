package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import set.starlev.utils.ConfigUtils

class DisplayConfig {

    @ConfigOption(name = "Редактор HUD", desc = "Открывает настройку HUD. Используйте колёсико мыши для изменения размера!")
    @ConfigEditorButton(buttonText = "клик ^-^")
    var openHudEditor: Runnable = Runnable { ConfigUtils.openHudEditor() }
}

