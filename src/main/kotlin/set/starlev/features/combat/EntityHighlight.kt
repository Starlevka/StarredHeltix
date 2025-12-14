package set.starlev.features.combat

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.animal.wolf.Wolf
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.monster.EnderMan
import set.starlev.StarredHeltix
import set.starlev.render.RenderEvents
import set.starlev.render.RenderContext
import java.awt.Color

/**
 * Система подсветки мобов в мире.
 * Подсвечивает эндерменов, волков и заряженных криперов.
 */
object EntityHighlight {

    private val mc = Minecraft.getInstance()

    /**
     * Инициализация системы подсветки мобов
     */
    fun init() {
        RenderEvents.register { context ->
            val level = mc.level ?: return@register
            val config = StarredHeltix.feature.combat

            // Получаем все сущности для рендеринга
            val entities = level.entitiesForRendering()

            for (entity in entities) {
                when (entity) {
                    is EnderMan -> {
                        if (config.enderman.enabled) {
                            val color = parseColorString(config.enderman.color)
                            renderEntityBox(context, entity, color, config.enderman.transparency)
                        }
                    }
                    is Creeper -> {
                        if (config.creeper.enabled && entity.isPowered) {
                            val color = parseColorString(config.creeper.color)
                            renderEntityBox(context, entity, color, config.creeper.transparency)
                        }
                    }
                    is Wolf -> {
                        if (config.wolf.enabled) {
                            val color = parseColorString(config.wolf.color)
                            renderEntityBox(context, entity, color, config.wolf.transparency)
                        }
                    }
                }
            }
        }
    }

    private fun renderEntityBox(context: RenderContext, entity: Entity, color: Int, globalAlpha: Float) {
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val baseAlpha = ((color shr 24) and 0xFF) / 255f
        
        // Применяем глобальную прозрачность
        val a = baseAlpha * globalAlpha
        
        context.renderBox(entity.boundingBox, r, g, b, a, true)
    }

    /**
     * Парсит цветовую строку из конфига в ARGB integer.
     * Формат: "chroma:alpha:red:green:blue"
     */
    private fun parseColorString(colorString: String): Int {
        return try {
            val parts = colorString.split(":")
            if (parts.size < 5) return 0xFFFFFFFF.toInt()

            val chroma = parts[0].toInt()
            val a = parts[1].toInt().coerceIn(0, 255)
            val r = parts[2].toInt().coerceIn(0, 255)
            val g = parts[3].toInt().coerceIn(0, 255)
            val b = parts[4].toInt().coerceIn(0, 255)

            if (chroma != 0) {
                val invertedChroma = (256 - chroma).coerceIn(1, 255)
                val periodInMillis = (invertedChroma / 255.0) * 60000.0
                if (periodInMillis <= 0) {
                    return (a shl 24) or (r shl 16) or (g shl 8) or b
                }

                val hue = (System.currentTimeMillis() % periodInMillis.toLong()) / periodInMillis.toFloat()

                val rainbowRgb = Color.HSBtoRGB(hue, 1.0f, 1.0f)
                (a shl 24) or (rainbowRgb and 0x00FFFFFF)
            } else {
                (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        } catch (e: NumberFormatException) {
            0xFFFFFFFF.toInt()
        }
    }
}
