package set.starlev.features.combat.solvers.dungeons

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement
import set.starlev.utils.detectors.ScoreboardDetector
import java.util.regex.Pattern

object BloodRoomTimer : HudElement("BloodRoomTimer") {
    private val CLIENT = Minecraft.getInstance()
    private var bloodRoomEndTime = 0L
    private var notificationEndTime = 0L
    private var bloodRoomTimerActive = false
    private var notificationActive = false
    
    private val BOSS_MESSAGE_PATTERN = Pattern.compile(".*\\[БОСС] Наблюдатель: Поздравляю.*")
    private val FLOOR_PATTERN = Pattern.compile(".*# (\\d+) этаж.*")
    
    fun register() {
        ClientReceiveMessageEvents.GAME.register(this::onChatMessage)
    }
    
    private fun onChatMessage(message: Component, overlay: Boolean) {
        if (!StarredHeltix.feature.dungeons.bloodRoom.enabled) return
        
        val messageString = message.string
        val matcher = BOSS_MESSAGE_PATTERN.matcher(messageString)
        if (matcher.matches()) {
            val floor = getCurrentFloor()
            val delay = when (floor) {
                1 -> 38000L
                2 -> 41000L
                3 -> 50000L
                else -> 30000L
            }
            bloodRoomEndTime = System.currentTimeMillis() + delay
            bloodRoomTimerActive = true
        }
    }
    
    private fun getCurrentFloor(): Int {
        try {
            val scoreboardLines = ScoreboardDetector.getScoreboardText()
            for (line in scoreboardLines) {
                val matcher = FLOOR_PATTERN.matcher(line)
                if (matcher.matches()) {
                    return matcher.group(1)?.toIntOrNull() ?: 1
                }
            }
        } catch (e: Exception) {
            StarredHeltix.LOGGER.warn("Ошибка при определении этажа: ${e.message}")
        }
        return 1
    }
    
    override fun render() {
        if (!StarredHeltix.feature.dungeons.bloodRoom.enabled) return
        
        val currentTime = System.currentTimeMillis()
        val message: String
        
        if (isEditing) {
            message = "Кровавая комната: 9999.0с"
        } else if (notificationActive) {
            if (currentTime >= notificationEndTime) {
                notificationActive = false
                return
            }
            message = "ᯓ★ Кровавая комната заполнена!"
        } else if (!bloodRoomTimerActive) {
            return
        } else if (currentTime >= bloodRoomEndTime) {
            notificationActive = true
            notificationEndTime = currentTime + 2000
            CLIENT.player?.connection?.sendCommand("pc " + StarredHeltix.feature.dungeons.bloodRoom.message)
            CLIENT.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
            bloodRoomTimerActive = false
            message = "ᯓ★ Кровавая комната заполнена!"
        } else {
            val timeLeft = (bloodRoomEndTime - currentTime) / 1000.0
            message = "Кровавая комната: " + String.format("%.1f", timeLeft) + "с"
        }
        
        cachedGraphics?.drawString(CLIENT.font, message, x, y, 0xFFFF0000.toInt(), true)
    }

    override fun getWidth() = CLIENT.font.width("Кровавая комната: 30.0с") * 2
    override fun getHeight() = CLIENT.font.lineHeight * 2
    
    override fun getDefaultX(): Int {
        val window = CLIENT.window
        return (window.guiScaledWidth / 2) - (getWidth() / 2)
    }
    
    override fun getDefaultY(): Int {
        val window = CLIENT.window
        return (window.guiScaledHeight / 2) - 40
    }
}
