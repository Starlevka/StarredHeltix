package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import set.starlev.StarredHeltix

class ChatConfig {

    @Expose
    @Category(name = "Кастомные бинды", desc = "Настройки пользовательских горячих клавиш (/bind)")
    var binds = BindsConfig()

    @Expose
    @Category(name = "Команды пати", desc = "Настройки автоматизации команд для группы (Party)")
    var party = PartyCommandsConfig()

    @Expose
    @Category(name = "Фильтр сообщений", desc = "Скрывает сообщения, содержащие определенные слова")
    var messageFilter = MessageFilterConfig()

    @Expose
    @Category(name = "Алиасы команд", desc = "Свои сокращения для серверных команд: /dh → /warp dungeon_hub (/sh aliases)")
    var aliases = AliasesConfig()

    @Expose
    @Category(name = "Уведомления", desc = "Свои уведомления по тексту чата: ванильные титры и звук (/sh notifiers)")
    var notifiers = NotifiersConfig()

    class NotifiersConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Показывать заголовок и звук, когда в чате совпало правило")
        @ConfigEditorBoolean
        var enabled = true

        @ConfigOption(name = "Меню уведомлений", desc = "Добавить правила, настроить заголовок, звук и длительность")
        @ConfigEditorButton(buttonText = "Открыть")
        val openNotifiersMenu = Runnable {
            set.starlev.StarredHeltix.screenToOpen = set.starlev.render.NotifierGui(null)
        }

        @Expose
        var rules = mutableListOf<set.starlev.features.chat.NotifierRule>()
    }

    class AliasesConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Включает переписывание команд по алиасам")
        @ConfigEditorBoolean
        var enabled = false

        @ConfigOption(name = "Меню алиасов", desc = "Добавить и удалить алиасы через удобное меню")
        @ConfigEditorButton(buttonText = "Открыть")
        val openAliasesMenu = Runnable {
            set.starlev.StarredHeltix.screenToOpen = set.starlev.render.AliasesGui(null)
        }

        @Expose
        var aliasesMap = mutableMapOf<String, String>()
    }

    class BindsConfig {
        @Expose
        @ConfigOption(name = "Бинды", desc = "Включает поддержку пользовательских горячих клавиш (/bind)")
        @ConfigEditorBoolean
        var enabled = true

        @ConfigOption(name = "Меню биндов", desc = "Настроить пользовательские клавиши через удобное меню")
        @ConfigEditorButton(buttonText = "Открыть")
        val openBindsMenu = Runnable {
            set.starlev.StarredHeltix.screenToOpen = set.starlev.render.BindsGui(null)
        }

        @Expose
        var customBindsMap = mutableMapOf<String, String>()

        @Expose
        var customBindsKeys = mutableMapOf<String, Int>()
    }

    class PartyConfig {
        @Accordion
        @Expose
        @ConfigOption(name = "Команды в пати и ЛС", desc = "Позволяет другим игрокам управлять вашей группой через команды")
        var partyCommands = PartyCommandsConfig()
    }

    class MessageFilterConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Включает фильтрацию сообщений. /sh filter")
        @ConfigEditorBoolean
        var enabled = false

        @ConfigOption(name = "Открыть меню фильтров", desc = "Настроить слова для фильтрации через удобное меню")
        @ConfigEditorButton(buttonText = "Открыть")
        val openFilterMenu = Runnable {
            net.minecraft.client.Minecraft.getInstance().setScreenAndShow(set.starlev.render.FilterGui(null))
        }

        @Expose
        var filters = mutableListOf<String>()
    }

    class PartyCommandsConfig {
        @Expose
        @ConfigOption(name = "Пати команды", desc = "Разрешает выполнение команд из чата")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Звук пати/ЛС", desc = "Проигрывать звук при новом сообщении из пати или ЛС")
        @ConfigEditorBoolean
        var messageSound = true

        @Expose
        @ConfigOption(name = "Конвертировать ! команды", desc = "Заменяет '!' на '/' для команд (например, !p -> /p)")
        @ConfigEditorBoolean
        var convertCommands = false

        @Expose
        @ConfigOption(name = "!promote", desc = "Разрешает команду повышения игрока")
        @ConfigEditorBoolean
        var promote = true

        @Expose
        @ConfigOption(name = "!kick", desc = "Разрешает команду исключения игрока")
        @ConfigEditorBoolean
        var kick = true

        @Expose
        @ConfigOption(name = "!invite", desc = "Разрешает команду приглашения игрока")
        @ConfigEditorBoolean
        var invite = true

        @Expose
        @ConfigOption(name = "!ping", desc = "Разрешает команду проверки пинга")
        @ConfigEditorBoolean
        var ping = true

        @Expose
        @ConfigOption(name = "!fps", desc = "Разрешает команду проверки FPS")
        @ConfigEditorBoolean
        var fps = true

        @Expose
        @ConfigOption(name = "!time", desc = "Разрешает команду проверки времени")
        @ConfigEditorBoolean
        var time = true

        @Expose
        @ConfigOption(name = "!coords", desc = "Разрешает команду отправки координат")
        @ConfigEditorBoolean
        var coords = true

        @Expose
        @ConfigOption(name = "!rng", desc = "Разрешает команду генерации случайного числа")
        @ConfigEditorBoolean
        var rng = true

        @Expose
        @ConfigOption(name = "!dt", desc = "Разрешает команду напоминания о перерыве")
        @ConfigEditorBoolean
        var dt = true

        @Expose
        @ConfigOption(name = "!boykisser", desc = "Разрешает команду boykisser :3")
        @ConfigEditorBoolean
        var boykisser = true
    }
}

