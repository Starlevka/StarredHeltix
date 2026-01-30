package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam
import set.starlev.StarredHeltix
import set.starlev.utils.CacheManager

object ScoreboardDetector {

    private const val COLOR_PATTERN = "(?i)§[0-9a-fk-orlnmxz]"

    /**
     * Получить информацию со скорборда
     * @return Список строк скорборда (левая сторона экрана)
     */
    fun getScoreboardText(): List<String> {
        val client = Minecraft.getInstance()
        val scoreboard = client.level?.scoreboard ?: return emptyList()
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return emptyList()
        val objectiveId = objective.name

        val cached = CacheManager.getCachedScoreboard(objectiveId)
        if (cached != null) return cached

        return try {
            // Получаем все очки для этого объектива
            val scores = scoreboard.listPlayerScores(objective)
            
            val result = scores.sortedByDescending { it.value }
                .take(15)
                .map { score ->
                    val owner = score.owner
                    val team = scoreboard.getPlayersTeam(owner)
                    
                    val lineComponent = score.display ?: if (team != null) {
                        PlayerTeam.formatNameForTeam(team, Component.literal(owner))
                    } else {
                        Component.literal(owner)
                    }
                    
                    val cleanText = lineComponent.string.replace(CacheManager.getRegex(COLOR_PATTERN), "").trim()
                    cleanText
                }
                .filter { it.isNotEmpty() }
            
            CacheManager.cacheScoreboard(objectiveId, result)
            result
        } catch (e: Exception) {
            StarredHeltix.LOGGER.error("ScoreboardDetector error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Получить заголовок скорборда (обычно название сервера или локации)
     */
    fun getScoreboardTitle(): String {
        val client = Minecraft.getInstance()
        val scoreboard = client.level?.scoreboard ?: return ""
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return ""
        return objective.displayName.string.replace(CacheManager.getRegex(COLOR_PATTERN), "").trim()
    }
}
