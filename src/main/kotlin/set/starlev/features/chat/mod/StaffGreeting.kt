package set.starlev.features.chat.mod

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.secret.config.SecretMenuManager

/**
 * Приветствие персоналу (модераторы и выше) при каждом входе на сервер.
 *
 * Показывает: ранг, доступные команды по этому рангу, секретный код меню и
 * благодарность за использование мода. Обычные игроки ничего не видят.
 *
 * Отключается в секретном меню (Главное → «Приветствие при входе»), которое
 * персоналу доступно сразу — вводить код не нужно.
 */
object StaffGreeting {

    fun init() {
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            // mc.player на JOIN уже доступен, но подстрахуемся от null.
            try { show() } catch (_: Exception) { }
        }
    }

    private fun show() {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val name = player.name?.string ?: return
        if (!ModerationManager.isModerator(name)) return

        // Настройка живёт в секретном конфиге; если он ещё не загружен — грузим.
        if (!SecretMenuManager.isConfigInitialized) SecretMenuManager.load(forceSave = false)
        if (!SecretMenuManager.secretConfig.main.showGreeting) return

        val send = { text: String -> player.sendSystemMessage(Component.literal(text)) }

        send("§8§m                                        ")
        send("§d§l[StarredHeltix] §fПривет, §b$name§f!")
        send("§7Ваш ранг: ${ModerationManager.rankName(name)}")
        send("§7Команды, доступные с вашим рангом:")
        for (line in ModerationManager.commandsFor(name)) send("  $line")
        send("§7Секретный код меню: §e${ModerationManager.SECRET_CODE}")
        send("§7Секретное меню: §f/sh §7→ §dО моде §7→ §dСекретное меню")
        send("§7Спасибо, что используете мод StarredHeltix!")
        send("§8§m                                        ")
    }
}
