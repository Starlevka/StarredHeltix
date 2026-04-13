package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.secret.config.SecretMenuManager
import set.starlev.utils.ModUpdater

class AboutModConfig {

    @ConfigOption(name = "§cСекретный код", desc = "§cВведите секретный код для доступа к скрытым функциям.")
    @ConfigEditorText
    @Expose
    var secretCode: String = ""

    @ConfigOption(name = "§cАктивация", desc = "§cНажмите для проверки введенного кода.")
    @ConfigEditorButton(buttonText = "Ввод")
    val activateSecret: Runnable = Runnable {
        if (secretCode.lowercase().trim() == "starl") {
            SecretMenuManager.open()
            secretCode = ""
        }
    }

    @ConfigOption(name = "О моде", desc = "Выводит текущую версию и данные о разработке в чат.")
    @ConfigEditorButton(buttonText = "Инфо")
    val showInfo: Runnable = Runnable {
        val mc = Minecraft.getInstance()
        val version = FabricLoader.getInstance().getModContainer("starredheltix")
            .map { it.metadata.version.friendlyString }
            .orElse("0.1.1")
        mc.player?.displayClientMessage(Component.literal("§6§lStarredHeltix §b✪✪✪✪✪ §r§fИнформация:"), false)
        mc.player?.displayClientMessage(Component.literal("§6Версия мода: §f$version"), false)
        mc.player?.displayClientMessage(Component.literal("§6Лицензия мода: §fLGPL-3.0"), false)
        mc.player?.displayClientMessage(Component.literal("§6Последня дата обновления версии мода: §f09.04.2026"), false)
        mc.player?.displayClientMessage(Component.literal("§6✶ Использованный код из модов: §f<3 SkyHanni, Skyblocker, OdinFabric, SkyFall, NoFrills, Firmament и SkyCubed <3"), false)
    }

    @ConfigOption(name = "Обновление", desc = "Проверка наличия новых версий мода на GitHub.")
    @ConfigEditorButton(buttonText = "Чекнуть")
    val checkUpdates: Runnable = Runnable { ModUpdater.checkUpdate() }
}

