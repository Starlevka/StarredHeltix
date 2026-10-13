package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import set.starlev.features.skyblock.scoreboard.ScoreboardConfigElement

/** Стиль фона кастомного скорборда. */
enum class ScoreboardBackgroundStyle(val label: String) {
    DEFAULT("Прямоугольный"),
    ROUNDED("Скруглённый"),
    NONE("Без фона"),
    CUSTOM("Своя картинка");

    override fun toString(): String = label
}

class SkyblockConfig {
    @Expose
    @ConfigOption(name = "Авто-спринт", desc = "Автоматически включает бег при движении вперед")
    @ConfigEditorBoolean
    var autoSprint = false

    @Expose
    @Category(name = "Лог предметов", desc = "Отображает историю изменения предметов в инвентаре")
    var inventoryHistory = VisualsConfig.InventoryHistoryConfig()

    @Expose
    @Category(name = "Кнопки инвентаря", desc = "Кастомные кнопки в инвентаре (как в NEU/Firmament)")
    var inventoryButtons = InventoryButtonsConfig()

    @Expose
    var migratedInventoryHistoryToMisc = false

    @Expose
    var migratedInfoHudToScoreboard = false

    /** Миграция раскрытия раздела «Общее» в корень категории (см. ConfigManager). */
    @Expose
    var migratedGeneralFlatten = false

    /**
     * Legacy-контейнер «Общее» (скрыт из UI — без @ConfigOption): источник
     * значений для одноразовой миграции в плоские поля выше.
     */
    @Expose
    var general = GeneralConfig()

    @Expose
    @Category(name = "/вход", desc = "Настройки для быстрого входа в систему через команду /вход")
    var autoLogin = LoginConfig()

    @Expose
    @Category(name = "Метки", desc = "Настройки меток в мире")
    var waypoints = WaypointsConfig()

    class GeneralConfig {
        @Expose
        var autoSprint = false

        @Expose
        var inventoryHistory = VisualsConfig.InventoryHistoryConfig()

        @Expose
        var inventoryButtons = InventoryButtonsConfig()
    }

    class LoginConfig {
        @Expose
        @ConfigOption(name = "Пароль", desc = "Ваш пароль для команды /вход (замены /login) §aХранится в зашифрованном виде (ENC:...)")
        @ConfigEditorText
        var password = ""
    }

