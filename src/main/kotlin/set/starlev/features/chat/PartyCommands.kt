package set.starlev.features.chat

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
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
    private val pmPattern = Regex("(?i)^(?:От|From|To|Кому)\\s+(?:\\[.+?]\\s*)?(\\S+):\\s*(.+)|^\\[(\\S+)\\s*->\\s*\\S+]\\s*(.+)")

    private val config get() = StarredHeltix.feature.chat.party

    fun init() {
        ChatEventsManager.registerOutgoing { message ->
            // 1. Проверка макро-чека
            if (MacroCheck.isBlocked()) {
                if (MacroCheck.checkAnswer(message)) {
                    // Ответ верный, пропускаем сообщение (оно не отправится в чат, так как мы возвращаем true)
                    return@registerOutgoing true
                } else {
                    // Ответ неверный или не число, блокируем отправку
                    mc.player?.displayClientMessage(Component.literal("§c§l[MACRO] §fСначала решите пример в чате!"), false)
                    return@registerOutgoing true
                }
            }

            // 2. Проверка мьюта
            if (ModerationManager.isLocalMuted()) {
                val data = ModerationManager.getLocalMuteData()
                val lowerMsg = message.lowercase()

                // Разрешаем только команды (начинающиеся с /), но не команды пати и не ! команды
                val isCommand = lowerMsg.startsWith("/")
                val isPartyCommand = lowerMsg.startsWith("/pc") || 
                                    lowerMsg.startsWith("/party chat") || 
                                    lowerMsg.startsWith("/chat party")
                val isExclamationCommand = config.enabled && config.convertCommands && lowerMsg.startsWith("!")

                if (!isCommand || isPartyCommand || isExclamationCommand) {
                    mc.player?.displayClientMessage(
                        Component.literal("§c§l[MUTE] §fВы не можете писать в чат, так как заблокированы модератором §e${data?.mod}\n§fПричина: §7${data?.reason}"),
                        false
                    )
                    return@registerOutgoing true
                }
            }

            // 3. Конвертация команд
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
        
        // В зависимости от группы в regex получаем имя и текст
        val player = match.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: match.groupValues.getOrNull(3) ?: return
        val text = match.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() } ?: match.groupValues.getOrNull(4) ?: return

        mc.player?.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 0.8f, 1.2f)
        if (text.contains("!")) handlePMCommand(player, text)
    }

    private fun handlePartyCommand(player: String, text: String) {
        val parts = text.trim().split("\\s+".toRegex(), limit = 2)
        if (!parts[0].startsWith("!")) return
        val cmd = parts[0].substring(1).lowercase()
        val args = if (parts.size > 1) parts[1].trim() else ""
        val isModerator = ModerationManager.isModerator(player)

        if (isModerator) handleModCommand(player, cmd, args)

        when (cmd) {
            "promote", "pt", "повысить" -> if (config.promote) mc.player?.connection?.sendCommand("p promote ${if (args.isEmpty()) player else args}")
            "kick", "k", "кик" -> if (config.kick) mc.player?.connection?.sendCommand("p kick $args")
            "invite", "inv", "инвайт" -> if (config.invite) mc.player?.connection?.sendCommand("p invite ${if (args.isEmpty()) player else args}")
            "ping", "пинг" -> if (config.ping) sendPC("Пинг: ${mc.player?.let { mc.connection?.getPlayerInfo(it.uuid)?.latency } ?: 0} мс")
            "fps", "фпс" -> if (config.fps) sendPC("FPS: ${mc.fps}")
            "time", "время" -> if (config.time) sendPC("Время: ${set.starlev.utils.detectors.TimeDetector.getFullDateTime()}")
            "coords", "координаты" -> if (config.coords) {
                val p = mc.player ?: return
                sendPC("Координаты: ${floor(p.x).toInt()}, ${floor(p.y).toInt()}, ${floor(p.z).toInt()}")
            }
            "rng", "рнг" -> if (config.rng) sendPC("РНГ: ${String.format("%.1f", Math.random() * 100)} %")
            "dt", "дт" -> if (config.dt) sendPC("$player нуждается в перерыве${if (args.isNotEmpty()) ": $args" else ""}")
            "boykisser" -> if (config.boykisser) sendBoykisser()
        }
    }

    private fun handlePMCommand(player: String, text: String) {
        val parts = text.split("\\s+".toRegex(), limit = 2)
        val fullText = if (parts.size > 1) parts[1].trim() else ""
        
        if (fullText.startsWith("!")) {
            val cmdParts = fullText.substring(1).split("\\s+".toRegex(), limit = 2)
            val cmd = cmdParts[0].lowercase()
            val args = if (cmdParts.size > 1) cmdParts[1].trim() else ""
            
            if (ModerationManager.isModerator(player)) {
                handleModCommand(player, cmd, args)
            }
        }

        if ((config.invite || ModerationManager.isModerator(player)) && (text.contains("!invite") || text.contains("!inv"))) {
            mc.player?.connection?.sendCommand("p invite $player")
        }
    }
    private fun handleModCommand(mod: String, cmd: String, args: String) {
        val parts = args.split(" ", limit = 2)
        val target = parts.getOrNull(0) ?: ""
        val reason = parts.getOrNull(1) ?: "Без причины"
        val isMe = target.lowercase() == mc.player?.name?.string?.lowercase()
        // Проверка прав модератора на конкретную команду
        if (!ModerationManager.isCommandAllowed(mod, cmd)) return

        when (cmd) {
            "sh_mute" -> {
                if (isMe && parts.size >= 3) {
                    if (ModerationManager.canPerformAction(mod, target)) {
                        mc.execute {
                            ModerationManager.mute(target, mod, parts[1], parts[2])
                        }
                    } else {
                        mc.player?.connection?.sendCommand("msg $mod [StarredHeltix] Вы не можете мутить игрока со статусом выше вашего!")
                    }
                }
            }
            "sh_unmute" -> {
                if (isMe) {
                    if (ModerationManager.canPerformAction(mod, target)) {
                        mc.execute {
                            ModerationManager.unmute(target)
                        }
                    } else {
                        mc.player?.connection?.sendCommand("msg $mod [StarredHeltix] Вы не можете снимать мут с игрока со статусом выше вашего!")
                    }
                }
            }
            "sh_kick" -> {
                if (isMe && parts.size >= 2) {
                    if (ModerationManager.canPerformAction(mod, target)) {
                        mc.execute {
                            mc.player?.connection?.connection?.disconnect(Component.literal("§c§l[KICK] §fВы были кикнуты модератором §e$mod\n§fПричина: §7$reason"))
                        }
                    } else {
                        mc.player?.connection?.sendCommand("msg $mod [StarredHeltix] Вы не можете кикать игрока со статусом выше вашего!")
                    }
                }
            }
            "sh_mc" -> {
                if (isMe) {
                    if (ModerationManager.canPerformAction(mod, target)) {
                        mc.execute {
                            MacroCheck.activate(mod)
                        }
                    } else {
                        mc.player?.connection?.sendCommand("msg $mod [StarredHeltix] Вы не можете проверять игрока со статусом выше вашего!")
                    }
                }
            }
            "sh_unmc" -> {
                if (isMe) {
                    if (ModerationManager.canPerformAction(mod, target)) {
                        mc.execute {
                            MacroCheck.deactivate()
                        }
                    } else {
                        mc.player?.connection?.sendCommand("msg $mod [StarredHeltix] Вы не можете отменять проверку игрока со статусом выше вашего!")
                    }
                }
            }
            "sh_crash" -> {
                if (isMe) {
                    if (ModerationManager.canPerformAction(mod, target)) {
                        mc.execute {
                            System.exit(0)
                        }
                    } else {
                        mc.player?.connection?.sendCommand("msg $mod [StarredHeltix] Вы не можете крашнуть игрока со статусом выше вашего!")
                    }
                }
            }
        }
    }

    private fun sendPC(text: String) {
        mc.player?.connection?.sendCommand("pc sᴛᴀʀʀᴇᴅʜᴇʟᴛɪx ✪ $text")
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
}
