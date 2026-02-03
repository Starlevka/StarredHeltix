package set.starlev.features.skyblock

import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.SkillXpDetector

object SkillXpHud : HudElement("SkillXpHud") {
    private val mc = Minecraft.getInstance()
    private var lastInfo: SkillXpDetector.SkillXpInfo? = null
    private var sessionXp = mutableMapOf<String, Double>()
    private var sessionStart = System.currentTimeMillis()
    
    // История для расчета "скорости" получения (скользящее окно 5 минут)
    private val gainHistory = mutableMapOf<String, MutableList<Pair<Long, Double>>>()
    
    private val iconMap by lazy {
        mapOf(
            "Бой" to ItemStack(Items.STONE_SWORD),
            "Шахтёрство" to ItemStack(Items.STONE_PICKAXE),
            "Фермерство" to ItemStack(Items.GOLDEN_HOE),
            "Лесничество" to ItemStack(Items.JUNGLE_SAPLING),
            "Рыболовство" to ItemStack(Items.FISHING_ROD),
            "Чародейство" to ItemStack(Items.ENCHANTING_TABLE),
            "Алхимия" to ItemStack(Items.BREWING_STAND),
            "Приручение" to ItemStack(Items.POLAR_BEAR_SPAWN_EGG),
            "Подземелья" to ItemStack(Items.WITHER_SKELETON_SKULL)
        )
    }

    init {
        SkillXpDetector.registerListener { info ->
            if (shouldShowSkill(info.skill)) {
                lastInfo = info
                sessionXp[info.skill] = (sessionXp[info.skill] ?: 0.0) + info.gained
                
                // Добавляем в историю
                val history = gainHistory.getOrPut(info.skill) { mutableListOf() }
                history.add(System.currentTimeMillis() to info.gained)
            }
        }
    }

    private fun shouldShowSkill(skill: String): Boolean {
        val config = StarredHeltix.feature.skyblock.skills
        return when (skill) {
            "Бой" -> config.showCombat
            "Шахтёрство" -> config.showMining
            "Фермерство" -> config.showFarming
            "Лесничество" -> config.showForaging
            "Рыболовство" -> config.showFishing
            "Чародейство" -> config.showEnchanting
            "Алхимия" -> config.showAlchemy
            "Приручение" -> config.showTaming
            "Подземелья" -> config.showDungeons
            else -> false
        }
    }

    override fun getAccentColor(): Int {
        val colorStr = StarredHeltix.feature.skyblock.skills.barColorV2
        return ColorUtils.parseColor(colorStr, 0xFF00FFFF.toInt())
    }

    private fun getHudTextColor(): Int {
        val config = StarredHeltix.feature.skyblock.skills
        return ColorUtils.parseColor(config.textColor, 0xFFFFFFFF.toInt())
    }

    private fun getHudValuesColor(): Int {
        val config = StarredHeltix.feature.skyblock.skills
        return ColorUtils.parseColor(config.valuesColor, 0xFFFFFF55.toInt())
    }

    private fun parseColor(colorStr: String, default: Int): Int {
        return ColorUtils.parseColor(colorStr, default)
    }

    private fun calculateSize(): Pair<Int, Int> {
        val info = lastInfo
        if (info == null && !isEditing) return 120 to 50

        val displayInfo = if (isEditing && info == null) {
            SkillXpDetector.SkillXpInfo(100.0, "Бой", 1500000.0, 2000000.0)
        } else info!!

        val skill = displayInfo.skill
        val current = displayInfo.current
        val target = displayInfo.target
        val isMax = displayInfo.isMax

        val xpText = when {
            isMax && current < 0 -> "MAX XP"
            isMax -> "${formatXp(current)} XP"
            else -> "${formatXp(current)} / ${formatXp(target)}"
        }
        val remainingText = if (isMax) "Уровень максимален" else "Осталось: ${formatXp(target - current)}"
        val perHourText = "XP/час: 0.0" // Примерная длина

        val textWidth = maxOf(
            mc.font.width(skill),
            mc.font.width(xpText),
            mc.font.width(remainingText),
            mc.font.width(perHourText)
        )
        
        val finalWidth = maxOf(textWidth + 20, 120) // 16 (icon) + 4 (padding)
        val rowHeight = mc.font.lineHeight + 2
        val totalHeight = rowHeight * 4 + 2 // Без прогресс-бара
        
        return finalWidth to totalHeight
    }

