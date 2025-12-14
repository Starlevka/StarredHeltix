package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MiscConfig {
    @Accordion
    @Expose
    @ConfigOption(name = "Авто-спринт", desc = "Автоматический спринт при движении вперёд")
    var autoSprint = AutoSprintConfig()

    @Accordion
    @Expose
    @ConfigOption(name = "Напоминание о голосовании", desc = "Напоминание голосовать за сервер")
    var votingReminder = VotingReminderConfig()

    @Accordion
    @Expose
    @ConfigOption(name = "Вход по команде (/вход)", desc = "Настройки для команды входа в систему")
    var loginCommand = LoginCommandConfig()

    @Accordion
    @Expose
    @ConfigOption(name = "Кастомные бинды", desc = "Привязка команд и сообщений к клавишам")
    var customBinds = CustomBindsConfig()

    @Expose
    var welcomeMessage: String = "1"

    class AutoSprintConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Включить автоматический спринт")
        @ConfigEditorBoolean
        var enabled = false
    }

    class CustomBindsConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Включить кастомные бинды. Используйте /sh binds")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        var binds = mutableMapOf<String, String>()

        @Expose
        var keys = mutableMapOf<String, Int>()
    }

    class VotingReminderConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Включить напоминание о голосовании")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        var hasVotedToday = false

        @Expose
        var hasShownReminderToday = false

        @Expose
        var lastCheckTime = System.currentTimeMillis()
    }

    class LoginCommandConfig {
        @Expose
        @ConfigOption(name = "Пароль :>", desc = "Пароль для команды /вход")
        @ConfigEditorText
        var password = ""
    }

}
