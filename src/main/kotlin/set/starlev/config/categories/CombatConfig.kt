package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CombatConfig {

    @Expose
    @ConfigOption(name = "Подсветка/Обводка мобов", desc = "Настройки визуального выделения существ для удобства обнаружения.")
    @Accordion
    var highlight = EntityHighlightConfig()

    class EntityHighlightConfig {

        @Expose
        @ConfigOption(name = "Эндермены", desc = "Настройки подсветки эндерменов.")
        @Accordion
        var enderman = EndermanConfig()

        @Expose
        @ConfigOption(name = "Криперы", desc = "Настройки подсветки криперов.")
        @Accordion
        var creeper = CreeperConfig()

        @Expose
        @ConfigOption(name = "Волки", desc = "Настройки подсветки волков.")
        @Accordion
        var wolf = WolfConfig()

        @Expose
        @ConfigOption(name = "Пауки", desc = "Настройки подсветки пауков.")
        @Accordion
        var spider = SpiderConfig()

        @Expose
        @ConfigOption(name = "Пещерные пауки", desc = "Настройки подсветки пещерных пауков.")
        @Accordion
        var caveSpider = CaveSpiderConfig()

        @Expose
        @ConfigOption(name = "Зомби", desc = "Настройки подсветки зомби.")
        @Accordion
        var zombie = ZombieConfig()

        @Expose
        @ConfigOption(name = "Слеер боссы (T1-T4)", desc = "Подсветка боссов Слеера: Мститель, Тарантул, Свен.")
        @Accordion
        var slayerBosses = SlayerBossesConfig()
    }

    class EndermanConfig {
        @Expose
        @ConfigOption(name = "Подсветка", desc = "Активирует обводку для эндерменов.")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "=> Бокс", desc = "Рисует бокс вокруг сущности (заливка или контур).")
        @ConfigEditorBoolean
        var box = true

        @Expose
        @ConfigOption(name = "=> Обводка", desc = "Будет обводить в виде хитбокса сущность.")
        @ConfigEditorBoolean
        var outline = false

        @Expose
        @ConfigOption(name = "=> Glow", desc = "Включает ванильное свечение (outline shader) для сущности.")
        @ConfigEditorBoolean
        var glow = false

        @Expose
        @ConfigOption(name = "Цвет", desc = "Выберите цвет обводки.")
        @ConfigEditorColour
        var colorV2 = "0:255:255:0:255"

        @Expose
        @ConfigOption(name = "Прозрачность", desc = "Установите уровень прозрачности подсветки.")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.05f)
        var transparency = 0.5f
    }

    class CreeperConfig {
        @Expose
        @ConfigOption(name = "Подсветка", desc = "Активирует обводку для криперов.")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "=> Бокс", desc = "Рисует бокс вокруг сущности (заливка или контур).")
        @ConfigEditorBoolean
        var box = true

        @Expose
        @ConfigOption(name = "=> Обводка", desc = "Будет обводить в виде хитбокса сущность.")
        @ConfigEditorBoolean
        var outline = false

        @Expose
        @ConfigOption(name = "=> Glow", desc = "Включает ванильное свечение (outline shader) для сущности.")
        @ConfigEditorBoolean
        var glow = false

        @Expose
        @ConfigOption(name = "Цвет", desc = "Выберите цвет обводки.")
        @ConfigEditorColour
        var colorV2 = "0:255:0:255:255"

        @Expose
        @ConfigOption(name = "Прозрачность", desc = "Установите уровень прозрачности подсветки.")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.05f)
        var transparency = 0.5f
    }

    class WolfConfig {
        @Expose
        @ConfigOption(name = "Подсветка", desc = "Активирует обводку для волков.")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "=> Бокс", desc = "Рисует бокс вокруг сущности (заливка или контур).")
        @ConfigEditorBoolean
        var box = true

        @Expose
        @ConfigOption(name = "=> Обводка", desc = "Будет обводить в виде хитбокса сущность.")
        @ConfigEditorBoolean
        var outline = false

        @Expose
        @ConfigOption(name = "=> Glow", desc = "Включает ванильное свечение (outline shader) для сущности.")
        @ConfigEditorBoolean
        var glow = false

        @Expose
        @ConfigOption(name = "Цвет", desc = "Выберите цвет обводки.")
        @ConfigEditorColour
        var colorV2 = "0:255:0:255:0"

        @Expose
        @ConfigOption(name = "Прозрачность", desc = "Установите уровень прозрачности подсветки.")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.05f)
        var transparency = 0.5f
    }

    class SpiderConfig {
        @Expose
        @ConfigOption(name = "Подсветка", desc = "Активирует обводку для пауков.")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "=> Бокс", desc = "Рисует бокс вокруг сущности (заливка или контур).")
        @ConfigEditorBoolean
        var box = true

        @Expose
        @ConfigOption(name = "=> Обводка", desc = "Будет обводить в виде хитбокса сущность.")
        @ConfigEditorBoolean
        var outline = false

        @Expose
        @ConfigOption(name = "=> Glow", desc = "Включает ванильное свечение (outline shader) для сущности.")
        @ConfigEditorBoolean
        var glow = false

        @Expose
        @ConfigOption(name = "Цвет", desc = "Выберите цвет обводки.")
        @ConfigEditorColour
        var colorV2 = "0:255:255:0:0"

        @Expose
        @ConfigOption(name = "Прозрачность", desc = "Установите уровень прозрачности подсветки.")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.05f)
        var transparency = 0.5f
    }

    class CaveSpiderConfig {
        @Expose
        @ConfigOption(name = "Подсветка", desc = "Активирует обводку для пещерных пауков.")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "=> Бокс", desc = "Рисует бокс вокруг сущности (заливка или контур).")
        @ConfigEditorBoolean
        var box = true

        @Expose
        @ConfigOption(name = "=> Обводка", desc = "Будет обводить в виде хитбокса сущность.")
        @ConfigEditorBoolean
        var outline = false

        @Expose
        @ConfigOption(name = "=> Glow", desc = "Включает ванильное свечение (outline shader) для сущности.")
        @ConfigEditorBoolean
        var glow = false

        @Expose
        @ConfigOption(name = "Цвет", desc = "Выберите цвет обводки.")
        @ConfigEditorColour
        var colorV2 = "0:255:128:0:128"

        @Expose
        @ConfigOption(name = "Прозрачность", desc = "Установите уровень прозрачности подсветки.")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.05f)
        var transparency = 0.5f
    }

    class ZombieConfig {
        @Expose
        @ConfigOption(name = "Подсветка", desc = "Активирует обводку для зомби.")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "=> Бокс", desc = "Рисует бокс вокруг сущности (заливка или контур).")
        @ConfigEditorBoolean
        var box = true

        @Expose
        @ConfigOption(name = "=> Обводка", desc = "Будет обводить в виде хитбокса сущность.")
        @ConfigEditorBoolean
        var outline = false

        @Expose
        @ConfigOption(name = "=> Glow", desc = "Включает ванильное свечение (outline shader) для сущности.")
        @ConfigEditorBoolean
        var glow = false

        @Expose
        @ConfigOption(name = "Цвет", desc = "Выберите цвет обводки.")
        @ConfigEditorColour
        var colorV2 = "0:255:0:128:0"

        @Expose
        @ConfigOption(name = "Прозрачность", desc = "Установите уровень прозрачности подсветки.")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.05f)
        var transparency = 0.5f
    }

    class SlayerBossesConfig {
        @Expose
        @ConfigOption(name = "Подсветка", desc = "Активирует обводку для боссов Слеера (T1-T4).")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "=> Бокс", desc = "Рисует бокс вокруг сущности (заливка или контур).")
        @ConfigEditorBoolean
        var box = true

        @Expose
        @ConfigOption(name = "=> Обводка", desc = "Будет обводить в виде хитбокса сущность.")
        @ConfigEditorBoolean
        var outline = true

        @Expose
        @ConfigOption(name = "=> Glow", desc = "Включает ванильное свечение (outline shader) для сущности.")
        @ConfigEditorBoolean
        var glow = false

        @Expose
        @ConfigOption(name = "Цвет", desc = "Выберите цвет обводки.")
        @ConfigEditorColour
        var colorV2 = "0:255:255:0:255"

        @Expose
        @ConfigOption(name = "Прозрачность", desc = "Установите уровень прозрачности подсветки.")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.05f)
        var transparency = 0.65f
    }
}

