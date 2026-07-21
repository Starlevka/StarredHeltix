package set.starlev.features.farming

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.AreaEffectCloud
import net.minecraft.world.phys.AABB
import set.starlev.StarredHeltix
import set.starlev.features.Category
import set.starlev.features.Feature
import set.starlev.render.RenderEvents
import set.starlev.utils.ColorUtils
import java.util.concurrent.CopyOnWriteArrayList

object GlowingMushroomHighlight : Feature(
    name = "Glowing Mushroom Highlight",
    category = Category.FARMING,
    description = "Подсветка RGB зелий"
) {
    private const val RADIUS = 16.0
    private var lastCheck = 0L
    private val trackedClouds = CopyOnWriteArrayList<AreaEffectCloud>()

    override fun init() {
        RenderEvents.register { context ->
            val level = mc.level ?: return@register
            val player = mc.player ?: return@register
            val config = StarredHeltix.feature.farming.glowingMushroom
            if (!config.enabled) return@register

            val now = System.currentTimeMillis()
            if (now - lastCheck > 500L) {
                lastCheck = now
                val entities = level.getEntitiesOfClass(AreaEffectCloud::class.java, player.boundingBox.inflate(RADIUS)) {
                    it.isAlive && it.duration > 0
                }
                trackedClouds.clear()
                trackedClouds.addAll(entities)
            }

            val color = ColorUtils.parseColor(config.colorV2)
            val alpha = config.transparency
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8) and 0xFF) / 255f
            val b = (color and 0xFF) / 255f
            val a = ((color shr 24) and 0xFF) / 255f * alpha

            for (cloud in trackedClouds) {
                if (!cloud.isAlive) continue
                val box = cloud.boundingBox.inflate(0.1)
                context.renderBox(box, r, g, b, a, !config.outline)
            }
        }
    }
}
