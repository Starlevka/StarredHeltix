package set.starlev.features.farming.garden

import net.minecraft.client.Minecraft
import set.starlev.StarredHeltix
import set.starlev.utils.detectors.ScoreboardDetector
import set.starlev.utils.detectors.TabListDetector

object GardenApi {

    fun inGarden(): Boolean = GardenDetector.inGarden()

    fun getCurrentCropFromHand(): CropType? {
        val player = Minecraft.getInstance().player ?: return null
        val held = player.mainHandItem
        val name = held.hoverName.string.lowercase()

        // 1. Точное совпадение по toolPrefix или displayName
        for (crop in CropType.entries) {
            if (name.contains(crop.toolPrefix.lowercase()) || name.contains(crop.displayName.lowercase())) {
                return crop
            }
        }

        // 2. Fallback: поиск по русским/общим названиям инструментов
        return when {
            name.contains("нож") || name.contains("knife") -> CropType.CACTUS
            name.contains("резчик") || name.contains("dicer") -> {
                // Dicer может быть для тыквы или арбуза
                if (name.contains("тыкв") || name.contains("pumpkin")) CropType.PUMPKIN
                else CropType.MELON
            }
            name.contains("топор") || name.contains("axe") -> {
                // Топор может быть для какао-бобов или грибов
                if (name.contains("какао") || name.contains("cocoa")) CropType.COCOA_BEANS
                else CropType.MUSHROOM
            }
            name.contains("мотыга") || name.contains("hoe") -> {
                // Общая мотыга — пробуем все Theoretical Hoe культуры
                val theoretical = listOf(
                    CropType.WHEAT, CropType.CARROT, CropType.POTATO,
                    CropType.NETHER_WART, CropType.SUGAR_CANE, CropType.MUSHROOM
                )
                theoretical.firstOrNull { name.contains(it.displayName.lowercase()) }
                    ?: theoretical.firstOrNull { name.contains(it.toolPrefix.lowercase()) }
                    ?: CropType.WHEAT // fallback
            }
            else -> null
        }
    }

    fun getVisitorNamesFromTab(): List<String> {
        if (!inGarden()) return emptyList()
        val lines = TabListDetector.getHeaderAndFooterLines()
        val visitors = mutableListOf<String>()
        var foundVisitorsHeader = false
        for (line in lines) {
            val clean = line.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
            if (clean.contains("Посетители:") || clean.contains("Visitors:")) {
                foundVisitorsHeader = true
                continue
            }
            if (foundVisitorsHeader) {
                if (clean.isBlank()) break
                visitors.add(clean)
            }
        }
        return visitors
    }

    fun getVisitorCount(): Int {
        if (!inGarden()) return 0
        val lines = TabListDetector.getHeaderAndFooterLines()
        for (line in lines) {
            val clean = line.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
            val match = Regex("(?:Посетители|Visitors):\\s*(\\d+)").find(clean)
            if (match != null) {
                return match.groupValues[1].toIntOrNull() ?: 0
            }
        }
        return 0
    }

    fun getCurrentPlotFromScoreboard(): String? {
        if (!inGarden()) return null
        val lines = ScoreboardDetector.getScoreboardText()
        for (line in lines) {
            val clean = line.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
            val match = Regex("Участок\\s*-\\s*(\\S+)|Plot\\s*-\\s*(\\S+)").find(clean)
            if (match != null) {
                return match.groupValues[1].ifEmpty { match.groupValues[2] }
            }
        }
        return null
    }

}
