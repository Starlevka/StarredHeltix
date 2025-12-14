package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ForagingConfig {
    @Accordion
    @Expose
    @ConfigOption(name = "Отображение КД топоров", desc = "Визуализация задержки способностей топоров (Древоточец и Джунглиевский топор)")
    var treeCapCooldown = TreeCapCooldownConfig()

    class TreeCapCooldownConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Включить визуализацию задержки способностей топоров")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "КД топоров (сек)", desc = "Время КД способностей топоров в секундах")
        @ConfigEditorText
        var cooldown = "2"
    }
}
