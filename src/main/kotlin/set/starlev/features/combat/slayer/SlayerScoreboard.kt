package set.starlev.features.combat.slayer

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.utils.detectors.ScoreboardDetector
import set.starlev.utils.PersonalBestManager
import java.util.Locale

object SlayerScoreboard {
    private val mc = Minecraft.getInstance()
    private var bossStartTime = 0L
    private var isBossTimerRunning = false
    private var currentSlayerType = ""
    private var currentSlayerTier = ""

    fun init() {
    }

    fun getExtraLines(): List<Component> {
        val scoreboard = ScoreboardDetector.getScoreboardText()
        val result = mutableListOf<Component>()
        val config = StarredHeltix.feature.slayer.slayerHud
        
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
        if (config.bossTimer) {
            val hasKillBossMsg = scoreboard.any { it.contains("Убейте босса!", ignoreCase = true) || it.contains("Kill the boss!", ignoreCase = true) }
            val hasBossKilledMsg = scoreboard.any { it.contains("Босс убит!", ignoreCase = true) || it.contains("Boss slain!", ignoreCase = true) }

            if (hasKillBossMsg && !isBossTimerRunning) {
                bossStartTime = System.currentTimeMillis()
                isBossTimerRunning = true
            } else if ((hasBossKilledMsg || !hasKillBossMsg) && isBossTimerRunning) {
                if (hasBossKilledMsg) {
                    handleBossKill()
                }
                isBossTimerRunning = false
            }
        } else {
            isBossTimerRunning = false
        }

        // 3. Формирование строк
        if (config.bossTimer && isBossTimerRunning) {
            val dur = (System.currentTimeMillis() - bossStartTime) / 1000.0
            result.add(Component.literal("§cТаймер: §b${String.format(Locale.US, "%.1f", dur)}s"))
        } else if (scoreboard.any { it.contains("spawned", ignoreCase = true) || it.contains("появился", ignoreCase = true) }) {
             result.add(Component.literal("§c☠ Босс!"))
        }

        return result
    }

    private fun handleBossKill() {
        val durationMs = System.currentTimeMillis() - bossStartTime
        val durationSec = durationMs / 1000.0
        val localeUS = Locale.US
        val formattedTime = String.format(localeUS, "%.3f", durationSec)

        val bossName = if (currentSlayerType.isEmpty()) "Босса" else currentSlayerType
        val bossTier = currentSlayerTier

        val config = StarredHeltix.feature.slayer.slayerHud
        var message = "§7[§dStarredHeltix§7] §fВы одолели босса §d$bossName $bossTier §fза §b$formattedTime §fсекунд!"

        if (config.personalBests) {
            val (isNewPB, oldPB) = PersonalBestManager.updatePB(bossName, bossTier, durationSec)
            if (isNewPB) {
                message += " §6§l(НОВЫЙ ЛИЧНЫЙ РЕКОРД!)"
            } else if (oldPB != null) {
                val formattedOldPB = String.format(localeUS, "%.3f", oldPB)
                message += " §7(Прошлый личный рекорд - $formattedOldPB секунд)"
            }
        }

        val finalMessage = message
        mc.execute {
            mc.player?.displayClientMessage(Component.literal(finalMessage), false)
        }
    }

    }
