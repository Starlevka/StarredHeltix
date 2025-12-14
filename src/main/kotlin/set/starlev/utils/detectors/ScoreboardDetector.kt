package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam

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
            
            // Получить все scores этого объектива
            val scores = scoreboard.listPlayerScores(objective)
            
            return scores.sortedByDescending { it.value }
                .take(15)
                .map { score ->
                    val owner = score.owner
                    val team = scoreboard.getPlayersTeam(owner)
                    val displayName = PlayerTeam.formatNameForTeam(team, Component.literal(owner))
                    displayName.string
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
