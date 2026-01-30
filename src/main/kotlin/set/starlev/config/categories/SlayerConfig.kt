package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SlayerConfig {
    @Expose
    @ConfigOption(name = "Слеерство", desc = "Функции для автоматизации и помощи в прохождении слееров.")
    @Accordion
    var general = GeneralConfig()

    @Expose
    @ConfigOption(name = "HUD Слеера", desc = "Отображение текущего квеста Слеера.")
    @Accordion
    var slayerHud = SlayerHudConfig()

    class GeneralConfig {
        @Expose
        @ConfigOption(name = "Авто-слеер", desc = "Автоматически предлагает позвонить Маддоксу с телефоном в хотбаре.")
        @ConfigEditorBoolean
        var autoSlayer = false

        @Expose
        @ConfigOption(name = "А-С уведомление", desc = "Показывает большой текст на экране для использования телефона.")
        @ConfigEditorBoolean
        var showTitle = true
    }

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

        @Expose
        @ConfigOption(name = "Слеер в Scoreboard", desc = "Интегрирует Слеер HUD в Scoreboard. §cНужно ВЫКЛ HUD Слеера.")
        @ConfigEditorBoolean
        var slayerScoreboardHud = true
    }
}
