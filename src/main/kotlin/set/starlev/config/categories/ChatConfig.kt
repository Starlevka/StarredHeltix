package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import set.starlev.StarredHeltix

class ChatConfig {

    @Expose
    @ConfigOption(name = "Функции чата", desc = "Общие настройки для взаимодействия с чатом.")
    @Accordion
    var general = GeneralChatConfig()

    @Expose
    @ConfigOption(name = "Команды пати", desc = "Настройки автоматизации команд для группы (Party).")
    @Accordion
    var party = PartyCommandsConfig()

    class GeneralChatConfig {
        @Accordion
        @Expose
        @ConfigOption(name = "Копирование чата", desc = "Позволяет копировать сообщения из чата при нажатии Shift.")
        var chatCopy = ChatCopyConfig()

        @Accordion
        @Expose
        @ConfigOption(name = "Фильтр сообщений", desc = "Скрывает сообщения, содержащие определенные слова.")
        var messageFilter = MessageFilterConfig()
    }

    class PartyConfig {
        @Accordion
        @Expose
        @ConfigOption(name = "Команды в пати и ЛС", desc = "Позволяет другим игрокам управлять вашей группой через команды.")
        var partyCommands = PartyCommandsConfig()
    }

    class ChatCopyConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Включает функционал копирования.")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "Цвет выделения", desc = "Цвет, которым подсвечивается копируемое сообщение.")
        @ConfigEditorColour
        var highlightColor = "0:255:255:255:255"

        @Expose
        @ConfigOption(name = "Инфо", desc = "Наведите на сообщение с Shift")
        @ConfigEditorText
        var info = "Наведите на сообщение с Shift"
    }

    class MessageFilterConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Включает фильтрацию сообщений. /sh filter")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        var filters = mutableListOf<String>()
    }

    class PartyCommandsConfig {
        @Expose
        @ConfigOption(name = "Включить команды", desc = "Разрешает выполнение команд из чата.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Конвертировать ! команды", desc = "Заменяет '!' на '/' для команд (например, !p -> /p).")
        @ConfigEditorBoolean
        var convertCommands = false

        @Expose
        @ConfigOption(name = "!promote", desc = "Разрешает команду повышения игрока.")
        @ConfigEditorBoolean
        var promote = true

        @Expose
        @ConfigOption(name = "!kick", desc = "Разрешает команду исключения игрока.")
        @ConfigEditorBoolean
        var kick = true

        @Expose
        @ConfigOption(name = "!invite", desc = "Разрешает команду приглашения игрока.")
        @ConfigEditorBoolean
        var invite = true

        @Expose
        @ConfigOption(name = "!ping", desc = "Разрешает команду проверки пинга.")
        @ConfigEditorBoolean
        var ping = true

        @Expose
        @ConfigOption(name = "!fps", desc = "Разрешает команду проверки FPS.")
        @ConfigEditorBoolean
        var fps = true

        @Expose
        @ConfigOption(name = "!time", desc = "Разрешает команду проверки времени.")
        @ConfigEditorBoolean
        var time = true

        @Expose
        @ConfigOption(name = "!coords", desc = "Разрешает команду отправки координат.")
        @ConfigEditorBoolean
        var coords = true

        @Expose
        @ConfigOption(name = "!rng", desc = "Разрешает команду генерации случайного числа.")
        @ConfigEditorBoolean
        var rng = true

        @Expose
        @ConfigOption(name = "!dt", desc = "Разрешает команду напоминания о перерыве.")
        @ConfigEditorBoolean
        var dt = true

        @Expose
        @ConfigOption(name = "!boykisser", desc = "Разрешает команду boykisser :3")
        @ConfigEditorBoolean
        var boykisser = true
    }
}

