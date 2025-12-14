package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MiningConfig {
    
    @Accordion
    @Expose
    @ConfigOption(name = "Отображение КД способностей", desc = "Показывает КД способностей")
    var abilityCooldown = AbilityCooldownConfig()

    class AbilityCooldownConfig {
        @Expose
        @ConfigOption(name = "Киркобулус", desc = "Показывать КД Киркобулуса")
        @ConfigEditorBoolean
        var pickaxeBoostEnabled = true

        @Expose
        @ConfigOption(name = "Увеличение скорости копания", desc = "Показывать КД увеличения скорости копания")
        @ConfigEditorBoolean
        var speedBoostEnabled = true

        @Expose
        @ConfigOption(name = "КД Киркобулуса (сек)", desc = "Время КД Киркобулуса в секундах")
        @ConfigEditorText
        var pickaxeBoostCooldown = "60"

        @Expose
        @ConfigOption(name = "КД скорости копания (сек)", desc = "Время КД Увеличения скорости копания в секундах")
        @ConfigEditorText
        var speedBoostCooldown = "120"
    }
}