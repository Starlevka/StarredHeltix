package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ForagingConfig {

    @Expose
    @ConfigOption(name = "Топоры", desc = "Настройки для работы с топорами и их способностями.")
    @Accordion
    var axes = AxesConfig()

    class AxesConfig {
        @Expose
        @ConfigOption(name = "Отображение КД топоров", desc = "Показывает перезарядку Древоточеца и Джунглевского топора.")
        @Accordion
        var treeCapCooldown = TreeCapCooldownConfig()
    }

    class TreeCapCooldownConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Включает или выключает отображение КД.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "КД топоров (сек)", desc = "Время перезарядки в секундах (обычно 2 сек).")
        @ConfigEditorText
        var cooldown = "2"
    }
}

