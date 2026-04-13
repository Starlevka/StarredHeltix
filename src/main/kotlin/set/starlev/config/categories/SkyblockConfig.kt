package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import set.starlev.features.skyblock.scoreboard.ScoreboardConfigElement

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
    @ConfigOption(name = "Диалоги с Персонажами", desc = "Настройки оверлея диалогов с NPC.")
    @Accordion
    var npcDialogue = NpcDialogueConfig()

    @Expose
    @ConfigOption(name = "Плавный Аспект Энда", desc = "Настройки плавной телепортации (Аспект Энда).")
    @Accordion
    var smoothAote = SmoothAoteConfig()

    @Expose
    @ConfigOption(name = "Scoreboard", desc = "Визуальные настройки скорборда.")
    @Accordion
    var scoreboard = ScoreboardConfig()

    @Expose
    @ConfigOption(name = "Экипировка", desc = "Отображение экипировки рядом с инвентарём.")
    @Accordion
    var equipmentOverlay = EquipmentOverlayConfig()


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
        @ConfigOption(name = "Внешний вид", desc = "Перетаскивайте элементы для изменения порядка отображения. Удалите элемент чтобы скрыть его.")
        @ConfigEditorDraggableList
        var scoreboardEntries: MutableList<ScoreboardConfigElement> = ScoreboardConfigElement.defaultOptions.toMutableList()

        @Expose
        @ConfigOption(name = "Инфо элементы", desc = "Настройки FPS/Ping/CPS/BPS внутри кастомного scoreboard.")
        @Accordion
        var infoElements = ScoreboardInfoElementsConfig()

        @ConfigOption(name = "Сбросить внешний вид", desc = "Возвращает порядок элементов к стандартному.")
        @ConfigEditorButton(buttonText = "Сброс")
        val resetAppearance: Runnable = Runnable {
            scoreboardEntries.clear()
            scoreboardEntries.addAll(ScoreboardConfigElement.defaultOptions)
        }
    }

    class ScoreboardInfoElementsConfig {
        @Expose
        @ConfigOption(name = "ФПС", desc = "Настройки строки FPS в кастомном scoreboard.")
        @Accordion
        var fps = InfoElementConfig(enabled = false)

        @Expose
        @ConfigOption(name = "Пинг", desc = "Настройки строки Ping в кастомном scoreboard.")
        @Accordion
        var ping = InfoElementConfig(enabled = false)

        @Expose
        @ConfigOption(name = "КПС", desc = "Настройки строки CPS в кастомном scoreboard.")
        @Accordion
        var cps = InfoElementConfig(enabled = false)

        @Expose
        @ConfigOption(name = "БПС", desc = "Настройки строки BPS в кастомном scoreboard.")
        @Accordion
        var bps = InfoElementConfig(enabled = false)

    }

    open class InfoElementConfig(
        defaultColor: String = "0:255:85:255:85",
        enabled: Boolean = true
    ) {
        @Expose
        @ConfigOption(name = "Показывать?", desc = "Добавлять элемент в кастомный scoreboard.")
        @ConfigEditorBoolean
        var enabled = enabled

        @Expose
        @ConfigOption(name = "Цвет значения", desc = "Цвет числового значения элемента.")
        @ConfigEditorColour
        var valueColorV2 = defaultColor
    }

    class SmoothAoteConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Включает плавную анимацию телепортации Аспекта Энда. Присутсвует прикол с другими телепортами")
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
        var showBackground = false

        @Expose
        @ConfigOption(name = "Бой", desc = "Показывать опыт навыка Боя.")
        @ConfigEditorBoolean
        var showCombat = true

        @Expose
        @ConfigOption(name = "Шахтерство", desc = "Показывать опыт навыка Шахтерства.")
        @ConfigEditorBoolean
        var showMining = true

        @Expose
        @ConfigOption(name = "Фермерство", desc = "Показывать опыт навыка Фермерства.")
        @ConfigEditorBoolean
        var showFarming = true

        @Expose
        @ConfigOption(name = "Добыча", desc = "Показывать опыт навыка Добыча.")
        @ConfigEditorBoolean
        var showForaging = true

        @Expose
        @ConfigOption(name = "Рыболовство", desc = "Показывать опыт навыка Рыболовства.")
        @ConfigEditorBoolean
        var showFishing = true

        @Expose
        @ConfigOption(name = "Чародейство", desc = "Показывать опыт навыка Чародейства.")
        @ConfigEditorBoolean
        var showEnchanting = true

        @Expose
        @ConfigOption(name = "Алхимия", desc = "Показывать опыт навыка Алхимии.")
        @ConfigEditorBoolean
        var showAlchemy = true

        @Expose
        @ConfigOption(name = "Приручение", desc = "Показывать опыт навыка Приручение.")
        @ConfigEditorBoolean
        var showTaming = true

        @Expose
        @ConfigOption(name = "Подземелья", desc = "Показывать опыт навыка Подземелий (мб не работает).")
        @ConfigEditorBoolean
        var showDungeons = true

        @Expose
        @ConfigOption(name = "Улучшающие зачарования", desc = "Показывает прогресс улучшающих зачарований на предмете в руке (Компактность, Экспертиза, Чемпион, Культивирование).")
        @ConfigEditorBoolean
        var showEnchantmentProgress = true
        // Цвета захардкожены: название=лайм, опыт=жёлтый, осталось=красный, XP/час=лайм
    }

    class EquipmentOverlayConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Показывает экипировку рядом с инвентарём.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Фон", desc = "Цвет фона меню экипировки.")
        @ConfigEditorColour
        var backgroundColorV2 = "0:224:29:29:43"

        @Expose
        @ConfigOption(name = "Акцент", desc = "Цвет верхней полоски и рамки меню экипировки.")
        @ConfigEditorColour
        var accentColorV2 = "0:255:58:175:217"

        @Expose
        @ConfigOption(name = "Фон слота", desc = "Цвет фона слотов экипировки.")
        @ConfigEditorColour
        var slotColorV2 = "0:96:255:255:255"

        @Expose
        @ConfigOption(name = "Подсветка слота", desc = "Цвет при наведении на слот экипировки.")
        @ConfigEditorColour
        var slotHoverColorV2 = "0:170:58:175:217"

        @Expose
        var cachedEquipment: List<String> = emptyList()
    }
}