package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MiningConfig {

    @Expose
    @ConfigOption(name = "Способности", desc = "Настройки отображения перезарядки способностей инструментов.")
    @Accordion
    var abilities = AbilitiesConfig()

    class AbilitiesConfig {
        @Expose
        @ConfigOption(name = "Отображение КД способностей", desc = "Показывает таймер перезарядки способностей в интерфейсе.")
        @Accordion
        var abilityCooldown = AbilityCooldownConfig()
    }

    class AbilityCooldownConfig {
        @Expose
        @ConfigOption(name = "Киркобулус", desc = "Включает отслеживание КД способности Pickobulus.")
        @ConfigEditorBoolean
        var pickaxeBoostEnabled = true

        @Expose
        @ConfigOption(name = "Увеличение скорости копания", desc = "Включает отслеживание КД способности Mining Speed Boost.")
        @ConfigEditorBoolean
        var speedBoostEnabled = true

        @Expose
        @ConfigOption(name = "КД Киркобулуса (сек)", desc = "Время перезарядки способности Киркобулус.")
        @ConfigEditorText
        var pickaxeBoostCooldown = "60"

        @Expose
        @ConfigOption(name = "КД скорости копания (сек)", desc = "Время перезарядки способности Speed Boost.")
        @ConfigEditorText
        var speedBoostCooldown = "120"
    }
}

