package set.starlev.features.combat.dungeons

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.LivingEntity
import set.starlev.StarredHeltix
import set.starlev.render.RenderEvents
import set.starlev.utils.detectors.DungeonDetector
import set.starlev.utils.detectors.MobHeadDisplayDetector
import set.starlev.features.Feature
import set.starlev.features.Category

/**
 * Подсветка мини-боссов в подземельях.
 * Работает аналогично Floor4 с Духом медведя — только отображение текста над мобом.
 *
 * Мини-боссы и их цвета:
 * - Потерянный путешественник -> зелёный (0xFF55FF55)
 * - Теневой убийца -> красный (0xFFFF5555)
 * - Ледяной путешественник -> голубой (0xFF55FFFF)
 * - Злой археолог -> ярко-синий (0xFF5555FF)
 */
object MiniBossHighlight : Feature(
    name = "Mini Boss Highlight",
    category = Category.DUNGEON,
    description = "Подсветка мини-боссов"
) {

    /** Карта: имя моба -> цвет текста (ARGB Int) */
    private val miniBosses = mapOf(
        "Потерянный путешественник" to 0xFF55FF55.toInt(), // зелёный
        "Теневой убийца" to 0xFFFF5555.toInt(),             // красный
        "Ледяной путешественник" to 0xFF55FFFF.toInt(),     // голубой
        "Злой археолог" to 0xFF5555FF.toInt()               // ярко-синий
    )

    override fun init() {
        RenderEvents.register { context ->
            val config = StarredHeltix.feature.combat.highlight.miniBosses
            if (!config.enabled) return@register
            if (!DungeonDetector.isInDungeon()) return@register

            val level = mc.level ?: return@register

            for (entity in level.entitiesForRendering()) {
                if (entity !is LivingEntity) continue

                val displayName = getEntityDisplayName(entity)
                if (displayName.isBlank()) continue

                // Проверяем, является ли моб мини-боссом
                val color = miniBosses.entries.firstOrNull { (name, _) ->
                    displayName.contains(name, ignoreCase = true)
                }?.value ?: continue

                if (config.showText) {
                    val box = entity.boundingBox
                    val centerX = box.minX + (box.maxX - box.minX) / 2
                    val centerZ = box.minZ + (box.maxZ - box.minZ) / 2
                    val textY = box.maxY + 1.5
                    context.renderText(
                        "§l${displayName}",
                        centerX,
                        textY,
                        centerZ,
                        scale = 1.6f,
                        color = color,
                        seeThrough = true
                    )
                }
            }
        }
    }

    private fun getEntityDisplayName(entity: LivingEntity): String {
        // 1. Проверяем кастомное имя
        entity.customName?.let { return cleanText(it.string) }

        // 2. Проверяем TextDisplay над головой
        val headDisplays = MobHeadDisplayDetector.getHeadDisplays(entity)
        for (comp in headDisplays.textDisplays) {
            val text = cleanText(comp.string)
            if (text.isNotBlank()) return text
        }

        return ""
    }

    private fun cleanText(text: String): String {
        return text.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
    }
}