package set.starlev.features.combat

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.animal.wolf.Wolf
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.monster.Spider
import net.minecraft.world.entity.monster.CaveSpider
import net.minecraft.world.entity.monster.*
import set.starlev.StarredHeltix
import set.starlev.render.RenderEvents
import set.starlev.render.RenderContext
import set.starlev.utils.ColorUtils

/**
 * Система подсветки мобов в мире.
 * Подсвечивает эндерменов, волков, заряженных криперов и летучих мышей в данжах.
 */
object EntityHighlight {

    private val mc = Minecraft.getInstance()

    /**
     * Инициализация системы подсветки мобов
     */
    fun init() {
        RenderEvents.register { context ->
            val level = mc.level ?: return@register
            val config = StarredHeltix.feature.combat.highlight
            val endermanEnabled = config.enderman.enabled
            val creeperEnabled = config.creeper.enabled
            val wolfEnabled = config.wolf.enabled
            val caveSpiderEnabled = config.caveSpider.enabled
            val spiderEnabled = config.spider.enabled
            val zombieEnabled = config.zombie.enabled

            if (!endermanEnabled && !creeperEnabled && !wolfEnabled && !caveSpiderEnabled && !spiderEnabled && !zombieEnabled) {
                return@register
            }

            val endermanColor = if (endermanEnabled) ColorUtils.parseColor(config.enderman.colorV2) else 0
            val endermanAlpha = config.enderman.transparency
            val endermanOutline = config.enderman.outline

            val creeperColor = if (creeperEnabled) ColorUtils.parseColor(config.creeper.colorV2) else 0
            val creeperAlpha = config.creeper.transparency
            val creeperOutline = config.creeper.outline

            val wolfColor = if (wolfEnabled) ColorUtils.parseColor(config.wolf.colorV2) else 0
            val wolfAlpha = config.wolf.transparency
            val wolfOutline = config.wolf.outline

            val caveSpiderColor = if (caveSpiderEnabled) ColorUtils.parseColor(config.caveSpider.colorV2) else 0
            val caveSpiderAlpha = config.caveSpider.transparency
            val caveSpiderOutline = config.caveSpider.outline

            val spiderColor = if (spiderEnabled) ColorUtils.parseColor(config.spider.colorV2) else 0
            val spiderAlpha = config.spider.transparency
            val spiderOutline = config.spider.outline

            val zombieColor = if (zombieEnabled) ColorUtils.parseColor(config.zombie.colorV2) else 0
            val zombieAlpha = config.zombie.transparency
            val zombieOutline = config.zombie.outline

            for (entity in level.entitiesForRendering()) {
                when (entity) {
                    is EnderMan -> {
                        if (endermanEnabled) {
                            renderEntityBox(context, entity, endermanColor, endermanAlpha, endermanOutline)
                        }
                    }
                    is Creeper -> {
                        if (creeperEnabled && entity.isPowered) {
                            renderEntityBox(context, entity, creeperColor, creeperAlpha, creeperOutline)
                        }
                    }
                    is Wolf -> {
                        if (wolfEnabled) {
                            renderEntityBox(context, entity, wolfColor, wolfAlpha, wolfOutline)
                        }
                    }
                    is CaveSpider -> {
                        if (caveSpiderEnabled) {
                            renderEntityBox(context, entity, caveSpiderColor, caveSpiderAlpha, caveSpiderOutline)
                        }
                    }
                    is Spider -> {
                        if (spiderEnabled) {
                            renderEntityBox(context, entity, spiderColor, spiderAlpha, spiderOutline)
                        }
                    }
                    is Zombie -> {
                        if (zombieEnabled) {
                            renderEntityBox(context, entity, zombieColor, zombieAlpha, zombieOutline)
                        }
                    }
                }
            }
        }
    }

    private fun renderEntityBox(context: RenderContext, entity: Entity, color: Int, globalAlpha: Float, outline: Boolean = false) {
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val baseAlpha = ((color shr 24) and 0xFF) / 255f
        
        // Применяем глобальную прозрачность
        val a = baseAlpha * globalAlpha
        
        context.renderBox(entity.boundingBox, r, g, b, a, !outline)
    }


}
