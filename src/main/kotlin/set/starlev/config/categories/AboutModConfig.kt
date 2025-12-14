package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.utils.ModUpdater

class AboutModConfig {

    @Expose(serialize = false)
    @ConfigOption(name = "Информация о моде (в чате)", desc = "Показать информацию о моде в чате")
    @ConfigEditorButton(buttonText = "Показать")
    val showInfo: Runnable = Runnable {
        val mc = Minecraft.getInstance()
        val version = FabricLoader.getInstance().getModContainer("starredheltix")
            .map { it.metadata.version.friendlyString }
            .orElse("0.0.10")
        mc.player?.displayClientMessage(Component.literal("§6§lStarredHeltix ✪✪✪✪✪ §r§6Информация:"), false)
        mc.player?.displayClientMessage(Component.literal("§eВерсия мода: §f$version"), false)
        mc.player?.displayClientMessage(Component.literal("§eЛицензия мода: §fLGPL-3.0"), false)
        mc.player?.displayClientMessage(Component.literal("§eПоследня дата обновления версии мода: §f11.12.2025"), false)
        mc.player?.displayClientMessage(Component.literal("§e✶ Использованный код из модов: §f<3 SkyHanni, Skyblocker, OdinFabric, SkyFall, NoFrills, zen и Firmament <3"), false)
    }

    @Expose(serialize = false)
    @ConfigOption(name = "Проверить обновления", desc = "Проверить наличие новых версий мода")
    @ConfigEditorButton(buttonText = "Проверить")
    val checkUpdates: Runnable = Runnable { ModUpdater.checkUpdate() }
}