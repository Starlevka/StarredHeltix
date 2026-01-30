package set.starlev.features.skyblock

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement
import set.starlev.utils.detectors.TabListDetector
import java.util.regex.Pattern

object PetOverlay : HudElement("PetOverlay") {
    private val mc = Minecraft.getInstance()
    
    data class PetInfo(
        val line1: Component, // Оригинальный компонент с уровнем и именем
        val line2: Component, // Оригинальный компонент с опытом
        val isMaxLevel: Boolean
    )

    private var lastPetInfo: PetInfo? = null
    private var lastUpdate = 0L

    // Сессионные данные
    private var sessionXp = 0.0
    private var sessionStart = 0L
    private var lastRawXp = -1.0
    private var currentPetKey = ""
    
    // История для расчета "скорости" получения (скользящее окно 5 минут)
    private val gainHistory = mutableListOf<Pair<Long, Double>>()

    private val LEVEL_PATTERN = Pattern.compile("(?i)\\[ур(\\d+)\\]\\s*(.*)")
    private val XP_PATTERN = Pattern.compile("([\\d,.]+)/([\\d,.]+)")

    private fun calculateSize(): Pair<Int, Int> {
        val info = lastPetInfo ?: if (isEditing) {
            PetInfo(
                Component.literal("§7[ур100] ").append(Component.literal("Мифриловый голем").withStyle { it.withColor(0xFF55FF) }), 
                Component.literal("§6Максимальный уровень"), 
                true
            )
        } else return 100 to 40

        val padding = 4
        val rowHeight = mc.font.lineHeight + 2
        
        val titleText = Component.literal("§b§lПитомец:")
        val petLine = info.line1
        val xpLine = info.line2
        
        val now = System.currentTimeMillis()
        val tempGainHistory = gainHistory.toMutableList()
        tempGainHistory.removeAll { now - it.first > 5 * 60 * 1000 }
        
        val recentGained = tempGainHistory.sumOf { it.second }
        val timeInWindow = if (tempGainHistory.isEmpty()) 0L else now - tempGainHistory.first().first
        val calculationTime = maxOf(timeInWindow, 30000L)
        val xpPerHour = (recentGained / calculationTime) * 3600000
        
        val hasXpHr = !info.isMaxLevel && xpPerHour > 0
        val xpHrLine = if (hasXpHr) Component.literal("§7XP/час: §a${formatXp(xpPerHour)}") else null

        val textWidth = maxOf(
            mc.font.width(titleText),
            mc.font.width(petLine),
            mc.font.width(xpLine),
            if (xpHrLine != null) mc.font.width(xpHrLine) else 0
        )
        
        val finalWidth = maxOf(textWidth + 8, 100)
        val totalHeight = rowHeight * (if (hasXpHr) 4 else 3) + 8
        
        return finalWidth to totalHeight
    }

    override fun render() {
        val config = StarredHeltix.feature.skyblock.pet
        if (!config.enabled) return

        updatePetInfo()

        val info = lastPetInfo ?: if (isEditing) {
            PetInfo(
                Component.literal("§7[ур100] ").append(Component.literal("Мифриловый голем").withStyle { it.withColor(0xFF55FF) }), 
                Component.literal("§6Максимальный уровень"), 
                true
            )
        } else return

        val (finalWidth, totalHeight) = calculateSize()
        val padding = 4
        val rowHeight = mc.font.lineHeight + 2
        
        val titleText = Component.literal("§b§lПитомец:")
        val petLine = info.line1
        val xpLine = info.line2
        
        // Получаем xpPerHour для отрисовки из того же окна, что и calculateSize
        val now = System.currentTimeMillis()
        val tempGainHistory = gainHistory.toMutableList()
        tempGainHistory.removeAll { now - it.first > 5 * 60 * 1000 }
        
        val recentGained = tempGainHistory.sumOf { it.second }
        val timeInWindow = if (tempGainHistory.isEmpty()) 0L else now - tempGainHistory.first().first
        val calculationTime = maxOf(timeInWindow, 30000L)
        val xpPerHour = (recentGained / calculationTime) * 3600000
        
        val hasXpHr = !info.isMaxLevel && xpPerHour > 0
        val xpHrLine = if (hasXpHr) Component.literal("§7XP/час: §a${formatXp(xpPerHour)}") else null

        this.showBackground = config.showBackground
        // Петы обычно содержат несколько строк, поэтому оставляем обычный фон
        drawBackground(finalWidth, totalHeight, 0)
        
        val xOffset = x + padding
        var currentY = y + padding
        
        cachedGraphics?.drawString(mc.font, titleText, xOffset, currentY, 0xFFFFFFFF.toInt(), true)
        currentY += rowHeight
        
        cachedGraphics?.drawString(mc.font, petLine, xOffset, currentY, 0xFFFFFFFF.toInt(), true)
        currentY += rowHeight
        
        cachedGraphics?.drawString(mc.font, xpLine, xOffset, currentY, 0xFFFFFFFF.toInt(), true)
        
        if (xpHrLine != null) {
            currentY += rowHeight
            cachedGraphics?.drawString(mc.font, xpHrLine, xOffset, currentY, 0xFFFFFFFF.toInt(), true)
        }
    }

