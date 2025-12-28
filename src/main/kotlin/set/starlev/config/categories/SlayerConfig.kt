package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SlayerConfig {
    @Expose
    @ConfigOption(name = "Слеерство", desc = "Функции для автоматизации и помощи в прохождении слееров.")
    @Accordion
    var general = GeneralConfig()

    class GeneralConfig {
        @Expose
        @ConfigOption(name = "Авто-слеер", desc = "Автоматически предлагает позвонить Маддоксу при получении опыта слеера.")
        @ConfigEditorBoolean
        var autoSlayer = true
    }
}
