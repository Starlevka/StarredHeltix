package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack

object EnchantmentProgressDetector {

    data class EnchantmentData(
        val type: EnchantType,
        val level: Int,
        val progress: Progress?
    )

    enum class EnchantType(val displayName: String, val skillName: String) {
        COMPACTNESS("Компактность", "Шахтёрство"),
        EXPERTISE("Экспертиза", "Рыболовство"),
        CHAMPION("Чемпион", "Бой"),
        CULTIVATING("Культивирование", "Фермерство")
    }

    data class Progress(val current: Double, val target: Double)

    // Пороги: индекс [i] = накопленное для достижения уровня (i+2)
    val COMPACTNESS_THRESHOLDS = doubleArrayOf(1500.0, 500.0, 1500.0, 5000.0, 15000.0, 50000.0, 150000.0, 500000.0, 1000000.0)
    val EXPERTISE_THRESHOLDS = doubleArrayOf(50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0, 5500.0, 10000.0, 15000.0)
    val CHAMPION_THRESHOLDS = doubleArrayOf(50000.0, 100000.0, 250000.0, 500000.0, 1000000.0, 1500000.0, 2000000.0, 2500000.0, 3000000.0)
    val CULTIVATING_THRESHOLDS = doubleArrayOf(1000.0, 5000.0, 25000.0, 100000.0, 300000.0, 1500000.0, 5000000.0, 20000000.0, 100000000.0)

    fun getEnchantmentData(stack: ItemStack): List<EnchantmentData> {
        if (stack.isEmpty) return emptyList()
        val lore = ItemLoreDetector.getLore(stack)
        val cleanLore = lore.map { stripFormattingCodes(it) }
        val result = mutableListOf<EnchantmentData>()

        result.addNotNull(parseCompactness(cleanLore))
        result.addNotNull(parseExpertise(cleanLore))
        result.addNotNull(parseChampion(cleanLore))
        result.addNotNull(parseCultivating(cleanLore))

        return result
    }

    fun getHeldEnchantmentData(): List<EnchantmentData> {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return emptyList()
        return getEnchantmentData(player.mainHandItem)
    }

    /**
     * Возвращает только те зачарования, навык которых сейчас активен (по action bar)
     */
    fun getActiveEnchantmentData(): List<EnchantmentData> {
        val allData = getHeldEnchantmentData()
        if (allData.isEmpty()) return emptyList()
        
        val lastSkill = SkillXpDetector.getLastSkill() ?: return emptyList()
        return allData.filter { it.type.skillName == lastSkill }
    }

    private fun <T> MutableList<T>.addNotNull(item: T?) {
        if (item != null) add(item)
    }

    private fun stripFormattingCodes(text: String): String {
        return text.replace(Regex("§[0-9a-fk-or]"), "")
    }

    // Компактность: "Компактность N (current/target)"
    private fun parseCompactness(lore: List<String>): EnchantmentData? {
        for (line in lore) {
            if (!line.contains("Компактность")) continue
            val level = extractLevel(line) ?: continue
            if (level >= 10) return EnchantmentData(EnchantType.COMPACTNESS, 10, null)
            val progress = parseParenthesesProgress(line)
            return EnchantmentData(EnchantType.COMPACTNESS, level, progress)
        }
        return null
    }

    // Культивирование: "Культивирование N (current/target)"
    private fun parseCultivating(lore: List<String>): EnchantmentData? {
        for (line in lore) {
            if (!line.contains("Культивирование")) continue
            val level = extractLevel(line) ?: continue
            if (level >= 10) return EnchantmentData(EnchantType.CULTIVATING, 10, null)
            val progress = parseParenthesesProgress(line)
            return EnchantmentData(EnchantType.CULTIVATING, level, progress)
        }
        return null
    }

    // Экспертиза: "Экспертиза N", затем "X убийств ... до следующего уровня"
    private fun parseExpertise(lore: List<String>): EnchantmentData? {
        for (i in lore.indices) {
            val line = lore[i]
            if (!line.contains("Экспертиза")) continue
            val level = extractLevel(line) ?: continue
            if (level >= 10) return EnchantmentData(EnchantType.EXPERTISE, 10, null)
            val progress = if (i + 1 < lore.size) parseRemainingProgress(lore[i + 1]) else null
            return EnchantmentData(EnchantType.EXPERTISE, level, progress)
        }
        return null
    }

    // Чемпион: "Чемпион N", затем "X опыта ... до следующего уровня"
    private fun parseChampion(lore: List<String>): EnchantmentData? {
        for (i in lore.indices) {
            val line = lore[i]
            if (!line.contains("Чемпион")) continue
            val level = extractLevel(line) ?: continue
            if (level >= 10) return EnchantmentData(EnchantType.CHAMPION, 10, null)
            val progress = if (i + 1 < lore.size) parseRemainingProgress(lore[i + 1]) else null
            return EnchantmentData(EnchantType.CHAMPION, level, progress)
        }
        return null
    }

    private fun extractLevel(line: String): Int? {
        val match = Regex("""(?:Компактность|Экспертиза|Чемпион|Культивирование)\s+(\d+)""").find(line) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    // "(342 520/500 000)" → Progress(342520, 500000)
    private fun parseParenthesesProgress(line: String): Progress? {
        val match = Regex("""\(([^)]+)\)""").find(line) ?: return null
        val content = match.groupValues[1]
        val parts = content.split("/")
        if (parts.size != 2) return null
        val current = parseNumber(parts[0]) ?: return null
        val target = parseNumber(parts[1]) ?: return null
        return Progress(current, target)
    }

    // "318 убийств морских существ до следующего уровня" → remaining=318
    // Lore показывает СКОЛЬКО ОСТАЛОСЬ
    private fun parseRemainingProgress(line: String): Progress? {
        if (!line.contains("до следующего уровня")) return null
        // Извлекаем число из начала строки
        val numberMatch = Regex("""([\d\s,.]+)""").find(line) ?: return null
        val remaining = parseNumber(numberMatch.groupValues[1]) ?: return null
        return Progress(remaining, -1.0)
    }

    fun parseNumber(str: String): Double? {
        val s = str.trim()
        if (s.isEmpty()) return null

        // Обработка K/M суффиксов
        if (s.endsWith("K", ignoreCase = true)) {
            val num = s.dropLast(1).trim().replace(",", ".").toDoubleOrNull() ?: return null
            return num * 1000
        }
        if (s.endsWith("М", ignoreCase = true) || s.endsWith("M", ignoreCase = true)) {
            val num = s.dropLast(1).trim().replace(",", ".").toDoubleOrNull() ?: return null
            return num * 1_000_000
        }

        // Убираем пробелы и запятые (разделители тысяч), точку оставляем как десятичный
        val cleaned = s.replace(" ", "").replace(",", "")
        return cleaned.toDoubleOrNull()
    }

    fun formatNumber(value: Double): String {
        val intVal = value.toLong()
        val str = intVal.toString()
        val result = StringBuilder()
        var count = 0
        for (i in str.length - 1 downTo 0) {
            if (count > 0 && count % 3 == 0) result.insert(0, ' ')
            result.insert(0, str[i])
            count++
        }
        return result.toString()
    }
}