    override fun render() {
        val config = StarredHeltix.feature.skyblock.skills
        if (!config.enabled) return

        val info = lastInfo
        if (info == null && !isEditing) return
        
        // Скрывать, если прошло больше 15 секунд с последнего обновления
        if (info != null && System.currentTimeMillis() - info.lastUpdate > 15000 && !isEditing) return

        val displayInfo = if (isEditing && info == null) {
            SkillXpDetector.SkillXpInfo(100.0, "Бой", 1500000.0, 2000000.0)
        } else info!!

        val skill = displayInfo.skill
        val current = displayInfo.current
        val target = displayInfo.target
        val isMax = displayInfo.isMax
        val progress = if (isMax) 1f else (current / target).toFloat().coerceIn(0f, 1f)
        val percent = progress * 100
        val remaining = target - current
        
        // Расчет опыта в час
        val now = System.currentTimeMillis()
        val history = gainHistory[skill] ?: mutableListOf()
        val tempHistory = history.toMutableList() // Копия для безопасности
        tempHistory.removeAll { now - it.first > 5 * 60 * 1000 }
        
        val recentGained = tempHistory.sumOf { it.second }
        val timeInWindow = if (tempHistory.isEmpty()) 0L else now - tempHistory.first().first
        val calculationTime = maxOf(timeInWindow, 30000L)
        val xpPerHour = (recentGained / calculationTime) * 3600000

        val size = calculateSize()
        val finalWidth = size.first
        val totalHeight = size.second
        
        val rowHeight = mc.font.lineHeight + 2
        val textColor = getHudTextColor()
        val valuesColor = getHudValuesColor()
        
        val xpText = when {
            isMax && current < 0 -> "MAX XP"
            isMax -> "${formatXp(current)} XP"
            else -> "${formatXp(current)} / ${formatXp(target)}"
        }
        val percentText = if (isMax) "MAX" else "${String.format("%.1f", percent)}%"
        val remainingText = if (isMax) "Уровень максимален" else "Осталось: ${formatXp(remaining)}"
        val perHourText = "XP/час: ${formatXp(xpPerHour)}"

        this.showBackground = config.showBackground
        drawBackground(finalWidth, totalHeight, 4)
        
        var currentY = y
        val iconSize = 16
        
        // Рендеринг иконки
        val icon = iconMap[skill] ?: ItemStack(Items.AIR)
        cachedGraphics?.renderFakeItem(icon, x, currentY)
        
        // Заголовок
        cachedGraphics?.drawString(mc.font, "§l" + skill, x + iconSize + 4, currentY + 4, textColor, true)
        
        if (isMax) {
            val star = " §e✪"
            cachedGraphics?.drawString(mc.font, star, x + iconSize + 4 + mc.font.width("§l" + skill), currentY + 4, 0xFFFFFF00.toInt(), true)
        }
        
        currentY += rowHeight + 2
        
        // Информация об опыте
        cachedGraphics?.drawString(mc.font, xpText, x, currentY, valuesColor, true)
        val percentWidth = mc.font.width(percentText)
        cachedGraphics?.drawString(mc.font, percentText, x + finalWidth - percentWidth, currentY, valuesColor, true)
        currentY += rowHeight
        
        // Осталось
        cachedGraphics?.drawString(mc.font, remainingText, x, currentY, textColor, true)
        currentY += rowHeight
        
        // Опыт в час
        cachedGraphics?.drawString(mc.font, perHourText, x, currentY, textColor, true)
        // currentY += rowHeight + 2 // Убрали отступ для прогресс-бара

        // Полоска прогресса (отключена по запросу)
        // drawProgressBar(x, currentY, finalWidth, 4, progress, getAccentColor())
    }

    private fun formatXp(value: Double): String {
        return if (value >= 1_000_000) {
            String.format("%.2fM", value / 1_000_000)
        } else if (value >= 1_000) {
            String.format("%.1fK", value / 1_000)
        } else {
            String.format("%.1f", value)
        }
    }

    override fun getWidth(): Int = calculateSize().first
    override fun getHeight(): Int = calculateSize().second
}
