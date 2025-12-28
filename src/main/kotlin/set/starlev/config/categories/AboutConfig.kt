package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.utils.ModUpdater

class AboutModConfig {

    @ConfigOption(name = "Информация о моде", desc = "Выводит текущую версию и данные о разработке в чат.")
    @ConfigEditorButton(buttonText = "Инфо")
    val showInfo: Runnable = Runnable {
        val mc = Minecraft.getInstance()
        val version = FabricLoader.getInstance().getModContainer("starredheltix")
            .map { it.metadata.version.friendlyString }
            .orElse("0.0.11")
        mc.player?.displayClientMessage(Component.literal("§b§lStarredHeltix §b✪✪✪✪✪ §r§fИнформация:"), false)
        mc.player?.displayClientMessage(Component.literal("§bВерсия мода: §f$version"), false)
        mc.player?.displayClientMessage(Component.literal("§bЛицензия мода: §fLGPL-3.0"), false)
        mc.player?.displayClientMessage(Component.literal("§bПоследня дата обновления версии мода: §f28.12.2025"), false)
        mc.player?.displayClientMessage(Component.literal("§b✶ Использованный код из модов: §f<3 SkyHanni, Skyblocker, OdinFabric, SkyFall, NoFrills, zen и Firmament <3"), false)
    }

    @ConfigOption(name = "Обновление", desc = "Проверка наличия новых версий мода на GitHub.")
    @ConfigEditorButton(buttonText = "Чекнуть")
    val checkUpdates: Runnable = Runnable { ModUpdater.checkUpdate() }
}

