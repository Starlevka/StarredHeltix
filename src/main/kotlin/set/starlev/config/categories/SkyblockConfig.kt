package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SkyblockConfig {
    @Expose
    @ConfigOption(name = "Навыки и опыт", desc = "Настройки отображения опыта навыков и HUD.")
    @Accordion
    var skills = SkillsXpConfig()

    @Expose
    @ConfigOption(name = "Помощник Музея", desc = "Настройки помощника для музея.")
    @Accordion
    var museum = MuseumConfig()

    @Expose
    @ConfigOption(name = "Оверлей Питомца", desc = "Настройки отображения информации о питомце.")
    @Accordion
    var pet = PetConfig()

    class PetConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Показывает информацию о текущем питомце из таб листа.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Отображает фон в оверлея питомца.")
        @ConfigEditorBoolean
        var showBackground = false
    }

    class MuseumConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Отслеживает недостающие предметы в музее и показывает их в HUD.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Отображает фон в помощнике Музея.")
        @ConfigEditorBoolean
        var showBackground = true
    }

    class SkillsXpConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Показывает HUD с прогрессом навыка при получении опыта.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Цвет прогресс-бара", desc = "Цвет полоски прогресса в HUD навыков.")
        @ConfigEditorColour
        var barColorV2 = "0:255:85:255:85"

        @Expose
        @ConfigOption(name = "Цвет текста", desc = "Цвет основного текста в HUD навыков.")
        @ConfigEditorColour
        var textColor = "0:255:255:255:255"

        @Expose
        @ConfigOption(name = "Цвет значений", desc = "Цвет числовых значений и процентов.")
        @ConfigEditorColour
        var valuesColor = "0:255:255:85:255"

        @Expose
        @ConfigOption(name = "Отслеживать Бой", desc = "Показывать HUD для навыка Боя.")
        @ConfigEditorBoolean
        var showCombat = true

        @Expose
        @ConfigOption(name = "Отслеживать Шахтёрство", desc = "Показывать HUD для навыка Шахтёрства.")
        @ConfigEditorBoolean
        var showMining = true

        @Expose
        @ConfigOption(name = "Отслеживать Фермерство", desc = "Показывать HUD для навыка Фермерства.")
        @ConfigEditorBoolean
        var showFarming = true

        @Expose
        @ConfigOption(name = "Отслеживать Лесничество", desc = "Показывать HUD для навыка Лесничества.")
        @ConfigEditorBoolean
        var showForaging = true

        @Expose
        @ConfigOption(name = "Отслеживать Рыболовство", desc = "Показывать HUD для навыка Рыболовства.")
        @ConfigEditorBoolean
        var showFishing = true

        @Expose
        @ConfigOption(name = "Отслеживать Чародейство", desc = "Показывать HUD для навыка Чародейства.")
        @ConfigEditorBoolean
        var showEnchanting = true

        @Expose
        @ConfigOption(name = "Отслеживать Алхимию", desc = "Показывать HUD для навыка Алхимии.")
        @ConfigEditorBoolean
        var showAlchemy = true

        @Expose
        @ConfigOption(name = "Отслеживать Приручение", desc = "Показывать HUD для навыка Приручения.")
        @ConfigEditorBoolean
        var showTaming = true

        @Expose
        @ConfigOption(name = "Отслеживать Подземелья", desc = "Показывать HUD для навыка Подземелий.")
        @ConfigEditorBoolean
        var showDungeons = true

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Отображает фон в HUD навыков.")
        @ConfigEditorBoolean
        var showBackground = true
    }
}
