package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
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

    @Expose
    @ConfigOption(name = "NPC Диалоги", desc = "Настройки оверлея диалогов с NPC.")
    @Accordion
    var npcDialogue = NpcDialogueConfig()

    @Expose
    @ConfigOption(name = "Плавный AOTE / Эндерперлы", desc = "Настройки плавной телепортации (Аспект Энда, Бездны, Гиперион).")
    @Accordion
    var smoothAote = SmoothAoteConfig()

    @Expose
    @ConfigOption(name = "Scoreboard", desc = "Визуальные настройки скорборда.")
    @Accordion
    var scoreboard = ScoreboardConfig()

    class ScoreboardConfig {
        @Expose
        @ConfigOption(name = "Кастомный?", desc = "Включает кастомизируемый scoreboard.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Фон Scoreboard", desc = "Отображает фон у Scoreboard.")
        @ConfigEditorBoolean
        var showBackground = true

        @Expose
        @ConfigOption(name = "Гемы (Tab)", desc = "Настройки отображения Гемов (парсинг из Tab).")
        @Accordion
        var gems = GemsConfig()

        @Expose
        @ConfigOption(name = "Банк (Tab)", desc = "Настройки отображения Банка (парсинг из Tab).")
        @Accordion
        var bank = BankConfig()

        @Expose
        @ConfigOption(name = "Печенье (Tab)", desc = "Настройки отображения статуса Магического Печенья (парсинг из Tab).")
        @Accordion
        var cookie = CookieConfig()

        class GemsConfig {
            @Expose
            @ConfigOption(name = "Scoreboard", desc = "Добавлять Гемы в кастомный Scoreboard.")
            @ConfigEditorBoolean
            var scoreboard = true
        }

        class BankConfig {
            @Expose
            @ConfigOption(name = "Scoreboard", desc = "Добавлять Банк в кастомный Scoreboard.")
            @ConfigEditorBoolean
            var scoreboard = true
        }

        class CookieConfig {
            @Expose
            @ConfigOption(name = "Scoreboard", desc = "Добавлять статус Печенья в кастомный Scoreboard.")
            @ConfigEditorBoolean
            var scoreboard = true
        }
    }

    class SmoothAoteConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Включает плавную анимацию при использовании Аспекта Энда, AOTV и других телепортов.")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "Время анимации (мс)", desc = "Длительность анимации перемещения.")
        @ConfigEditorSlider(minValue = 50f, maxValue = 500f, minStep = 10f)
        var time = 150f
    }

    class NpcDialogueConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Включает оверлей диалогов с NPC.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Скрывать сообщения", desc = "Скрывает сообщения NPC в чате при активном оверлее.")
        @ConfigEditorBoolean
        var hideMessages = true

        @Expose
        @ConfigOption(name = "Фон", desc = "Отображать фон диалогового окна.")
        @ConfigEditorBoolean
        var showBackground = true

        @Expose
        @ConfigOption(name = "Показывать опции", desc = "Отображать список вариантов ответов в оверлее.")
        @ConfigEditorBoolean
        var showOptions = true

        @Expose
        @ConfigOption(name = "Компактный режим", desc = "Уменьшает отступы и размеры окна для экономии места.")
        @ConfigEditorBoolean
        var compactMode = true

        @Expose
        @ConfigOption(name = "Закрывать на ESC", desc = "Закрывает диалог при нажатии клавиши ESC.")
        @ConfigEditorBoolean
        var closeOnEsc = true

        @Expose
        @ConfigOption(name = "Тайм-аут (сек)", desc = "Время в секундах, через которое диалог исчезнет автоматически.")
        @ConfigEditorSlider(minValue = 1f, maxValue = 60f, minStep = 1f)
        var timeoutSeconds = 30f
    }

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
        @ConfigOption(name = "Фон", desc = "Отображать фон для списка предметов.")
        @ConfigEditorBoolean
        var showBackground = true
    }

    class SkillsXpConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Показывает прогресс опыта навыков на экране.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Фон", desc = "Отображать фон HUD.")
        @ConfigEditorBoolean
        var showBackground = true

        @Expose
        @ConfigOption(name = "Бой (Combat)", desc = "Показывать опыт навыка Бой.")
        @ConfigEditorBoolean
        var showCombat = true

        @Expose
        @ConfigOption(name = "Шахтерство (Mining)", desc = "Показывать опыт навыка Шахтерство.")
        @ConfigEditorBoolean
        var showMining = true

        @Expose
        @ConfigOption(name = "Фермерство (Farming)", desc = "Показывать опыт навыка Фермерство.")
        @ConfigEditorBoolean
        var showFarming = true

        @Expose
        @ConfigOption(name = "Добыча (Foraging)", desc = "Показывать опыт навыка Добыча.")
        @ConfigEditorBoolean
        var showForaging = true

        @Expose
        @ConfigOption(name = "Рыбалка (Fishing)", desc = "Показывать опыт навыка Рыбалка.")
        @ConfigEditorBoolean
        var showFishing = true

        @Expose
        @ConfigOption(name = "Зачарование (Enchanting)", desc = "Показывать опыт навыка Зачарование.")
        @ConfigEditorBoolean
        var showEnchanting = true

        @Expose
        @ConfigOption(name = "Алхимия (Alchemy)", desc = "Показывать опыт навыка Алхимия.")
        @ConfigEditorBoolean
        var showAlchemy = true

        @Expose
        @ConfigOption(name = "Приручение (Taming)", desc = "Показывать опыт навыка Приручение.")
        @ConfigEditorBoolean
        var showTaming = true

        @Expose
        @ConfigOption(name = "Подземелья (Dungeons)", desc = "Показывать опыт навыка Подземелья.")
        @ConfigEditorBoolean
        var showDungeons = true

        @Expose
        @ConfigOption(name = "Цвет полосы", desc = "Цвет полосы прогресса (если используется).")
        @ConfigEditorColour
        var barColorV2 = "0:255:0:255"

        @Expose
        @ConfigOption(name = "Цвет текста", desc = "Цвет текста навыков.")
        @ConfigEditorColour
        var textColor = "255:255:255:255"

        @Expose
        @ConfigOption(name = "Цвет значений", desc = "Цвет цифр опыта.")
        @ConfigEditorColour
        var valuesColor = "255:255:255:255"
    }
}
