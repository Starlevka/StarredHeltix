package set.starlev.features.chat.mod

object ModerationManager {
    private val admins = setOf("Starlev", "Owner", "Penguin", "Starlevka")
    private val moderators = setOf("ZurGames", "MegaChromeX", "nik36c")
    private val muted = mutableMapOf<String, MuteData>()

    fun isAdmin(player: String) = admins.contains(player)
    fun isModerator(player: String) = moderators.contains(player) || isAdmin(player)

    fun mute(target: String, mod: String, duration: String, reason: String) {
        muted[target.lowercase()] = MuteData(mod, duration, reason, System.currentTimeMillis())
    }

    fun unmute(target: String) = muted.remove(target.lowercase()) != null

    fun isMuted(player: String) = muted.containsKey(player.lowercase())

    data class MuteData(val mod: String, val duration: String, val reason: String, val time: Long)
}
