package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MiningConfig {

    @Expose
    @ConfigOption(name = "Поручения", desc = "Настройки отображения поручений в интерфейсе.")
    @Accordion
    var commissions = CommissionsConfig()

    @Expose
    @ConfigOption(name = "Способности", desc = "Настройки отображения перезарядки способностей инструментов.")
    @Accordion
    var abilities = AbilitiesConfig()

    class CommissionsConfig {
        @Expose
        @ConfigOption(name = "Поручений HUD", desc = "Показывает текущие поручения на экране.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Метки локаций", desc = "Показывает метки поручений в Гномьих шахтах.")
        @ConfigEditorBoolean
        var waypointsEnabled = true

        @Expose
        @ConfigOption(name = "Авто-поручения", desc = "Автоматически предлагает использовать Королевского голубя в хотбаре при выполнении поручения.")
        @ConfigEditorBoolean
        var autoCommissions = true

        @Expose
        @ConfigOption(name = "Показывать уведомление", desc = "Показывает большой текст на экране для активации голубя.")
        @ConfigEditorBoolean
        var showTitle = true
    }

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

