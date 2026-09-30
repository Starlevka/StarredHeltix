package set.starlev.features.visual

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import set.starlev.config.ConfigManager
import set.starlev.utils.ColorUtils

object BlockOverlayFeature {
    fun onRenderBlockOverlay(
        poseStack: PoseStack,
        entity: Entity,
        camX: Double,
        camY: Double,
        camZ: Double,
        blockPos: BlockPos,
        blockState: BlockState,
        submitNodeCollector: SubmitNodeCollector,
        cameraRenderState: CameraRenderState?
    ): Boolean {
        val config = ConfigManager.features.visuals.blockOverlay
        if (!config.enabled) return false

        val world = entity.level()
        val shape = blockState.getShape(world, blockPos, CollisionContext.of(entity))
        if (shape.isEmpty) return false

        val mc = Minecraft.getInstance()

        // ВАЖНО: на входе poseStack в камеро-относительных координатах
        // (ванильный LevelRenderer.submitBlockOutline транслирует под блок сам,
        // внутри метода). Поэтому как ванилла: push + translate(blockPos - camera),
        // shape/боксы — блок-локальные (0..1). Без транслейта контур 0..1 липнет
        // к камере (гигантские полосы), а заливка в мировых координатах улетает
        // на +cameraPos и каллится фрустумом.
        val camPos = cameraRenderState?.pos
            ?: net.minecraft.world.phys.Vec3(entity.x, entity.y, entity.z)
        poseStack.pushPose()
        try {
            poseStack.translate(
                blockPos.x - camPos.x,
                blockPos.y - camPos.y,
                blockPos.z - camPos.z
            )

            // Заливка — первой (свой SHRenderTypes.FILLED: quads + VIEW_OFFSET_Z_LAYERING —
            // ванильный debugQuads без z-смещения z-fighting'ится с гранью блока и пропадает
            // под углами). Боксы shape.forAllBoxes — блок-локальные, квады двусторонние.
            val fill = ColorUtils.parseColor(config.fillColor)
            val fillA = (fill ushr 24) and 0xFF
            if (fillA > 0) {
                shape.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
                    submitNodeCollector.submitCustomGeometry(poseStack, set.starlev.render.SHRenderTypes.FILLED) { pose, buffer ->
                        val m = pose.pose()
                        val x1 = minX.toFloat(); val y1 = minY.toFloat(); val z1 = minZ.toFloat()
                        val x2 = maxX.toFloat(); val y2 = maxY.toFloat(); val z2 = maxZ.toFloat()
                        val r = ((fill shr 16) and 0xFF) / 255f
                        val g = ((fill shr 8) and 0xFF) / 255f
                        val b = (fill and 0xFF) / 255f
                        val a = fillA / 255f
                        fun quad(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float, cx: Float, cy: Float, cz: Float, dx: Float, dy: Float, dz: Float) {
                            buffer.addVertex(m, ax, ay, az).setColor(r, g, b, a)
                            buffer.addVertex(m, bx, by, bz).setColor(r, g, b, a)
                            buffer.addVertex(m, cx, cy, cz).setColor(r, g, b, a)
                            buffer.addVertex(m, dx, dy, dz).setColor(r, g, b, a)
                            // Обратный обход — та же грань с другой стороны
                            buffer.addVertex(m, dx, dy, dz).setColor(r, g, b, a)
                            buffer.addVertex(m, cx, cy, cz).setColor(r, g, b, a)
                            buffer.addVertex(m, bx, by, bz).setColor(r, g, b, a)
                            buffer.addVertex(m, ax, ay, az).setColor(r, g, b, a)
                        }
                        quad(x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2)
                        quad(x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1)
                        quad(x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1)
                        quad(x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2)
                        quad(x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1)
                        quad(x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2)
                    }
                }
            }

            // Контур — поверх заливки (иначе полупрозрачная заливка перекрывает линии
            // на ближних гранях). Ванильный submitShapeOutline (ShapeOutlineFeatureRenderer),
            // ширина = appropriateLineWidth как у ванильного выделения.
            val outline = ColorUtils.parseColor(config.outlineColor)
            val outlineA = (outline ushr 24) and 0xFF
            if (outlineA > 0) {
                try {
                    val width = mc.gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth
                    submitNodeCollector.submitShapeOutline(poseStack, shape, RenderTypes.lines(), outline, width, false)
                } catch (_: Exception) {
                }
            }
        } finally {
            poseStack.popPose()
        }

        return true
    }
}
