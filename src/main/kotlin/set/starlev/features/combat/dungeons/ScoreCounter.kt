package set.starlev.features.combat.dungeons

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement
import set.starlev.utils.detectors.DungeonDetector
import set.starlev.utils.detectors.ScoreboardDetector
import java.util.regex.Pattern

object ScoreCounter : HudElement("ScoreCounter") {
    private val MC = Minecraft.getInstance()
    private val SCORE_PATTERN = Pattern.compile("(?i)Зачищено:\\s*\\d+\\.\\d+%\\s*\\((\\d+)\\)")
    
    private var currentScore = 0
    private var notified = false
    private var titleNotified = false
    private var lastUpdate = 0L

    fun init() {
        // Инициализация не требует регистрации событий, так как мы проверяем скорборд в render
    }

    private fun updateScore() {
        if (!DungeonDetector.isInDungeon()) {
            currentScore = 0
            notified = false
            titleNotified = false
            BloodRoomTimer.isBloodReady = false // Сбрасываем статус блада при выходе из данжа
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdate < 500) return // Обновляем раз в полсекунды
        lastUpdate = currentTime

        val lines = ScoreboardDetector.getScoreboardText()
        var foundScore = 0
        
        for (line in lines) {
            val matcher = SCORE_PATTERN.matcher(line)
            if (matcher.find()) {
                foundScore = matcher.group(1).toIntOrNull() ?: 0
                break
            }
        }

        currentScore = foundScore
        
        val config = StarredHeltix.feature.dungeons.scoreCounter
        if (config.enabled && currentScore >= 270 && !notified) {
            notified = true
            MC.player?.connection?.sendCommand("pc ${config.message}")
        } else if (currentScore < 270) {
            notified = false // Сбрасываем, если очки упали (новое подземелье)
            titleNotified = false
        }
        
        // Логика уведомления Title (270+ и Блад готов)
        if (config.enabled && config.title270AndBlood && !titleNotified) {
            if (currentScore >= 270 && BloodRoomTimer.isBloodReady) {
                titleNotified = true
                MC.gui.setTitle(Component.literal("§a270+ Очков & Блад готов!"))
                MC.gui.setSubtitle(Component.literal("§eМожно идти к боссу"))
                MC.gui.setTimes(10, 60, 20)
                MC.soundManager.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1.0f))
            }
        }
    }

    override fun render() {
        if (!StarredHeltix.feature.dungeons.scoreCounter.enabled) return
        
        updateScore()
        
        if (currentScore == 0 && !isEditing) return

        val config = StarredHeltix.feature.dungeons.scoreCounter
        val scoreText = if (isEditing) "270" else currentScore.toString()
        val color = if (currentScore >= 270) "§a" else "§c"
        val message = "Очков подземелья: $color$scoreText"

        this.showBackground = config.showBackground
        drawBackground(getWidth(), getHeight())
        cachedGraphics?.drawString(MC.font, message, x, y, 0xFFFFFFFF.toInt())
    }

    override fun getWidth(): Int = MC.font.width("Очков подземелья: 270")
    override fun getHeight(): Int = MC.font.lineHeight

    override fun getDefaultScale(): Float = 1.0f
    override fun getDefaultX(): Int = 161
    override fun getDefaultY(): Int = 14
}
