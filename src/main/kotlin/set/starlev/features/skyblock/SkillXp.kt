package set.starlev.features.skyblock

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import set.starlev.StarredHeltix
import set.starlev.render.HudElement
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.SkillXpDetector
import set.starlev.render.drawString
import set.starlev.render.drawCenteredString
import java.nio.file.Files
import java.nio.file.Paths

object SkillXpHud : HudElement("SkillXpHud") {
    private val mc = Minecraft.getInstance()
    private var lastInfo: SkillXpDetector.SkillXpInfo? = null
    private var sessionXp = mutableMapOf<String, Double>()
    private var sessionStart = System.currentTimeMillis()

    // История для расчета скорости получения (скользящее окно 5 минут)
    private val gainHistory = mutableMapOf<String, MutableList<Pair<Long, Double>>>()
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val sessionFile = Paths.get("config", "starredheltix", "skill_session.json")

    private val iconMap by lazy {
        mapOf(
            "Бой" to ItemStack(Items.STONE_SWORD),
            "Combat" to ItemStack(Items.STONE_SWORD),
            "Шахтёрство" to ItemStack(Items.STONE_PICKAXE),
            "Mining" to ItemStack(Items.STONE_PICKAXE),
            "Фермерство" to ItemStack(Items.GOLDEN_HOE),
            "Farming" to ItemStack(Items.GOLDEN_HOE),
            "Лесничество" to ItemStack(Items.JUNGLE_SAPLING),
            "Foraging" to ItemStack(Items.JUNGLE_SAPLING),
            "Рыболовство" to ItemStack(Items.FISHING_ROD),
            "Fishing" to ItemStack(Items.FISHING_ROD),
            "Чародейство" to ItemStack(Items.ENCHANTING_TABLE),
            "Enchanting" to ItemStack(Items.ENCHANTING_TABLE),
            "Алхимия" to ItemStack(Items.BREWING_STAND),
            "Alchemy" to ItemStack(Items.BREWING_STAND),
            "Приручение" to ItemStack(Items.POLAR_BEAR_SPAWN_EGG),
            "Taming" to ItemStack(Items.POLAR_BEAR_SPAWN_EGG),
            "Подземелья" to ItemStack(Items.WITHER_SKELETON_SKULL),
            "Dungeons" to ItemStack(Items.WITHER_SKELETON_SKULL)
        )
    }

    init {
        loadSession()
        SkillXpDetector.registerListener { info ->
            if (shouldShowSkill(info.skill)) {
                lastInfo = info
                sessionXp[info.skill] = (sessionXp[info.skill] ?: 0.0) + info.gained
                val history = gainHistory.getOrPut(info.skill) { mutableListOf() }
                history.add(System.currentTimeMillis() to info.gained)
                // Чистим старые >5 мин сразу чтобы не копилось
                val now = System.currentTimeMillis()
                history.removeAll { now - it.first > 5 * 60 * 1000 }
                saveSession()
            }
        }
    }

    private fun shouldShowSkill(skill: String): Boolean {
        val config = StarredHeltix.feature.skyblock.skills
        return when (skill.lowercase()) {
            "бой", "combat" -> config.showCombat
            "шахтёрство", "шахтерство", "mining" -> config.showMining
            "фермерство", "farming" -> config.showFarming
            "лесничество", "foraging" -> config.showForaging
            "рыболовство", "fishing" -> config.showFishing
            "чародейство", "enchanting" -> config.showEnchanting
            "алхимия", "alchemy" -> config.showAlchemy
            else -> false
        }
    }

