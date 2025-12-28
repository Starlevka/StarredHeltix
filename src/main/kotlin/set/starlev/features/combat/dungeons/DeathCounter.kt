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

    fun init() {
        ChatEventsManager.registerIncoming { message ->
            if (config.deathDetect) {
                // Ищем символ смерти и ник игрока после него
                val deathIndex = message.indexOf("☠")
                if (deathIndex != -1 && deathIndex + 1 < message.length) {
                    // Есть символ смерти и после него есть текст
                    trigger()
                }
            }
        }

        ClientTickEvents.END_CLIENT_TICK.register {
            if (!config.deathDetect) return@register
            
            val currentTitle = TitleDetector.getTitleText()
            val deathIndexTitle = currentTitle.indexOf("☠")
            if (deathIndexTitle != -1 && deathIndexTitle + 1 < currentTitle.length && currentTitle != lastTitle) {
                trigger()
            }
            lastTitle = currentTitle

            val currentActionBar = ActionBarDetector.getActionBarText()
            val deathIndexAction = currentActionBar.indexOf("☠")
            if (deathIndexAction != -1 && deathIndexAction + 1 < currentActionBar.length && currentActionBar != lastActionBar) {
                trigger()
            }
            lastActionBar = currentActionBar
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
