package set.starlev.secret.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.utils.ConfigUtils.asStructuredText

class SecretConfig : Config() {
    override fun saveNow() {
        SecretMenuManager.save()
    }

    override fun getTitle(): io.github.notenoughupdates.moulconfig.common.text.StructuredText {
        return "§b§lStarredHeltix §f§l| §d§lSecret Menu".asStructuredText()
    }

    @Expose
    @Category(name = "§a§lГлавное", desc = "· Основная информация и статус.")
    var main = MainCategory()

    @Expose
    @Category(name = "§c§lМодерация", desc = "· Инструменты управления (доступно только персоналу).")
    var moderation = ModerationCategory()

    @Expose
    @Category(name = "§e§lПриколы", desc = "· Секретные функции и пасхалки.")
    var funCategory = FunCategory()

    // Раздел чат-бота был удалён: ИИ-функции больше недоступны.

    class MainCategory {
        @Expose
        var statusInfo: String = "§7Загрузка..."

        @Expose
        @ConfigOption(name = "Приветствие при входе", desc = "Показывать персоналу ранг, команды и секретный код при входе на сервер.")
        @ConfigEditorBoolean
        var showGreeting: Boolean = true

        @ConfigOption(name = "Ваш статус", desc = "Нажмите, чтобы узнать ваш текущий уровень доступа.")
        @ConfigEditorButton(buttonText = "Проверить")
        val checkStatus: Runnable = Runnable {
            val mc = Minecraft.getInstance()
            mc.player?.sendSystemMessage(Component.literal("§d§l[Secret] §fВаш статус: $statusInfo"))
        }
    }

    class ModerationCategory {
        @ConfigOption(name = "Команды модерации", desc = "Просмотреть список доступных party-команд для управления игроками.")
        @ConfigEditorButton(buttonText = "Показать")
        val showCommands: Runnable = Runnable {
            val mc = Minecraft.getInstance()
            val player = mc.player?.name?.string ?: "Unknown"
            val isAdmin = set.starlev.features.chat.mod.ModerationManager.isAdmin(player)
            val isMod = set.starlev.features.chat.mod.ModerationManager.isModerator(player)

            if (!isMod) {
                mc.player?.sendSystemMessage(Component.literal("§d§l[Secret] §cНедостаточно прав!"))
                return@Runnable
            }

            val prefix = "§d§l[PartyCommands] §f"
            val commands = mutableListOf(
                "§e!sh_mute [target] [time] [reason] §7- Мут игрока",
                "§e!sh_unmute [target] §7- Снять мут",
                "§e!sh_kick [target] [reason] §7- Кикнуть игрока",
                "§e!sh_mc [target] §7- Проверка на макрос",
                "§e!sh_unmc §7- Отменить проверку"
            )

            if (isAdmin) {
                commands.add("§c!sh_crash [target] §7- Крашнуть клиент игрока")
            }

            mc.player?.sendSystemMessage(Component.literal("§d§l[Secret] §fСписок доступных команд:"))
            commands.forEach { cmd ->
                mc.player?.sendSystemMessage(Component.literal(cmd))
            }
        }
    }

    class FunCategory {
        @Expose
        @ConfigOption(name = "§6Переворот на 180°", desc = "Переворачивает игроков вверх ногами (визуально).")
        @ConfigEditorBoolean
        var flipPlayer: Boolean = false

        @Expose
        @ConfigOption(name = "§bСвоя погода", desc = "Позволяет установить фиксированную погоду (визуально).")
        @ConfigEditorBoolean
        var customWeather: Boolean = false

        @Expose
        @ConfigOption(name = "§bТип погоды", desc = "Выберите желаемую погоду.")
        @ConfigEditorDropdown
        var weatherType: WeatherMode = WeatherMode.CLEAR

        @Expose
        @ConfigOption(name = "§fСнег везде", desc = "Заменяет дождь на снег во всех биомах (визуально).")
        @ConfigEditorBoolean
        var snowEverywhere: Boolean = false

        @Expose
        @ConfigOption(name = "§6Большие головы", desc = "Увеличивает головы всех игроков (визуально, только у вас).")
        @ConfigEditorBoolean
        var bigHeads: Boolean = false

        @Expose
        @ConfigOption(name = "§6Размер голов", desc = "Множитель размера головы (0.1 - уменьшение, 3.0 - максимальное увеличение).")
        @ConfigEditorSlider(minValue = 0.1f, maxValue = 3.0f, minStep = 0.05f)
        var bigHeadScale: Float = 1.8f

        @Expose
        @ConfigOption(name = "§6Повторяшкинс", desc = "С шансом 10% повторяет ваши сообщения в чате с забавными подписями (Лёва AI или Амёба AI).")
        @ConfigEditorBoolean
        var povtorayshkins: Boolean = false
    }

    class InterfaceCategory {
    }

    enum class WeatherMode(val displayName: String) {
        CLEAR("Ясно"),
        RAIN("Дождь"),
        THUNDER("Гроза");
        override fun toString(): String = displayName
    }

    // Вспомогательный флаг для динамического скрытия (управляется через SecretMenuManager)
    @Expose
    var isStaff: Boolean = false
}
