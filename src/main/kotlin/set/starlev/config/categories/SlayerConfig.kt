package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SlayerConfig {

    @Expose
    @Category(name = "HUD Слеера", desc = "Отображение текущего квеста Слеера")
    var slayerHud = SlayerHudConfig()

    @Expose
    @Category(name = "Слеер боссы", desc = "Подсветка боссов Слеера: Мститель, Тарантул, Свен")
    var slayerBosses = SlayerBossHighlightConfig()

    class SlayerHudConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Включает отображение текущего Слеера через HUD")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Отображает фон позади у HUD Слеера")
        @ConfigEditorBoolean
        var showBackground = true

        @Expose
        @ConfigOption(name = "Таймер убийства", desc = "Засекает время убийства босса и пишет в чат")
        @ConfigEditorBoolean
        var bossTimer = true

        @Expose
        @ConfigOption(name = "Личные рекорды", desc = "Отслеживает и сохраняет лучшие результаты убийства боссов")
        @ConfigEditorBoolean
        var personalBests = true
    }

    class SlayerBossHighlightConfig {
        @Expose
        @ConfigOption(name = "Подсветка", desc = "Активирует обводку для боссов Слеера (T1-T4)")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "=> Бокс", desc = "Рисует бокс вокруг сущности (заливка или контур)")
        @ConfigEditorBoolean
        var box = true

        @Expose
        @ConfigOption(name = "=> Обводка", desc = "Будет обводить в виде хитбокса сущность")
        @ConfigEditorBoolean
        var outline = true

        @Expose
        @ConfigOption(name = "=> Свечение", desc = "Включает ванильное свечение (outline shader) для сущности")
        @ConfigEditorBoolean
        var glow = false

        @Expose
        @ConfigOption(name = "Цвет", desc = "Выберите цвет обводки")
        @ConfigEditorColour
        var colorV2 = "0:255:255:0:255"

        @Expose
        @ConfigOption(name = "Прозрачность", desc = "Установите уровень прозрачности подсветки")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.05f)
        var transparency = 0.65f
    }
}
