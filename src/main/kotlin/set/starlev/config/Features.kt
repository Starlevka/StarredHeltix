package set.starlev.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Category
import set.starlev.StarredHeltix
import set.starlev.config.categories.AboutModConfig
import set.starlev.config.categories.ChatConfig
import set.starlev.config.categories.CombatConfig
import set.starlev.config.categories.DisplayConfig
import set.starlev.config.categories.DungeonsConfig
import set.starlev.config.categories.FishingConfig
import set.starlev.config.categories.ForagingConfig
import set.starlev.config.categories.MiningConfig
import set.starlev.config.categories.MiscConfig
import set.starlev.config.categories.OptimizationConfig
import set.starlev.config.categories.VisualsConfig
import set.starlev.utils.ConfigUtils.asStructuredText

class Features : Config() {
    override fun saveNow() {
        StarredHeltix.configManager.saveConfig("close-gui")
    }

    override fun getTitle() = "§6§lStarredHeltix §r§6v0.0.10".asStructuredText()

    @Expose
    @Category(name = "Разное", desc = "· Разные настройки")
    var misc = MiscConfig()

    @Expose
    @Category(name = "Дисплей", desc = "· Настройки HUD'а")
    var display = DisplayConfig()

    @Expose
    @Category(name = "Визуал", desc = "· Визуальные настройки")
    var visuals = VisualsConfig()

    @Expose
    @Category(name = "Чат", desc = "· Настройки чата")
    var chat = ChatConfig()

    @Expose
    @Category(name = "Оптимизация", desc = "· Настройки оптимизации")
    var optimization = OptimizationConfig()

    @Expose
    @Category(name = "✦ Бой", desc = "· Настройки боя")
    var combat = CombatConfig()

    @Expose
    @Category(name = "✧ Подземелья", desc = "· Настройки для подземелий")
    var dungeons = DungeonsConfig()

    @Expose
    @Category(name = "✦ Рыболовство", desc = "· Настройки рыболовства")
    var fishing = FishingConfig()

    @Expose
    @Category(name = "✦ Лесорубство", desc = "· Настройки лесорубства")
    var foraging = ForagingConfig()

    @Expose
    @Category(name = "✦ Шахтёрство", desc = "· Настройки шахтёрства")
    var mining = MiningConfig()

    @Expose
    @Category(name = "О моде =D", desc = "· Информация о моде")
    var about = AboutModConfig()
}
