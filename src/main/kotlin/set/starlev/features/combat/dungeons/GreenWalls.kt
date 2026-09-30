package set.starlev.features.combat.dungeons

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import set.starlev.StarredHeltix
import set.starlev.render.RenderContext
import set.starlev.render.RenderEvents
import set.starlev.render.SHRenderTypes
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.DungeonDetector

/**
 * Зелёные стены в подземельях — сплошная зелёная заливка потресканного
 * каменного кирпича (фулл-заливка тем же оттенком, что зелёный сундук).
 *
 * Почему заливка, а не подмена текстуры как у сундука: сундуки/мыши выбирают
 * спрайт каждый кадр, а обычные блоки запекают UV в меши чанков один раз —
 * PNG подменился бы глобально, а не только в данжах. Поэтому открытые грани
 * кирпичей перекрываются заливкой: скан рядом с игроком раз в 500мс
 * (кэш позиций + битмаска граней), затем один submitCustomGeometry на кадр.
 * Только в данжах.
 */
object GreenWalls {
    private val mc = Minecraft.getInstance()

    private const val RADIUS = 24
    private const val RESCAN_MS = 500L
    private const val MAX_BRICKS = 192
    private const val EPS = 0.0025

    private data class Brick(val x: Int, val y: Int, val z: Int, val faces: Int)

    @Volatile
    private var cached: List<Brick> = emptyList()
    private var lastScanMs = 0L

    fun init() {
        RenderEvents.register { context -> onWorldRender(context) }
    }

    private fun isEnabled(): Boolean = try {
        StarredHeltix.feature.dungeons.visuals.greenTextures.greenWalls
    } catch (_: Exception) {
        false
    }

    private fun fillColor(): Int = try {
        ColorUtils.parseColor(
            StarredHeltix.feature.dungeons.visuals.greenTextures.greenWallsColorV2,
            0xFF2BB449.toInt()
        )
    } catch (_: Exception) {
        0xFF2BB449.toInt()
    }

