package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

/** Стиль рамки (фона) карты подземелий. */
enum class MapBackgroundStyle(val label: String) {
    VANILLA("Ванильная"),
    DARK("Тёмная"),
    CHECKER("Клетчатая"),
    CUSTOM("Своя картинка");

    override fun toString(): String = label
}

class DungeonsConfig {
    @Expose
    @Category(name = "Решатели пазлов", desc = "Автоматические решения для различных пазлов в подземельях")
    var solvers = SolversConfig()

    @Expose
    @Category(name = "Кровавая комната", desc = "Настройки для автоматизации кровавой комнаты")
    var bloodRoom = BloodRoomConfig()

    @Expose
    @Category(name = "Авто-готовность", desc = "Автоматически пишет в пати чат о готовности, находясь за Мортом")
    var autoReady = AutoReadyConfig()

    @Expose
    @Category(name = "Детект смерти §c☠", desc = "Отправляет сообщение в пати чат при нахождении иконки смерти")
    var deathCounter = DeathCounterConfig()

    @Expose
    @Category(name = "Визуал", desc = "Визуальные улучшения для подземелий")
    var visuals = DungeonsVisualsConfig()

    @Expose
    @Category(name = "Ф4", desc = "Функции для четвёртого этажа подземелий")
    var floor4 = Floor4Config()

    @Expose
    @Category(name = "Оверлей прыжка духа", desc = "Крупные кнопки тиммейтов в меню Прыжок духа + хоткеи 1-4 для телепорта")
    var leap = LeapOverlayConfig()

    class Floor4Config {
        @Expose
        @ConfigOption(name = "Дух медведя - Подсветка", desc = "Подсвечивает моба с ником Дух медведя")
        @ConfigEditorBoolean
        var bearSpiritHighlight = true

        @Expose
        @ConfigOption(name = "Дух медведя - Текст", desc = "Показывает текст над мобом Дух медведя")
        @ConfigEditorBoolean
        var bearSpiritText = true

        @Expose
        @ConfigOption(name = "Дух медведя - Цвет", desc = "Цвет подсветки для Духа медведя")
        @ConfigEditorColour
        var bearSpiritColorV2 = "0:255:255:105:180"

        @Expose
        @ConfigOption(name = "Дух курицы - Алерт", desc = "Показывает предупреждение и звук при обнаружении Духа курицы в радиусе 64 блоков")
        @ConfigEditorBoolean
        var chickenSpiritAlert = true

        @Expose
        @ConfigOption(name = "Дух курицы - Предупреждение", desc = "Показывать предупреждение на экране")
        @ConfigEditorBoolean
        var chickenTitleEnabled = true

        @Expose
        @ConfigOption(name = "Дух курицы - Заголовок", desc = "Текст заголовка предупреждения")
        @ConfigEditorText
        var chickenTitleText = "§c§l⚠ ЧИКЕНС КФС ⚠"

        @Expose
        @ConfigOption(name = "Дух курицы - Подзаголовок", desc = "Текст подзаголовка предупреждения")
        @ConfigEditorText
        var chickenSubtitleText = ""

        @Expose
        @ConfigOption(name = "Дух курицы - Звук", desc = "Воспроизводить предупреждающий звук")
        @ConfigEditorBoolean
        var chickenSoundEnabled = true
}

    class DungeonsVisualsConfig {
        @Expose
        @ConfigOption(name = "Карта подземелий", desc = "Рендер карты из офф-хенда 1в1 как в heltix-моде")
        @Accordion
        var dungeonMap = DungeonMapConfig()

        @Expose
        @ConfigOption(name = "Склепы (HUD)", desc = "Показ строки Склепы: N из Tab-листа в подземелье")
        @Accordion
        var crypts = CryptsCounterConfig()

        @Expose
        @ConfigOption(name = "Счётчик очков", desc = "Очки подземелья (привязан к карте, без отдельного HUD)")
        @Accordion
        var dungeonScore = ScoreCounterConfig()

