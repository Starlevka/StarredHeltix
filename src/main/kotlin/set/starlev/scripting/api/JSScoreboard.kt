package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard

object JSScoreboard {
    private val mc: Minecraft get() = Minecraft.getInstance()
    private fun sb(): Scoreboard? = mc.level?.scoreboard
    private fun sidebar(): net.minecraft.world.scores.Objective? = sb()?.getDisplayObjective(DisplaySlot.SIDEBAR)

    fun getTitle(): String = try { sidebar()?.displayName?.string ?: "" } catch (_: Exception) { "" }
    fun setTitle(title: String) { try { sidebar()?.let { it.displayName = Component.literal(title.replace('&','§')) } } catch (_: Exception) {} }
    fun getLines(): List<String> {
        return try {
            val s = sb() ?: return emptyList()
            val o = sidebar() ?: return emptyList()
            s.listPlayerScores(o).sortedByDescending { it.value }.take(15).map { it.display()?.string ?: it.owner }
        } catch (_: Exception) { emptyList() }
    }
    fun getLineByIndex(index: Int): String? = getLines().getOrNull(index)
    fun getLinesByScore(score: Int): List<String> {
        return try {
            val s = sb() ?: return emptyList()
            val o = sidebar() ?: return emptyList()
            s.listPlayerScores(o).filter { it.value == score }.map { it.display()?.string ?: it.owner }
        } catch (_: Exception) { emptyList() }
    }
    fun setLine(score: Int, line: String, override: Boolean = false) {
        try {
            if (override) { removeScores(score); addLine(score, line); return }
            addLine(score, line)
        } catch (_: Exception) {}
    }
    fun addLine(score: Int, line: String) {
        try {
            val s = sb() ?: return
            val o = sidebar() ?: return
            val holder = net.minecraft.world.scores.ScoreHolder.forNameOnly(line)
            val access = s.getOrCreatePlayerScore(holder, o, true)
            access.set(score)
            try { access.display(Component.literal(line.replace('&','§'))) } catch (_: Exception) {}
        } catch (_: Exception) {}
    }
    fun removeScores(score: Int) {
        try {
            val s = sb() ?: return
            val o = sidebar() ?: return
            for (holder in s.trackedPlayers.toList()) {
                val info = try { s.getPlayerScoreInfo(holder, o) } catch (_: Exception) { null } ?: continue
                if (info.value() == score) s.resetSinglePlayerScore(holder, o)
            }
        } catch (_: Exception) {}
    }
    fun removeIndex(index: Int) {
        val target = getLines().getOrNull(index) ?: return
        try {
            val s = sb() ?: return
            val o = sidebar() ?: return
            for (holder in s.trackedPlayers) {
                if (holder.getScoreboardName() == target) { s.resetSinglePlayerScore(holder, o); break }
            }
        } catch (_: Exception) {}
    }
    fun setShouldRender(shouldRender: Boolean) {}
    fun getShouldRender(): Boolean = true
    fun createTeam(name: String): JSTeam {
        val s = sb() ?: throw IllegalStateException("No scoreboard")
        val team = s.getPlayerTeam(name) ?: s.addPlayerTeam(name)
        return JSTeam(team)
    }
    fun getTeam(name: String): JSTeam? {
        val t = sb()?.getPlayerTeam(name) ?: return null
        return JSTeam(t)
    }
    fun removeTeam(name: String) {
        try { val s = sb() ?: return; val t = s.getPlayerTeam(name) ?: return; s.removePlayerTeam(t) } catch (_: Exception) {}
    }

    // ==================== Детальный Score API ====================
    fun getScores(): List<JSScore> {
        return try {
            val s = sb() ?: return emptyList()
            val o = sidebar() ?: return emptyList()
            s.listPlayerScores(o).sortedByDescending { it.value }.map { JSScore(it, s, o) }
        } catch (_: Exception) { emptyList() }
    }
    fun getScore(index: Int): JSScore? = getScores().getOrNull(index)
    fun getScoresByScore(score: Int): List<JSScore> = getScores().filter { it.getScore() == score }
    fun getScoreByName(name: String): JSScore? = getScores().firstOrNull { it.getName() == name || it.getDisplayName() == name }

    // ==================== Кастомный скорборд (HudScoreboard) для модулей ====================
    @JvmOverloads
    fun addCustomLine(id: String, text: String, anchor: String = "BOTTOM", order: Int = 99) {
        val mod = set.starlev.scripting.SHScriptEngine.currentModule ?: "unknown"
        set.starlev.scripting.ModuleCustomScoreboard.add(mod, id, text, anchor, order)
    }
    fun setCustomLine(id: String, text: String) {
        val mod = set.starlev.scripting.SHScriptEngine.currentModule ?: "unknown"
        set.starlev.scripting.ModuleCustomScoreboard.set(mod, id, text)
    }
    fun removeCustomLine(id: String) {
        val mod = set.starlev.scripting.SHScriptEngine.currentModule ?: "unknown"
        set.starlev.scripting.ModuleCustomScoreboard.remove(mod, id)
    }
    fun getCustomLines(): List<Map<String, Any>> {
        val mod = set.starlev.scripting.SHScriptEngine.currentModule ?: "unknown"
        return set.starlev.scripting.ModuleCustomScoreboard.getForModule(mod).map {
            mapOf("id" to it.id, "text" to it.component.string, "anchor" to it.anchor, "order" to it.order)
        }
    }
    fun clearCustomLines() {
        val mod = set.starlev.scripting.SHScriptEngine.currentModule ?: "unknown"
        // clear only own? Для совместимости — own only via removeForModule
        set.starlev.scripting.ModuleCustomScoreboard.removeForModule(mod)
    }
    fun getAllCustomLines(): List<Map<String, Any>> {
        return set.starlev.scripting.ModuleCustomScoreboard.getAll().map {
            mapOf("module" to it.module, "id" to it.id, "text" to it.component.string, "anchor" to it.anchor, "order" to it.order)
        }
    }
}

