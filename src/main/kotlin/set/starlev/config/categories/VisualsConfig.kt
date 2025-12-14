package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class VisualsConfig {

    @Expose
    @ConfigOption(name = "Анимация удара (БЕТА)", desc = "Настройки анимации удара")
    @Accordion
    var swingAnimation = SwingAnimationConfig()

    class SwingAnimationConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Изменяет анимацию удара")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "Положение X", desc = "Множитель по оси X")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 5.0f, minStep = 0.1f)
        var swingX = 1.0

        @Expose
        @ConfigOption(name = "Положение Y", desc = "Множитель по оси Y")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 5.0f, minStep = 0.1f)
        var swingY = 1.0

        @Expose
        @ConfigOption(name = "Положение Z", desc = "Множитель по оси Z")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 5.0f, minStep = 0.1f)
        var swingZ = 1.0

        @Expose
        @ConfigOption(name = "Скорость анимации", desc = "Изменяет скорость анимации взмаха")
        @ConfigEditorBoolean
        var swingSpeedEnabled = false

        @Expose
        @ConfigOption(name = "Множитель скорости", desc = "Скорость анимации (выше = быстрее)")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 10.0f, minStep = 0.1f)
        var swingSpeed = 1.0f
    }
}
