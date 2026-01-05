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

    private val DEATH_PATTERN = java.util.regex.Pattern.compile("^☠\\s*(\\w+)")

    fun init() {
        ChatEventsManager.registerIncoming { message ->
            if (config.deathDetect) {
                // Ищем символ черепка и ник игрока после него
                val cleanMessage = message.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "")
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
                val cleanTitle = currentTitle.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "")
                val matcher = DEATH_PATTERN.matcher(cleanTitle)
                if (matcher.find()) {
                    trigger()
                }
                lastTitle = currentTitle
            }

            val currentActionBar = ActionBarDetector.getActionBarText()
            if (currentActionBar != lastActionBar) {
                val cleanActionBar = currentActionBar.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "")
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
        val level = mc.level ?: return
        
        // Проверка на нахождение в подземельях
        if (!level.dimension().location().toString().startsWith("minecraft:dungeon_")) return
        
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
