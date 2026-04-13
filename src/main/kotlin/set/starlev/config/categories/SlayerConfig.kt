package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SlayerConfig {

    @Expose
    @ConfigOption(name = "HUD Слеера", desc = "Отображение текущего квеста Слеера.")
    @Accordion
    var slayerHud = SlayerHudConfig()

    class SlayerHudConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Включает отображение текущего Слеера через HUD.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Отображает фон позади у HUD Слеера.")
        @ConfigEditorBoolean
        var showBackground = true

        @Expose
        @ConfigOption(name = "Таймер убийства", desc = "Засекает время убийства босса и пишет в чат.")
        @ConfigEditorBoolean
        var bossTimer = true

        @Expose
        @ConfigOption(name = "Личные рекорды", desc = "Отслеживает и сохраняет лучшие результаты убийства боссов.")
        @ConfigEditorBoolean
        var personalBests = true
    }
}
