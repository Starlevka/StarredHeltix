package set.starlev.features.combat.dungeons

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.render.HudElement
import set.starlev.utils.detectors.ScoreboardDetector
import java.util.regex.Pattern
import set.starlev.render.drawString
import set.starlev.render.drawCenteredString

object BloodRoomTimer : HudElement("BloodRoomTimer") {
    private val MC = Minecraft.getInstance()
    private var endTime = 0L
    private var noteEndTime = 0L
    private var active = false
    private var noteActive = false
    
    // Публичный флаг готовности кровавой комнаты для других модулей (например, ScoreCounter)
    var isBloodReady = false

    // Отслеживаем, закончилось ли текущее подземелье — после этого игнор любых [БОСС] до следующего захода
    private var dungeonFinished = false
    private var wasInDungeon = false

    // Паттерны завершения подземелья (чат) — + "ДОПОЛНИТЕЛЬНАЯ СТАТИСТИКА" как маркер конца на Heltix
    private val DUNGEON_END_CHAT_PATTERNS = listOf(
        Pattern.compile("(?i).*подземелье.*(?:завершено|пройдено|окончено|победа).*"),
        Pattern.compile("(?i).*данж.*(?:завершен|пройден|окончен|победа).*"),
        Pattern.compile("(?i).*dungeon.*(?:complete|finished|cleared|victory).*"),
        Pattern.compile("(?i).*(?:босс.*побежден|boss.*defeated).*"),
        Pattern.compile("(?i).*дополнительная\\s+статистика.*")
    )
    private val DUNGEON_END_SCOREBOARD_PATTERNS = listOf(
        Pattern.compile("(?i).*(?:завершено|пройдено|победа|зачищено.*100%|cleared.*100%).*"),
        Pattern.compile("(?i).*dungeon.*(?:complete|finished).*"),
        Pattern.compile("(?i).*дополнительная\\s+статистика.*")
    )
    
    // Детектим САМОЕ ПЕРВОЕ сообщение вида "[БОСС] Наблюдатель:" — любой текст после, не только "Поздравляю"
    private val BOSS_PATTERN = Pattern.compile(".*\\[БОСС]\\s*Наблюдатель:.*")
    // Улучшенные паттерны для поиска этажа
    private val FLOOR_PATTERNS = listOf(
        Pattern.compile("(?i).*Катакомбы\\s*\\(?(\\d+)\\s*этаж\\)?.*")
    )
    private val SCOREBOARD_TITLE_PATTERN = Pattern.compile("(?i)(?:КАТАКОМБЫ|CATACOMBS)\\s*(?:-|:)?\\s*(?:ЭТАЖ|FLOOR)?\\s*([\\dIVXLC]+)")

    private fun romanToInt(s: String): Int? {
        val roman = s.uppercase()
        if (roman.all { it.isDigit() }) return roman.toIntOrNull()
        
        var res = 0
        var i = 0
        val map = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100)
        
