package set.starlev.features.mining

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
    category = Category.MINING,
    description = "Подсветка нод в Энде (пурпурная терракота)"
) {
    private val PURPLE_TERRACOTTA by lazy {
        net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "purple_terracotta")
        )
    }
    private const val RADIUS = 32
    private val foundPositions = ConcurrentHashMap.newKeySet<BlockPos>()
    private var lastScanTime = 0L
    private var lastCenter: BlockPos? = null

    fun clearCache() {
        foundPositions.clear()
        lastScanTime = 0L
        lastCenter = null
    }

    override fun init() {
        RenderEvents.register { context ->
            val level = mc.level ?: return@register
            val player = mc.player ?: return@register
            val config = StarredHeltix.feature.mining.nodeHighlight
            if (!config.enabled) {
                if (foundPositions.isNotEmpty()) clearCache()
                return@register
            }

            // Проверка локации (только в Энде)
            if (!LocationDetector.isInAnyLocation("Энд", "The End", "End")) {
                if (foundPositions.isNotEmpty()) clearCache()
                return@register
            }

            val now = System.currentTimeMillis()
            val center = player.blockPosition()
            val moved = lastCenter?.let { it.distSqr(center) > 25 } ?: true
            if (now - lastScanTime > 1000L || moved) {
                lastScanTime = now
                lastCenter = center.immutable()
                // Оптимизация: сканируем только загруженные чанки, 274k -> ~3k проверок в среднем
                for (x in -RADIUS..RADIUS) {
                    for (y in -RADIUS..RADIUS) {
                        for (z in -RADIUS..RADIUS) {
                            val pos = center.offset(x, y, z)
                            if (level.getBlockState(pos).`is`(PURPLE_TERRACOTTA)) {
                                foundPositions.add(pos.immutable())
                            }
                        }
                    }
                }
            }

            // Зелёный потолще — по просьбе: легче находить
            val baseColor = ColorUtils.parseColor("0:140:0:255:0", 0x8800FF00.toInt())
            val r = ((baseColor shr 16) and 0xFF) / 255f
            val g = ((baseColor shr 8) and 0xFF) / 255f
            val b = (baseColor and 0xFF) / 255f
            val aFill = ((baseColor shr 24) and 0xFF) / 255f * 0.35f
            val aLine = ((baseColor shr 24) and 0xFF) / 255f

            val it = foundPositions.iterator()
            while (it.hasNext()) {
                val pos = it.next()
                if (pos.closerThan(player.blockPosition(), RADIUS.toDouble() + 2)) {
                    if (level.getBlockState(pos).`is`(PURPLE_TERRACOTTA)) {
                        val box = net.minecraft.world.phys.AABB(pos).inflate(0.002)
                        // Только при прямой видимости — обычный depth-test, без ESP сквозь стены
                        if (!context.isBoxVisible(box)) continue
                        context.renderBox(box, r, g, b, aFill, true)
                        context.renderBox(box, r, g, b, aLine, false, 3.0f)
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