    // Цвета из конфига
    private fun getColorSkillName(): Int = ColorUtils.parseColor(StarredHeltix.feature.skyblock.skills.colorSkillNameV2, 0xFF55FF55.toInt())
    private fun getColorXp(): Int = ColorUtils.parseColor(StarredHeltix.feature.skyblock.skills.colorXpV2, 0xFFFFFF55.toInt())
    private fun getColorRemaining(): Int = ColorUtils.parseColor(StarredHeltix.feature.skyblock.skills.colorRemainingV2, 0xFFFF5555.toInt())
    private fun getColorPerHour(): Int = ColorUtils.parseColor(StarredHeltix.feature.skyblock.skills.colorPerHourV2, 0xFF55FF55.toInt())
    private fun getColorProgressBar(): Int = ColorUtils.parseColor(StarredHeltix.feature.skyblock.skills.colorProgressBarV2, 0xFF55FF55.toInt())

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
        val perHourText = "XP/час: 0.0"
        val etaText = "ETA: 00:00"
        val config = StarredHeltix.feature.skyblock.skills
        val textWidth = maxOf(
            mc.font.width(skill),
            mc.font.width(xpText),
            mc.font.width(remainingText),
            mc.font.width(perHourText),
            if (config.showEta && !isMax) mc.font.width(etaText) else 0
        )
        val finalWidth = maxOf(textWidth + 20, 120)
        val rowHeight = mc.font.lineHeight + 2
        var rows = 4 // skill + xp/percent + remaining + perHour
        if (config.showEta && !isMax) rows += 1
        if (config.showProgressBar) rows += 1
        val totalHeight = rowHeight * rows + 2
        return finalWidth to totalHeight
    }

    override fun render() {
        val config = StarredHeltix.feature.skyblock.skills
        if (!config.enabled) return
        val info = lastInfo
        if (info == null && !isEditing) return
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

        val now = System.currentTimeMillis()
        val history = gainHistory[skill] ?: mutableListOf()
        val tempHistory = history.toMutableList()
        tempHistory.removeAll { now - it.first > 5 * 60 * 1000 }
        val recentGained = tempHistory.sumOf { it.second }
        val timeInWindow = if (tempHistory.isEmpty()) 0L else now - tempHistory.first().first
        val calculationTime = maxOf(timeInWindow, 30000L)
        val xpPerHour = if (recentGained == 0.0) 0.0 else (recentGained / calculationTime) * 3600000

        val etaText: String? = if (config.showEta && !isMax && xpPerHour > 0) {
            val hours = remaining / xpPerHour
            val totalSeconds = (hours * 3600).toLong().coerceAtLeast(0)
            "ETA: ${formatDuration(totalSeconds)}"
        } else null

        val size = calculateSize()
        val finalWidth = size.first
        val totalHeight = size.second
        val rowHeight = mc.font.lineHeight + 2

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
        val icon = iconMap[skill] ?: iconMap[skill.lowercase()] ?: ItemStack(Items.AIR)
        cachedGraphics?.fakeItem(icon, x, currentY)
        cachedGraphics?.drawString(mc.font, "§l" + skill, x + iconSize + 4, currentY + 1, getColorSkillName(), true)
        if (isMax) {
            val star = " §e✪"
            cachedGraphics?.drawString(mc.font, star, x + iconSize + 4 + mc.font.width("§l" + skill), currentY + 1, getColorXp(), true)
        }
        currentY += rowHeight + 2
        cachedGraphics?.drawString(mc.font, xpText, x, currentY, getColorXp(), true)
        val percentWidth = mc.font.width(percentText)
        cachedGraphics?.drawString(mc.font, percentText, x + finalWidth - percentWidth, currentY, getColorXp(), true)
        currentY += rowHeight
        cachedGraphics?.drawString(mc.font, remainingText, x, currentY, getColorRemaining(), true)
        currentY += rowHeight
        if (etaText != null) {
            cachedGraphics?.drawString(mc.font, etaText, x, currentY, getColorRemaining(), true)
            currentY += rowHeight
        }
        cachedGraphics?.drawString(mc.font, perHourText, x, currentY, getColorPerHour(), true)
        currentY += rowHeight
        if (config.showProgressBar) {
            drawProgressBar(x, currentY, finalWidth, 4, progress, getColorProgressBar())
        }
    }

    private fun formatXp(value: Double): String {
        return if (value >= 1_000_000_000) {
            String.format("%.2fB", value / 1_000_000_000)
        } else if (value >= 1_000_000) {
            String.format("%.2fM", value / 1_000_000)
        } else if (value >= 1_000) {
            String.format("%.1fK", value / 1_000)
        } else {
            String.format("%.1f", value)
        }
    }

    private fun formatDuration(totalSeconds: Long): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    // --- Персистентность сессии ---
    private data class SessionData(
        val sessionXp: Map<String, Double> = emptyMap(),
        val sessionStart: Long = System.currentTimeMillis()
    )

    private fun loadSession() {
        try {
            if (!Files.exists(sessionFile)) return
            val json = Files.readString(sessionFile)
            val type = object : TypeToken<SessionData>() {}.type
            val data: SessionData = gson.fromJson(json, type) ?: return
            sessionXp.clear()
            sessionXp.putAll(data.sessionXp)
            sessionStart = data.sessionStart
        } catch (_: Exception) {}
    }

    private fun saveSession() {
        try {
            Files.createDirectories(sessionFile.parent)
            val data = SessionData(sessionXp.toMap(), sessionStart)
            Files.writeString(sessionFile, gson.toJson(data))
        } catch (_: Exception) {}
    }

    fun resetSession() {
        sessionXp.clear()
        gainHistory.clear()
        sessionStart = System.currentTimeMillis()
        try { Files.deleteIfExists(sessionFile) } catch (_: Exception) {}
    }

    override fun getWidth(): Int = calculateSize().first
    override fun getHeight(): Int = calculateSize().second
}
