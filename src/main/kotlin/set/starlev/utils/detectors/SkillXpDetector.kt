package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import java.util.regex.Pattern

object SkillXpDetector {
    private val mc = Minecraft.getInstance()
    private val formattingRegex = Regex("(?i)§[0-9a-fk-orlnmxz]")
    private var lastUpdateAt = 0L
    
    // Паттерн: +0.1 Бой (600,000.9/1,100,000)
    private val xpPattern = Pattern.compile("(?:\\+([\\d,.]+)\\s+)?([\\wА-яЁё]+) \\(([\\d,.]+)/([\\d,.]+)\\)")
    
    // Паттерн для макс. уровня: +1.3 Шахтёрство (1,000,000.5) или +1.3 Шахтёрство (MAX)
    private val maxLevelPattern = Pattern.compile("(?:\\+([\\d,.]+)\\s+)?([\\wА-яЁё]+) \\(([\\d,.]+|MAX)\\)")
    
    private val listeners = mutableListOf<(SkillXpInfo) -> Unit>()
    private var lastActionBarText = ""
    private var lastInfo: SkillXpInfo? = null
    private var lastSkill: String? = null

    fun getLastSkill(): String? = lastSkill
    fun getLastInfo(): SkillXpInfo? = lastInfo

    data class SkillXpInfo(
        val gained: Double,
        val skill: String,
        val current: Double,
        val target: Double,
        val isMax: Boolean = false,
        val lastUpdate: Long = System.currentTimeMillis()
    ) {
        // Кастомный equals для сравнения данных без учета времени обновления
        fun isSameAs(other: SkillXpInfo?): Boolean {
            if (other == null) return false
            return gained == other.gained && 
                   skill == other.skill && 
                   current == other.current && 
                   target == other.target && 
                   isMax == other.isMax
        }
    }

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (client.level != null) {
                val now = System.currentTimeMillis()
                if (now - lastUpdateAt >= 50) {
                    lastUpdateAt = now
                    update()
                }
            }
        }
    }

    fun detectFromText(text: String): SkillXpInfo? {
        val cleanText = text.replace(formattingRegex, "").trim()
        
        // Сначала пробуем обычный паттерн
        val matcher = xpPattern.matcher(cleanText)
        if (matcher.find()) {
            try {
                val gainedStr = matcher.group(1)
                val gained = gainedStr?.replace(",", "")?.toDouble() ?: 0.0
                val skill = matcher.group(2)
                val current = matcher.group(3).replace(",", "").toDouble()
                val target = matcher.group(4).replace(",", "").toDouble()
                // Если текущий опыт равен или больше целевого, считаем уровень максимальным для текущей полоски
                val isMax = current >= target
                return SkillXpInfo(gained, skill, current, target, isMax)
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }
        
        // Если не подошло, пробуем паттерн для макс. уровня
        val maxMatcher = maxLevelPattern.matcher(cleanText)
        if (maxMatcher.find()) {
            try {
                val gainedStr = maxMatcher.group(1)
                val gained = gainedStr?.replace(",", "")?.toDouble() ?: 0.0
                val skill = maxMatcher.group(2)
                val currentStr = maxMatcher.group(3)
                
                val current = if (currentStr.equals("MAX", ignoreCase = true)) -1.0 else currentStr.replace(",", "").toDouble()
                return SkillXpInfo(gained, skill, current, current, true)
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
        if (actionBarText.isNotEmpty() && actionBarText != lastActionBarText) {
            lastActionBarText = actionBarText
            val info = detectFromText(actionBarText)
            if (info != null && !info.isSameAs(lastInfo)) {
                lastInfo = info
                lastSkill = info.skill
                listeners.forEach { it(info) }
            }
        }
    }
}
