package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CombatConfig {

    @Expose
    @ConfigOption(name = "Эндермены", desc = "Настройки подсветки эндерменов")
    @Accordion
    var enderman = EndermanConfig()

    @Expose
    @ConfigOption(name = "Криперы", desc = "Настройки подсветки заряженных криперов")
    @Accordion
    var creeper = CreeperConfig()

    @Expose
    @ConfigOption(name = "Волки", desc = "Настройки подсветки волков")
    @Accordion
    var wolf = WolfConfig()

    class EndermanConfig {
        @Expose
        @ConfigOption(name = "Включить подсветку", desc = "Подсвечивает эндерменов")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "Цвет", desc = "Цвет подсветки")
        @ConfigEditorColour
        var color = "0:255:255:0:255"

        @Expose
        @ConfigOption(name = "Прозрачность", desc = "Прозрачность хитбокса (0.0 - 1.0)")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.05f)
        var transparency = 0.5f
    }

    class CreeperConfig {
        @Expose
        @ConfigOption(name = "Включить подсветку", desc = "Подсвечивает заряженных криперов")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "Цвет", desc = "Цвет подсветки")
        @ConfigEditorColour
        var color = "0:255:0:255:255"

        @Expose
        @ConfigOption(name = "Прозрачность", desc = "Прозрачность хитбокса (0.0 - 1.0)")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.05f)
        var transparency = 0.5f
    }

    class WolfConfig {
        @Expose
        @ConfigOption(name = "Включить подсветку", desc = "Подсвечивает волков")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "Цвет", desc = "Цвет подсветки")
        @ConfigEditorColour
        var color = "0:255:0:255:0"

        @Expose
        @ConfigOption(name = "Прозрачность", desc = "Прозрачность хитбокса (0.0 - 1.0)")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.05f)
        var transparency = 0.5f
    }
}
