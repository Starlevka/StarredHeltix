package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DungeonsConfig {
    @Expose
    @ConfigOption(name = "Решатели пазлов", desc = "Автоматические решения для различных пазлов в подземельях.")
    @Accordion
    var solvers = SolversConfig()

    @Expose
    @ConfigOption(name = "Кровавая комната", desc = "Настройки для автоматизации кровавой комнаты.")
    @Accordion
    var bloodRoom = BloodRoomConfig()

    @Expose
    @ConfigOption(name = "Авто-готовность", desc = "Автоматически пишет в пати чат о готовности, находясь за Мортом.")
    @Accordion
    var autoReady = AutoReadyConfig()

    @Expose
    @ConfigOption(name = "Детект смерти §c☠", desc = "Отправляет сообщение в пати чат при нахождении иконки смерти.")
    @Accordion
    var deathCounter = DeathCounterConfig()

    @Expose
    @ConfigOption(name = "Счётчик очков", desc = "Отслеживает очки подземелья и уведомляет при достижении 270.")
    @Accordion
    var scoreCounter = ScoreCounterConfig()

    @Expose
    @ConfigOption(name = "Визуал", desc = "Визуальные улучшения для подземелий.")
    @Accordion
    var visuals = DungeonsVisualsConfig()

    @Expose
    @ConfigOption(name = "Ф4", desc = "Функции для четвёртого этажа подземелий.")
    @Accordion
    var floor4 = Floor4Config()

    class Floor4Config {
        @Expose
        @ConfigOption(name = "Дух медведя - Подсветка", desc = "Подсвечивает моба с ником Дух медведя.")
        @ConfigEditorBoolean
        var bearSpiritHighlight = true

        @Expose
        @ConfigOption(name = "Дух медведя - Текст", desc = "Показывает текст над мобом Дух медведя.")
        @ConfigEditorBoolean
        var bearSpiritText = true

        @Expose
        @ConfigOption(name = "Дух медведя - Цвет", desc = "Цвет подсветки для Духа медведя.")
        @ConfigEditorColour
        var bearSpiritColorV2 = "0:255:255:105:180"

        @Expose
        @ConfigOption(name = "Дух курицы - Алерт", desc = "Показывает Title и звук при обнаружении Духа курицы в радиусе 64 блоков.")
        @ConfigEditorBoolean
        var chickenSpiritAlert = true

        @Expose
        @ConfigOption(name = "Дух курицы - Title", desc = "Показывать Title на экране.")
        @ConfigEditorBoolean
        var chickenTitleEnabled = true

        @Expose
        @ConfigOption(name = "Дух курицы - Заголовок", desc = "Текст заголовка Title.")
        @ConfigEditorText
        var chickenTitleText = "§c§l⚠ ЧИКЕНС КФС ⚠"

        @Expose
        @ConfigOption(name = "Дух курицы - Подзаголовок", desc = "Текст подзаголовка Title.")
        @ConfigEditorText
        var chickenSubtitleText = ""

        @Expose
        @ConfigOption(name = "Дух курицы - Звук", desc = "Воспроизводить предупреждающий звук.")
        @ConfigEditorBoolean
        var chickenSoundEnabled = true
}

    class DungeonsVisualsConfig {
        @Expose
        @ConfigOption(name = "Зеленые сундуки", desc = "Меняет текстуру сундуков в данже на зеленую. §cПонижает ФПС.")
        @ConfigEditorBoolean
        var greenChests = true

        @Expose
        @ConfigOption(name = "Зеленые летучие мыши", desc = "Окрашивает летучих мышей в зеленый цвет для лучшей видимости. §cПонижает ФПС.")
        @ConfigEditorBoolean
        var greenBats = true

        @Expose
        @ConfigOption(name = "Glow звёздных мобов (✯)", desc = "Включает свечение звёздных мобов в данжах по символу ✯ над головой.")
        @ConfigEditorBoolean
        var starredMobGlow = true

        @Expose
        @ConfigOption(name = "Цвет glow (✯)", desc = "Цвет свечения для звёздных мобов.")
        @ConfigEditorColour
        var starredMobGlowColorV2 = "0:255:245:119:56"

    }

    class SolversConfig {
        @Expose
        @ConfigOption(name = "Три незнакомца", desc = "Подсвечивает правильного незнакомца в пазле Three Weirdos.")
        @ConfigEditorBoolean
        var threeWeirdos = true

        @Expose
        @ConfigOption(name = "Крипер-лучи", desc = "Отображает лучи для пазла Creeper Beams. (Нужен лук)")
        @ConfigEditorBoolean
        var creeperBeams = true

        @Expose
        @ConfigOption(name = "Крестики-нолики", desc = "Автоматически решает пазл с крестиками-ноликами и подсвечивает лучший ход.")
        @ConfigEditorBoolean
        var ticTacToe = true
    }

    class BloodRoomConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Активирует оповещение о готовности кровавой комнаты.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Сообщение", desc = "Текст сообщения, который будет отправлен в /pc")
        @ConfigEditorText
        var message = "sᴛᴀʀʀᴇᴅʜᴇʟᴛɪx ✪ Кровавая комната готова!"

        @Expose
        @ConfigOption(name = "Заголовок Title", desc = "Текст Title при готовности кровавой комнаты.")
        @ConfigEditorText
        var titleText = "§cКРОВАВАЯ ГОТОВА"

        @Expose
        @ConfigOption(name = "Подзаголовок Title", desc = "Текст подзаголовка при готовности кровавой комнаты.")
        @ConfigEditorText
        var subtitleText = ""

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Показывать темный фон позади текста.")
        @ConfigEditorBoolean
        var showBackground = false
    }

    class AutoReadyConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Отправка сообщения о готовности, находясь за Мортом")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Сообщение", desc = "Текст сообщения, который будет отправлен в /pc.")
        @ConfigEditorText
        var readyMessage = "sᴛᴀʀʀᴇᴅʜᴇʟᴛɪx ✪ Я готов к подземельям!"
    }

    class DeathCounterConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Отправляет сообщение в пати чат, когда кто-то погибает.")
        @ConfigEditorBoolean
        var deathDetect = true

        @Expose
        @ConfigOption(name = "Сообщение при смерти", desc = "Текст сообщения, который будет отправлен в /pc.")
        @ConfigEditorText
        var deathMessage = "sᴛᴀʀʀᴇᴅʜᴇʟᴛɪx | Кто погиб, тот ЛЛЛ"
    }

    class ScoreCounterConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Активирует HUD и уведомления об очках.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Сообщение в пати", desc = "Текст сообщения для отправки в /pc.")
        @ConfigEditorText
        var message = "sᴛᴀʀʀᴇᴅʜᴇʟᴛɪx ✪ Ранг S ✪ Можно идти к боссу."

        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Показывать темный фон позади текста.")
        @ConfigEditorBoolean
        var showBackground = true

        @Expose
        @ConfigOption(name = "Title 270 очков", desc = "Показывает Title на экране при достижении 270 очков.")
        @ConfigEditorBoolean
        var title270Enabled = true

        @Expose
        @ConfigOption(name = "Заголовок 270", desc = "Текст Title при 270 очках.")
        @ConfigEditorText
        var title270Text = "§a270 ОЧКОВ"

        @Expose
        @ConfigOption(name = "Подзаголовок 270", desc = "Текст подзаголовка при 270 очках.")
        @ConfigEditorText
        var subtitle270Text = "§eРанг S"

        @Expose
        @ConfigOption(name = "Title 300 очков", desc = "Показывает Title на экране при достижении 300 очков.")
        @ConfigEditorBoolean
        var title300Enabled = true

        @Expose
        @ConfigOption(name = "Заголовок 300", desc = "Текст Title при 300 очках.")
        @ConfigEditorText
        var title300Text = "§6300 ОЧКОВ"

        @Expose
        @ConfigOption(name = "Подзаголовок 300", desc = "Текст подзаголовка при 300 очках.")
        @ConfigEditorText
        var subtitle300Text = "§eРанг S+"

        @Expose
        @ConfigOption(name = "Сообщение 300 очков", desc = "Текст сообщения для 300 очков.")
        @ConfigEditorText
        var message300 = "sᴛᴀʀʀᴇᴅʜᴇʟᴛɪx ✪ 300 ОЧКОВ ✪ Ранк S+ gogogo!"
    }
}