        while (i < roman.length) {
            val s1 = map[roman[i]] ?: return null
            if (i + 1 < roman.length) {
                val s2 = map[roman[i + 1]] ?: 0
                if (s1 >= s2) {
                    res += s1
                    i++
                } else {
                    res += s2 - s1
                    i += 2
                }
            } else {
                res += s1
                i++
            }
        }
        return res
    }
    
    fun init() {
        ClientReceiveMessageEvents.GAME.register(this::onChat)
    }
    
    private fun onChat(message: Component, overlay: Boolean) {
        if (!StarredHeltix.feature.dungeons.bloodRoom.enabled) return
        val level = MC.level ?: return
        
        // Проверка на нахождение в подземельях (dimension + Detector)
        if (!level.dimension().identifier().toString().startsWith("minecraft:dungeon_") && !set.starlev.utils.detectors.DungeonDetector.isInDungeon()) return
        
        val text = message.string

        // Считываем, закончилось ли подземелье — если да, то игнор любых дальнейших [БОСС] до следующего захода
        for (p in DUNGEON_END_CHAT_PATTERNS) {
            if (p.matcher(text).find()) {
                if (!dungeonFinished) {
                    dungeonFinished = true
                    // Не сбрасываем таймер мгновенно, но запрещаем новый старт; активный таймер пусть дотикает
                }
                return
            }
        }

        val matcher = BOSS_PATTERN.matcher(text)
        if (matcher.find()) {
            // Только самое первое [БОСС] Наблюдатель: за заход — игнор повторов пока таймер идёт, готов или данж уже закончен
            if (dungeonFinished) return
            if (active) return
            if (isBloodReady) return
            val floor = getFloor()
            val delay = when (floor) {
                1 -> 38000L
                2 -> 42000L
                3 -> 50000L
                4 -> 60900L
                5 -> 61000L
                else -> 33000L
            }
            endTime = System.currentTimeMillis() + delay
            active = true
            isBloodReady = false // Сбрасываем готовность при начале нового таймера
            
            if (floor == -1) {
            } else {
            }
        }
    }
    
    private fun getFloor(): Int {
        try {
            // 1. Проверяем заголовок скорборда (иногда там "КАТАКОМБЫ - ЭТАЖ 5")
            val title = ScoreboardDetector.getScoreboardTitle()
            val titleMatcher = SCOREBOARD_TITLE_PATTERN.matcher(title)
            if (titleMatcher.find()) {
                val floorStr = titleMatcher.group(1)
                val floor = romanToInt(floorStr)
                if (floor != null) {
                    return floor
                }
            }

            // 2. Проверяем линии скорборда
            val scoreboardLines = ScoreboardDetector.getScoreboardText()
            
            for (line in scoreboardLines) {
                for (pattern in FLOOR_PATTERNS) {
                    val m = pattern.matcher(line)
                    if (m.find()) {
                        val floor = romanToInt(m.group(1))
                        if (floor != null) {
                            return floor
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            StarredHeltix.LOGGER.warn("Ошибка при определении этажа: ${e.message}")
        }
        return -1
    }
    
    fun reset() {
        active = false
        isBloodReady = false
        endTime = 0L
        dungeonFinished = false
        wasInDungeon = false
    }

    override fun render() {
        if (!StarredHeltix.feature.dungeons.bloodRoom.enabled) return
        val isInDungeon = set.starlev.utils.detectors.DungeonDetector.isInDungeon()
        // Трекер нового захода — сброс флага завершения, чтобы первый [БОСС] снова считался
        if (!wasInDungeon && isInDungeon) {
            dungeonFinished = false
        }
        wasInDungeon = isInDungeon
        if (!isInDungeon) {
            if (active || isBloodReady) reset()
            return
        }
        // Проверка по скорборду, закончилось ли подземелье (даже внутри данжа) — после этого игнор любых [БОСС]
        if (!dungeonFinished) {
            try {
                val sbLines = ScoreboardDetector.getScoreboardText()
                for (line in sbLines) {
                    for (p in DUNGEON_END_SCOREBOARD_PATTERNS) {
                        if (p.matcher(line).find()) {
                            dungeonFinished = true
                            break
                        }
                    }
                    if (dungeonFinished) break
                }
                if (!dungeonFinished) {
                    val title = ScoreboardDetector.getScoreboardTitle()
                    for (p in DUNGEON_END_SCOREBOARD_PATTERNS) {
                        if (p.matcher(title).find()) {
                            dungeonFinished = true
                            break
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        
        val currentTime = System.currentTimeMillis()
        val message: String
        
        if (isEditing) {
            message = "Кровавая комната: 9999.0с"
        } else if (active) {
            val remaining = endTime - currentTime
            if (remaining <= 0) {
                active = false
                isBloodReady = true // Помечаем, что блад готов
                
                // Отправляем сообщение в чат ПАРТИИ
                val config = StarredHeltix.feature.dungeons.bloodRoom
                MC.player?.connection?.sendCommand("pc ${config.message}")
                
                // Показываем Title (кастомизируемый)
                MC.gui.hud.setTitle(Component.literal(config.titleText))
                MC.gui.hud.setSubtitle(Component.literal(config.subtitleText))
                MC.gui.hud.setTimes(10, 100, 30)
                
                MC.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f))
                MC.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.2f))
                return
            }
            message = "Кровавая комната: ${String.format(java.util.Locale.ROOT, "%.1f", remaining / 1000.0)}с"
        } else {
            return
        }
        
        // HUD теперь красного цвета (0xFFFF5555)
        this.showBackground = StarredHeltix.feature.dungeons.bloodRoom.showBackground
        drawBackground(getWidth(), getHeight())
        cachedGraphics?.drawString(MC.font, message, x, y, 0xFFFF5555.toInt())
    }

    override fun getWidth(): Int = MC.font.width("Кровавая комната: 9999.0с")
    override fun getHeight(): Int = MC.font.lineHeight

    override fun getDefaultScale(): Float = 1.0f
    override fun getDefaultX(): Int = 14
    override fun getDefaultY(): Int = 149
}
