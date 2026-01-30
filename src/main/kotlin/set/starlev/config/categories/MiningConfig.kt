package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
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
        @ConfigOption(name = "HUD поручений", desc = "Показывает текущие поручения на экране.")
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
        @ConfigOption(name = "А-П уведомление", desc = "Показывает большой текст на экране для активации голубя.")
        @ConfigEditorBoolean
        var showTitle = true

        @Expose
        @ConfigOption(name = "Цвет акцента", desc = "Цвет полоски прогресса и важных элементов.")
        @ConfigEditorColour
        var accentColorV2 = "0:255:255:170:0"

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Отображает фон у HUD поручений.")
        @ConfigEditorBoolean
        var showBackground = true
    }

    class AbilitiesConfig {
        @Expose
        @ConfigOption(name = "Отображение КД способностей", desc = "Показывает таймер перезарядки способностей в интерфейсе.")
        @Accordion
        var abilityCooldown = AbilityCooldownConfig()
    }

    class AbilityCooldownConfig {
        @Expose
        @ConfigOption(name = "Киркобулус", desc = "Включает отслеживание КД способности Киркобулуса.")
        @ConfigEditorBoolean
        var pickaxeBoostEnabled = true

        @Expose
        @ConfigOption(name = "УСК", desc = "Включает отслеживание КД способности Увеличения скорости копания.")
        @ConfigEditorBoolean
        var speedBoostEnabled = true

        @Expose
        @ConfigOption(name = "КД Киркобулуса (сек)", desc = "Время перезарядки способности Киркобулуса.")
        @ConfigEditorText
        var pickaxeBoostCooldown = "60"

        @Expose
        @ConfigOption(name = "КД УСК (сек)", desc = "Время перезарядки способности УСК.")
        @ConfigEditorText
        var speedBoostCooldown = "120"

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Отображает фон у КД способностей.")
        @ConfigEditorBoolean
        var showBackground = false
    }
}