    override fun getWidth(): Int = calculateSize().first
    override fun getHeight(): Int = calculateSize().second
    
    override fun getDefaultScale(): Float = 1.0f
    override fun getDefaultX(): Int = 807
    override fun getDefaultY(): Int = 483

    private fun updatePetInfo() {
        val now = System.currentTimeMillis()
        if (now - lastUpdate < 500) return
        lastUpdate = now

        val components = TabListDetector.getAllTabListComponents()
        val petHeaderIndex = components.indexOfFirst { 
            val s = it.string.lowercase()
            s.contains("питомец:") || s.contains("pet:") || s.contains("active pet:")
        }

        if (petHeaderIndex == -1) {
            lastPetInfo = null
            return
        }

        // Пытаемся найти инфо о питомце (обычно следующие 2-3 строки)
        var nameComponent: Component? = null
        var xpComponent: Component? = null
        
        for (i in 1..3) {
            if (petHeaderIndex + i >= components.size) break
            val comp = components[petHeaderIndex + i]
            val text = cleanLine(comp.string)
            if (text.isEmpty()) continue

            if (nameComponent == null && LEVEL_PATTERN.matcher(text).find()) {
                nameComponent = comp
            } else if (xpComponent == null && (XP_PATTERN.matcher(text).find() || text.contains("МАКС", ignoreCase = true) || text.contains("MAX", ignoreCase = true))) {
                xpComponent = comp
            }
        }

        if (nameComponent != null && xpComponent != null) {
            val xpText = cleanLine(xpComponent.string)
            val isMax = xpText.contains("МАКС", ignoreCase = true) || xpText.contains("MAX", ignoreCase = true)
            
            // Обработка опыта для XP/час
            if (!isMax) {
                val matcher = XP_PATTERN.matcher(xpText)
                if (matcher.find()) {
                    val currentXp = matcher.group(1).replace(",", "").toDoubleOrNull() ?: 0.0
                    val petKey = cleanLine(nameComponent.string)
                    
                    if (currentPetKey != petKey) {
                        currentPetKey = petKey
                        lastRawXp = currentXp
                        // Не добавляем в историю при смене питомца, просто сбрасываем базу
                    } else if (lastRawXp != -1.0 && currentXp > lastRawXp) {
                        val gained = currentXp - lastRawXp
                        gainHistory.add(now to gained)
                        sessionXp += gained
                        lastRawXp = currentXp
                    } else if (currentXp < lastRawXp) {
                        // Вероятно уровень апнулся или сброс
                        lastRawXp = currentXp
                    }
                }
            }

            lastPetInfo = PetInfo(nameComponent, xpComponent, isMax)
        } else {
            lastPetInfo = null
        }
    }

    private fun formatXp(value: Double): String {
        return if (value >= 1_000_000) {
            String.format("%.2fM", value / 1_000_000)
        } else if (value >= 1_000) {
            String.format("%.1fK", value / 1_000)
        } else {
            String.format("%.0f", value)
        }
    }

    private fun cleanLine(text: String): String {
        return text.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
    }
}
