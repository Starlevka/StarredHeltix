package set.starlev.features.chat.mod

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object ModerationManager {
    private val admins = setOf("Starlev", "Owner", "Penguin", "Starlevka")
    private val moderators = setOf("ZurGames", "MegaChromeX", "nik36c", "Tasik12")
    private val muted = mutableMapOf<String, MuteData>()
    private var localMuted: MuteData? = null

    fun isAdmin(player: String) = admins.contains(player)
    fun isModerator(player: String) = moderators.contains(player) || isAdmin(player)

    /**
     * Проверяет, может ли mod совершить действие над target.
     * Админ может всё над всеми (кроме, возможно, других админов, но тут админ > модератор).
     * Модератор НЕ может мутить/кикать админов.
     */
    fun canPerformAction(mod: String, target: String): Boolean {
        if (isAdmin(mod)) return true
        if (isModerator(mod) && isAdmin(target)) return false
        return isModerator(mod)
    }

    /**
     * Список команд, доступных модераторам.
     * Админы имеют доступ ко всем командам.
     */
    private val moderatorCommands = setOf("sh_mute", "sh_kick", "sh_unmute", "sh_mc", "sh_unmc")

    fun isCommandAllowed(mod: String, command: String): Boolean {
        if (isAdmin(mod)) return true
        return moderatorCommands.contains(command.lowercase())
    }

    fun mute(target: String, mod: String, duration: String, reason: String) {
        val data = MuteData(mod, duration, reason, System.currentTimeMillis())
        if (target.lowercase() == Minecraft.getInstance().player?.name?.string?.lowercase()) {
            localMuted = data
            Minecraft.getInstance().player?.displayClientMessage(
                Component.literal("§c§l[MUTE] §fВы были временно заблокированы в чате модератором §e$mod\n§fВремя: §b$duration\n§fПричина: §7$reason"),
                false
            )
        }
        muted[target.lowercase()] = data
    }

    fun unmute(target: String): Boolean {
        if (target.lowercase() == Minecraft.getInstance().player?.name?.string?.lowercase()) {
            localMuted = null
            Minecraft.getInstance().player?.displayClientMessage(
                Component.literal("§a§l[MUTE] §fВаша блокировка чата была снята."),
                false
            )
        }
        return muted.remove(target.lowercase()) != null
    }

    fun isLocalMuted(): Boolean = localMuted != null

    fun getLocalMuteData(): MuteData? = localMuted

    fun isMuted(player: String) = muted.containsKey(player.lowercase())

    /**
        * Проверяет, является ли сообщение командой модератора (разрешённой при муте).
        */
    fun isModeratorCommand(lowerMsg: String): Boolean {
        if (!lowerMsg.startsWith("!")) return false
        val cmd = lowerMsg.substringBefore(" ").lowercase()
        return setOf("!sh_mute", "!sh_unmute", "!sh_kick", "!sh_mc", "!sh_unmc", "!sh_crash").contains(cmd)
    }

    data class MuteData(val mod: String, val duration: String, val reason: String, val time: Long)
}
