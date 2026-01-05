package set.starlev.features.mining

import net.minecraft.client.Minecraft
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement
import set.starlev.utils.detectors.TabListDetector

object CommissionsHud : HudElement("CommissionsHud") {
    private val mc = Minecraft.getInstance()
    private var commissions: List<String> = emptyList()
    private var lastUpdate = 0L

    override fun getAccentColor(): Int = 0xFFFFAA00.toInt() // Оранжевый/Золотой для Майнинга

    override fun render() {
        updateInfo()
        
        val config = StarredHeltix.feature.mining.commissions
        if (!config.enabled) return

        if (commissions.isEmpty() && !isEditing) return

        val displayLines = if (isEditing && commissions.isEmpty()) {
            listOf("§6§lПоручения:", "§fТитан: 50%", "§fМифрил: 10/100", "§fУбийство: 0/10")
        } else {
            commissions
        }

        // Рендеринг в стиле SkyHanni
        val padding = 4
        val title = "§6§lПоручения"
        val titleWidth = mc.font.width(title)
        
        // Очищаем строки от префиксов для корректного расчета ширины
        val contentLines = displayLines.drop(1) // Пропускаем "Поручения:"
        val contentWidth = if (contentLines.isEmpty()) titleWidth else maxOf(titleWidth, contentLines.maxOf { mc.font.width(it) })
        val finalWidth = maxOf(contentWidth, 100)
        
        val rowHeight = mc.font.lineHeight + 2
        // Высота: заголовок + каждая строка поручения + прогресс-бар для каждой строки (если есть %)
        var totalHeight = rowHeight // Для заголовка
        
        val linesWithProgress = mutableListOf<CommissionDisplayLine>()
        
        contentLines.forEach { line ->
            val progress = extractProgress(line)
            linesWithProgress.add(CommissionDisplayLine(line, progress))
            totalHeight += rowHeight
            if (progress != null) {
                totalHeight += 6 // Доп. место для прогресс-бара
            }
        }
        
        drawBackground(finalWidth, totalHeight, padding)
        
        var currentY = y
        // 1. Заголовок
        cachedGraphics?.drawString(mc.font, title, x, currentY, 0xFFFFFFFF.toInt())
        currentY += rowHeight
        
        // 2. Строки поручений
        linesWithProgress.forEach { item ->
            cachedGraphics?.drawString(mc.font, item.text, x, currentY, 0xFFFFFFFF.toInt())
            currentY += rowHeight
            
            if (item.progress != null) {
                drawProgressBar(x, currentY, finalWidth, 3, item.progress, 0xFFFFAA00.toInt())
                currentY += 6
            }
        }
    }

    private data class CommissionDisplayLine(val text: String, val progress: Float?)

    private fun extractProgress(line: String): Float? {
        // 1. Проверка процентов (например, 50%)
        val percentMatch = Regex("(\\d+)%").find(line)
        if (percentMatch != null) {
            return percentMatch.groupValues[1].toFloat() / 100f
        }
        
        // 2. Проверка дробей (например, 10/100)
        val fractionMatch = Regex("(\\d+)/(\\d+)").find(line)
        if (fractionMatch != null) {
            val current = fractionMatch.groupValues[1].toFloat()
            val total = fractionMatch.groupValues[2].toFloat()
            if (total > 0) return (current / total).coerceIn(0f, 1f)
        }
        
        return null
    }

    private fun updateInfo() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdate < 500) return
        lastUpdate = currentTime

        val allLines = TabListDetector.getHeaderAndFooterLines()
        val commissionHeaders = listOf("Поручения:", "Комиссии:", "Задания:", "Commissions:", "Assignments:")
        
        var index = -1
        for (header in commissionHeaders) {
            index = allLines.indexOfFirst { it.contains(header, ignoreCase = true) }
            if (index != -1) break
        }
        
        // Список ключевых слов для начала строки поручения
        val keywords = listOf(
            "Титан", "Мифрил", "Сборщик", "Убийство", "Охота", "Звёздные", "Звездные", 
            "Комиссия", "Поручение", "Задание", "Исследователь", "Добыча", "Кристалл",
            "Titanium", "Mithril", "Slayer", "Collector", "Star", "Mining"
        )

        if (index != -1) {
            val result = mutableListOf<String>()
            result.add("§6§lПоручения:")
            
            // Ищем подходящие строки после заголовка
            var foundCount = 0
            for (i in 1..15) {
                if (index + i < allLines.size) {
                    val line = allLines[index + i].trim()
                    if (line.isEmpty()) continue
                    
                    val isHeader = (line.endsWith(":") || line.endsWith("：")) && !line.contains("/")
                    val startsWithKeyword = keywords.any { line.startsWith(it, ignoreCase = true) || line.contains(it, ignoreCase = true) }
                    val hasPercentage = line.contains("%")
                    val hasProgress = line.contains("/") && Regex("\\d+/\\d+").containsMatchIn(line)
                    
                    if (startsWithKeyword || hasPercentage || hasProgress) {
                        result.add("§f$line")
                        foundCount++
                        if (foundCount >= 5) break
                    } else if (isHeader && foundCount > 0) {
                        break
                    }
                }
            }
            
            if (result.size > 1) {
                commissions = result
                return
            }
        }

        // Если в хедере/футере не нашли, пробуем по всему табу
        val fullLines = TabListDetector.getAllTabListLines()
        var fullIndex = -1
        for (header in commissionHeaders) {
            fullIndex = fullLines.indexOfFirst { it.contains(header, ignoreCase = true) }
            if (fullIndex != -1) break
        }

        if (fullIndex != -1) {
            val result = mutableListOf<String>()
            result.add("§6§lПоручения:")
            var foundCount = 0
            for (i in 1..15) {
                if (fullIndex + i < fullLines.size) {
                    val line = fullLines[fullIndex + i].trim()
                    if (line.isEmpty()) continue
                    
                    val isHeader = (line.endsWith(":") || line.endsWith("：")) && !line.contains("/")
                    val startsWithKeyword = keywords.any { line.startsWith(it, ignoreCase = true) || line.contains(it, ignoreCase = true) }
                    val hasPercentage = line.contains("%")
                    val hasProgress = line.contains("/") && Regex("\\d+/\\d+").containsMatchIn(line)
                    
                    if (startsWithKeyword || hasPercentage || hasProgress) {
                        result.add("§f$line")
                        foundCount++
                        if (foundCount >= 5) break
                    } else if (isHeader && foundCount > 0) {
                        break
                    }
                }
            }
            commissions = if (result.size > 1) result else emptyList()
        } else {
            commissions = emptyList()
        }
    }

    override fun getWidth(): Int {
        val displayLines = if (isEditing && commissions.isEmpty()) {
            listOf("§6§lПоручения:", "§f- Копать руду: 50%", "§f- Убить мобов: 0/10", "§f- Сбор ресурсов: 10%")
        } else {
            commissions
        }
        if (displayLines.isEmpty()) return 0
        return displayLines.maxOf { mc.font.width(it) }
    }

    override fun getHeight(): Int {
        val displayLines = if (isEditing && commissions.isEmpty()) {
            listOf("§6§lПоручения:", "§f- Копать руду: 50%", "§f- Убить мобов: 0/10", "§f- Сбор ресурсов: 10%")
        } else {
            commissions
        }
        if (displayLines.isEmpty()) return 0
        return displayLines.size * (mc.font.lineHeight + 2)
    }

    override fun getDefaultX(): Int = 10
    override fun getDefaultY(): Int = 100

    fun getActiveCommissions(): List<String> {
        updateInfo()
        return commissions
    }
}
