package set.starlev.features.skyblock.scoreboard.elements

import net.minecraft.network.chat.Component
import set.starlev.features.skyblock.HudScoreboard.ScoreboardLine
import set.starlev.features.skyblock.scoreboard.ScoreboardElement

object ScoreboardElementEmptyLine : ScoreboardElement() {
    override fun getDisplay(): List<ScoreboardLine> {
        return listOf(ScoreboardLine("empty", Component.literal(""), centered = false))
    }
    
    override val configLine = "§7Пустая строка"
}