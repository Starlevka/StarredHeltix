package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.features.chat.mod.ModerationManager
import set.starlev.secret.config.SecretMenuManager
import set.starlev.utils.ModUpdater

class AboutModConfig {

    @Expose
    @ConfigOption(name = "Discord RPC", desc = "Rich Presence в Discord (как в SkyHanni)")
    @Accordion
    var discordRPC = DiscordRPCConfig()

    @ConfigOption(name = "§cСекретный код", desc = "§cВведите секретный код для доступа к скрытым функциям")
    @ConfigEditorText
    @Expose
    var secretCode: String = ""

    @ConfigOption(
        name = "§cСекретное меню",
        desc = "§cПерсоналу (модератор и выше) открывается сразу. Остальным — по секретному коду"
    )
    @ConfigEditorButton(buttonText = "Открыть")
    val activateSecret: Runnable = Runnable {
        val me = Minecraft.getInstance().player?.name?.string ?: ""
        // Персоналу код не нужен — ранг проверяется по списку ников.
        if (ModerationManager.isModerator(me)) {
            SecretMenuManager.open()
            return@Runnable
        }
        if (secretCode.trim().equals(ModerationManager.SECRET_CODE, ignoreCase = true)) {
            SecretMenuManager.open()
            secretCode = ""
        } else {
            Minecraft.getInstance().player?.sendSystemMessage(
                Component.literal("§c§cНеверный код.")
            )
        }
    }

    @ConfigOption(name = "О моде", desc = "Выводит текущую версию и данные о разработке в чат")
    @ConfigEditorButton(buttonText = "Инфо")
    val showInfo: Runnable = Runnable {
        val mc = Minecraft.getInstance()
        val version = FabricLoader.getInstance().getModContainer("starredheltix")
            .map { it.metadata.version.friendlyString }
            .orElse("1.0.0")
        mc.player?.sendSystemMessage(Component.literal("§6§lStarredHeltix §b✪✪✪✪✪ §r§fИнформация:"))
        mc.player?.sendSystemMessage(Component.literal("§6Версия мода: §f$version"))
        mc.player?.sendSystemMessage(Component.literal("§6Лицензия мода: §fLGPL-3.0"))
        mc.player?.sendSystemMessage(Component.literal("§6Последня дата обновления версии мода: §f22 сентября 2026"))
        mc.player?.sendSystemMessage(Component.literal("§6✶ Использованный код из модов: §f<3 SkyHanni, Skyblocker, OdinFabric, Skysoft, NoFrills, Firmament, SkyCubed и heltix-mod <3"))
    }

    @ConfigOption(name = "Поддержать", desc = "Копирует номер карты в буфер обмена")
    @ConfigEditorButton(buttonText = "Скопировать номер")
    val copyDonateCard: Runnable = Runnable {
        Minecraft.getInstance().keyboardHandler.setClipboard(DONATE_CARD)
        Minecraft.getInstance().player?.sendSystemMessage(
            Component.literal("§aНомер карты скопирован: §f$DONATE_CARD")
        )
    }

    @ConfigOption(name = "Обновление", desc = "Проверка наличия новых версий мода на GitHub")
    @ConfigEditorButton(buttonText = "Чекнуть")
    val checkUpdates: Runnable = Runnable { ModUpdater.checkUpdate() }

    companion object {
        /** Номер карты для пожертвования — копируется кнопкой выше. */
        const val DONATE_CARD: String = "4100118578875130"
    }
}

class DiscordRPCConfig {
    @Expose
    @ConfigOption(name = "Включено", desc = "Показывает статус StarredHeltix в Discord")
    @ConfigEditorBoolean
    var enabled = false

    @Expose
    @ConfigOption(name = "Показывать локацию", desc = "Показывает где вы находитесь (Хаб, Энд, Данж и т.д.)")
    @ConfigEditorBoolean
    var showLocation = true

    @Expose
    @ConfigOption(name = "Показывать IP", desc = "Показывает starlev.heltix.net в статусе")
    @ConfigEditorBoolean
    var showIP = true

    @Expose
    @ConfigOption(name = "Показывать версию", desc = "Показывает версию мода в details")
    @ConfigEditorBoolean
    var showVersion = true
}
