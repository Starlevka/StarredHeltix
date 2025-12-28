package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam
import set.starlev.StarredHeltix

object ScoreboardDetector {

    /**
     * Получить информацию со скорборда
     * @return Список строк скорборда (левая сторона экрана)
     */
    fun getScoreboardText(): List<String> {
        val client = Minecraft.getInstance()
        val scoreboard = client.level?.scoreboard ?: return emptyList()
        
        return try {
            val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return emptyList()
            
            // Получаем все очки для этого объектива
            val scores = scoreboard.listPlayerScores(objective)
            
            return scores.sortedByDescending { it.value }
                .take(15)
                .map { score ->
                    val owner = score.owner
                    val team = scoreboard.getPlayersTeam(owner)
                    
                    // Если у очка есть кастомное отображение (1.21+), используем его
                    // Иначе используем имя владельца с учетом префикса/суффикса команды
                    val lineComponent = score.display ?: if (team != null) {
                        PlayerTeam.formatNameForTeam(team, Component.literal(owner))
                    } else {
                        Component.literal(owner)
                    }
                    
                    val cleanText = lineComponent.string.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
                    cleanText
                }
                .filter { it.isNotEmpty() }
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
        return objective.displayName.string.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
    }
}
