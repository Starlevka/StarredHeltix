package set.starlev.features.combat.dungeons

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import set.starlev.StarredHeltix
import set.starlev.features.chat.ChatEventsManager
import set.starlev.utils.detectors.ActionBarDetector
import set.starlev.utils.detectors.TitleDetector

object DeathCounter {
    private val mc = Minecraft.getInstance()
    private val config get() = StarredHeltix.feature.dungeons.deathCounter
    private var lastTriggered = 0L
    private const val COOLDOWN = 5000L // 5 seconds cooldown to prevent spam

    private var lastTitle = ""
    private var lastActionBar = ""

    private val DEATH_PATTERN = java.util.regex.Pattern.compile("^☠\\s+(?:.*\\s+)?([a-zA-Z0-9_]{3,16})\\s+[а-яА-ЯёЁ]")

    fun init() {
        ChatEventsManager.registerIncoming { message ->
            if (config.deathDetect) {
                // Ищем символ черепка, ник игрока и фразу "был убит"
                val cleanMessage = message.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
                val matcher = DEATH_PATTERN.matcher(cleanMessage)
                if (matcher.find()) {
                    trigger()
                }
            }
        }

        ClientTickEvents.END_CLIENT_TICK.register {
            if (!config.deathDetect) return@register
            
            val currentTitle = TitleDetector.getTitleText()
            if (currentTitle != lastTitle) {
                val cleanTitle = currentTitle.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
                val matcher = DEATH_PATTERN.matcher(cleanTitle)
                if (matcher.find()) {
                    trigger()
                }
                lastTitle = currentTitle
            }

            val currentActionBar = ActionBarDetector.getActionBarText()
            if (currentActionBar != lastActionBar) {
                val cleanActionBar = currentActionBar.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
                val matcher = DEATH_PATTERN.matcher(cleanActionBar)
                if (matcher.find()) {
                    trigger()
                }
                lastActionBar = currentActionBar
            }
        }
    }

    fun trigger() {
        if (!config.deathDetect) return
        
        // Проверка на нахождение в подземельях через заголовок скорборда
        val title = set.starlev.utils.detectors.ScoreboardDetector.getScoreboardTitle()
        val isDungeon = title.contains("КАТАКОМБЫ", ignoreCase = true) || 
                        title.contains("CATACOMBS", ignoreCase = true) ||
                        (mc.level?.dimension()?.location()?.toString()?.startsWith("minecraft:dungeon_") ?: false)
        
        if (!isDungeon) return
        
        val now = System.currentTimeMillis()
        if (now - lastTriggered < COOLDOWN) return
        
        lastTriggered = now
        sendDeathMessage()
    }

    private fun sendDeathMessage() {
        val player = mc.player ?: return
        val message = config.deathMessage
        player.connection?.sendCommand("pc $message")
    }
}
