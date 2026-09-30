package set.starlev.features.combat.dungeons

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.render.HudElement
import set.starlev.utils.detectors.DungeonDetector
import set.starlev.utils.detectors.ScoreboardDetector
import java.util.regex.Pattern
import set.starlev.render.drawString
import set.starlev.render.drawCenteredString

object ScoreCounter {
    private val MC = Minecraft.getInstance()
    private val SCORE_PATTERN = Pattern.compile("(?i)(?:Зачищено|Cleared|Очки|Баллы|Score):?\\s*(?:\\d+(?:\\.\\d+)?%)?\\s*\\(?(\\d+)\\)?")
    
    private var currentScore = 0
    private var notified270 = false
    private var title270Notified = false
    private var notified300 = false
    private var title300Notified = false
    private var lastUpdate = 0L

    fun init() {
        // Тик для уведомлений даже без HUD — привязан к карте
        set.starlev.render.RenderEvents.register {
            // Обновляем только в данже, иначе сбрасываем
            if (!set.starlev.utils.detectors.DungeonDetector.isInDungeon()) {
                if (currentScore != 0 || notified270 || notified300) {
                    currentScore = 0
                    notified270 = false
                    title270Notified = false
                    notified300 = false
                    title300Notified = false
                    BloodRoomTimer.isBloodReady = false
                }
                return@register
            }
            updateScore()
        }
    }

    private fun updateScore() {
        if (!set.starlev.utils.detectors.DungeonDetector.isInDungeon()) {
            currentScore = 0
            notified270 = false
            title270Notified = false
            notified300 = false
            title300Notified = false
            BloodRoomTimer.isBloodReady = false
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdate < 500) return
        lastUpdate = currentTime

        val lines = set.starlev.utils.detectors.ScoreboardDetector.getScoreboardText()
        var foundScore = 0
        
        for (line in lines) {
            val matcher = SCORE_PATTERN.matcher(line)
            if (matcher.find()) {
                foundScore = matcher.group(1).toIntOrNull() ?: 0
                break
            }
        }

        currentScore = foundScore
        
        val config = StarredHeltix.feature.dungeons.visuals.dungeonScore
        
        if (config.enabled && currentScore >= 270 && !notified270) {
            notified270 = true
            MC.player?.connection?.sendCommand("pc ${config.message}")
        }
        
        if (config.enabled && config.title270Enabled && !title270Notified && currentScore >= 270) {
            title270Notified = true
            MC.gui.hud.setTitle(Component.literal(config.title270Text))
            MC.gui.hud.setSubtitle(Component.literal(config.subtitle270Text))
            MC.gui.hud.setTimes(10, 100, 30)
            MC.soundManager.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1.0f))
        }

        if (config.enabled && currentScore >= 300 && !notified300) {
            notified300 = true
            MC.player?.connection?.sendCommand("pc ${config.message300}")
        }

        if (config.enabled && config.title300Enabled && !title300Notified && currentScore >= 300) {
            title300Notified = true
            MC.gui.hud.setTitle(Component.literal(config.title300Text))
            MC.gui.hud.setSubtitle(Component.literal(config.subtitle300Text))
            MC.gui.hud.setTimes(10, 100, 30)
            MC.soundManager.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1.0f))
        }
        
        if (currentScore < 270) {
            notified270 = false
            title270Notified = false
        }
        if (currentScore < 300) {
            notified300 = false
            title300Notified = false
        }
    }

    /** Для DungeonMapHud — текущий счёт, 0 если не в данже. Обновляет кэш если нужно. */
    fun getCurrentScore(): Int {
        updateScore()
        return currentScore
    }

    fun getScoreColor(): String = when {
        currentScore >= 300 -> "§6"
        currentScore >= 270 -> "§a"
        else -> "§c"
    }

    fun isEnabled(): Boolean = try { StarredHeltix.feature.dungeons.visuals.dungeonScore.enabled } catch (_: Exception) { false }

    // Совместимость: старый HudElement API больше не используется, но оставляем для миграций
    fun getDisplayText(isEditing: Boolean = false): String {
        val scoreText = if (isEditing) "300" else currentScore.toString()
        val color = getScoreColor()
        return "Очков: $color$scoreText"
    }
}
