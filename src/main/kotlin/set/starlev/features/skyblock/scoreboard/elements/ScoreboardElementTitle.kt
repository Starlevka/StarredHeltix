package set.starlev.features.skyblock.scoreboard.elements

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.features.skyblock.HudScoreboard.ScoreboardLine
import set.starlev.features.skyblock.scoreboard.ScoreboardElement
import set.starlev.secret.features.SecretFunFeatures

object ScoreboardElementTitle : ScoreboardElement() {
    override fun getDisplay(): List<ScoreboardLine> {
        val mc = Minecraft.getInstance()
        val scoreboard = mc.level?.scoreboard
        val objective = scoreboard?.getDisplayObjective(net.minecraft.world.scores.DisplaySlot.SIDEBAR)
        if (objective == null) return emptyList()
        
        val processedTitle = SecretFunFeatures.processComponent(objective.displayName, true)
        return listOf(ScoreboardLine("title", processedTitle, centered = true))
    }
    
    override val configLine = "§6§lЗаголовок"
}