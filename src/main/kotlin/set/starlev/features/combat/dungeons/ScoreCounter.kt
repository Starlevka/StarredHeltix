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
    private val SCORE_PATTERN = Pattern.compile("(?i)(?:Зачищено|Cleared|Очки|Баллы|Score):?\\s*(?:\\d+(?:\\.\\d+)?%)?\\s*\\(?(\\d+)\\)?")
    
    private var currentScore = 0
    private var notified270 = false
    private var title270Notified = false
    private var notified300 = false
    private var title300Notified = false
    private var lastUpdate = 0L

    fun init() {
        // Инициализация не требует регистрации событий, так как мы проверяем скорборд в render
    }

    private fun updateScore() {
        if (!DungeonDetector.isInDungeon()) {
            currentScore = 0
            notified270 = false
            title270Notified = false
            notified300 = false
            title300Notified = false
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

        // Очки найдены, обновляем текущее значение
        currentScore = foundScore
        
        val config = StarredHeltix.feature.dungeons.scoreCounter
        
        // Логика уведомления 270 очков (отдельно)
        if (config.enabled && currentScore >= 270 && !notified270) {
            notified270 = true
            MC.player?.connection?.sendCommand("pc ${config.message}")
        }
        
        // Логика Title для 270 очков
        if (config.enabled && config.title270Enabled && !title270Notified && currentScore >= 270) {
            title270Notified = true
            MC.gui.setTitle(Component.literal(config.title270Text))
            MC.gui.setSubtitle(Component.literal(config.subtitle270Text))
            MC.gui.setTimes(10, 100, 30)
            MC.soundManager.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1.0f))
        }

        // Логика уведомления 300 очков (отдельно, независимо от 270)
        if (config.enabled && currentScore >= 300 && !notified300) {
            notified300 = true
            MC.player?.connection?.sendCommand("pc ${config.message300}")
        }

        // Логика Title для 300 очков (отдельно)
        if (config.enabled && config.title300Enabled && !title300Notified && currentScore >= 300) {
            title300Notified = true
            MC.gui.setTitle(Component.literal(config.title300Text))
            MC.gui.setSubtitle(Component.literal(config.subtitle300Text))
            MC.gui.setTimes(10, 100, 30)
            MC.soundManager.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1.0f))
        }
        
        // Сброс всех уведомлений, если очки упали ниже порога
        if (currentScore < 270) {
            notified270 = false
            title270Notified = false
        }
        if (currentScore < 300) {
            notified300 = false
            title300Notified = false
        }
    }

    override fun render() {
        if (!StarredHeltix.feature.dungeons.scoreCounter.enabled) return
        
        updateScore()
        
        if (currentScore == 0 && !isEditing) return

        val config = StarredHeltix.feature.dungeons.scoreCounter
        val scoreText = if (isEditing) "300" else currentScore.toString()
        val color = when {
            currentScore >= 300 -> "§6" // Gold for 300+
            currentScore >= 270 -> "§a" // Green for 270+
            else -> "§c" // Red for below 270
        }
        val message = "Очков подземелья: $color$scoreText"

        this.showBackground = config.showBackground
        drawBackground(getWidth(), getHeight())
        cachedGraphics?.drawString(MC.font, message, x, y, 0xFFFFFFFF.toInt())
    }

    override fun getWidth(): Int = MC.font.width("Очков подземелья: 300")
    override fun getHeight(): Int = MC.font.lineHeight

    override fun getDefaultScale(): Float = 1.0f
    override fun getDefaultX(): Int = 161
    override fun getDefaultY(): Int = 14
}
