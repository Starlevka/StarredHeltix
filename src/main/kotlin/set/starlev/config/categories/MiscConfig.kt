package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
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

    class GeneralConfig {
        @Expose
        @ConfigOption(name = "Авто-спринт", desc = "Автоматически включает бег при движении вперед.")
        @ConfigEditorBoolean
        var autoSprint = false
    }

    class MouseLockConfig {
        @Expose
        @ConfigOption(name = "Фон HUD", desc = "Отображает фон во время блокировки движения мыши")
        @ConfigEditorBoolean
        var showBackground = false
    }

    @Expose
    @ConfigOption(name = "/sh mouselock", desc = "Настройки блокировки мыши.")
    @Accordion
    var mouselock = MouseLockConfig()

    class LoginConfig {
        @Expose
        @ConfigOption(name = "Пароль", desc = "Ваш пароль для команды /вход (замены /login). §cВнимание: В конфиге хранится в открытом виде!")
        @ConfigEditorText
        var password = ""
    }

    @Expose
    var hasShownWelcome013 = false
}
