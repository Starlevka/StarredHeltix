package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import set.starlev.config.categories.GardenConfig

class FarmingConfig {
    @ConfigOption(name = "/sh mouselock", desc = "Команда для фиксации курсора мыши. Полезно для фермерства")
    @ConfigEditorButton(buttonText = "Инфо")
    val mouseLockInfo: Runnable = Runnable {
        Minecraft.getInstance().player?.sendSystemMessage(Component.literal("§e[StarredHeltix] §fИспользуйте §6/sh mouselock §fдля фиксации мыши."))
    }

    @ConfigOption(name = "/starredheltix rotation", desc = "Команда для установки точного угла взгляда. Пример: /starredheltix rotation 90 0")
    @ConfigEditorButton(buttonText = "Инфо")
    val rotationInfo: Runnable = Runnable {
        Minecraft.getInstance().player?.sendSystemMessage(Component.literal("§e[StarredHeltix] §fИспользуйте §6/sh rotation <yaw> <pitch> §fдля точного угла."))
    }

    @Expose
    @Category(name = "Подсказка Ранчера", desc = "Настройки ускорения скорости для Ботинок Ранчера")
    var rancherSpeed = RancherSpeedConfig()

    @Expose
    @Category(name = "Сад", desc = "Функции Сада")
    var garden = GardenConfig()

    @Expose
    @Category(name = "Прозрачное окно", desc = "See Through Farming: делает окно игры полупрозрачным. В полноэкранном режиме не работает")
    var seeThroughWindow = SeeThroughWindowConfig()

    class SeeThroughWindowConfig {
        @Expose
        @ConfigOption(name = "Непрозрачность, %", desc = "Процент непрозрачности окна, пока включено. 5 — почти видно только игру, 100 — обычное окно")
        @ConfigEditorSlider(minValue = 5f, maxValue = 100f, minStep = 5f)
        var opacity = 80

        @Expose
        @ConfigOption(name = "Клавиша", desc = "Переключает прозрачность окна")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
        var keybind: Int = GLFW.GLFW_KEY_UNKNOWN
    }

    class RancherSpeedConfig {
        @Expose
        @ConfigOption(name = "Включить?", desc = "Показывает GUI поверх таблички и помогает ставить скорость")
        @ConfigEditorBoolean
        var enabled = true

        @Expose
        @ConfigOption(name = "Фон", desc = "Рисовать фон как у других HUD")
        @ConfigEditorBoolean
        var showBackground = true

        @Expose
        @ConfigOption(name = "Авто-подстановка", desc = "При открытии таблички подставляет текущую скорость из ботинок")
        @ConfigEditorBoolean
        var autoFill = true

        @ConfigOption(name = "Редактор пресетов", desc = "Открывает отдельный экран для редактирования скоростей по культурам")
        @ConfigEditorButton(buttonText = "Открыть")
        val openPresetsEditor: Runnable = Runnable {
            set.starlev.features.farming.RancherSpeedHud.openPresetsEditor()
        }

        @ConfigOption(name = "Сброс пресетов", desc = "Сбрасывает пресеты культур на значения по умолчанию")
        @ConfigEditorButton(buttonText = "Сбросить")
        val resetPresets: Runnable = Runnable {
            set.starlev.features.farming.RancherSpeedHud.resetPresetsToDefault()
        }

        @Expose
        var cropPresetsJson: String? = null
    }
}
