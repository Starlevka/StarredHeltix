package set.starlev.features.combat.slayer

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.utils.detectors.ScoreboardDetector
import set.starlev.utils.detectors.SkillXpDetector
import set.starlev.utils.PersonalBestManager
import java.util.Locale
import kotlin.math.ceil

object SlayerScoreboard {
    private val mc = Minecraft.getInstance()
    private var lastCombatXpPerMob = 50.0
    private var isFirstKill = true
    private var bossStartTime = 0L
    private var isBossTimerRunning = false
    private var currentSlayerType = ""
    private var currentSlayerTier = ""

    fun init() {
        SkillXpDetector.registerListener { info ->
            if (info.skill.contains("Бой", ignoreCase = true)) {
                updateCombatXp(info.gained)
            }
        }
    }

    fun getExtraLines(): List<Component> {
        val scoreboard = ScoreboardDetector.getScoreboardText()
        val result = mutableListOf<Component>()
        
        // 1. Детекция типа слеера
        val slayerTypes = listOf("Мститель", "Тарантул", "Свен", "Revenant", "Tarantula", "Sven", "Voidgloom", "Inferno", "Riftstalker")
        var foundSlayer = false
        for (line in scoreboard) {
            for (type in slayerTypes) {
                if (line.contains(type, ignoreCase = true)) {
                    currentSlayerType = type
                    val tierMatch = Regex("\\d+").find(line)
                    currentSlayerTier = tierMatch?.value ?: ""
                    foundSlayer = true
                    break
                }
            }
            if (foundSlayer) break
        }

        if (!foundSlayer) {
            currentSlayerType = ""
            currentSlayerTier = ""
            isBossTimerRunning = false
            return emptyList()
        }

        // 2. Логика таймера босса
        val hasKillBossMsg = scoreboard.any { it.contains("Убейте босса!", ignoreCase = true) || it.contains("Kill the boss!", ignoreCase = true) }
        val hasBossKilledMsg = scoreboard.any { it.contains("Босс убит!", ignoreCase = true) || it.contains("Boss slain!", ignoreCase = true) }

        if (hasKillBossMsg && !isBossTimerRunning) {
            bossStartTime = System.currentTimeMillis()
            isBossTimerRunning = true
        } else if ((hasBossKilledMsg || !hasKillBossMsg) && isBossTimerRunning) {
            // В идеале тут должен быть вызов handleBossKill, но для скорборда просто стопаем таймер
            isBossTimerRunning = false
        }

        // 3. Формирование строк
        val progressLine = scoreboard.find { it.contains("/") && (it.contains("опыта", ignoreCase = true) || it.contains("XP", ignoreCase = true)) }
        
        if (progressLine != null) {
            val cleanProgress = progressLine.replace(",", "")
            val xpMatch = Regex("(\\d+)/(\\d+)").find(cleanProgress)
            
            if (xpMatch != null) {
                val currentXp = xpMatch.groupValues[1].toDouble()
                val targetXp = xpMatch.groupValues[2].toDouble()
                val remaining = targetXp - currentXp
                
                if (remaining > 0) {
                    val mobs = ceil(remaining / lastCombatXpPerMob).toInt()
                    result.add(Component.literal("§cМобов: §b$mobs"))
                }
            }
        }

        if (isBossTimerRunning) {
            val dur = (System.currentTimeMillis() - bossStartTime) / 1000.0
            result.add(Component.literal("§cТаймер: §b${String.format(Locale.US, "%.1f", dur)}s"))
        } else if (scoreboard.any { it.contains("spawned", ignoreCase = true) || it.contains("появился", ignoreCase = true) }) {
             result.add(Component.literal("§c☠ Босс!"))
        }

        return result
    }

    // Обновление опыта за моба (вызывается из SkillXpDetector)
    fun updateCombatXp(gained: Double) {
        if (gained < 700.0) {
            lastCombatXpPerMob = gained
        }
    }
}
