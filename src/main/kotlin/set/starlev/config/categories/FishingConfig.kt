package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FishingConfig {
    @Expose
    @Category(name = "Светящиеся грибы", desc = "Подсветка светящихся грибов")
    var glowingMushroom = GlowingMushroomConfig()

    class GlowingMushroomConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Подсвечивает светящиеся грибы на Грибном острове")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "Цвет", desc = "Цвет подсветки")
        @ConfigEditorColour
        var colorV2 = "0:255:0:255:0"
    }

    @Expose
    @Category(name = "Уведомления", desc = "Настройки оповещений о различных событиях при рыбалке")
    var notifications = FishingNotificationsConfig()

    class FishingNotificationsConfig {
        @Expose
        @ConfigOption(name = "Рыба на крючке", desc = "Показывает уведомление и проигрывает звук, когда рыба клюет")
        @ConfigEditorBoolean
        var fishingNotifier = true

        @Expose
        @ConfigOption(name = "Легендарный улов", desc = "Особое уведомление при поимке легендарных морских существ")
        @ConfigEditorBoolean
        var legendaryFishingNotifier = true

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Отображает фон у уведомлений")
        @ConfigEditorBoolean
        var showBackground = false
    }

}

