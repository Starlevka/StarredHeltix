package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MiscConfig {
    @Expose
    @ConfigOption(name = "Общее", desc = "Общие функции для удобства игры.")
    @Accordion
    var general = GeneralConfig()

    @Expose
    @ConfigOption(name = "/вход", desc = "Настройки для быстрого входа в систему через команду /вход.")
    @Accordion
    var autoLogin = LoginConfig()

    @Expose
    @ConfigOption(name = "§b§lНовый год!", desc = "§fНовогодние фишки мода!")
    @Accordion
    var newYear = NewYearConfig()

    class GeneralConfig {
        @Expose
        @ConfigOption(name = "Авто-спринт", desc = "Автоматически включает бег при движении вперед.")
        @ConfigEditorBoolean
        var autoSprint = false

        @Expose
        @ConfigOption(name = "Напоминание о голосовании", desc = "Напоминает о возможности проголосовать за сервер один раз в день.")
        @ConfigEditorBoolean
        var votingReminder = true

        @Expose
        @ConfigOption(name = "Кастомные бинды", desc = "Включает поддержку пользовательских горячих клавиш (/bind).")
        @ConfigEditorBoolean
        var customBinds = true
    }

    class LoginConfig {
        @Expose
        @ConfigOption(name = "Пароль", desc = "Ваш пароль для команды /вход (замены /login). §cВнимание: В конфиге хранится в открытом виде!")
        @ConfigEditorText
        var password = ""
    }

    class NewYearConfig {
        @Expose
        @ConfigOption(name = "§fЗвуковой диалог NPC", desc = "Включает озвучку диалога с Мега-Ящиком.")
        @ConfigEditorBoolean
        var npcDialogueWithSound = true

        @Expose
        @ConfigOption(name = "§fЗимняя атмосфера", desc = "Включает зимний биом и визуальный дождь (снег) в обычном мире.")
        @ConfigEditorBoolean
        var winterAtmosphere = true

        @ConfigOption(name = "§fСбросить диалог NPC", desc = "Сбрасывает прогресс диалога с Мега-Ящиком, позволяя поговорить с ним снова.")
        @io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton(buttonText = "Сбросить")
        val resetNPCDialogue: Runnable = Runnable {
            hasTalkedToNPC = false
            set.starlev.features.visual.GhostNPCHandler.resetDialogue()
            net.minecraft.client.Minecraft.getInstance().player?.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§e[StarredHeltix] §fДиалог с NPC сброшен!"),
                false
            )
        }

        @Expose
        var ghostNPC = true

        @Expose
        var secretFrame = true

        @Expose
        var secretFrame2 = true

        @Expose
        var hasTalkedToNPC = false
    }

    @Expose
    var hasVotedToday = false

    @Expose
    var hasShownWelcome = false

    @Expose
    var hasShownReminderToday = false

    @Expose
    var lastCheckTime = System.currentTimeMillis()

    @Expose
    var customBindsMap = mutableMapOf<String, String>()

    @Expose
    var customBindsKeys = mutableMapOf<String, Int>()
}
