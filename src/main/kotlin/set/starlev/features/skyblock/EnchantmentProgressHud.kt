package set.starlev.features.skyblock

import net.minecraft.client.Minecraft
import set.starlev.hud.HudElement
import set.starlev.StarredHeltix
import set.starlev.utils.detectors.EnchantmentProgressDetector
import set.starlev.utils.detectors.EnchantmentProgressDetector.EnchantType
import set.starlev.utils.detectors.SkillXpDetector

object EnchantmentProgressHud : HudElement("EnchantmentProgressHud") {
    private val mc = Minecraft.getInstance()
    private val COLOR_CYAN = 0xFF55FFFF.toInt()

    private fun getData(): List<EnchantmentProgressDetector.EnchantmentData> {
        return EnchantmentProgressDetector.getActiveEnchantmentData()
    }

    private fun calculateSize(): Pair<Int, Int> {
        val data = getData()
        if (data.isEmpty() && !isEditing) return 100 to 20

        val displayData = if (isEditing && data.isEmpty()) {
            listOf(
                EnchantmentProgressDetector.EnchantmentData(EnchantType.COMPACTNESS, 5, EnchantmentProgressDetector.Progress(342520.0, 500000.0))
            )
        } else data

        var maxWidth = 0
        for (entry in displayData) {
            val nameText = getLine1Text(entry)
            maxWidth = maxOf(maxWidth, mc.font.width(nameText))

            if (entry.level < 10) {
                val progressText = getProgressText(entry)
                if (progressText != null) {
                    maxWidth = maxOf(maxWidth, mc.font.width(progressText))
                }
            }
        }

        val lineHeight = mc.font.lineHeight + 2
        val totalHeight = displayData.size * lineHeight * 2 + displayData.size
        return maxOf(maxWidth + 8, 100) to totalHeight
    }

    private fun getLine1Text(entry: EnchantmentProgressDetector.EnchantmentData): String {
        return if (entry.level >= 10) {
            "${entry.type.displayName} ${entry.level} §eMAX"
        } else {
            "${entry.type.displayName} ${entry.level}"
        }
    }

    private fun getProgressText(entry: EnchantmentProgressDetector.EnchantmentData): String? {
        if (entry.level >= 10) return null
        val progress = entry.progress ?: return null

        val thresholds = when (entry.type) {
            EnchantType.COMPACTNESS -> EnchantmentProgressDetector.COMPACTNESS_THRESHOLDS
            EnchantType.EXPERTISE -> EnchantmentProgressDetector.EXPERTISE_THRESHOLDS
            EnchantType.CHAMPION -> EnchantmentProgressDetector.CHAMPION_THRESHOLDS
            EnchantType.CULTIVATING -> EnchantmentProgressDetector.CULTIVATING_THRESHOLDS
        }
        val thresholdIndex = entry.level - 1
        if (thresholdIndex < 0 || thresholdIndex >= thresholds.size) return null
        val target = thresholds[thresholdIndex]

        return when (entry.type) {
            // Компактность и Культивирование: lore показывает (current/target) напрямую
            EnchantType.COMPACTNESS, EnchantType.CULTIVATING -> {
                val current = progress.current
                "(${EnchantmentProgressDetector.formatNumber(current)}/${EnchantmentProgressDetector.formatNumber(target)})"
            }
            // Экспертиза и Чемпион: lore показывает "X ... до следующего уровня" (сколько осталось)
            EnchantType.EXPERTISE, EnchantType.CHAMPION -> {
                val remaining = progress.current
                val current = (target - remaining).coerceAtLeast(0.0)
                "(${EnchantmentProgressDetector.formatNumber(current)}/${EnchantmentProgressDetector.formatNumber(target)})"
            }
        }
    }

    override fun render() {
        val config = StarredHeltix.feature.skyblock.skills
        if (!config.enabled || !config.showEnchantmentProgress) return

        val lastInfo = SkillXpDetector.getLastInfo()
        if (lastInfo != null && System.currentTimeMillis() - lastInfo.lastUpdate > 15000 && !isEditing) return

        val data = getData()
        if (data.isEmpty() && !isEditing) return

        val displayData = if (isEditing && data.isEmpty()) {
            listOf(
                EnchantmentProgressDetector.EnchantmentData(EnchantType.COMPACTNESS, 5, EnchantmentProgressDetector.Progress(342520.0, 500000.0))
            )
        } else data

        if (displayData.isEmpty()) return

        val size = calculateSize()
        val finalWidth = size.first
        val totalHeight = size.second

        this.showBackground = config.showBackground
        drawBackground(finalWidth, totalHeight, 4)

        var currentY = y
        val lineHeight = mc.font.lineHeight + 2

        for (entry in displayData) {
            // Строка 1: название зачарования + уровень
            val line1 = getLine1Text(entry)
            cachedGraphics?.drawString(mc.font, line1, x, currentY, COLOR_CYAN, true)
            currentY += lineHeight

            // Строка 2: прогресс (если не макс уровень)
            if (entry.level < 10) {
                val progressText = getProgressText(entry)
                if (progressText != null) {
                    cachedGraphics?.drawString(mc.font, progressText, x, currentY, COLOR_CYAN, true)
                }
                currentY += lineHeight
            }

            currentY += 1
        }
    }

    override fun getWidth(): Int = calculateSize().first
    override fun getHeight(): Int = calculateSize().second

    override fun getDefaultX(): Int = 17
    override fun getDefaultY(): Int = 328
}

