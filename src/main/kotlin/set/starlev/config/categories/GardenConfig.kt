package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GardenConfig {

    @Expose
    @ConfigOption(name = "Milestone урожая (HUD)", desc = "Показывает прогресс milestone текущей культуры.")
    @Accordion
    var cropMilestone = CropMilestoneConfig()

    class CropMilestoneConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Показывает HUD milestone урожая.")
        @ConfigEditorBoolean
        var enabled = true
    }

    @Expose
    @ConfigOption(name = "Участки (HUD)", desc = "Показывает текущий участок Garden.")
    @Accordion
    var plots = PlotsConfig()

    class PlotsConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Показывает HUD участков.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        var cachedPlotNames: MutableList<String> = (0 until 25).map { index ->
            when (index) {
                12 -> "Сарай"  // центр — всегда Сарай
                else -> ""
            }
        }.toMutableList()
    }

    // Быстрые геттеры для обратной совместимости
    val cropMilestoneEnabled get() = cropMilestone.enabled
    val plotsEnabled get() = plots.enabled
}
