package set.starlev.features.combat.dungeons

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import org.joml.Matrix4f
import set.starlev.StarredHeltix
import set.starlev.render.RenderContext
import set.starlev.render.RenderEvents
import set.starlev.render.SHRenderTypes
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.DungeonDetector

/**
 * Зелёные стены в подземельях — сплошная заливка потресканного каменного кирпича
 * цветом из конфига (`dungeons.visuals.greenTextures`).
 *
 * Вкл/выкл — тумблер «Зеленые стены»: состояние читается каждый кадр, поэтому
 * переключение мгновенное, а при выключении кэш позиций сбрасывается (фича
 * «выгружается» и не держит геометрию в памяти).
 *
 * Почему без текстуры кирпича (атлас, `Sheets.chooseSprite`, кастомный
 * `core/position_tex_color`): у блоков UV запекаются в меши чанков один раз
 * (`SectionCompiler.compile` → `MeshData`), подмена PNG ударила бы по всей игре,
 * а не только по данжам. Плюс атлас пересобирается при ресорспаках — UV спрайтов
 * едут, и близкая стена начинает мерцать. Сундуки и летучие мыши спрайт выбирают
 * каждый кадр, поэтому там подмена спрайта осталась (см. рендереры сундуков/мышей).
 *
 * Скан рядом с игроком раз в 500мс (кэш позиций + битмаска граней), затем один
 * submitCustomGeometry на кадр. Только в данжах.
 */
object GreenWalls {
    private val mc = Minecraft.getInstance()

    private const val RADIUS = 24
    private const val RESCAN_MS = 500L
    private const val MAX_BRICKS = 192
    private const val EPS = 0.0025

    private data class Brick(val x: Int, val y: Int, val z: Int, val faces: Int)

    /**
     * Таблица граней: 4 вершины как смещения (dx,dy,dz) внутри блока, затем ось U,
     * ось V и нормаль (для EPS-смещения наружу). Порядок граней совпадает с
     * Direction.ordinal, развёртка обратная продублируется в эмиттере (cull выкл).
     */
    private val FACES = arrayOf(
        // DOWN: u=X, v=Z
        floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f, 0f, 1f, 0f, 2f, 0f, -1f, 0f),
        // UP: u=X, v=Z
        floatArrayOf(0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 1f, 1f, 0f, 0f, 0f, 2f, 0f, 1f, 0f),
        // NORTH: u=X, v=Y
        floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, -1f),
        // SOUTH: u=X, v=Y
        floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f),
        // WEST: u=Z, v=Y
        floatArrayOf(0f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f, 0f, 2f, 1f, -1f, 0f, 0f),
        // EAST: u=Z, v=Y
        floatArrayOf(0f, 0f, 0f, 1f, 0f, 1f, 1f, 1f, 1f, 0f, 1f, 0f, 2f, 1f, 1f, 0f, 0f),
    )

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
                for (br in bricks) {
                    if (!context.isBoxVisible(AABB(br.x.toDouble(), br.y.toDouble(), br.z.toDouble(), br.x + 1.0, br.y + 1.0, br.z + 1.0))) continue
                    val x = br.x.toDouble()
                    val y = br.y.toDouble()
                    val z = br.z.toDouble()
                    for (face in 0..5) {
                        if (br.faces and (1 shl face) == 0) continue
                        emitFlat(buffer, m, x, y, z, FACES[face], r, g, b, a)
                    }
                }
            }
        } finally {
            context.matrices.popPose()
        }
    }

    /** Плоская заливка: формат POSITION_COLOR, атрибут только цвет. */
    private fun emitFlat(
        buffer: VertexConsumer,
        m: Matrix4f,
        bx: Double,
        by: Double,
        bz: Double,
        face: FloatArray,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
    ) {
        val x0 = bx + face[14] * EPS
        val y0 = by + face[15] * EPS
        val z0 = bz + face[16] * EPS
        for (i in 0 until 4) {
            val o = i * 3
            buffer.addVertex(
                m,
                (x0 + face[o]).toFloat(),
                (y0 + face[o + 1]).toFloat(),
                (z0 + face[o + 2]).toFloat()
            ).setColor(r, g, b, a)
        }
        for (i in 3 downTo 0) {
            val o = i * 3
            buffer.addVertex(
                m,
                (x0 + face[o]).toFloat(),
                (y0 + face[o + 1]).toFloat(),
                (z0 + face[o + 2]).toFloat()
            ).setColor(r, g, b, a)
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
