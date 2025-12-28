package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

class FarmingConfig {
    @ConfigOption(name = "/sh mouselock", desc = "Команда для фиксации курсора мыши. Полезно для фермерства.")
    @ConfigEditorButton(buttonText = "Инфо")
    val mouseLockInfo: Runnable = Runnable {
        Minecraft.getInstance().player?.displayClientMessage(
            Component.literal("§e[StarredHeltix] §fИспользуйте §6/sh mouselock §fдля фиксации мыши."),
            false
        )
    }

    @ConfigOption(name = "/sh rotation", desc = "Команда для установки точного угла взгляда. Пример: /sh rotation 90 0")
    @ConfigEditorButton(buttonText = "Инфо")
    val rotationInfo: Runnable = Runnable {
        Minecraft.getInstance().player?.displayClientMessage(
            Component.literal("§e[StarredHeltix] §fИспользуйте §6/sh rotation <yaw> <pitch> §fдля точного угла."),
            false
        )
    }
}
