package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import set.starlev.utils.ConfigUtils

class DisplayConfig {

    @Expose(serialize = false)
    @ConfigOption(name = "Открыть редактор HUD", desc = "Нажмите для открытия редактора HUD элементов")
    @ConfigEditorButton(buttonText = "Открыть")
    var openHudEditor: Runnable = Runnable { ConfigUtils.openHudEditor() }
}
