package set.starlev.features.farming.garden

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.features.Category
import set.starlev.hud.HudElement
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.TabListDetector

object CropMilestoneHud : HudElement(
    id = "GardenCropMilestoneHud",
    name = "Crop Milestone HUD",
    category = Category.FARMING,
    description = "Milestone урожая"
) {

    override fun render() {
        val config = StarredHeltix.feature.farming.garden
        if (!config.cropMilestoneEnabled || !GardenApi.inGarden()) return

        val graphics = cachedGraphics ?: return
        val font = mc.font

        val milestones = parseMilestonesFromTab()
        val currentCrop = GardenApi.getCurrentCropFromHand()

        val lines = mutableListOf<String>()
        lines.add("§6§lУровень растений")

        if (milestones.isEmpty()) {
            lines.add("§7Нет данных в табе")
        } else {
            for ((crop, level, progress) in milestones) {
                val highlight = currentCrop != null && crop == currentCrop
                val prefix = if (highlight) "§e" else "§7"
                val name = if (highlight) "§l${crop.displayName}" else crop.displayName
                lines.add("$prefix$name: §f$level §a$progress%")
            }
        }

        val contentW = 130
        val contentH = lines.size * (font.lineHeight + 2) + 2
        drawBackground(contentW, contentH, 4)

        var curY = y
        for (line in lines) {
            graphics.drawString(font, Component.literal(line), x, curY, 0xFFFFFFFF.toInt())
            curY += font.lineHeight + 2
        }
    }

    /**
     * Парсит секцию "Уровень растений:" из таба (tab list).
     * Каждая строка под заголовком: <название> <уровень> <проценты>%
     * Например: "Кактус 13 61%"
     */
    private fun parseMilestonesFromTab(): List<MilestoneEntry> {
        val tabLines = TabListDetector.getHeaderAndFooterLines()
        val result = mutableListOf<MilestoneEntry>()
        var foundHeader = false

        for (raw in tabLines) {
            val clean = ColorUtils.stripColor(raw).trim()

            if (!foundHeader) {
                if (clean.contains("Уровень растений", ignoreCase = true) ||
                    clean.contains("Plant Level", ignoreCase = true)) {
                    foundHeader = true
                }
                continue
            }

            if (clean.isBlank()) continue
            // Следующий заголовок (с : или другой секцией) — выходим
            if (clean.contains(":") && !clean.contains("%")) break

            // Парсинг: "Кактус 13 61%"
            val parts = clean.split(" ")
            if (parts.size < 3) continue

            val rawPercent = parts.last()
            val levelStr = parts.dropLast(1).lastOrNull() ?: continue
            val nameParts = parts.dropLast(2)
            if (nameParts.isEmpty()) continue
            val cropName = nameParts.joinToString(" ")

            val level = levelStr.toIntOrNull() ?: continue
            val progress = rawPercent.replace("%", "").toIntOrNull() ?: continue

            // Найти CropType по русскому названию
            val crop = CropType.entries.firstOrNull { it.displayName.equals(cropName, ignoreCase = true) }
            if (crop != null) {
                result.add(MilestoneEntry(crop, level, progress))
            }
        }

        return result
    }

    private data class MilestoneEntry(
        val crop: CropType,
        val level: Int,
        val progress: Int,
    )

    override fun getWidth(): Int = 130
    override fun getHeight(): Int = 60
    override fun getDefaultX(): Int = 10
    override fun getDefaultY(): Int = 280

    override fun init() {}
}
