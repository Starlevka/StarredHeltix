package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
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
    @ConfigOption(name = "Детект смерти ☠", desc = "Отправляет сообщение в пати чат при нахождении иконки смерти.")
    @Accordion
    var deathCounter = DeathCounterConfig()

    class SolversConfig {
        @Expose
        @ConfigOption(name = "Три незнакомца", desc = "Подсвечивает правильного незнакомца в пазле Three Weirdos.")
        @ConfigEditorBoolean
        var threeWeirdos = true

        @Expose
        @ConfigOption(name = "Крестики-нолики", desc = "Помогает в решении пазла Tic Tac Toe.")
        @ConfigEditorBoolean
        var ticTacToe = true

        @Expose
        @ConfigOption(name = "Крипер-лучи", desc = "Отображает лучи для пазла Creeper Beams. (Нужен лук)")
        @ConfigEditorBoolean
        var creeperBeams = true
    }

    class BloodRoomConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Активирует оповещение о готовности кровавой комнаты.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Сообщение", desc = "Текст сообщения, отправляемого при готовности комнаты.")
        @ConfigEditorText
        var message = "starreдheltix ✪ Кровавая комната готова!"
    }

    class AutoReadyConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Включает автоматическую отправку сообщения о готовности.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Сообщение", desc = "Текст сообщения, который будет отправлен.")
        @ConfigEditorText
        var readyMessage = "starreдheltix ✪ Я готов к подземельям!"
    }

    class DeathCounterConfig {
        @Expose
        @ConfigOption(name = "Детект смерти ☠", desc = "Отправляет сообщение в пати чат при нахождении иконки смерти.")
        @ConfigEditorBoolean
        var deathDetect = true

        @Expose
        @ConfigOption(name = "Сообщение при смерти", desc = "Текст сообщения, который будет отправлен в /pc.")
        @ConfigEditorText
        var deathMessage = "Кто погиб, тот ЛЛЛ"
    }
}

