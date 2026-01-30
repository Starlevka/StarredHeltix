package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FishingConfig {
    @Expose
    @ConfigOption(name = "Уведомления", desc = "Настройки оповещений о различных событиях при рыбалке.")
    @Accordion
    var notifications = FishingNotificationsConfig()

    class FishingNotificationsConfig {
        @Expose
        @ConfigOption(name = "Рыба на крючке", desc = "Показывает уведомление и проигрывает звук, когда рыба клюет.")
        @ConfigEditorBoolean
        var fishingNotifier = true

        @Expose
        @ConfigOption(name = "Легендарный улов", desc = "Особое уведомление при поимке легендарных морских существ.")
        @ConfigEditorBoolean
        var legendaryFishingNotifier = true

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Отображает фон у уведомлений")
        @ConfigEditorBoolean
        var showBackground = false
    }
}

