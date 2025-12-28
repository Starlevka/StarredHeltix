package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class OptimizationConfig {
    @Expose
    @ConfigOption(name = "Визуальные оптимизации", desc = "Настройки для повышения FPS и уменьшения визуального мусора.")
    @Accordion
    var visualOptimizations = VisualOptimizationsConfig()

    class VisualOptimizationsConfig {
        @Expose
        @ConfigOption(name = "Убрать анимацию после смерти", desc = "Мгновенно убирает модель моба после его смерти.")
        @ConfigEditorBoolean
        var hideDeathAnimation = false

        @Expose
        @ConfigOption(name = "Скрыть огонь", desc = "Убирает визуальный эффект огня на экране, когда вы горите.")
        @ConfigEditorBoolean
        var hideFireOverlay = false

        @Expose
        @ConfigOption(name = "Скрыть эффекты", desc = "Убирает отображение иконок статус-эффектов (зелий) в углу экрана.")
        @ConfigEditorBoolean
        var hideStatusEffects = false

        @Expose
        @ConfigOption(name = "Отключить свечение", desc = "Отключает эффект свечения у всех сущностей для повышения FPS.")
        @ConfigEditorBoolean
        var disableGlowing = false

        @Expose
        @ConfigOption(name = "Уменьшение частиц", desc = "Уменьшает количество создаваемых частиц (0% - без изменений, 100% - в 2 раза меньше).")
        @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
        var particleReduction: Float = 0f

        @Expose
        @ConfigOption(name = "Fullbright", desc = "Режим полной яркости.")
        @ConfigEditorBoolean
        var fullbright = false
    }
}


