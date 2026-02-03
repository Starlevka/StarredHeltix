package set.starlev.features.combat.dungeons

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement
import set.starlev.utils.detectors.ScoreboardDetector
import java.util.regex.Pattern

object BloodRoomTimer : HudElement("BloodRoomTimer") {
    private val MC = Minecraft.getInstance()
    private var endTime = 0L
    private var noteEndTime = 0L
    private var active = false
    private var noteActive = false
    
    // Публичный флаг готовности кровавой комнаты для других модулей (например, ScoreCounter)
    var isBloodReady = false
    
    private val BOSS_PATTERN = Pattern.compile(".*\\[БОСС] Наблюдатель: Поздравляю.*")
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
        
        // Проверка на нахождение в подземельях
        if (!level.dimension().location().toString().startsWith("minecraft:dungeon_")) return
        
        val text = message.string
        val matcher = BOSS_PATTERN.matcher(text)
        if (matcher.find()) {
            val floor = getFloor()
            val delay = when (floor) {
                1 -> 38000L
                2 -> 42000L
                3 -> 50000L
                else -> 31000L
            }
            endTime = System.currentTimeMillis() + delay
            active = true
            isBloodReady = false // Сбрасываем готовность при начале нового таймера
            
            StarredHeltix.LOGGER.info("BloodRoomTimer: [DEBUG] Chat trigger: $text")
            if (floor == -1) {
                StarredHeltix.LOGGER.info("BloodRoomTimer: Этаж не найден, используем стандартную задержку 30с")
            } else {
                StarredHeltix.LOGGER.info("BloodRoomTimer: Определен этаж $floor, задержка ${delay/1000}с")
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
                    StarredHeltix.LOGGER.info("BloodRoomTimer: Этаж $floor найден в заголовке скорборда: $title")
                    return floor
                }
            }

            // 2. Проверяем линии скорборда
            val scoreboardLines = ScoreboardDetector.getScoreboardText()
            StarredHeltix.LOGGER.info("BloodRoomTimer Debug Scoreboard Lines: ${scoreboardLines.size}")
            
            for (line in scoreboardLines) {
                StarredHeltix.LOGGER.info("BloodRoomTimer Line: '$line'")
                for (pattern in FLOOR_PATTERNS) {
                    val m = pattern.matcher(line)
                    if (m.find()) {
                        val floor = romanToInt(m.group(1))
                        if (floor != null) {
                            StarredHeltix.LOGGER.info("BloodRoomTimer: Этаж $floor найден в строке скорборда: $line (паттерн: ${pattern.pattern()})")
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
    
    override fun render() {
        if (!StarredHeltix.feature.dungeons.bloodRoom.enabled) return
        
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
                
                // Показываем Title
                MC.gui.setTitle(Component.literal("§cКровавая комната заполнена!"))
                MC.gui.setTimes(10, 40, 10)
                
                MC.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f))
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