        @Expose
        @ConfigOption(name = "Зелёные текстуры", desc = "Зелёные текстуры в подземельях (сундуки, мыши). Работают только в данжах")
        @Accordion
        var greenTextures = GreenTexturesConfig()

        @Expose
        @ConfigOption(name = "Звёздные мобы", desc = "Подсветка звёздных мобов в данжах по ✯ над головой")
        @Accordion
        var starredMobs = StarredMobHighlightConfig()

    }

    class GreenTexturesConfig {
        @Expose
        @ConfigOption(name = "Зеленые сундуки", desc = "Меняет текстуру сундуков в данже на зеленую. §cПонижает ФПС")
        @ConfigEditorBoolean
        var greenChests = true

        @Expose
        @ConfigOption(name = "Зеленые летучие мыши", desc = "Окрашивает летучих мышей в зеленый цвет для лучшей видимости. §cПонижает ФПС")
        @ConfigEditorBoolean
        var greenBats = true

        @Expose
        @ConfigOption(name = "Зеленые стены", desc = "Сплошная зелёная заливка потресканного каменного кирпича в данжах. Вкл/вылк подгружает и выгружает подсветку на лету")
        @ConfigEditorBoolean
        var greenWalls = true

        @Expose
        @ConfigOption(name = "Цвет стен", desc = "Цвет заливки стен (по умолчанию — под зелёный сундук)")
        @ConfigEditorColour
        var greenWallsColorV2 = "0:255:43:180:73"
    }

    class StarredMobHighlightConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Подсветка звёздных мобов в данжах по ✯ над головой")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "=> Бокс", desc = "Рисует бокс вокруг сущности (заливка или контур)")
        @ConfigEditorBoolean
        var box = false

        @Expose
        @ConfigOption(name = "=> Обводка", desc = "Будет обводить в виде хитбокса сущность (иначе — заливающий бокс)")
        @ConfigEditorBoolean
        var outline = false

        @Expose
        @ConfigOption(name = "=> Свечение", desc = "Включает ванильное свечение (outline shader) для сущности")
        @ConfigEditorBoolean
        var glow = true

        @Expose
        @ConfigOption(name = "Цвет", desc = "Цвет подсветки звёздных мобов")
        @ConfigEditorColour
        var colorV2 = "0:255:245:119:56"

