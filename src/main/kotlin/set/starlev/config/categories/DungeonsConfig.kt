package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DungeonsConfig {
    @Accordion
    @Expose
    @ConfigOption(name = "Кровавая комната", desc = "Таймер для кровавой комнаты")
    var bloodRoom = BloodRoomConfig()

    @Accordion
    @Expose
    @ConfigOption(name = "Три незнакомца", desc = "Сольвер для головоломки трёх незнакомцев")
    var threeWeirdos = ThreeWeirdosConfig()

    class BloodRoomConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Включить таймер кровавой комнаты")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Сообщение", desc = "Сообщение при готовности комнаты")
        @ConfigEditorText
        var message = "starreдheltix ✪ Кровавая комната готова!"
    }

    class ThreeWeirdosConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Включить автоматический сольвер трёх незнакомцев")
        @ConfigEditorBoolean
        var enabled = true
    }
}
