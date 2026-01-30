package set.starlev.features.mining

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.TabListDetector

object CommissionsHud : HudElement("CommissionsHud") {
    private val mc = Minecraft.getInstance()
    private var commissions: List<Component> = emptyList()
    private var lastUpdate = 0L

    override fun getAccentColor(): Int {
        val config = StarredHeltix.feature.mining.commissions
        return ColorUtils.parseColor(config.accentColorV2, 0xFFFFAA00.toInt())
    }

    private fun getHudTextColor(): Int {
        return 0xFFFFFFFF.toInt()
    }

    private fun parseColor(colorStr: String, default: Int): Int {
        return ColorUtils.parseColor(colorStr, default)
    }

    override fun render() {
        updateInfo()
        
        val config = StarredHeltix.feature.mining.commissions
        if (!config.enabled) return

        if (commissions.isEmpty() && !isEditing) return

        val displayLines = if (isEditing && commissions.isEmpty()) {
            listOf(
                Component.literal("§6§lПоручения:"), 
                Component.literal("§fТитан: 50%"), 
                Component.literal("§fМифрил: 10/100"), 
                Component.literal("§fУбийство: 0/10")
            )
        } else {
            commissions
        }

        // Рендеринг в стиле SkyHanni
        val padding = 4
        val title = Component.literal("§6§lПоручения")
        val titleWidth = mc.font.width(title)
        
        val contentLines = displayLines.drop(1)
        val contentWidth = if (contentLines.isEmpty()) titleWidth else maxOf(titleWidth, contentLines.maxOf { mc.font.width(it) })
        val finalWidth = maxOf(contentWidth, 100)
        
        val rowHeight = mc.font.lineHeight + 2
        var totalHeight = rowHeight
        
        val linesWithProgress = mutableListOf<CommissionDisplayLine>()
        
        contentLines.forEach { line ->
            val progress = extractProgress(line.string)
            linesWithProgress.add(CommissionDisplayLine(line, progress))
            totalHeight += rowHeight
        }
        
        // Синхронизируем состояние фона с конфигом
        this.showBackground = config.showBackground
        
        drawBackground(finalWidth, totalHeight, padding)
        
        var currentY = y
        val textColor = getHudTextColor()

        // 1. Заголовок
        cachedGraphics?.drawString(mc.font, title, x, currentY, textColor)
        currentY += rowHeight
        
        // 2. Строки поручений
        linesWithProgress.forEach { item ->
            cachedGraphics?.drawString(mc.font, item.text, x, currentY, textColor)
            currentY += rowHeight
        }
    }

    private data class CommissionDisplayLine(val text: Component, val progress: Float?)

    private fun extractProgress(line: String): Float? {
        // Убираем цветовые коды перед парсингом, если они там остались
        val cleanLine = line.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
        
        // 1. Проверка процентов (например, 50%, 50.5%, 50.0 %, 50.0% или 50 %)
        // Добавляем поддержку опциональных пробелов перед % и более гибкий поиск
        val percentMatch = Regex("(\\d+(?:[.,]\\d+)?)\\s*%").find(cleanLine)
        if (percentMatch != null) {
            val value = percentMatch.groupValues[1].replace(",", ".").toFloatOrNull() ?: return null
            return (value / 100f).coerceIn(0f, 1f)
        }
        
        // 2. Проверка дробей (например, 10/100, 10 / 100, 1.5/10, [10/100])
        // Ищем паттерн число/число, игнорируя окружающие символы
        val fractionMatch = Regex("(\\d+(?:[.,]\\d+)?)\\s*/\\s*(\\d+(?:[.,]\\d+)?)").find(cleanLine)
        if (fractionMatch != null) {
            val current = fractionMatch.groupValues[1].replace(",", ".").toFloatOrNull() ?: return null
            val total = fractionMatch.groupValues[2].replace(",", ".").toFloatOrNull() ?: return null
            if (total > 0) return (current / total).coerceIn(0f, 1f)
        }
        
        return null
    }

    private fun updateInfo() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdate < 500) return
        lastUpdate = currentTime

        val components = TabListDetector.getAllTabListComponents()
        val commissionHeaders = listOf("Поручения", "Комиссии", "Задания", "Commissions", "Assignments", "Mining Commissions")
        
        var index = -1
        for (header in commissionHeaders) {
            index = components.indexOfFirst { it.string.contains(header, ignoreCase = true) }
            if (index != -1) break
        }
        
        val keywords = listOf(
            "Титан", "Мифрил", "Сборщик", "Убийство", "Охота", "Звёздные", "Звездные", 
            "Комиссия", "Поручение", "Задание", "Исследователь", "Добыча", "Кристалл", "Переплавка",
            "Комиссии", "Задания", "Ядро", "Событие", "Горн", "Залежи",
            "Titanium", "Mithril", "Slayer", "Collector", "Star", "Mining", "Crystal", "Nucleus", "Commission", "Event", "Forge"
        )

        if (index != -1) {
            val result = mutableListOf<Component>()
            result.add(Component.literal("§6§lПоручения:"))
            
            var foundCount = 0
            for (i in 1..20) {
                if (index + i < components.size) {
                    val comp = components[index + i]
                    val line = comp.string.trim()
                    if (line.isEmpty()) continue
                    
                    val isHeader = (line.endsWith(":") || line.endsWith("：")) && !line.contains("/")
                    val containsKeyword = keywords.any { line.contains(it, ignoreCase = true) }
                    val hasPercentage = line.contains("%")
                    val hasProgress = line.contains("/") || Regex("\\d+(?:[.,]\\d+)?\\s*/\\s*\\d+").containsMatchIn(line)
                    
                    if (containsKeyword || hasPercentage || hasProgress) {
                        result.add(comp)
                        foundCount++
                        if (foundCount >= 6) break
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
        
        commissions = emptyList()
    }

    override fun getWidth(): Int {
        val displayLines = if (isEditing && commissions.isEmpty()) {
            listOf(
                Component.literal("§6§lПоручения:"), 
                Component.literal("§f- Копать руду: 50%"), 
                Component.literal("§f- Убить мобов: 0/10"), 
                Component.literal("§f- Сбор ресурсов: 10%")
            )
        } else {
            commissions
        }
        if (displayLines.isEmpty()) return 0
        return displayLines.maxOf { mc.font.width(it) }
    }

    override fun getHeight(): Int {
        val displayLines = if (isEditing && commissions.isEmpty()) {
            listOf(
                Component.literal("§6§lПоручения:"), 
                Component.literal("§f- Копать руду: 50%"), 
                Component.literal("§f- Убить мобов: 0/10"), 
                Component.literal("§f- Сбор ресурсов: 10%")
            )
        } else {
            commissions
        }
        if (displayLines.isEmpty()) return 0
        return displayLines.size * (mc.font.lineHeight + 2)
    }

    override fun getDefaultX(): Int = 17
    override fun getDefaultY(): Int = 243
    override fun getDefaultScale(): Float = 1.0f

    fun getActiveCommissions(): List<Component> {
        updateInfo()
        return commissions
    }
}
