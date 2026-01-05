package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.features.chat.ChatEventsManager
import java.util.regex.Pattern

object SkillXpDetector {
    private val mc = Minecraft.getInstance()
    
    // Паттерн: +0.1 Бой (600,000.9/1,100,000)
    private val xpPattern = Pattern.compile("\\+([\\d,.]+) ([\\wА-я]+) \\(([\\d,.]+)/([\\d,.]+)\\)")
    
    private val listeners = mutableListOf<(SkillXpInfo) -> Unit>()

    data class SkillXpInfo(
        val gained: Double,
        val skill: String,
        val current: Double,
        val target: Double
    )

    fun init() {
        // Мы будем проверять ActionBar на наличие опыта навыков
        // Но так как ActionBar может быстро меняться, мы полагаемся на ActionBarDetector или прямое получение
    }

    fun detectFromText(text: String): SkillXpInfo? {
        val cleanText = text.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").replace(",", "")
        val matcher = xpPattern.matcher(cleanText)
        if (matcher.find()) {
            try {
                val gained = matcher.group(1).toDouble()
                val skill = matcher.group(2)
                val current = matcher.group(3).toDouble()
                val target = matcher.group(4).toDouble()
                return SkillXpInfo(gained, skill, current, target)
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }
        return null
    }

    fun registerListener(listener: (SkillXpInfo) -> Unit) {
        listeners.add(listener)
    }

    fun update() {
        val actionBarText = ActionBarDetector.getActionBarText()
        if (actionBarText.isNotEmpty()) {
            val info = detectFromText(actionBarText)
            if (info != null) {
                listeners.forEach { it(info) }
            }
        }
    }
}
