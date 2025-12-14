package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FishingConfig {
    @Accordion
    @Expose
    @ConfigOption(name = "Уведомление о поклёвке", desc = "Звук и сообщение на экране при поклёвке")
    var fishingNotifier = FishingNotifierConfig()

    class FishingNotifierConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Включить уведомление о поклёвке")
        @ConfigEditorBoolean
        var enabled = true
    }
}
