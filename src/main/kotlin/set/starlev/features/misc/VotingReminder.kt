package set.starlev.features.misc

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.ModSounds
import set.starlev.StarredHeltix
import set.starlev.features.chat.ChatEventsManager
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Timer
import java.util.TimerTask

object VotingReminder {
    private val mc = Minecraft.getInstance()
    private val moscowZone = ZoneId.of("Europe/Moscow")
    private var isReminderScheduled = false

    fun init() {
        ChatEventsManager.registerIncoming { message ->
            if (message.contains("Спасибо за голосование") || message.contains("Вы успешно проголосовали")) {
                markAsVoted()
            }
        }
    }

    fun checkAndShowReminder() {
        val config = StarredHeltix.feature.misc.votingReminder
        if (!config.enabled) return

        checkAndResetDailyVoteStatus()

        // Additional voting check: check inventory for voting crate
        val player = mc.player
        if (player != null) {
            val hasVotingCrate = (0..35).any { player.inventory.getItem(it).displayName.string.contains("Voting Crate") } ||
                    player.offhandItem.displayName.string.contains("Voting Crate")
            if (hasVotingCrate && !config.hasVotedToday) {
                config.hasVotedToday = true
                StarredHeltix.configManager.saveConfig("voted-detected")
            }
        }

        if (!config.hasShownReminderToday && !config.hasVotedToday && !isReminderScheduled) {
            isReminderScheduled = true
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    showReminder()
                    config.hasShownReminderToday = true
                    StarredHeltix.configManager.saveConfig("reminder-shown")
                    isReminderScheduled = false
                }
            }, 180000L)
        }
    }

    private fun checkAndResetDailyVoteStatus() {
        val config = StarredHeltix.feature.misc.votingReminder
        val now = LocalDateTime.now(moscowZone)
        val lastCheck = LocalDateTime.ofEpochSecond(config.lastCheckTime / 1000, 0, java.time.ZoneOffset.UTC)
        val nextResetTime = getNextDailyResetTime(lastCheck)

        if (now.isAfter(nextResetTime)) {
            config.hasVotedToday = false
            config.hasShownReminderToday = false
            isReminderScheduled = false
            config.lastCheckTime = System.currentTimeMillis()
            StarredHeltix.configManager.saveConfig("voting-reset")
        }
    }

    private fun getNextDailyResetTime(lastResetTime: LocalDateTime): LocalDateTime {
        var nextReset = lastResetTime.withHour(14).withMinute(0).withSecond(0).withNano(0)
        if (nextReset.isBefore(LocalDateTime.now(moscowZone))) {
            nextReset = nextReset.plusDays(1)
        }
        return nextReset
    }

    fun markAsVoted() {
        val config = StarredHeltix.feature.misc.votingReminder
        config.hasVotedToday = true
        StarredHeltix.configManager.saveConfig("voted")
        Timer().schedule(object : TimerTask() {
            override fun run() {
                mc.player?.displayClientMessage(
                    Component.literal("§aСпасибо за голосование! Следующее напоминание завтра в 14:00 МСК."),
                    false
                )
            }
        }, 1000)
    }

    fun showNow() {
        if (!StarredHeltix.feature.misc.votingReminder.enabled) return
        showReminder()
    }

    private fun showReminder() {
        mc.player?.displayClientMessage(
            Component.literal("§6§lНе забудьте проголосовать за сервер! §e/голосование"),
            false
        )
        mc.gui?.setTitle(Component.literal("§6§lГолосование"))
        mc.gui?.setSubtitle(Component.literal("§eНе забудьте проголосовать за сервер!"))
        mc.player?.playSound(ModSounds.VOTING_REMINDER, 1.0f, 1.0f)
    }
}
