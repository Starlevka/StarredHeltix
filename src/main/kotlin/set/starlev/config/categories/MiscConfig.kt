
package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
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

        @Expose
        @ConfigOption(name = "Инфо HUD", desc = "FPS, Ping, CPS и BPS на HUD и в Scoreboard.")
        @Accordion
        var hudStats = HudStatsConfig()

        @Expose
        @ConfigOption(name = "Лог предметов", desc = "Отображает историю изменения предметов в инвентаре.")
        @Accordion
        var inventoryHistory = VisualsConfig.InventoryHistoryConfig()

        @Expose
        @ConfigOption(name = "Кнопки инвентаря", desc = "Кастомные кнопки в инвентаре (как в NEU/Firmament).")
        @Accordion
        var inventoryButtons = InventoryButtonsConfig()

        // EquipmentOverlay перенесён в SkyblockConfig

        @Expose
        var migratedInventoryHistoryToMisc = false
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

    class WaypointsConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Показывает метки (waypoints) в мире.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Парсинг из чата", desc = "Создаёт метки по сообщениям с координатами (Waypoint/Вейпоинт/Метка).")
        @ConfigEditorBoolean
        var parseFromChat = true

        @Expose
        @ConfigOption(name = "Чат → обычные метки", desc = "Создаёт обычные (не временные) метки из чата.")
        @ConfigEditorBoolean
        var chatCreatesRegular = false

        @Expose
        @ConfigOption(name = "Чат → временные метки", desc = "Создаёт временные метки из чата.")
        @ConfigEditorBoolean
        var chatCreatesTemporary = true

        @Expose
        @ConfigOption(name = "Длительность временной (сек)", desc = "Сколько живёт временная метка (по умолчанию).")
        @ConfigEditorSlider(minValue = 5f, maxValue = 100f, minStep = 5f)
        var defaultTemporarySeconds = 30f

        @Expose
        @ConfigOption(name = "Цвет", desc = "Цвет меток.")
        @ConfigEditorColour
        var colorV2 = "0:0:255:255:255"

        @Expose
        @ConfigOption(name = "Бокс", desc = "Рисовать обводку/заливку вокруг блока.")
        @ConfigEditorBoolean
        var showBox = true

        @Expose
        @ConfigOption(name = "Луч", desc = "Рисовать луч маяка.")
        @ConfigEditorBoolean
        var showBeam = true

        @Expose
        @ConfigOption(name = "Текст", desc = "Показывать подпись и дистанцию.")
        @ConfigEditorBoolean
        var showText = true

        @ConfigOption(name = "Открыть меню меток", desc = "Управление списком меток.")
        @ConfigEditorButton(buttonText = "Открыть")
        val openWaypointsMenu: Runnable = Runnable {
            set.starlev.StarredHeltix.screenToOpen = set.starlev.render.WaypointsGui(null)
        }
    }

    @Expose
    @ConfigOption(name = "Waypoints", desc = "Настройки меток в мире.")
    @Accordion
    var waypoints = WaypointsConfig()

    class LoginConfig {
        @Expose
        @ConfigOption(name = "Пароль", desc = "Ваш пароль для команды /вход (замены /login). §cВнимание: В конфиге хранится в открытом виде!")
        @ConfigEditorText
        var password = ""
    }

    @Expose
    var hasShownWelcome013 = false
    
    @Expose
    var hasShownWelcome014 = false

    class HudStatsConfig {
        @Expose
        @ConfigOption(name = "ФПС", desc = "Настройки отображения FPS.")
        @Accordion
        var fps = FpsConfig()

        @Expose
        @ConfigOption(name = "Пинг", desc = "Настройки отображения Ping.")
        @Accordion
        var ping = PingConfig()

        @Expose
        @ConfigOption(name = "КПС", desc = "Настройки отображения кликов в секунду (CPS).")
        @Accordion
        var cps = CpsConfig()

        @Expose
        @ConfigOption(name = "БПС", desc = "Настройки отображения ломаемых блоков в секунду (BPS).")
        @Accordion
        var bps = BpsConfig()

        class FpsConfig {
            @Expose
            @ConfigOption(name = "HUD", desc = "Отображать FPS отдельным HUD элементом.")
            @ConfigEditorBoolean
            var hud = false

            @Expose
            @ConfigOption(name = "Scoreboard", desc = "Добавлять FPS в кастомный Scoreboard.")
            @ConfigEditorBoolean
            var scoreboard = false

            @Expose
            @ConfigOption(name = "Цвет", desc = "Цвет текста.")
            @ConfigEditorColour
            var colorV2 = "0:255:255:255:255"

            @Expose
            @ConfigOption(name = "Фон HUD", desc = "Отображает фон у HUD элемента.")
            @ConfigEditorBoolean
            var showBackground = true
        }

        class PingConfig {
            @Expose
            @ConfigOption(name = "HUD", desc = "Отображать Ping отдельным HUD элементом.")
            @ConfigEditorBoolean
            var hud = false

            @Expose
            @ConfigOption(name = "Scoreboard", desc = "Добавлять Ping в кастомный Scoreboard.")
            @ConfigEditorBoolean
            var scoreboard = false
            @Expose
            @ConfigOption(name = "Цвет", desc = "Цвет текста.")
            @ConfigEditorColour
            var colorV2 = "0:255:255:255:255"

            @Expose
            @ConfigOption(name = "Фон HUD", desc = "Отображает фон у HUD элемента.")
            @ConfigEditorBoolean
            var showBackground = true
        }

        class CpsConfig {
            @Expose
            @ConfigOption(name = "HUD", desc = "Отображать CPS отдельным HUD элементом.")
            @ConfigEditorBoolean
            var hud = false

            @Expose
            @ConfigOption(name = "Scoreboard", desc = "Добавлять CPS в кастомный Scoreboard.")
            @ConfigEditorBoolean
            var scoreboard = false

            @Expose
            @ConfigOption(name = "Цвет", desc = "Цвет текста.")
            @ConfigEditorColour
            var colorV2 = "0:255:255:255:255"

            @Expose
            @ConfigOption(name = "Фон HUD", desc = "Отображает фон у HUD элемента.")
            @ConfigEditorBoolean
            var showBackground = true
        }

        class BpsConfig {
            @Expose
            @ConfigOption(name = "HUD", desc = "Отображать BPS отдельным HUD элементом.")
            @ConfigEditorBoolean
            var hud = false

            @Expose
            @ConfigOption(name = "Scoreboard", desc = "Добавлять BPS в кастомный Scoreboard.")
            @ConfigEditorBoolean
            var scoreboard = false

            @Expose
            @ConfigOption(name = "Цвет", desc = "Цвет текста.")
            @ConfigEditorColour
            var colorV2 = "0:255:255:255:255"

            @Expose
            @ConfigOption(name = "Фон HUD", desc = "Отображает фон у HUD элемента.")
            @ConfigEditorBoolean
            var showBackground = true
        }
    }

    class InventoryButtonsConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Включает кастомные кнопки в инвентаре.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Только инвентарь", desc = "Показывать кнопки только в инвентаре игрока, а не в сундуках.")
        @ConfigEditorBoolean
        var onlyInventory = true

        @ConfigOption(name = "Настроить кнопки", desc = "Открывает меню настройки кнопок инвентаря.")
        @ConfigEditorButton(buttonText = "Открыть")
        val openButtonsMenu: Runnable = Runnable {
            set.starlev.StarredHeltix.screenToOpen = set.starlev.features.inventory.InventoryButtonsGui()
        }
    }
}