        @Expose
        @ConfigOption(name = "Прозрачность", desc = "Прозрачность бокса подсветки")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.05f)
        var transparency = 0.65f
    }

    class DungeonMapConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Рендер карты подземелий 1в1 как в heltix-моде (карта из офф-хенда)")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Текстура фона", desc = "Рисовать ванильную текстуру фона карты (textures/map/map_background.png)")
        @ConfigEditorBoolean
        var showBackgroundTexture = true

        @Expose
        @ConfigOption(name = "Фон карты", desc = "Стиль рамки карты. Своя картинка берётся из config/starredheltix/textures/")
        @ConfigEditorDropdown
        var background: MapBackgroundStyle = MapBackgroundStyle.VANILLA

        @Expose
        @ConfigOption(name = "Путь к картинке", desc = "Только PNG (приоритет над папкой конфига). Пусто = map_background.png из config/starredheltix/textures/")
        @ConfigEditorText
        var customImagePath: String = ""

        @Expose
        @ConfigOption(name = "Разметка/маркировки", desc = "Рисовать маркеры игроков/декорации карты поверх")
        @ConfigEditorBoolean
        var showDecorations = true

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Тёмный фон позади карты (HUD-редактор)")
        @ConfigEditorBoolean
        var showBackground = false

        @Expose
        @ConfigOption(name = "Прозрачность фона", desc = "Прозрачность всех фонов карты: HUD-панели, своей картинки и рамки (0-100%)")
        @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 5f)
        var backgroundAlpha = 45f
    }

    class CryptsCounterConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Показывать строку Склепы: N из Tab-листа в подземелье")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Показывать темный фон позади текста")
        @ConfigEditorBoolean
        var showBackground = true
    }

    class SolversConfig {
        @Expose
        @ConfigOption(name = "Три незнакомца", desc = "Подсвечивает правильного незнакомца в пазле Three Weirdos")
        @ConfigEditorBoolean
        var threeWeirdos = true

        @Expose
        @ConfigOption(name = "Крипер-лучи", desc = "Отображает лучи для пазла Creeper Beams. (Нужен лук)")
        @ConfigEditorBoolean
        var creeperBeams = true

        @Expose
        @ConfigOption(name = "Крестики-нолики", desc = "Автоматически решает пазл с крестиками-ноликами и подсвечивает лучший ход")
        @ConfigEditorBoolean
        var ticTacToe = true

        @Expose
        @ConfigOption(name = "Водяной", desc = "Решатель для водяного пазла")
        @Accordion
        var waterboard = WaterboardConfig()
    }

    class LeapOverlayConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Заменяет мелкие головы в меню липа крупными кнопками с именами и хоткеями")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Заголовки меню липа", desc = "Через запятую, без регистра (contains). Дефолт покрывает EN/RU варианты названия")
        @ConfigEditorText
        var menuTitles = "spirit leap,прыжок дух,прыжок"

        @Expose
        @ConfigOption(name = "Цвет рамки", desc = "Обводка кнопок (и подсветка наведения)")
        @ConfigEditorColour
        var accentColorV2 = "0:255:255:85:255"

        @Expose
        @ConfigOption(name = "Размер текста", desc = "Масштаб имён и подписей хоткеев")
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 4f, minStep = 0.1f)
        var scale = 1.6f

        @Expose
        @ConfigOption(name = "Сообщение в пати при липе", desc = "Текст, отправляемый в /pc после телепорта; %name% — имя тиммейта. Пусто = не отправлять")
        @ConfigEditorText
        var leapMessage = ""
    }

    class WaterboardConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Подсветка рычагов и таймеры для Водяного")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Задержка стечения воды (сек)", desc = "Время после включения воды до начала последовательности (≈13с на Heltix)")
        @ConfigEditorSlider(minValue = 5f, maxValue = 25f, minStep = 0.5f)
        var waterFlowDelaySec = 13f

        @Expose
        @ConfigOption(name = "Порядок рычагов", desc = "Порядок нажатия рычагов после воды (через запятую). Дефолт: изумруд>алмаз>золото>кварц")
        @ConfigEditorText
        var leverOrder = "EMERALD,DIAMOND,GOLD,QUARTZ"

        @Expose
        @ConfigOption(name = "Окно финального тайминга — открытие (сек)", desc = "Через сколько секунд после последнего рычага открывается окно для финального нажатия изумрудного рычага")
        @ConfigEditorSlider(minValue = 0f, maxValue = 10f, minStep = 0.25f)
        var finalWindowOpenSec = 2f

        @Expose
        @ConfigOption(name = "Окно финального тайминга — длительность (сек)", desc = "Длительность окна, когда нужно нажать финальный изумрудный рычаг (вода в центре)")
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 10f, minStep = 0.25f)
        var finalWindowDurationSec = 3f

        @Expose
        @ConfigOption(name = "Показывать таймер", desc = "Показывать обратный отсчёт над рычагами и в центре")
        @ConfigEditorBoolean
        var showTimer = true

        @Expose
        @ConfigOption(name = "Подсветка сквозь стены", desc = "Коробки рычагов видны сквозь стены (ESP)")
        @ConfigEditorBoolean
        var seeThrough = false

        @Expose
        @ConfigOption(name = "Цвет подсветки активного рычага", desc = "Формат: 0:А:R:G:B")
        @ConfigEditorColour
        var activeColorV2 = "0:255:40:90:40"

        @Expose
        @ConfigOption(name = "Цвет подсветки ожидания", desc = "Когда ждём стечения воды")
        @ConfigEditorColour
        var waitingColorV2 = "0:255:90:40:40"
    }

    class BloodRoomConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Активирует оповещение о готовности кровавой комнаты")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Сообщение", desc = "Текст сообщения, который будет отправлен в /pc")
        @ConfigEditorText
        var message = "sᴛᴀʀʀᴇᴅʜᴇʟᴛɪx ✪ Кровавая комната готова!"

        @Expose
        @ConfigOption(name = "Заголовок Title", desc = "Текст Title при готовности кровавой комнаты")
        @ConfigEditorText
        var titleText = "§cКРОВАВАЯ ГОТОВА"

        @Expose
        @ConfigOption(name = "Подзаголовок Title", desc = "Текст подзаголовка при готовности кровавой комнаты")
        @ConfigEditorText
        var subtitleText = ""

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Показывать темный фон позади текста")
        @ConfigEditorBoolean
        var showBackground = false
    }

    class AutoReadyConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Отправка сообщения о готовности, находясь за Мортом")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Сообщение", desc = "Текст сообщения, который будет отправлен в /pc")
        @ConfigEditorText
        var readyMessage = "sᴛᴀʀʀᴇᴅʜᴇʟᴛɪx ✪ Я готов к подземельям!"
    }

    class DeathCounterConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Отправляет сообщение в пати чат, когда кто-то погибает")
        @ConfigEditorBoolean
        var deathDetect = true

        @Expose
        @ConfigOption(name = "Сообщение при смерти", desc = "Текст сообщения, который будет отправлен в /pc")
        @ConfigEditorText
        var deathMessage = "sᴛᴀʀʀᴇᴅʜᴇʟᴛɪx | Кто погиб, тот ЛЛЛ"
    }

    class ScoreCounterConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Активирует HUD и уведомления об очках")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Сообщение в пати", desc = "Текст сообщения для отправки в /pc")
        @ConfigEditorText
        var message = "sᴛᴀʀʀᴇᴅʜᴇʟᴛɪx ✪ Ранг S ✪ Можно идти к боссу."

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Показывать темный фон позади текста")
        @ConfigEditorBoolean
        var showBackground = true

        @Expose
        @ConfigOption(name = "Надпись 270 очков", desc = "Показывает надпись на экране при достижении 270 очков")
        @ConfigEditorBoolean
        var title270Enabled = true

        @Expose
        @ConfigOption(name = "Заголовок (270)", desc = "Текст надпись при 270 очках")
        @ConfigEditorText
        var title270Text = "§a270 ОЧКОВ"

        @Expose
        @ConfigOption(name = "Подзаголовок (270)", desc = "Текст подзаголовка при 270 очках")
        @ConfigEditorText
        var subtitle270Text = "§eРанг S"

        @Expose
        @ConfigOption(name = "Надпись 300 очков", desc = "Показывает надпись на экране при достижении 300 очков")
        @ConfigEditorBoolean
        var title300Enabled = true

        @Expose
        @ConfigOption(name = "Заголовок (300)", desc = "Текст надпись при 300 очках")
        @ConfigEditorText
        var title300Text = "§6300 ОЧКОВ"

        @Expose
        @ConfigOption(name = "Подзаголовок (300)", desc = "Текст подзаголовка при 300 очках")
        @ConfigEditorText
        var subtitle300Text = "§eРанг S+"

        @Expose
        @ConfigOption(name = "Сообщение (300)", desc = "Текст сообщения в пати чат при 300 очках")
        @ConfigEditorText
        var message300 = "sᴛᴀʀʀᴇᴅʜᴇʟᴛɪx ✪ 300 ОЧКОВ ✪ Ранк S+ gogogo!"
    }
}

