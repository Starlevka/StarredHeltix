package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import java.util.regex.Pattern

object SkillXpDetector {
    private val mc = Minecraft.getInstance()
    private val formattingRegex = Regex("(?i)§[0-9a-fk-orlnmxz]")
    private var lastUpdateAt = 0L

    // Строгий список умений (RU/EN, см. SkillXp.shouldShowSkill). Без него паттерны
    // ловили любой текст вида «слово (число)» — включая строки ХП/маны,
    // которые hideActionBar затем прятал вместе с реальным XP.
    private const val SKILLS = "Фарминг|Фермерство|Шахтёрство|Шахтерство|Копание|Горное|Лесничество|Лесоводство|Рыболовство|Рыбачка|Чародейство|Алхимия|Приручение|Бой|Сражение|Подземелья|Подвалы|Dungeoneer|Dungeons|Farming|Mining|Combat|Foraging|Fishing|Enchanting|Alchemy|Taming|Speed|Fitness|Defense"
    private const val FLAGS = Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE

    private val xpPattern = Pattern.compile("(?:\\+([\\d,.]+[kKmMb]?)\\s+)?($SKILLS) \\(([\\d,.]+[kKmMb]?)/([\\d,.]+[kKmMb]?)\\)", FLAGS)
    private val percentPattern = Pattern.compile("(?:\\+([\\d,.]+[kKmMb]?)\\s+)?($SKILLS) \\(([\\d,.]+)%\\)", FLAGS)
    // Паттерн макс. уровня: +1.3 Шахтёрство (MAX) или (1,000,000.5)
    private val maxLevelPattern = Pattern.compile("(?:\\+([\\d,.]+[kKmMb]?)\\s+)?($SKILLS) \\(([\\d,.]+[kKmMb]?|MAX)\\)", FLAGS)

    private val listeners = mutableListOf<(SkillXpInfo) -> Unit>()
    private var lastActionBarText = ""
    private var lastInfo: SkillXpInfo? = null
    private var lastSkill: String? = null

    /** Сброс дедуп-состояния при смене мира/сервера (слушателей НЕ трогаем). */
    fun onWorldChange() {
        lastActionBarText = ""
        lastInfo = null
        lastSkill = null
        lastUpdateAt = 0L
    }

    fun getLastSkill(): String? = lastSkill
    fun getLastInfo(): SkillXpInfo? = lastInfo

