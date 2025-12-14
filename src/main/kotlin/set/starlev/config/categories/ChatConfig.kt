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
    @Accordion
    @Expose
    @ConfigOption(name = "Копирование чата", desc = "Копирование сообщений из чата")
    var chatCopy = ChatCopyConfig()

    @Accordion
    @Expose
    @ConfigOption(name = "Пати и ЛС команды", desc = "Настройки команд в пати и личных сообщениях")
    var partyCommands = PartyCommandsConfig()

    @Accordion
    @Expose
    @ConfigOption(name = "Авто-готовность", desc = "Автоматическое уведомление о готовности")
    var autoReady = AutoReadyConfig()

    @Accordion
    @Expose
    @ConfigOption(name = "Фильтр сообщений", desc = "(Используйте /sh filter ...)")
    var messageFilter = MessageFilterConfig()

    class ChatCopyConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Включает функцию копирования сообщений")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Цвет обводки (Не робит)", desc = "Цвет рамки при наведении (Shift)")
        @ConfigEditorColour
        var highlightColor = "0:255:255:255:255"

        @Expose
        @ConfigOption(name = "лЛл", desc = "Shift + ЛКМ: Скопировать последнее сообщение")
        @ConfigEditorText
        var info = "Наведите на сообщение с Shift"
    }

    class AutoReadyConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Включить авто-готовность")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "Сообщение", desc = "Сообщение о готовности")
        @ConfigEditorText
        var readyMessage = "starreдheltix ✪ Я готов к подземельям!"
    }

    class MessageFilterConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Включить фильтрацию")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        var filters = mutableListOf<String>()
    }

    class PartyCommandsConfig {
        @Expose
        @ConfigOption(name = "Включить команды", desc = "Включить обработку команд в пати и ЛС")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Конвертировать ! команды", desc = "Конвертировать сообщения начинающиеся с ! в партийные команды")
        @ConfigEditorBoolean
        var convertCommands = false

        @Expose
        @ConfigOption(name = "!promote", desc = "Повысить игрока в пати")
        @ConfigEditorBoolean
        var promote = true

        @Expose
        @ConfigOption(name = "!kick", desc = "Кикнуть игрока из пати")
        @ConfigEditorBoolean
        var kick = true

        @Expose
        @ConfigOption(name = "!invite", desc = "Пригласить игрока в пати (через ЛС и Пати чаты)")
        @ConfigEditorBoolean
        var invite = true

        @Expose
        @ConfigOption(name = "!ping", desc = "Показать пинг")
        @ConfigEditorBoolean
        var ping = true

        @Expose
        @ConfigOption(name = "!fps", desc = "Показать FPS")
        @ConfigEditorBoolean
        var fps = true

        @Expose
        @ConfigOption(name = "!time", desc = "Показать время")
        @ConfigEditorBoolean
        var time = true

        @Expose
        @ConfigOption(name = "!coords", desc = "Показать координаты")
        @ConfigEditorBoolean
        var coords = true

        @Expose
        @ConfigOption(name = "!rng", desc = "Случайное число")
        @ConfigEditorBoolean
        var rng = true

        @Expose
        @ConfigOption(name = "!dt", desc = "Сообщение о перерыве")
        @ConfigEditorBoolean
        var dt = true

        @Expose
        @ConfigOption(name = "!boykisser", desc = "бойкиссер артик :P")
        @ConfigEditorBoolean
        var boykisser = true
    }
}
