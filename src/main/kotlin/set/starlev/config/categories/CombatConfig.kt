package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CombatConfig {

    @Expose
    @Category(name = "Подсветка по клику", desc = "Клик колёсиком мыши по мобу — мод определяет ник сущности и обводит её. Повторный клик по той же — снять, клик по другой — переключиться")
    var clickHighlight = ClickHighlightConfig()

    @Expose
    @Category(name = "Костеранг", desc = "Показывает дотянется ли костеранг до моба: счётчик целей в прицеле и их подсветка (как BonemerangOverlay в Firmament)")
    var bonemerang = BonemerangConfig()

    class BonemerangConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Работает когда в руке костеранг (в названии предмета есть «Костеранг»)")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Счётчик целей (HUD)", desc = "Строка «Костеранг: N» — сколько мобов в радиусе прицела")
        @ConfigEditorBoolean
        var showHud = true

        @Expose
        @ConfigOption(name = "Подсветка целей", desc = "Обводит мобов, до которых дотянется бросок")
        @ConfigEditorBoolean
        var highlightTargets = true

        @Expose
        @ConfigOption(name = "Мобы рядом вне прицела", desc = "Подсвечивать другим цветом мобов в радиусе, но мимо прицела")
        @ConfigEditorBoolean
        var showOffAim = false

        @Expose
        @ConfigOption(name = "Дальность", desc = "Дальность полёта костеранга в блоках")
        @ConfigEditorSlider(minValue = 5f, maxValue = 30f, minStep = 1f)
        var range = 15f

        @Expose
        @ConfigOption(name = "Цвет достижимых", desc = "Цвет подсветки мобов в прицеле")
        @ConfigEditorColour
        var targetColorV2 = "0:255:0:255:0"

        @Expose
        @ConfigOption(name = "Цвет остальных", desc = "Цвет подсветки мобов рядом, но вне прицела")
        @ConfigEditorColour
        var offAimColorV2 = "0:255:255:255:0"

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Тёмный фон позади счётчика")
        @ConfigEditorBoolean
        var showBackground = true
    }

    class ClickHighlightConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Подсветка выбранной сущности. Клавиша выделения — в «Настройках управления» (категория «Прочее»), по умолчанию клик колёсиком; её можно переназначить, если колесо не доходит до игры")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Только кликнутый моб", desc = "Подсвечивать только ту сущность, по которой был клик, а не всех мобов с таким же ником")
        @ConfigEditorBoolean
        var singleTargetOnly = false

        @Expose
        @ConfigOption(name = "=> Бокс", desc = "Рисует бокс вокруг сущности (заливка или контур)")
        @ConfigEditorBoolean
        var box = true

        @Expose
        @ConfigOption(name = "=> Обводка", desc = "Будет обводить в виде хитбокса сущность (иначе — заливающий бокс)")
        @ConfigEditorBoolean
        var outline = false

        @Expose
        @ConfigOption(name = "=> Свечение", desc = "Включает ванильное свечение (outline shader) для сущности — только при прямой видимости, сквозь стены НЕ светит")
        @ConfigEditorBoolean
        var glow = false

        @Expose
        @ConfigOption(name = "Цвет", desc = "Выберите цвет подсветки")
        @ConfigEditorColour
        var colorV2 = "0:255:0:255:255"

        @Expose
        @ConfigOption(name = "Прозрачность", desc = "Установите уровень прозрачности подсветки")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.05f)
        var transparency = 0.5f
    }
}