    data class SkillXpInfo(
        val gained: Double,
        val skill: String,
        val current: Double,
        val target: Double,
        val isMax: Boolean = false,
        val lastUpdate: Long = System.currentTimeMillis(),
        val percent: Double? = null
    ) {
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

    /** Парсит суффиксы k/M/B как в EnchantmentProgressDetector + поддержка кириллицы М/к */
    fun parseNumber(str: String): Double? {
        val s = str.trim()
        if (s.isEmpty()) return null
        if (s.equals("MAX", ignoreCase = true)) return -1.0
        val lower = s.lowercase()
        // Проверяем суффиксы
        if (lower.endsWith("k")) {
            val num = s.dropLast(1).trim().replace(",", "").replace(" ", "").toDoubleOrNull() ?: return null
            return num * 1_000.0
        }
        if (lower.endsWith("m") || lower.endsWith("м")) {
            val num = s.dropLast(1).trim().replace(",", "").replace(" ", "").toDoubleOrNull() ?: return null
            return num * 1_000_000.0
        }
        if (lower.endsWith("b") || lower.endsWith("б")) {
            val num = s.dropLast(1).trim().replace(",", "").replace(" ", "").toDoubleOrNull() ?: return null
            return num * 1_000_000_000.0
        }
        // Пробуем заменить запятую как разделитель тысяч (но точка — десятичный)
        // Для чисел вида 1,100,000.9 -> убрать запятые, оставить точку
        // Для 1,5k уже обработали выше, для 600,000 -> 600000
        val cleaned = s.replace(",", "").replace(" ", "")
        return cleaned.toDoubleOrNull()
    }

    private fun parseGained(str: String?): Double {
        if (str == null) return 0.0
        return parseNumber(str) ?: 0.0
    }

    fun detectFromText(text: String): SkillXpInfo? {
        val cleanText = text.replace(formattingRegex, "").trim()

        // 1) Обычный паттерн с current/target и суффиксами
        val matcher = xpPattern.matcher(cleanText)
        if (matcher.find()) {
            try {
                val gained = parseGained(matcher.group(1))
                val skill = matcher.group(2)
                val curStr = matcher.group(3)
                val tgtStr = matcher.group(4)
                val current = parseNumber(curStr) ?: return null
                val target = parseNumber(tgtStr) ?: return null
                val isMax = current >= target || target == -1.0
                return SkillXpInfo(gained, skill, current, target, isMax)
            } catch (_: Exception) {}
        }

        // 2) Процентный паттерн: +1.1 Mining (48.39%)
        val percentMatcher = percentPattern.matcher(cleanText)
        if (percentMatcher.find()) {
            try {
                val gained = parseGained(percentMatcher.group(1))
                val skill = percentMatcher.group(2)
                val percentStr = percentMatcher.group(3).replace(",", ".")
                val percent = percentStr.toDoubleOrNull() ?: return null
                // Для процентов делаем current = percent, target = 100
                val isMax = percent >= 100.0
                return SkillXpInfo(gained, skill, percent, 100.0, isMax, percent = percent)
            } catch (_: Exception) {}
        }

        // 3) Макс паттерн (MAX или одиночное число в скобках)
        val maxMatcher = maxLevelPattern.matcher(cleanText)
        if (maxMatcher.find()) {
            try {
                val gained = parseGained(maxMatcher.group(1))
                val skill = maxMatcher.group(2)
                val currentStr = maxMatcher.group(3)
                if (currentStr.equals("MAX", ignoreCase = true)) {
                    return SkillXpInfo(gained, skill, -1.0, -1.0, true)
                }
                val current = parseNumber(currentStr) ?: return null
                // Если нашли только одно число без / — это прогресс без цели, считаем целевым
                return SkillXpInfo(gained, skill, current, current, true)
            } catch (_: Exception) {}
        }

        return null
    }

    /** Проверяет, является ли текст скилл-XP строкой (для парсинга) */
    fun isSkillXpText(text: String): Boolean = detectFromText(text) != null

    // Целиком XP-строка, без «хвостов»: для hideActionBar. Частичный матч (find)
    // прятал и комбинированные бары кастомного сервера, где XP идёт в одной строке
    // с ХП/маной («+12 Фарминг (45%) | ❤ 200 | ✎ 50») — терялась вся строка.
    private val pureXpPattern = Pattern.compile(
        "^\\s*(?:\\+[\\d,.]+[kKmMb]?\\s+)?(?:$SKILLS)\\s*\\(\\s*(?:MAX|[\\d,.]+[kKmMb]?(?:\\s*/\\s*[\\d,.]+[kKmMb]?)?\\s*%?)\\s*\\)\\s*$",
        FLAGS,
    )

    /** Скрывать action bar можно только если ОН ЦЕЛИКОМ является XP-строкой. */
    fun isPureSkillXpText(text: String): Boolean =
        pureXpPattern.matcher(text.replace(formattingRegex, "").trim()).matches()

    fun registerListener(listener: (SkillXpInfo) -> Unit) {
        listeners.add(listener)
    }

    /** Вызывается из миксина Hud.setOverlayMessage для событийного обновления (без polling) */
    fun handleActionBar(text: String) {
        if (text.isEmpty()) return
        // Дедуп по lastActionBarText
        if (text == lastActionBarText) return
        lastActionBarText = text
        val info = detectFromText(text) ?: return
        if (info.isSameAs(lastInfo)) return
        lastInfo = info
        lastSkill = info.skill
        listeners.forEach { it(info) }
    }

    fun update() {
        val actionBarText = ActionBarDetector.getActionBarText()
        if (actionBarText.isNotEmpty() && actionBarText != lastActionBarText) {
            handleActionBar(actionBarText)
        }
    }
}
