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

        @ConfigOption(name = "Ваш статус", desc = "Нажмите, чтобы узнать ваш текущий уровень доступа.")
        @ConfigEditorButton(buttonText = "Проверить")
        val checkStatus: Runnable = Runnable {
            val mc = Minecraft.getInstance()
            mc.player?.displayClientMessage(Component.literal("§d§l[Secret] §fВаш статус: $statusInfo"), false)
        }
    }

    // Ранее здесь был класс ChatBotCategory и enum LmMode для ИИ-чат-бота.

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
        @ConfigOption(name = "§bВолновой maksimwain", desc = "Включает волновой эффект для ника или слова maksimwain.")
        @ConfigEditorBoolean
        var maksimwainEffect: Boolean = true

        @Expose
        @ConfigOption(name = "§dПереливающийся ridar", desc = "Включает переливающийся фиолетово-синий эффект для ника или слова ridar.")
        @ConfigEditorBoolean
        var ridarEffect: Boolean = true

        @Expose
        @ConfigOption(name = "§bПереливающийся zinanel0", desc = "Включает голубо-синий переливающийся эффект для ника или слова zinanel0.")
        @ConfigEditorBoolean
        var zinanel0Effect: Boolean = true

        @Expose
        @ConfigOption(name = "§7Серый Apostol312", desc = "Включает серый цвет для ника или слова Apostol312.")
        @ConfigEditorBoolean
        var apostol312Effect: Boolean = true

        @Expose
        @ConfigOption(name = "§1Волновой Timyr12", desc = "Включает синий волновой эффект для ника или слова Timyr12.")
        @ConfigEditorBoolean
        var timyr12Effect: Boolean = true

        @Expose
        @ConfigOption(name = "§5Волновой ZurGames", desc = "Включает голубо-фиолетовый волновой эффект для ника или слова ZurGames.")
        @ConfigEditorBoolean
        var zurGamesEffect: Boolean = true

        @Expose
        @ConfigOption(name = "§dВолновой NiKoMao", desc = "Включает розовый волновой эффект для ника или слова NiKoMao.")
        @ConfigEditorBoolean
        var niKoMaoEffect: Boolean = true

        @Expose
        @ConfigOption(name = "§6Meow Music Rune III", desc = "Воспроизводит завораживающие мяуканья при убийстве моба поблизости (радиус 16 блоков). Звук из Hypixel Skyblock.")
        @ConfigEditorBoolean
        var meowMusicRune: Boolean = false

        @Expose
        @ConfigOption(name = "§6Громкость Meow", desc = "Громкость звука Meow Music Rune (0.0 - 1.0).")
        @ConfigEditorDropdown
        var meowVolume: MeowVolume = MeowVolume.NORMAL

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

    enum class MeowVolume(val displayName: String, val value: Float) {
        QUIET("Тихо (25%)", 0.25f),
        NORMAL("Нормально (50%)", 0.5f),
        LOUD("Громко (75%)", 0.75f),
        VERY_LOUD("Очень громко (100%)", 1.0f);
        override fun toString(): String = displayName
    }

    enum class NameEffectType(val displayName: String, val colorValue: Int?) {
        NONE("Нет", null),
        RAINBOW("Радужный (Shader)", 0xFFFFFC), // ID 3 on White
        WAVE("Волна (Shader)", 0xFFFFFD),    // ID 2 on White
        SHAKE("Тряска (Shader)", 0xFFFFFE),   // ID 1 on White
        BOUNCE("Прыжок (Shader)", 0xFFFFFB),  // ID 4 on White
        PULSE("Пульс (Shader)", 0xFFFFF9),    // ID 6 on White
        BLINK("Мигание (Shader)", 0xFFFFFA),  // ID 5 on White
        STARLEV("Starlev (Special)", 0xFFFFF5), // ID 10
        BLACK("Черный", 0x000000),
        DARK_RED("Темно-красный", 0xAA0000),
        GOLD("Золотой", 0xFFAA00),
        AQUA("Голубой", 0x55FFFF);

        override fun toString(): String = displayName
    }

    // Вспомогательный флаг для динамического скрытия (управляется через SecretMenuManager)
    @Expose
    var isStaff: Boolean = false
}
