package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class OptimizationConfig {
    @Expose
    @ConfigOption(name = "Убрать анимацию после смерти", desc = "Убирает анимацию смерти мобов")
    @ConfigEditorBoolean
    var hideDeathAnimation = false

    @Expose
    @ConfigOption(name = "Скрыть огонь", desc = "Убирает оверлей огня на экране")
    @ConfigEditorBoolean
    var hideFireOverlay = false

    @Expose
    @ConfigOption(name = "Скрыть эффекты", desc = "Убирает отображение эффектов в инвентаре")
    @ConfigEditorBoolean
    var hideStatusEffects = false

}

