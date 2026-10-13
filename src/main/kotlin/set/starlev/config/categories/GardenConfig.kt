package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GardenConfig {

    @Expose
    @Category(name = "Участки (HUD)", desc = "Показывает текущий участок Garden — сетка 5x5")
    var plots = PlotsConfig()

    class PlotsConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Показывает HUD участков")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        var cachedPlotNames: MutableList<String> = (0 until 25).map { index ->
            when (index) {
                12 -> "Сарай"  // центр — всегда Сарай
                else -> ""
            }
        }.toMutableList()

        @Expose
        var cachedPlotItems: MutableList<String> = MutableList(25) { "" }

        @Expose
        @ConfigOption(name = "Фон HUD плотов", desc = "Показывать фон за сеткой плотов")
        @ConfigEditorBoolean
        var showBackground = true

        @Expose
        @ConfigOption(name = "Показывать вредителей", desc = "Подсветка заражённых плотов красным")
        @ConfigEditorBoolean
        var showPests = true

        @Expose
        @ConfigOption(name = "Цвет вредителей", desc = "Цвет рамки заражённых плотов")
        @ConfigEditorColour
        var pestColorV2 = "0:0:255:255:0"
    }

    // Распаковано: вредители — прямой тумблер в разделе Сад, без вложенного аккордеона
    @Expose
    @ConfigOption(name = "Подсветка вредителей", desc = "Красный маяк над вредителями в радиусе 48 блоков (ESP)")
    @ConfigEditorBoolean
    var pestHighlight = true

    // Обратная совместимость: старый объект pestHighlight.enabled → новый boolean
    // GSON проигнорирует старый объект при десериализации в boolean, поэтому оставляем геттер
    val plotsEnabled get() = plots.enabled
    val pestHighlightEnabled get() = pestHighlight

    // Миграция старого pestHighlight.enabled (объект) → boolean выполняется в ConfigManager при загрузке
    // если в JSON старый объект {"enabled":true}, GSON не сможет в boolean — оставляем fallback в PestHighlight.kt
}