    private fun onWorldRender(context: RenderContext) {
        if (!isEnabled()) {
            if (cached.isNotEmpty()) cached = emptyList()
            return
        }
        val player = mc.player ?: return
        val level = mc.level ?: return
        if (!DungeonDetector.isInDungeon()) {
            if (cached.isNotEmpty()) cached = emptyList()
            return
        }
        maybeRescan(level, player.blockPosition())
        val bricks = cached
        if (bricks.isEmpty()) return

        val color = fillColor()
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val a = ((color shr 24) and 0xFF) / 255f
        if (a <= 0f) return

        val cam = context.cameraPos
        context.matrices.pushPose()
        try {
            context.matrices.translate(-cam.x, -cam.y, -cam.z)
            context.submitNodeCollector.submitCustomGeometry(context.matrices, SHRenderTypes.FILLED) { pose, buffer ->
                val m = pose.pose()
                fun quad(
                    ax: Double, ay: Double, az: Double,
                    bx: Double, by: Double, bz: Double,
                    cx: Double, cy: Double, cz: Double,
                    dx: Double, dy: Double, dz: Double,
                ) {
                    buffer.addVertex(m, ax.toFloat(), ay.toFloat(), az.toFloat()).setColor(r, g, b, a)
                    buffer.addVertex(m, bx.toFloat(), by.toFloat(), bz.toFloat()).setColor(r, g, b, a)
                    buffer.addVertex(m, cx.toFloat(), cy.toFloat(), cz.toFloat()).setColor(r, g, b, a)
                    buffer.addVertex(m, dx.toFloat(), dy.toFloat(), dz.toFloat()).setColor(r, g, b, a)
                    // Обратный обход — та же грань с другой стороны
                    buffer.addVertex(m, dx.toFloat(), dy.toFloat(), dz.toFloat()).setColor(r, g, b, a)
                    buffer.addVertex(m, cx.toFloat(), cy.toFloat(), cz.toFloat()).setColor(r, g, b, a)
                    buffer.addVertex(m, bx.toFloat(), by.toFloat(), bz.toFloat()).setColor(r, g, b, a)
                    buffer.addVertex(m, ax.toFloat(), ay.toFloat(), az.toFloat()).setColor(r, g, b, a)
                }
                for (br in bricks) {
                    if (!context.isBoxVisible(AABB(br.x.toDouble(), br.y.toDouble(), br.z.toDouble(), br.x + 1.0, br.y + 1.0, br.z + 1.0))) continue
                    val x = br.x.toDouble()
                    val y = br.y.toDouble()
                    val z = br.z.toDouble()
                    val f = br.faces
                    if (f and (1 shl Direction.DOWN.ordinal) != 0) {
                        val yy = y - EPS
                        quad(x, yy, z, x + 1, yy, z, x + 1, yy, z + 1, x, yy, z + 1)
                    }
                    if (f and (1 shl Direction.UP.ordinal) != 0) {
                        val yy = y + 1 + EPS
                        quad(x, yy, z, x, yy, z + 1, x + 1, yy, z + 1, x + 1, yy, z)
                    }
                    if (f and (1 shl Direction.NORTH.ordinal) != 0) {
                        val zz = z - EPS
                        quad(x, y, zz, x + 1, y, zz, x + 1, y + 1, zz, x, y + 1, zz)
                    }
                    if (f and (1 shl Direction.SOUTH.ordinal) != 0) {
                        val zz = z + 1 + EPS
                        quad(x, y, zz, x, y + 1, zz, x + 1, y + 1, zz, x + 1, y, zz)
                    }
                    if (f and (1 shl Direction.WEST.ordinal) != 0) {
                        val xx = x - EPS
                        quad(xx, y, z, xx, y, z + 1, xx, y + 1, z + 1, xx, y + 1, z)
                    }
                    if (f and (1 shl Direction.EAST.ordinal) != 0) {
                        val xx = x + 1 + EPS
                        quad(xx, y, z, xx, y + 1, z, xx, y + 1, z + 1, xx, y, z + 1)
                    }
                }
            }
        } finally {
            context.matrices.popPose()
        }
    }

    private fun maybeRescan(level: net.minecraft.client.multiplayer.ClientLevel, center: BlockPos) {
        val now = System.currentTimeMillis()
        if (now - lastScanMs < RESCAN_MS) return
        lastScanMs = now
        try {
            val found = ArrayList<Brick>(64)
            val mutable = BlockPos.MutableBlockPos()
            val neighbour = BlockPos.MutableBlockPos()
            for (dx in -RADIUS..RADIUS) {
                for (dy in -RADIUS..RADIUS) {
                    for (dz in -RADIUS..RADIUS) {
                        mutable.set(center.x + dx, center.y + dy, center.z + dz)
                        if (!level.getBlockState(mutable).`is`(Blocks.CRACKED_STONE_BRICKS)) continue
                        var faces = 0
                        for (dir in Direction.entries) {
                            neighbour.set(mutable.x + dir.stepX, mutable.y + dir.stepY, mutable.z + dir.stepZ)
                            if (level.getBlockState(neighbour).isAir) {
                                faces = faces or (1 shl dir.ordinal)
                            }
                        }
                        if (faces != 0) found.add(Brick(mutable.x, mutable.y, mutable.z, faces))
                    }
                }
            }
            // Ближние стены важнее: сортируем по дистанции и режем лимит
            found.sortBy { (it.x - center.x) * (it.x - center.x) + (it.y - center.y) * (it.y - center.y) + (it.z - center.z) * (it.z - center.z) }
            cached = if (found.size > MAX_BRICKS) found.subList(0, MAX_BRICKS) else found
        } catch (_: Exception) {
        }
    }

    /** Сброс при смене мира/сервера. */
    fun onWorldChange() {
        cached = emptyList()
        lastScanMs = 0L
    }
}
