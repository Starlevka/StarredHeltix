package set.starlev.features.skyblock.scoreboard.elements

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.features.skyblock.HudScoreboard.ScoreboardLine
import set.starlev.features.skyblock.scoreboard.ScoreboardElement
import set.starlev.secret.features.SecretFunFeatures

object ScoreboardElementPing : ScoreboardElement() {
    override fun getDisplay(): List<ScoreboardLine> {
        val mc = Minecraft.getInstance()
        val ping = mc.player?.let { mc.connection?.getPlayerInfo(it.uuid)?.latency } ?: 0
        val processed = SecretFunFeatures.processComponent(Component.literal("§7Ping: §a${ping}ms"), true)
        return listOf(ScoreboardLine("ping", processed, centered = false))
    }
    
    override val configLine = "§aPing"
}