    class WaypointsConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Показывает метки (waypoints) в мире")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Парсинг из чата", desc = "Создаёт метки по сообщениям с координатами (Waypoint/Вейпоинт/Метка)")
        @ConfigEditorBoolean
        var parseFromChat = true

        @Expose
        @ConfigOption(name = "Чат → обычные метки", desc = "Создаёт обычные (не временные) метки из чата")
        @ConfigEditorBoolean
        var chatCreatesRegular = false

        @Expose
        @ConfigOption(name = "Чат → временные метки", desc = "Создаёт временные метки из чата")
        @ConfigEditorBoolean
        var chatCreatesTemporary = true

        @Expose
        @ConfigOption(name = "Длительность временной (сек)", desc = "Сколько живёт временная метка (по умолчанию)")
        @ConfigEditorSlider(minValue = 5f, maxValue = 100f, minStep = 5f)
        var defaultTemporarySeconds = 30f

        @Expose
        @ConfigOption(name = "Цвет", desc = "Цвет меток")
        @ConfigEditorColour
        var colorV2 = "0:0:255:255:255"

        @Expose
        @ConfigOption(name = "Бокс", desc = "Рисовать обводку/заливку вокруг блока")
        @ConfigEditorBoolean
        var showBox = true

        @Expose
        @ConfigOption(name = "Луч", desc = "Рисовать луч маяка")
        @ConfigEditorBoolean
        var showBeam = true

        @Expose
        @ConfigOption(name = "Текст", desc = "Показывать подпись и дистанцию")
        @ConfigEditorBoolean
        var showText = true

        @ConfigOption(name = "Открыть меню меток", desc = "Управление списком меток")
        @ConfigEditorButton(buttonText = "Открыть")
        val openWaypointsMenu: Runnable = Runnable {
            set.starlev.StarredHeltix.screenToOpen = set.starlev.render.WaypointsGui(null)
        }
    }

    class InventoryButtonsConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Включает кастомные кнопки в инвентаре")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Только инвентарь", desc = "Показывать кнопки только в инвентаре игрока, а не в сундуках")
        @ConfigEditorBoolean
        var onlyInventory = true

        @Expose
        @ConfigOption(name = "Фон кнопки", desc = "Цвет фона кнопок /sh buttons")
        @ConfigEditorColour
        var tabBackgroundColorV2 = "0:128:0:0:0"

        @Expose
        @ConfigOption(name = "Фон кнопки (наведение)", desc = "Цвет фона при наведении на кнопку")
        @ConfigEditorColour
        var tabHoverBackgroundColorV2 = "0:192:58:175:217"

        @Expose
        @ConfigOption(name = "Рамка кнопки", desc = "Цвет рамки кнопки")
        @ConfigEditorColour
        var tabBorderColorV2 = "0:255:170:170:170"

        @Expose
        @ConfigOption(name = "Рамка кнопки (наведение)", desc = "Цвет рамки при наведении")
        @ConfigEditorColour
        var tabHoverBorderColorV2 = "0:255:255:255:255"

        @ConfigOption(name = "Настроить кнопки", desc = "Открывает меню настройки кнопок инвентаря")
        @ConfigEditorButton(buttonText = "Открыть")
        val openButtonsMenu: Runnable = Runnable {
            set.starlev.StarredHeltix.screenToOpen = set.starlev.features.skyblock.InventoryButtonsGui()
        }
    }

    @Expose
    @Category(name = "Навыки и опыт", desc = "Настройки отображения опыта навыков и HUD")
    var skills = SkillsXpConfig()

    @Expose
    @Category(name = "Помощник Музея", desc = "Настройки помощника для музея")
    var museum = MuseumConfig()

    @Expose
    @Category(name = "Оверлей Питомца", desc = "Настройки отображения информации о питомце")
    var pet = PetConfig()

    @Expose
    @Category(name = "Диалоги с Персонажами", desc = "Настройки оверлея диалогов с NPC")
    var npcDialogue = NpcDialogueConfig()

    @Expose
    @Category(name = "Плавный Аспект Энда", desc = "Настройки плавной телепортации (Аспект Энда)")
    var smoothAote = SmoothAoteConfig()

    @Expose
    @Category(name = "Табло", desc = "Визуальные настройки табло")
    var scoreboard = ScoreboardConfig()

    @Expose
    @Category(name = "Экипировка", desc = "Отображение экипировки рядом с инвентарём")
    var equipmentOverlay = EquipmentOverlayConfig()

    @Expose
    @Category(name = "Подсветка слотов", desc = "Подсветка слотов инвентаря по редкости предмета (порт из heltix-mod)")
    var slotOverlay = SlotOverlayConfig()

    @Expose
    @Category(name = "Предпросмотр хранилища", desc = "Подсказка с содержимым эндер-сундуков и рюкзаков Heltix (порт из heltix-mod)")
    var storagePreview = StoragePreviewConfig()

    @Expose
    @Category(name = "Ввод чисел (k/m/b)", desc = "Считает выражения прямо в полях ввода: 1k, 2.5m, 3b, 10к/10м/10б, 64s, 160e, 1k+2m*3 (порт из Skysoft)")
    var inputMath = InputMathConfig()

    class InputMathConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Заменяет содержимое поля ввода посчитанным числом. Только для полей Hypixel (с подсказкой ^^^)")
        @ConfigEditorBoolean
        var enabled = false
    }

    class StoragePreviewConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Показывать сетку предметов внутри хранилища при наведении на предмет-сумку/сундук")
        @ConfigEditorBoolean
        var enabled = true
    }

    class SlotOverlayConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Подсветка фона слотов по редкости")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "Стиль", desc = "Стиль подсветки: простой квадрат, квадрат с обводкой, круг")
        @ConfigEditorDropdown
        var style: SlotStyle = SlotStyle.BORDERED_SQUARE

        // deprecated: старый флаг circle, оставлен для миграции
        @Expose
        var circle: Boolean = false

        /** Миграция со старого boolean -> enum (вызывается лениво). */
        fun getEffectiveStyle(): SlotStyle {
            // Если стиль ещё дефолтный, а старый circle=true — мигрируем на круг
            if (circle && style == SlotStyle.BORDERED_SQUARE) return SlotStyle.CIRCLE
            return style
        }

        @Expose
        @ConfigOption(name = "Прозрачность", desc = "0-255")
        @ConfigEditorSlider(minValue = 0f, maxValue = 255f, minStep = 5f)
        var alpha = 80

        @Expose
        @ConfigOption(name = "Без ванильного хайлайта", desc = "Не рисовать белую рамку наведения на слотах с подсветкой редкости — она перекрывает цвет")
        @ConfigEditorBoolean
        var hideHoverHighlight = true

        enum class SlotStyle(val label: String) {
            SIMPLE_SQUARE("Простой квадрат"),
            BORDERED_SQUARE("Квадрат с обводкой"),
            CIRCLE("Круг");
            override fun toString(): String = label
        }
    }

    class ScoreboardConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Включает кастомный скорборд")
        @ConfigEditorBoolean
        var enabled = true

        // Одноразовая миграция: добавить строку POWER в сохранённый порядок
        // (дефолты применяются только на чистый конфиг, у существующих её нет).
        @Expose
        var migratedPowerLine = false

        @Expose
        @ConfigOption(name = "Стиль фона", desc = "Стиль фона табло. Своя картинка берётся из config/starredheltix/textures/")
        @ConfigEditorDropdown
        var backgroundStyle: ScoreboardBackgroundStyle = ScoreboardBackgroundStyle.ROUNDED

        @Expose
        @ConfigOption(name = "Фон табло", desc = "Отображает фон в табло (кроме стиля «Без фона»)")
        @ConfigEditorBoolean
        var showBackground = true

        @Expose
        @ConfigOption(name = "Прозрачность фона", desc = "Прозрачность фона табло для всех стилей, включая свою картинку (0-100%)")
        @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 5f)
        var backgroundAlpha = 45f

        @Expose
        @ConfigOption(name = "Радиус скругления", desc = "Гладкое закругление фона и картинки (0 = прямые углы)")
        @ConfigEditorSlider(minValue = 0f, maxValue = 10f, minStep = 1f)
        var cornerRadius = 4f

        @Expose
        @ConfigOption(name = "Путь к картинке", desc = "Только PNG (приоритет над папкой конфига). Пусто = scoreboard.png из config/starredheltix/textures/")
        @ConfigEditorText
        var customImagePath: String = ""

        @Expose
        @ConfigOption(name = "Внешний вид", desc = "Перетаскивайте элементы для изменения порядка отображения. Удалите элемент чтобы скрыть его")
        @ConfigEditorDraggableList
        var scoreboardEntries: MutableList<ScoreboardConfigElement> = ScoreboardConfigElement.defaultOptions.toMutableList()

        @Expose
        @ConfigOption(name = "Инфо элементы", desc = "Настройки FPS/Ping/CPS/BPS внутри кастомного scoreboard")
        @Accordion
        var infoElements = ScoreboardInfoElementsConfig()

        @ConfigOption(name = "Сбросить внешний вид", desc = "Возвращает порядок элементов к стандартному")
        @ConfigEditorButton(buttonText = "Сброс")
        val resetAppearance: Runnable = Runnable {
            scoreboardEntries.clear()
            scoreboardEntries.addAll(ScoreboardConfigElement.defaultOptions)
        }
    }

    class ScoreboardInfoElementsConfig {
        @Expose
        @ConfigOption(name = "ФПС", desc = "Настройки строки FPS в кастомном scoreboard")
        @Accordion
        var fps = InfoElementConfig(enabled = false)

        @Expose
        @ConfigOption(name = "Пинг", desc = "Настройки строки Ping в кастомном scoreboard")
        @Accordion
        var ping = InfoElementConfig(enabled = false)

        @Expose
        @ConfigOption(name = "КПС", desc = "Настройки строки CPS в кастомном scoreboard")
        @Accordion
        var cps = InfoElementConfig(enabled = false)

        @Expose
        @ConfigOption(name = "БПС", desc = "Настройки строки BPS в кастомном scoreboard")
        @Accordion
        var bps = InfoElementConfig(enabled = false)

        @Expose
        @ConfigOption(name = "Текущая сила", desc = "Строка магической/выбранной силы из меню Мешков в кастомном scoreboard")
        @Accordion
        var power = PowerElementConfig()

    }

    class PowerElementConfig : InfoElementConfig(enabled = false) {
        /** Кэш выбранной силы — переживает рестарт, строка видна без открытия меню. */
        @Expose
        var cachedPowerName: String = ""

        /** Кэш магической силы (-1 = неизвестно). */
        @Expose
        var cachedPowerMp: Int = -1
    }

    open class InfoElementConfig(
        defaultColor: String = "0:255:85:255:85",
        enabled: Boolean = true
    ) {
        @Expose
        @ConfigOption(name = "Показывать?", desc = "Добавлять элемент в кастомный scoreboard")
        @ConfigEditorBoolean
        var enabled = enabled

        @Expose
        @ConfigOption(name = "Цвет значения", desc = "Цвет числового значения элемента")
        @ConfigEditorColour
        var valueColorV2 = defaultColor
    }

    class SmoothAoteConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Включает плавную анимацию телепортации Аспекта Энда. Присутсвует прикол с другими телепортами")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "Время анимации (мс)", desc = "Длительность анимации перемещения")
        @ConfigEditorSlider(minValue = 50f, maxValue = 500f, minStep = 10f)
        var time = 150f
    }

    class NpcDialogueConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Включает оверлей диалогов с NPC")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Скрывать сообщения", desc = "Скрывает сообщения NPC в чате при активном оверлее")
        @ConfigEditorBoolean
        var hideMessages = true

        @Expose
        @ConfigOption(name = "Фон", desc = "Отображать фон диалогового окна")
        @ConfigEditorBoolean
        var showBackground = true

        @Expose
        @ConfigOption(name = "Показывать опции", desc = "Отображать список вариантов ответов в оверлее")
        @ConfigEditorBoolean
        var showOptions = true

        @Expose
        @ConfigOption(name = "Компактный режим", desc = "Уменьшает отступы и размеры окна для экономии места")
        @ConfigEditorBoolean
        var compactMode = true

        @Expose
        @ConfigOption(name = "Закрывать на ESC", desc = "Закрывает диалог при нажатии клавиши ESC")
        @ConfigEditorBoolean
        var closeOnEsc = true

        @Expose
        @ConfigOption(name = "Тайм-аут (сек)", desc = "Время в секундах, через которое диалог исчезнет автоматически")
        @ConfigEditorSlider(minValue = 1f, maxValue = 60f, minStep = 1f)
        var timeoutSeconds = 30f
    }

    class PetConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Показывает информацию о текущем питомце из таб листа")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Отображает фон в оверлея питомца")
        @ConfigEditorBoolean
        var showBackground = false
    }

    class MuseumConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Отслеживает недостающие предметы в музее и показывает их в HUD")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Фон", desc = "Отображать фон для списка предметов")
        @ConfigEditorBoolean
        var showBackground = true
    }

    class SkillsXpConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Показывает прогресс опыта навыков на экране")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Фон", desc = "Отображать фон HUD")
        @ConfigEditorBoolean
        var showBackground = false

        @Expose
        @ConfigOption(name = "Скрывать ванильный action bar", desc = "Скрывает оригинальную строку +XP в ванильном action bar, только когда СООБЩЕНИЕ ЦЕЛИКОМ является XP-строкой. Комбинированные бары (XP+ХП+мана кастомного сервера) и строки ХП/маны не скрываются.")
        @ConfigEditorBoolean
        var hideActionBar = true

        @Expose
        @ConfigOption(name = "Показывать прогресс-линию", desc = "Рисует полоску прогресса под текстом")
        @ConfigEditorBoolean
        var showProgressBar = true

        @Expose
        @ConfigOption(name = "Показывать ETA", desc = "Показывает оценку времени до след. уровня (Осталось / XP/час)")
        @ConfigEditorBoolean
        var showEta = true

        @Expose
        @ConfigOption(name = "Бой", desc = "Показывать опыт навыка Боя")
        @ConfigEditorBoolean
        var showCombat = true

        @Expose
        @ConfigOption(name = "Шахтерство", desc = "Показывать опыт навыка Шахтерства")
        @ConfigEditorBoolean
        var showMining = true

        @Expose
        @ConfigOption(name = "Фермерство", desc = "Показывать опыт навыка Фермерства")
        @ConfigEditorBoolean
        var showFarming = true

        @Expose
        @ConfigOption(name = "Лесничество", desc = "Показывать опыт навыка Лесничество")
        @ConfigEditorBoolean
        var showForaging = true

        @Expose
        @ConfigOption(name = "Рыболовство", desc = "Показывать опыт навыка Рыболовства")
        @ConfigEditorBoolean
        var showFishing = true

        @Expose
        @ConfigOption(name = "Чародейство", desc = "Показывать опыт навыка Чародейства")
        @ConfigEditorBoolean
        var showEnchanting = true

        @Expose
        @ConfigOption(name = "Алхимия", desc = "Показывать опыт навыка Алхимии")
        @ConfigEditorBoolean
        var showAlchemy = true

        @Expose
        @ConfigOption(name = "Улучшающие зачарования", desc = "Показывает прогресс улучшающих зачарований на предмете в руке (Компактность, Экспертиза, Чемпион, Культивирование)")
        @ConfigEditorBoolean
        var showEnchantmentProgress = true

        @Expose
        @ConfigOption(name = "Цвет названия навыка", desc = "Цвет заголовка")
        @ConfigEditorColour
        var colorSkillNameV2 = "0:255:85:255:85"

        @Expose
        @ConfigOption(name = "Цвет прогресса XP", desc = "Цвет строки опыта и процентов")
        @ConfigEditorColour
        var colorXpV2 = "0:255:255:255:85"

        @Expose
        @ConfigOption(name = "Цвет 'Осталось'", desc = "Цвет строки Осталось / ETA")
        @ConfigEditorColour
        var colorRemainingV2 = "0:255:255:85:85"

        @Expose
        @ConfigOption(name = "Цвет 'XP/час'", desc = "Цвет строки XP/час")
        @ConfigEditorColour
        var colorPerHourV2 = "0:255:85:255:85"

        @Expose
        @ConfigOption(name = "Цвет прогресс-бара", desc = "Цвет заливки прогресс-бара")
        @ConfigEditorColour
        var colorProgressBarV2 = "0:255:85:255:85"
    }

    class EquipmentOverlayConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Показывает экипировку рядом с инвентарём")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Фон", desc = "Цвет фона меню экипировки")
        @ConfigEditorColour
        var backgroundColorV2 = "0:224:29:29:43"

        @Expose
        @ConfigOption(name = "Акцент", desc = "Цвет верхней полоски и рамки меню экипировки")
        @ConfigEditorColour
        var accentColorV2 = "0:255:58:175:217"

        @Expose
        @ConfigOption(name = "Фон слота", desc = "Цвет фона слотов экипировки")
        @ConfigEditorColour
        var slotColorV2 = "0:96:255:255:255"

        @Expose
        @ConfigOption(name = "Подсветка слота", desc = "Цвет при наведении на слот экипировки")
        @ConfigEditorColour
        var slotHoverColorV2 = "0:170:58:175:217"

        @Expose
        var cachedEquipment: List<String> = emptyList()
    }
}