class JSScore internal constructor(
    private val entry: net.minecraft.world.scores.PlayerScoreEntry,
    private val scoreboard: Scoreboard,
    private val objective: net.minecraft.world.scores.Objective
) {
    fun getName(): String = try { entry.owner() } catch (_: Exception) { "" }
    fun getDisplayName(): String = try { entry.display()?.string ?: entry.owner() } catch (_: Exception) { "" }
    fun getScore(): Int = try { entry.value() } catch (_: Exception) { 0 }
    fun setScore(v: Int) { try { scoreboard.getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly(entry.owner()), objective, true).set(v) } catch (_: Exception) {} }
    fun setName(name: String) {
        try {
            val holder = net.minecraft.world.scores.ScoreHolder.forNameOnly(entry.owner())
            val acc = scoreboard.getOrCreatePlayerScore(holder, objective, true)
            acc.display(Component.literal(name.replace('&','§')))
        } catch (_: Exception) {}
    }
    fun getTeam(): JSTeam? {
        return try {
            val t = scoreboard.getPlayersTeam(entry.owner()) ?: return null
            JSTeam(t)
        } catch (_: Exception) { null }
    }
    fun setTeam(team: JSTeam?) {
        try {
            val name = entry.owner()
            if (team == null) scoreboard.removePlayerFromTeam(name) else scoreboard.addPlayerToTeam(name, team.toMC())
        } catch (_: Exception) {}
    }
    fun remove() {
        try { scoreboard.resetSinglePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly(entry.owner()), objective) } catch (_: Exception) {}
    }
    fun getNumberFormat(): String = try {
        entry.numberFormatOverride()?.let { it.toString() } ?: ""
    } catch (_: Exception) { "" }
    fun isHidden(): Boolean = try { entry.isHidden } catch (_: Exception) { false }
}

class JSTeam internal constructor(private val team: PlayerTeam) {
    fun getName(): String = try { team.name } catch (_: Exception) { "" }
    fun setName(name: String) {}
    fun getDisplayName(): String = try { team.displayName.string } catch (_: Exception) { "" }
    fun setDisplayName(name: String) { try { team.setDisplayName(Component.literal(name)) } catch (_: Exception) {} }
    fun getPrefix(): String = try { team.playerPrefix.string } catch (_: Exception) { "" }
    fun setPrefix(prefix: String) { try { team.setPlayerPrefix(Component.literal(prefix)) } catch (_: Exception) {} }
    fun getSuffix(): String = try { team.playerSuffix.string } catch (_: Exception) { "" }
    fun setSuffix(suffix: String) { try { team.setPlayerSuffix(Component.literal(suffix)) } catch (_: Exception) {} }
    fun getColor(): String = try { team.color.toString().lowercase() } catch (_: Exception) { "white" }
    fun setColor(color: String) { try { net.minecraft.ChatFormatting.valueOf(color.uppercase()); } catch (_: Exception) {} }
    fun getMembers(): List<String> = try { team.players.toList() } catch (_: Exception) { emptyList() }
    fun addMember(name: String) { try { team.scoreboard.addPlayerToTeam(name, team) } catch (_: Exception) {} }
    fun removeMember(name: String) { try { team.scoreboard.removePlayerFromTeam(name, team) } catch (_: Exception) {} }
    fun getFriendlyFire(): Boolean = try { team.isAllowFriendlyFire } catch (_: Exception) { true }
    fun setFriendlyFire(v: Boolean) { try { team.setAllowFriendlyFire(v) } catch (_: Exception) {} }
    fun getSeeFriendlyInvisibles(): Boolean = try { team.canSeeFriendlyInvisibles() } catch (_: Exception) { true }
    fun setSeeFriendlyInvisibles(v: Boolean) { try { team.setSeeFriendlyInvisibles(v) } catch (_: Exception) {} }
    fun getNameTagVisibility(): String = try { team.nameTagVisibility.name } catch (_: Exception) { "always" }
    fun setNameTagVisibility(v: String) { try { team.setNameTagVisibility(net.minecraft.world.scores.Team.Visibility.valueOf(v.uppercase())) } catch (_: Exception) {} }
    fun getDeathMessageVisibility(): String = try { team.deathMessageVisibility.name } catch (_: Exception) { "always" }
    fun setDeathMessageVisibility(v: String) { try { team.setDeathMessageVisibility(net.minecraft.world.scores.Team.Visibility.valueOf(v.uppercase())) } catch (_: Exception) {} }
    fun getCollisionRule(): String = try { team.collisionRule.name } catch (_: Exception) { "always" }
    fun setCollisionRule(v: String) { try { team.setCollisionRule(net.minecraft.world.scores.Team.CollisionRule.valueOf(v.uppercase())) } catch (_: Exception) {} }
    fun toMC(): PlayerTeam = team
}
