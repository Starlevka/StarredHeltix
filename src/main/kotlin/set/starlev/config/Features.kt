package set.starlev.config

import com.google.gson.annotations.Expose
import net.fabricmc.loader.api.FabricLoader
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.Social
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import net.minecraft.util.Util
import set.starlev.StarredHeltix
import set.starlev.config.categories.AboutModConfig
import set.starlev.config.categories.ChatConfig
import set.starlev.config.categories.CombatConfig
import set.starlev.config.categories.DungeonsConfig
import set.starlev.config.categories.EnchantingConfig
import set.starlev.config.categories.FarmingConfig
import set.starlev.config.categories.FishingConfig
import set.starlev.config.categories.ForagingConfig
import set.starlev.config.categories.JSConfig
import set.starlev.config.categories.MiningConfig
import set.starlev.config.categories.SoundConfig
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
        return "§6§lStarredHeltix §7| §6§lv${getModVersion()} §7сделал §cStarlev"
    }

    /**
     * Иконки-ссылки в строке заголовка (встроенная фича MoulConfig: рендерит
     * 16x16 иконки справа от названия, тултип при наведении, клик открывает URL).
     *
     * Наследуемся от Social и переопределяем onClick: базовый Social.forLink
     * дёргает AWT Desktop.getDesktop().browse(), что в клиенте Minecraft
     * ненадёжно. Util.getPlatform().openUri — штатный способ для 26.2.
     */
    override fun getSocials(): MutableList<Social> = mutableListOf(
        UrlSocial("Discord", "textures/gui/social/discord.png", "https://discord.com/invite/pBdty4wquF"),
        UrlSocial("Telegram — Heltix News", "textures/gui/social/telegram.png", "https://t.me/heltixnews"),
        UrlSocial("Telegram — Heltix Network", "textures/gui/social/telegram.png", "https://t.me/heltixnetwork"),
    )

    private class UrlSocial(
        private val label: String,
        iconPath: String,
        private val url: String,
    ) : Social() {
        private val icon = MyResourceLocation("starredheltix", iconPath)

        override fun onClick() = Util.getPlatform().openUri(url)

        override fun getTooltip(): List<StructuredText> = listOf(StructuredText.of(label))

        override fun getIcon(): MyResourceLocation = icon
    }

    private fun getModVersion(): String {
        return FabricLoader.getInstance()
            .getModContainer("starredheltix")
            .map { it.metadata.version.friendlyString }
            .orElse("1.0.0")
    }

    override fun formatCategoryName(category: io.github.notenoughupdates.moulconfig.processor.ProcessedCategory, isSelected: Boolean): io.github.notenoughupdates.moulconfig.common.text.StructuredText {
        if (isSelected) {
            return category.displayName.copyShallow().gold().underlined()
        }
        // Подкатегории (разделы внутри категории) приглушаем — иерархия читается по цвету:
        // родительский раздел светлее, вложенный темнее.
        return if (category.parentCategoryId != null) {
            category.displayName.copyShallow().darkGrey()
        } else {
            category.displayName.copyShallow().grey()
        }
    }

    // «О моде» — первым: порядок полей = порядок категорий в меню.
    @Expose
    @Category(name = "О моде", desc = "· Информация о моде и его обновления.")
    var about = AboutModConfig()

    @Expose
    @Category(name = "Визуал", desc = "· Визуальные эффекты и анимации.")
    var visuals = VisualsConfig()

    @Expose
    @Category(name = "Чат", desc = "· Система чата.")
    var chat = ChatConfig()

    @Expose
    @Category(name = "Оптимизация", desc = "· Производительность и приоритет потоков.")
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
    @Category(name = "✦ Чародейство", desc = "· Функции для стола экспериментов.")
    var enchanting = EnchantingConfig()

    @Expose
    @Category(name = "✦ Шахтёрство", desc = "· Функции для навыка шахтёрства.")
    var mining = MiningConfig()

    @Expose
    @Category(name = "𝄞 Аудио", desc = "· Визуальная музыка и эффекты по локациям.")
    var music = SoundConfig()

@Expose
    @Category(name = "🌠 Модули", desc = "· Настройки JavaScript и автоматизаций")
    var scripting = JSConfig()

    // Одноразовая миграция: строки цветов приведены к порядку пикера MoulConfig
    // (B:G:R:A:Chroma). Без @ConfigOption, поэтому в GUI не появляется.
    @Expose
    var migratedColourOrder = false
}
