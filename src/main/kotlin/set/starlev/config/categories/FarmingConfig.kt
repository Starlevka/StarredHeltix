package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
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

    @Expose
    @ConfigOption(name = "Скорость Ранчеров", desc = "Оверлей для удобной установки скорости Rancher's Boots.")
    @Accordion
    var rancherSpeed = RancherSpeedConfig()

    class RancherSpeedConfig {
        @Expose
        @ConfigOption(name = "Включить", desc = "Показывает GUI поверх таблички и помогает ставить скорость.")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Фон", desc = "Рисовать фон как у других HUD.")
        @ConfigEditorBoolean
        var showBackground = true

        @Expose
        @ConfigOption(name = "Авто-подстановка", desc = "При открытии таблички подставляет текущую скорость из ботинок.")
        @ConfigEditorBoolean
        var autoFill = true

        @Expose
        @ConfigOption(name = "Пресеты", desc = "Список скоростей через запятую. Пример: 100,200,300")
        @ConfigEditorText
        var presets = "100,200,300"

        @ConfigOption(name = "Редактор пресетов", desc = "Открывает отдельный экран для редактирования скоростей по культурам.")
        @ConfigEditorButton(buttonText = "Открыть")
        val openPresetsEditor: Runnable = Runnable {
            set.starlev.features.farming.RancherSpeedHud.openPresetsEditor()
        }

        @ConfigOption(name = "Сброс пресетов", desc = "Сбрасывает пресеты культур на значения по умолчанию.")
        @ConfigEditorButton(buttonText = "Сбросить")
        val resetPresets: Runnable = Runnable {
            set.starlev.features.farming.RancherSpeedHud.resetPresetsToDefault()
        }

        @Expose
        var cropPresetsJson: String? = null
    }
}
