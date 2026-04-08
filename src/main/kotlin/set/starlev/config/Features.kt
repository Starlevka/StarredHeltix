package set.starlev.config

import com.google.gson.annotations.Expose
import net.fabricmc.loader.api.FabricLoader
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Category
import set.starlev.StarredHeltix
import set.starlev.config.categories.AboutModConfig
import set.starlev.config.categories.ChatConfig
import set.starlev.config.categories.CombatConfig
import set.starlev.config.categories.DisplayConfig
import set.starlev.config.categories.DungeonsConfig
import set.starlev.config.categories.FarmingConfig
import set.starlev.config.categories.FishingConfig
import set.starlev.config.categories.ForagingConfig
import set.starlev.config.categories.MiningConfig
import set.starlev.config.categories.MiscConfig
import set.starlev.config.categories.MusicConfig
import set.starlev.config.categories.SlayerConfig
import set.starlev.config.categories.OptimizationConfig
import set.starlev.config.categories.SkyblockConfig
import set.starlev.config.categories.VisualsConfig
import net.minecraft.network.chat.Component
import set.starlev.features.chat.ChatFormatting
import set.starlev.utils.ConfigUtils.toLegacyHex

class Features : Config() {
    override fun saveNow() {
        StarredHeltix.configManager.saveConfig("close-gui")
    }

    override fun getTitle(): io.github.notenoughupdates.moulconfig.common.text.StructuredText {
        return io.github.notenoughupdates.moulconfig.common.text.StructuredText.of(buildTitle())
    }

    private fun buildTitle(): String {
        return "§6§lStarredHeltix §f§l| §6§lv${getModVersion()}"
    }

    private fun getModVersion(): String {
        return FabricLoader.getInstance()
            .getModContainer("starredheltix")
            .map { it.metadata.version.friendlyString }
            .orElse("0.1.0")
    }

    @Expose
    @Category(name = "Основные", desc = "· Общие настройки мода.")
    var misc = MiscConfig()

    @Expose
    @Category(name = "Дисплей", desc = "· Настройки HUD.")
    var display = DisplayConfig()

    @Expose
    @Category(name = "Визуал", desc = "· Визуальные эффекты и анимации.")
    var visuals = VisualsConfig()

    @Expose
    @Category(name = "Чат", desc = "· Система чата.")
    var chat = ChatConfig()

    @Expose
    @Category(name = "Оптимизация", desc = "· Настройки геймплея.")
    var optimization = OptimizationConfig()

    @Expose
    @Category(name = "✦ Скайблок", desc = "· Функции для Skyblock.")
    var skyblock = SkyblockConfig()

    @Expose
    @Category(name = "✦ Бой", desc = "· Функции для навыка боя.")
    var combat = CombatConfig()

    @Expose
    @Category(name = "✧ Слеерство", desc = "· Функции для слеерства.")
    var slayer = SlayerConfig()

    @Expose
    @Category(name = "✧ Подземелья", desc = "· Функции для подземелий.")
    var dungeons = DungeonsConfig()

    @Expose
    @Category(name = "✦ Фермерство", desc = "· Функции для навыка фермерства.")
    var farming = FarmingConfig()

    @Expose
    @Category(name = "✦ Рыболовство", desc = "· Функции для навыка рыболовства.")
    var fishing = FishingConfig()

    @Expose
    @Category(name = "✦ Лесничество", desc = "· Функции для навыка лесничества.")
    var foraging = ForagingConfig()

    @Expose
    @Category(name = "✦ Шахтёрство", desc = "· Функции для навыка шахтёрства.")
    var mining = MiningConfig()

    @Expose
    @Category(name = "𝄞 Аудио", desc = "· Визуальная музыка и эффекты по локациям.")
    var music = MusicConfig()
    
    @Expose
    @Category(name = "О моде", desc = "· Информация о моде и его обновления.")
    var about = AboutModConfig()
}
