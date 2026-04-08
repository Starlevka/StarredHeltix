package set.starlev.features.skyblock.scoreboard.elements

import net.minecraft.network.chat.Component
import set.starlev.features.skyblock.HudScoreboard.ScoreboardLine
import set.starlev.features.skyblock.scoreboard.ScoreboardElement
import set.starlev.secret.features.SecretFunFeatures
import set.starlev.utils.detectors.ScoreboardDetector
import set.starlev.utils.detectors.TabListDetector

object ScoreboardElementCookie : ScoreboardElement() {
    override fun getDisplay(): List<ScoreboardLine> {
        var status = ScoreboardDetector.getCookieStatus()
        if (status == "Не активно!") {
            val tabLines = TabListDetector.getAllTabListLines()
            for (i in tabLines.indices) {
                val line = tabLines[i]
                if (line.contains("Бонус печенья") || line.contains("Cookie Buff")) {
                    if (i + 1 < tabLines.size) {
                        val next = tabLines[i + 1]
                        if (next.any { it.isDigit() }) {
                            status = next
                            break
                        }
                    }
                }
            }
        }

        val displayText = if (status == "Не активно!") {
            "§cНе активно"
        } else {
            formatCookieTime(status)
        }

        val processed = SecretFunFeatures.processComponent(Component.literal("§7Печенье: $displayText"), true)
        return listOf(ScoreboardLine("cookie", processed, centered = false))
    }

    /**
    * Форматирует время печенья компактно:
    * - Если есть дни: "1д 12ч"
    * - Если нет дней, но есть часы: "12ч 34м"
    * - Если нет часов: "34м 56с"
    */
    private fun formatCookieTime(raw: String): String {
        val clean = raw.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()

        var days = 0
        var hours = 0
        var minutes = 0
        var seconds = 0

        val dayMatch = Regex("(\\d+)\\s*[дd]").find(clean)
        if (dayMatch != null) days = dayMatch.groupValues[1].toInt()

        val hourMatch = Regex("(\\d+)\\s*[чh]").find(clean)
        if (hourMatch != null) hours = hourMatch.groupValues[1].toInt()

        val minMatch = Regex("(\\d+)\\s*[мm]").find(clean)
        if (minMatch != null) minutes = minMatch.groupValues[1].toInt()

        val secMatch = Regex("(\\d+)\\s*[сs]").find(clean)
        if (secMatch != null) seconds = secMatch.groupValues[1].toInt()

        return when {
            days > 0 -> "§d${days}д ${hours}ч"
            hours > 0 -> "§d${hours}ч ${minutes}м"
            else -> "§d${minutes}м ${seconds}с"
        }
    }

    override val configLine = "§dПеченье"
}