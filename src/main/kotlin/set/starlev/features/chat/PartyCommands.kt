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
    // Расширенные форматы для Heltix: "Пати > * Nick: текст", "Party > Nick > текст", "[Пати] Nick: текст".
    // Флаг u обязателен: без него (?i) в Java/Kotlin игнорирует регистр только для ASCII — «Пати» не матчился.
    private val partyPattern = Regex("(?iu)(?:\\[\\s*)?(?:пати|party)(?:\\s*\\])?\\s*(?:\\[[^\\]]*\\])?\\s*[>»\\-–]?\\W*([\\p{L}\\p{N}_]{2,16})\\s*[:>]\\s*(.+)")
    // Звук/обработка — только для входящих строк, НАЧИНАЮЩИХСЯ с канала пати: «Пати >» / «Party >» / «[Пати] >».
    // Прежний partySimplePattern ловил подстроку «пати/party» в любом месте → звук срабатывал на каждой
    // второй строке чата (глобальный чат, системки с упоминанием party, и т.п.).
    private val partyPrefixPattern = Regex("(?iu)^\\s*(?:\\[)?\\s*(?:пати|party)\\s*(?:\\])?\\s*[>»]")
    private val pmPattern = Regex("(?iu)^(?:От|From|To|Кому)\\s+(?:\\[.+?\\]\\s*)?\\W*([\\p{L}\\p{N}_]{2,16})\\s*[:>]\\s*(.+)|^\\[(\\S+)\\s*->\\s*\\S+\\]\\s*(.+)")
    private val pmSimplePattern = Regex("(?iu)(?:^\\[.*->.*\\]|\\b(?:От|From)\\b.*:|^\\s*ЛС\\b)")

    // Запасной разбор отправителя ЛС, когда pmPattern не сработал (формат нестандартный).
    // Без него отправитель был бы пустым, isModerator("") == false и команда бы не выполнилась.
    // Держит: «[Nick -> …», «Nick -> …», «От Nick: …», «лс от Nick: …» (до двух префиксов- слов).
    private val pmSenderFallback = Regex(
        "(?iu)^\\s*\\[?\\s*(?:(?:\\*|от|from|лс|whisper)\\s*){0,2}([\\p{L}\\p{N}_]{2,16})\\s*(?:[:>]|->|—|–)"
    )

    // Единый разбор «!команда аргументы» для пати и ЛС — одинаковое поведение в обоих каналах.
    private val COMMAND_PATTERN = Regex("!([A-Za-z][A-Za-z_]*)\\s*(.*)")

    private val config get() = StarredHeltix.feature.chat.party
    private var lastPartySoundMs = 0L
    private var lastPmSoundMs = 0L

    fun init() {
        ChatEventsManager.registerOutgoing { message ->
            // 1. Проверка макро-чека
            if (MacroCheck.isBlocked()) {
                if (MacroCheck.checkAnswer(message)) {
                    // Ответ верный, пропускаем сообщение (оно не отправится в чат, так как мы возвращаем true)
                    return@registerOutgoing true
                } else {
                    // Ответ неверный или не число, блокируем отправку
                    mc.player?.sendSystemMessage(Component.literal("§c§l[MACRO] §fСначала решите пример в чате!"))
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
                    mc.player?.sendSystemMessage(Component.literal("§c§l[MUTE] §fВы не можете писать в чат, так как заблокированы модератором §e${data?.mod}\n§fПричина: §7${data?.reason}"))
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
            if (message != lastProcessedMessage) {
                lastProcessedMessage = message
                // Пати — только если строка начинается с «Пати >» (см. partyPrefixPattern).
                if (partyPrefixPattern.containsMatchIn(message)) {
                    if (partyPattern.containsMatchIn(message)) {
                        // Полноценный формат «Пати > Ник: текст» — разбор команд + звук внутри handlePartyMessage.
                        handlePartyMessage(message)
                    } else {
                        // Префикс пати есть, но формат нестандартный — просто звук.
                        playPartySound()
                    }
                } else if (pmPattern.containsMatchIn(message) || pmSimplePattern.containsMatchIn(message)) {
                    handlePrivateMessage(message)
                    if (!pmPattern.containsMatchIn(message)) playPmSound()
                }
            }
        }
    }

    private fun handlePartyMessage(message: String) {
        val match = partyPattern.find(message) ?: return
        val player = match.groupValues[1]
        val text = match.groupValues[2]

        // Звук как на Hypixel при входящем сообщении из пати (с анти-спамом 300мс)
        playPartySound()

        if (text.contains("!")) handlePartyCommand(player, text)
    }

    private fun handlePrivateMessage(message: String) {
        val match = pmPattern.find(message)
        var player: String? = null
        var text: String? = null

        if (match != null) {
            // В зависимости от группы в regex получаем имя и текст
            player = match.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: match.groupValues.getOrNull(3)
            text = match.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() } ?: match.groupValues.getOrNull(4)
        }

        // Раньше тут стоял `?: return`, и ЛС-команды молча терялись, когда совпадал только
        // pmSimplePattern («ЛС …», «[Nick -> …]») — а pmPattern не распарсил. Теперь при
        // неудаче разбираем команду прямо из сырой строки.
        if (player.isNullOrBlank() || text.isNullOrBlank()) {
            val fb = pmSenderFallback.find(message)
            player = fb?.groupValues?.get(1)
            text = fb?.let { m -> message.substring(m.range.last + 1).trim() }.takeUnless { it.isNullOrBlank() } ?: message
        }

        // Звук как на Hypixel при входящем ЛС (с анти-спамом 300мс)
        playPmSound()
        if (text.contains("!")) handlePMCommand(player ?: "", text)
    }

    private fun playPartySound() {
        if (!config.messageSound) return
        val now = System.currentTimeMillis()
        if (now - lastPartySoundMs < 300L) return
        lastPartySoundMs = now
        try {
            mc.execute {
                mc.soundManager.play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 1.0f
                    )
                )
            }
        } catch (_: Exception) {
            try { mc.execute { mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f)) } } catch (_: Exception) {}
        }
    }

    private fun playPmSound() {
        if (!config.messageSound) return
        val now = System.currentTimeMillis()
        if (now - lastPmSoundMs < 300L) return
        lastPmSoundMs = now
        try {
            mc.execute {
                mc.soundManager.play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 1.0f
                    )
                )
            }
        } catch (_: Exception) {
            try { mc.execute { mc.player?.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 1.0f) } } catch (_: Exception) {}
        }
    }

    private fun handlePartyCommand(player: String, text: String) {
        // Команда ищется в любом месте текста, а не только в начале — раньше «привет !kick X»
        // в пати молча игнорировалось, тогда как в ЛС работало (там ищем через Regex.find).
        val cmdMatch = COMMAND_PATTERN.find(text) ?: return
        val cmd = cmdMatch.groupValues[1].lowercase()
        val args = cmdMatch.groupValues[2].trim()
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
        // Мод-команда может быть не в начале сообщения («привет !sh_mute …») — ищем !cmd в любом месте.
        // Раньше брался parts[1], из-за чего сама команда (!cmd) отбрасывалась и модерация в ЛС не работала.
        val cmdMatch = COMMAND_PATTERN.find(text)
        if (cmdMatch != null && ModerationManager.isModerator(player)) {
            handleModCommand(player, cmdMatch.groupValues[1].lowercase(), cmdMatch.groupValues[2].trim())
        }

        if ((config.invite || ModerationManager.isModerator(player)) && (text.contains("!invite") || text.contains("!inv"))) {
            mc.player?.connection?.sendCommand("p invite $player")
        }
    }
    private fun handleModCommand(mod: String, cmd: String, args: String) {
        // Полный разбор по токену: sh_mute требует target + duration + reason (3+ части),
        // limit=2 здесь ломал аргументы (reason слипался с duration).
        val parts = args.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val target = parts.getOrNull(0) ?: ""
        val reason = if (parts.size > 1) parts.drop(1).joinToString(" ") else "Без причины"
        val isMe = target.lowercase() == mc.player?.name?.string?.lowercase()
        // Проверка прав модератора на конкретную команду
        if (!ModerationManager.isCommandAllowed(mod, cmd)) return

        when (cmd) {
            "sh_mute" -> {
                if (isMe && parts.size >= 3) {
                    if (ModerationManager.canPerformAction(mod, target)) {
                        mc.execute {
                            ModerationManager.mute(target, mod, parts[1], parts.drop(2).joinToString(" "))
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
                // sendCommand должен идти из клиентского потока, а не из этого фонового треда.
                mc.execute { mc.player?.connection?.sendCommand("pc $line") }
            }
        }.apply { isDaemon = true }.start()
    }
}
