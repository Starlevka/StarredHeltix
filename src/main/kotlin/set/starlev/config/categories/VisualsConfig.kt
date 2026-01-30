package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class VisualsConfig {

    @Expose
    @ConfigOption(name = "Анимации", desc = "Кастомные анимации для игрока и предметов.")
    @Accordion
    var animations = AnimationsConfig()

    @Expose
    @ConfigOption(name = "Призрачные рамки", desc = "Настройки отображения призрачных рамок с картинками.")
    @Accordion
    var ghostFrames = GhostFrameConfig()

    class GhostFrameConfig {
        @Expose
        @ConfigOption(name = "Картинка 1", desc = "Отображает первую картинку в Хабе.")
        @ConfigEditorBoolean
        var image1Enabled = true

        @Expose
        @ConfigOption(name = "Картинка 2", desc = "Отображает вторую картинку на острове Starlev.")
        @ConfigEditorBoolean
        var image2Enabled = true
    }

    @Expose
    @ConfigOption(name = "Мега-ящики", desc = "Настройки отображения Мега-ящиков в мире.")
    @Accordion
    var megaChests = MegaChestsConfig()

    @Expose
    @ConfigOption(name = "Scoreboard", desc = "Визуальные настройки скорборда.")
    @Accordion
    var scoreboard = ScoreboardConfig()

    @Expose
    @ConfigOption(name = "Лог предметов", desc = "Отображает историю изменения предметов в инвентаре.")
    @Accordion
    var inventoryHistory = InventoryHistoryConfig()

    class InventoryHistoryConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Включает отображение лога предметов на экране.")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "Длительность (сек)", desc = "Время отображения записи в логе.")
        @ConfigEditorSlider(minValue = 1f, maxValue = 15f, minStep = 0.5f)
        var duration = 5f

        @Expose
        @ConfigOption(name = "Макс. записей", desc = "Максимальное количество одновременно отображаемых записей.")
        @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 1f)
        var maxEntries = 8f

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Отображает фон в логе предметов.")
        @ConfigEditorBoolean
        var showBackground = false

        @Expose
        @ConfigOption(name = "Игнорировать экипировку", desc = "Не отображать изменения брони и предметов в руках. §cЛучше оставить ВКЛ.")
        @ConfigEditorBoolean
        var ignoreEquipped = true
    }

    class ScoreboardConfig {
        @Expose
        @ConfigOption(name = "Кастомный?", desc = "Включает кастомизируемый scoreboard.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Фон Scoreboard", desc = "Отображает фон у Scoreboard.")
        @ConfigEditorBoolean
        var showBackground = true

    }

    class MegaChestsConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Отображает Мега-ящиков в мире.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Спавн на координатах", desc = "Включает спавн Мега-ящиков на их стандартных координатах в Хабе.")
        @ConfigEditorBoolean
        var spawnAtCoords = true
    }

    class AnimationsConfig {
        @Expose
        @ConfigOption(name = "Анимация удара", desc = "Изменяет положение и скорость анимации руки.")
        @Accordion
        var swingAnimation = SwingAnimationConfig()
    }

    class SwingAnimationConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Включает кастомную анимацию удара.")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "Без анимации экипировки", desc = "Отключает анимацию поднятия предмета при его смене.")
        @ConfigEditorBoolean
        var noEquipAnimation = false

        @Expose
        @ConfigOption(name = "Скорость удара", desc = "Изменяет длительность анимации удара (в тиках). Чем меньше значение, тем быстрее анимация. (0 - стандарт)")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 20.0f, minStep = 1.0f)
        var swingSpeed = 0.0

        @Expose
        @ConfigOption(name = "Положение X", desc = "Статичное смещение предмета по горизонтали.")
        @ConfigEditorSlider(minValue = -2.0f, maxValue = 2.0f, minStep = 0.05f)
        var offX = 0.0

        @Expose
        @ConfigOption(name = "Положение Y", desc = "Статичное смещение предмета по вертикали.")
        @ConfigEditorSlider(minValue = -2.0f, maxValue = 2.0f, minStep = 0.05f)
        var offY = 0.0

        @Expose
        @ConfigOption(name = "Положение Z", desc = "Статичное смещение предмета в глубину.")
        @ConfigEditorSlider(minValue = -2.0f, maxValue = 2.0f, minStep = 0.05f)
        var offZ = 0.0

        @Expose
        @ConfigOption(name = "Множитель Swing X", desc = "Множитель анимации удара по X.")
        @ConfigEditorSlider(minValue = -2.0f, maxValue = 2.0f, minStep = 0.05f)
        var swingX = 1.0

        @Expose
        @ConfigOption(name = "Множитель Swing Y", desc = "Множитель анимации удара по Y.")
        @ConfigEditorSlider(minValue = -2.0f, maxValue = 2.0f, minStep = 0.05f)
        var swingY = 1.0

        @Expose
        @ConfigOption(name = "Множитель Swing Z", desc = "Множитель анимации удара по Z.")
        @ConfigEditorSlider(minValue = -2.0f, maxValue = 2.0f, minStep = 0.05f)
        var swingZ = 1.0
    }
}
