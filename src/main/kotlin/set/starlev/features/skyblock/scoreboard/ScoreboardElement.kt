package set.starlev.features.skyblock.scoreboard

import net.minecraft.network.chat.Component
import set.starlev.features.skyblock.HudScoreboard.ScoreboardLine

abstract class ScoreboardElement {
    abstract fun getDisplay(): List<ScoreboardLine>
    open fun showWhen(): Boolean = true
    abstract val configLine: String
    open fun showIsland(): Boolean = true
}