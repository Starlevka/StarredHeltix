package set.starlev.secret.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.secret.features.ai.AiPersona
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

    @Expose
    @Category(name = "§6§lИнтерфейс", desc = "· Настройка элементов интерфейса.")
    var interfaceCategory = InterfaceCategory()

    @Expose
    @Category(name = "§b§lЧат-бот", desc = "· Настройки автоответчика и ИИ.")
    var chatBot = ChatBotCategory()

    class MainCategory {
        @Expose
        var statusInfo: String = "§7Загрузка..."

        @ConfigOption(name = "Ваш статус", desc = "Нажмите, чтобы узнать ваш текущий уровень доступа.")
        @ConfigEditorButton(buttonText = "Проверить")
        val checkStatus: Runnable = Runnable {
            val mc = Minecraft.getInstance()
            mc.player?.displayClientMessage(Component.literal("§d§l[Secret] §fВаш статус: $statusInfo"), false)
        }
    }

    class ChatBotCategory {
        @Expose
        @ConfigOption(name = "§lБот АвтоОтветчик", desc = "Включает бота, который отвечает на сообщения в чате.")
        @ConfigEditorBoolean
        var autoResponderEnabled: Boolean = false

        @Expose
        @ConfigOption(name = "Авто-приветствия/прощания", desc = "Бот автоматически здоровается и прощается с игроками.")
        @ConfigEditorBoolean
        var greetingsEnabled: Boolean = false

        @Expose
        @ConfigOption(name = "Режим §cПО ПОЛНОЙ!", desc = "Бот отвечает на ВСЕ вопросы в чате (с знаком ?), даже если они не адресованы ему.")
        @ConfigEditorBoolean
        var fullModeEnabled: Boolean = false

        @Expose
        @ConfigOption(name = "Уведомления бота", desc = "Отправлять сообщения в чат при включении/выключении бота и его режима (outdated)")
        @ConfigEditorBoolean
        var sendStateMessages: Boolean = false

        @Expose
        @ConfigOption(name = "§cМини-ИИ", desc = "Включает запоминание контекста и предпочтений игроков.")
        @ConfigEditorBoolean
        var aiEnabled: Boolean = false

        @Expose
        @ConfigOption(name = "Активация ИИ", desc = "Введите для разблокировки систем ИИ и LM Studio.")
        @ConfigEditorText
        var aiActivationCode: String = ""

        @ConfigOption(name = "Разблокировать", desc = "или /sh code starl <code>")
        @ConfigEditorButton(buttonText = "Ввод")
        val activateAi: Runnable = Runnable {
            val h = aiActivationCode.trim().lowercase().hashCode()
            if ((h xor 4919) == -1643579854) {
                if (!isAiUnlocked) {
                    isAiUnlocked = true
                    SecretMenuManager.save()
                    Minecraft.getInstance().player?.displayClientMessage(
                        Component.literal("§d§l[Secret] §fСистема ИИ §aразблокирована§f!"),
                        false
                    )
                }
                aiActivationCode = ""
            } else {
                Minecraft.getInstance().player?.displayClientMessage(
                    Component.literal("§d§l[Secret] §cНеверный код активации!"),
                    false
                )
            }
        }

        @Expose
        var isAiUnlocked: Boolean = false

        @Expose
        @ConfigOption(name = "§aЛичность ИИ", desc = "Меняет характер и манеру общения бота.")
        @ConfigEditorDropdown
        var aiPersona: AiPersona = AiPersona.HELPFUL

        @ConfigEditorInfoText
        var lmStudioHeader: String = "§b§lLM Studio §7(Локальный ИИ)"

        @Expose
        @ConfigOption(name = "Использовать LM Studio", desc = "Включает использование локальной нейросети вместо встроенных алгоритмов.")
        @ConfigEditorBoolean
        var lmStudioEnabled: Boolean = false

        @Expose
        @ConfigOption(name = "API URL", desc = "Адрес сервера LM Studio (обычно http://localhost:1234/v1).")
        @ConfigEditorText
        var apiUrl: String = "http://localhost:1234/v1"

        @Expose
        @ConfigOption(name = "Название модели", desc = "ID модели в LM Studio (можно оставить пустым для автоматического выбора).")
        @ConfigEditorText
        var modelId: String = ""

        @Expose
        @ConfigOption(name = "Режим работы", desc = "Гибридный: сначала ищет простые ответы в моде, если не находит — спрашивает LM Studio.\nТолько LM Studio: всегда спрашивает нейросеть.")
        @ConfigEditorDropdown
        var mode: LmMode = LmMode.HYBRID

        @Expose
        @ConfigOption(name = "Температура", desc = "Случайность ответов (0.0 - 2.0).")
        @ConfigEditorText
        var temperature: String = "0.7"
    }

    enum class LmMode(val displayName: String) {
        HYBRID("Гибридный"),
        ALWAYS_LM("Только LM Studio");
        
        override fun toString(): String = displayName
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
                mc.player?.displayClientMessage(Component.literal("§d§l[Secret] §cНедостаточно прав!"), false)
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

            mc.player?.displayClientMessage(Component.literal("§d§l[Secret] §fСписок доступных команд:"), false)
            commands.forEach { cmd ->
                mc.player?.displayClientMessage(Component.literal(cmd), false)
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
        @ConfigOption(name = "§dРадужно-волновой Starlev", desc = "Включает секретный эффект для ника или слова Starlev.")
        @ConfigEditorBoolean
        var starlevNameEffect: Boolean = true

        @Expose
        @ConfigOption(name = "§4Fade+Shake MegaChromeX", desc = "Включает темно-красный Fade+Shake эффект для ника или слова MegaChromeX.")
        @ConfigEditorBoolean
        var megaChromeXEffect: Boolean = true

        @Expose
        @ConfigOption(name = "Пасхалка #1", desc = "Что-то секретное... §lТы л")
        @ConfigEditorInfoText
        var easterEgg: String = "§kSECRET_DATA"
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
