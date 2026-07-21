package set.starlev.features.combat

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import set.starlev.StarredHeltix
import set.starlev.render.RenderEvents
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.LocationDetector
import java.util.concurrent.ConcurrentHashMap
import set.starlev.features.Feature
import set.starlev.features.Category

object EndHighlight : Feature(
    name = "End Highlight",
    category = Category.DUNGEON,
    description = "Подсветка мобов The End"
) {
    private val PURPLE_TERRACOTTA = Blocks.PURPLE_TERRACOTTA
    private const val RADIUS = 32
    private val foundPositions = ConcurrentHashMap.newKeySet<BlockPos>()
    private var lastScanTime = 0L

    override fun init() {
        RenderEvents.register { context ->
            val level = mc.level ?: return@register
            val player = mc.player ?: return@register
            val config = StarredHeltix.feature.combat.endHighlight
            if (!config.enabled) return@register
            
            // Проверка локации (только в Энде)
            if (!LocationDetector.isInLocation("Энд") && !LocationDetector.isInLocation("The End")) {
                if (foundPositions.isNotEmpty()) foundPositions.clear()
                return@register
            }

            val now = System.currentTimeMillis()
            if (now - lastScanTime > 1000L) {
                lastScanTime = now
                val center = player.blockPosition()
                for (x in -RADIUS..RADIUS) {
                    for (y in -RADIUS..RADIUS) {
                        for (z in -RADIUS..RADIUS) {
                            val pos = center.offset(x, y, z)
                            if (level.getBlockState(pos).`is`(PURPLE_TERRACOTTA)) {
                                foundPositions.add(pos)
                            }
                        }
                    }
                }
            }

            val color = ColorUtils.parseColor(config.colorV2)
            val alpha = config.transparency
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8) and 0xFF) / 255f
            val b = (color and 0xFF) / 255f
            val a = ((color shr 24) and 0xFF) / 255f * alpha

            val it = foundPositions.iterator()
            while (it.hasNext()) {
                val pos = it.next()
                if (pos.closerThan(player.blockPosition(), RADIUS.toDouble() + 2)) {
                    if (level.getBlockState(pos).`is`(PURPLE_TERRACOTTA)) {
                        val box = net.minecraft.world.phys.AABB(pos).inflate(0.002)
                        context.renderBox(box, r, g, b, a, !config.outline)
                    } else {
                        it.remove()
                    }
                } else {
                    it.remove()
                }
            }
        }
    }
}
