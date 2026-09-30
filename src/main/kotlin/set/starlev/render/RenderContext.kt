package set.starlev.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f

/**
 * Контекст world-рендера 26.2 (extract/submit модель).
 * Все отрисовки собираются в SubmitNodeCollector через submitCustomGeometry.
 */
class RenderContext(
    val matrices: PoseStack,
    val camera: net.minecraft.client.Camera,
    val tickDelta: Float,
    val submitNodeCollector: SubmitNodeCollector,
    val cameraRenderState: net.minecraft.client.renderer.state.level.CameraRenderState? = null
) {
    val cameraPos: Vec3 get() = cameraRenderState?.pos ?: camera.position()

    private fun isVisible(aabb: AABB): Boolean {
        val frustum = cameraRenderState?.cullFrustum ?: return true
        return frustum.isVisible(aabb)
    }

    /** Публичная проверка фрустума — для LOD/каллинга из модулей. */
    fun isBoxVisible(aabb: AABB): Boolean = isVisible(aabb)

    fun renderBox(aabb: AABB, r: Float, g: Float, b: Float, a: Float, fill: Boolean = false, thickness: Float = 0f) {
        renderBoxInternal(aabb, r, g, b, a, fill, thickness, false, false)
    }

    fun renderBoxThroughBlocks(aabb: AABB, r: Float, g: Float, b: Float, a: Float, fill: Boolean = false, thickness: Float = 0f) {
        renderBoxInternal(aabb, r, g, b, a, fill, thickness, true, true)
    }

    fun renderBoxSeeThrough(aabb: AABB, r: Float, g: Float, b: Float, a: Float, fill: Boolean = false, thickness: Float = 0f) {
        renderBoxInternal(aabb, r, g, b, a, fill, thickness, true, true)
    }

    private fun renderBoxInternal(aabb: AABB, r: Float, g: Float, b: Float, a: Float, fill: Boolean, thickness: Float, skipVisibility: Boolean, seeThrough: Boolean = false) {
        if (!skipVisibility && !isVisible(aabb)) return

        val cam = cameraPos
        matrices.pushPose()
        matrices.translate((aabb.minX - cam.x), (aabb.minY - cam.y), (aabb.minZ - cam.z))

        val box = aabb.move(-aabb.minX, -aabb.minY, -aabb.minZ)

        // Контур — настоящие линии (POSITION_COLOR_NORMAL_LINE_WIDTH + setLineWidth),
        // как ванильный submitShapeOutline и LineBoxRenderer у Skysoft.
        // Раньше был lightning()-квады — тонко, тускло и с аддитивным блендом.
        if (fill) {
            drawFilledBoxQuads(box, r, g, b, a, seeThrough)
        } else {
            drawBoxLines(box, r, g, b, a, thickness)
        }

        matrices.popPose()
    }

    /** Заливка бокса: свой FILLED (debugQuads + VIEW_OFFSET_Z_LAYERING — ванильный debugQuads z-fighting'ится) или textSeeThrough для ESP. Двусторонние квады для надёжности. */
    private fun drawFilledBoxQuads(box: AABB, r: Float, g: Float, b: Float, a: Float, seeThrough: Boolean) {
        val renderType = if (seeThrough) {
            RenderTypes.textSeeThrough(Identifier.fromNamespaceAndPath("starredheltix", "textures/misc/white.png"))
        } else {
            SHRenderTypes.FILLED
        }
        submitNodeCollector.submitCustomGeometry(matrices, renderType) { pose, buffer ->
            val m = pose.pose()
            val x1 = box.minX.toFloat(); val y1 = box.minY.toFloat(); val z1 = box.minZ.toFloat()
            val x2 = box.maxX.toFloat(); val y2 = box.maxY.toFloat(); val z2 = box.maxZ.toFloat()
            if (seeThrough) {
                addQuadSeeThrough(buffer, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a)
                addQuadSeeThrough(buffer, m, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, b, a)
                addQuadSeeThrough(buffer, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, b, a)
                addQuadSeeThrough(buffer, m, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a)
                addQuadSeeThrough(buffer, m, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, b, a)
                addQuadSeeThrough(buffer, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a)
            } else {
                addQuad(buffer, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a)
                addQuad(buffer, m, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, b, a)
                addQuad(buffer, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, b, a)
                addQuad(buffer, m, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a)
                addQuad(buffer, m, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, b, a)
                addQuad(buffer, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a)
            }
        }
    }

    /** Обводка бокса — проверенным ванильным путём submitShapeOutline (фаза shapeOutlines,
     *  тот же вызов, что рисует контур выбранного блока). Своя геометрия с lines() из
     *  кастомных сабмитов не видна, поэтому только так. */
    private fun drawBoxLines(box: AABB, r: Float, g: Float, b: Float, a: Float, thickness: Float) {
        // Ширина линии: ваниль использует windowRenderState.appropriateLineWidth как есть.
        val mc = net.minecraft.client.Minecraft.getInstance()
        val width = try {
            mc.gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth
        } catch (_: Exception) { 2f } * (if (thickness > 0f) thickness else 1f)
        val color = ((a * 255f).toInt().coerceIn(0, 255) shl 24) or
            ((r * 255f).toInt().coerceIn(0, 255) shl 16) or
            ((g * 255f).toInt().coerceIn(0, 255) shl 8) or
            (b * 255f).toInt().coerceIn(0, 255)
        try {
            submitNodeCollector.submitShapeOutline(
                matrices,
                net.minecraft.world.phys.shapes.Shapes.create(box),
                RenderTypes.lines(),
                color, width, false
            )
        } catch (_: Exception) {
        }
    }

    private fun addQuad(buffer: VertexConsumer, m: Matrix4f, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float, x4: Float, y4: Float, z4: Float, r: Float, g: Float, b: Float, a: Float) {
        buffer.addVertex(m, x1, y1, z1).setColor(r, g, b, a)
        buffer.addVertex(m, x2, y2, z2).setColor(r, g, b, a)
        buffer.addVertex(m, x3, y3, z3).setColor(r, g, b, a)
        buffer.addVertex(m, x4, y4, z4).setColor(r, g, b, a)
        // Обратный обход — та же грань с другой стороны (debugQuads куллит задние грани)
        buffer.addVertex(m, x4, y4, z4).setColor(r, g, b, a)
        buffer.addVertex(m, x3, y3, z3).setColor(r, g, b, a)
        buffer.addVertex(m, x2, y2, z2).setColor(r, g, b, a)
        buffer.addVertex(m, x1, y1, z1).setColor(r, g, b, a)
    }

    private fun addQuadSeeThrough(buffer: VertexConsumer, m: Matrix4f, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float, x4: Float, y4: Float, z4: Float, r: Float, g: Float, b: Float, a: Float) {
        // textSeeThrough требует UV + light
        buffer.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setUv(0f, 0f).setUv2(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT)
        buffer.addVertex(m, x2, y2, z2).setColor(r, g, b, a).setUv(0f, 0f).setUv2(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT)
        buffer.addVertex(m, x3, y3, z3).setColor(r, g, b, a).setUv(0f, 0f).setUv2(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT)
        buffer.addVertex(m, x4, y4, z4).setColor(r, g, b, a).setUv(0f, 0f).setUv2(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT)
    }

    fun renderItem(itemStack: ItemStack, x: Double, y: Double, z: Double, scale: Float = 1f, rotation: Float = 0f) {
        val mc = net.minecraft.client.Minecraft.getInstance()
        val cam = cameraPos

        matrices.pushPose()
        matrices.translate(x - cam.x, y - cam.y, z - cam.z)
        matrices.scale(scale, scale, scale)
        matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation))

        val tracking = net.minecraft.client.renderer.item.ItemStackRenderState()
        mc.getItemModelResolver().updateForTopItem(
            tracking,
            itemStack,
            ItemDisplayContext.FIXED,
            mc.level,
            null,
            0
        )
        tracking.submit(matrices, submitNodeCollector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0)

        matrices.popPose()
    }

    fun renderLine(start: Vec3, end: Vec3, color: Int, thickness: Float = 1f) {
        val cam = cameraPos
        matrices.pushPose()
        matrices.translate(-cam.x, -cam.y, -cam.z)

        val renderType = RenderTypes.lightning()
        val r = (color shr 16 and 0xFF) / 255f
        val g = (color shr 8 and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val a = ((color shr 24 and 0xFF) / 255f).coerceAtLeast(0.25f)

        val t = 0.01f * thickness

        val dir = end.subtract(start).normalize()
        val up = if (kotlin.math.abs(dir.y) < 0.9) Vec3(0.0, 1.0, 0.0) else Vec3(1.0, 0.0, 0.0)
        val side = dir.cross(up).normalize().scale(t.toDouble())
        val perpendicular = dir.cross(side).normalize().scale(t.toDouble())

        val p1 = start.add(side).add(perpendicular)
        val p2 = start.subtract(side).add(perpendicular)
        val p3 = end.subtract(side).add(perpendicular)
        val p4 = end.add(side).add(perpendicular)

        val p5 = start.add(side).subtract(perpendicular)
        val p6 = start.subtract(side).subtract(perpendicular)
        val p7 = end.subtract(side).subtract(perpendicular)
        val p8 = end.add(side).subtract(perpendicular)

        submitNodeCollector.submitCustomGeometry(matrices, renderType) { pose, buffer ->
            val m = pose.pose()
            addQuad(buffer, m, p1.x.toFloat(), p1.y.toFloat(), p1.z.toFloat(),
                p2.x.toFloat(), p2.y.toFloat(), p2.z.toFloat(),
                p3.x.toFloat(), p3.y.toFloat(), p3.z.toFloat(),
                p4.x.toFloat(), p4.y.toFloat(), p4.z.toFloat(), r, g, b, a)

            addQuad(buffer, m, p5.x.toFloat(), p5.y.toFloat(), p5.z.toFloat(),
                p6.x.toFloat(), p6.y.toFloat(), p6.z.toFloat(),
                p7.x.toFloat(), p7.y.toFloat(), p7.z.toFloat(),
                p8.x.toFloat(), p8.y.toFloat(), p8.z.toFloat(), r, g, b, a)

            val p1_2 = start.add(side).add(perpendicular)
            val p4_2 = end.add(side).add(perpendicular)
            val p8_2 = end.add(side).subtract(perpendicular)
            val p5_2 = start.add(side).subtract(perpendicular)

            addQuad(buffer, m, p1_2.x.toFloat(), p1_2.y.toFloat(), p1_2.z.toFloat(),
                p4_2.x.toFloat(), p4_2.y.toFloat(), p4_2.z.toFloat(),
                p8_2.x.toFloat(), p8_2.y.toFloat(), p8_2.z.toFloat(),
                p5_2.x.toFloat(), p5_2.y.toFloat(), p5_2.z.toFloat(), r, g, b, a)

            val p2_2 = start.subtract(side).add(perpendicular)
            val p3_2 = end.subtract(side).add(perpendicular)
            val p7_2 = end.subtract(side).subtract(perpendicular)
            val p6_2 = start.subtract(side).subtract(perpendicular)

            addQuad(buffer, m, p2_2.x.toFloat(), p2_2.y.toFloat(), p2_2.z.toFloat(),
                p3_2.x.toFloat(), p3_2.y.toFloat(), p3_2.z.toFloat(),
                p7_2.x.toFloat(), p7_2.y.toFloat(), p7_2.z.toFloat(),
                p6_2.x.toFloat(), p6_2.y.toFloat(), p6_2.z.toFloat(), r, g, b, a)
        }

        matrices.popPose()
    }

    fun renderImage(
        texture: Identifier, x: Double, y: Double, z: Double,
        width: Float, height: Float, rotationY: Float = 0f,
        throughBlocks: Boolean = false
    ) {
        renderAnimatedImage(texture, x, y, z, width, height, rotationY, 1, 1, 0f, throughBlocks)
    }

    fun renderAnimatedImage(
        texture: Identifier,
        x: Double,
        y: Double,
        z: Double,
        width: Float,
        height: Float,
        rotationY: Float = 0f,
        columns: Int = 1,
        rows: Int = 1,
        fps: Float = 20f,
        throughBlocks: Boolean = false
    ) {
        val cam = cameraPos

        matrices.pushPose()
        matrices.translate(x - cam.x, y - cam.y, z - cam.z)
        matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotationY))

        // throughBlocks: textSeeThrough рисуется без depth-test — видна сквозь стены (ESP-эффект)
        val buffer = if (throughBlocks) RenderTypes.textSeeThrough(texture) else RenderTypes.text(texture)

        val totalFrames = columns * rows
        val frameIndex = if (totalFrames > 1) {
            (System.currentTimeMillis() / (1000 / fps)).toInt() % totalFrames
        } else 0

        val frameX = frameIndex % columns
        val frameY = frameIndex / columns

        val uMin = frameX.toFloat() / columns
        val uMax = (frameX + 1).toFloat() / columns
        val vMin = frameY.toFloat() / rows
        val vMax = (frameY + 1).toFloat() / rows

        submitNodeCollector.submitCustomGeometry(matrices, buffer) { pose, buf ->
            buf.addVertex(pose, 0f, height, 0f).setColor(1f, 1f, 1f, 1f).setUv(uMin, vMax).setUv2(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT)
            buf.addVertex(pose, width, height, 0f).setColor(1f, 1f, 1f, 1f).setUv(uMax, vMax).setUv2(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT)
            buf.addVertex(pose, width, 0f, 0f).setColor(1f, 1f, 1f, 1f).setUv(uMax, vMin).setUv2(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT)
            buf.addVertex(pose, 0f, 0f, 0f).setColor(1f, 1f, 1f, 1f).setUv(uMin, vMin).setUv2(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT)
        }

        matrices.popPose()
    }

    fun renderText(text: String, x: Double, y: Double, z: Double, scale: Float = 1f, color: Int = 0xFFFFFFFF.toInt(), shadow: Boolean = true, seeThrough: Boolean = false) {
        val mc = net.minecraft.client.Minecraft.getInstance()
        val font = mc.font
        val cam = cameraPos
        // Матрица как в ванильном submitNameTag (SubmitNodeCollection): billboard по
        // camera.orientation + scale с инверсией Y. От submitNameTag отказались:
        // он жёстко красит своим цветом, смещает на +0.5 и не слушает наш scale/color.
        val s = if (scale <= 0f) 1f else scale
        val textScale = 0.025f * s
        val billboard: org.joml.Quaternionf = (cameraRenderState?.orientation ?: camera.rotation()).let {
            if (it is org.joml.Quaternionf) it else org.joml.Quaternionf(it)
        }
        matrices.pushPose()
        matrices.translate(x - cam.x, y - cam.y, z - cam.z)
        matrices.mulPose(billboard)
        matrices.scale(textScale, -textScale, textScale)

        // DisplayMode как у DisplayRenderer: NORMAL — только для GUI, в мире POLYGON_OFFSET
        val displayMode = if (seeThrough) Font.DisplayMode.SEE_THROUGH else Font.DisplayMode.POLYGON_OFFSET
        val bgOpacity = if (seeThrough) (0.4f * 255).toInt() shl 24 else 0

        val sequence: net.minecraft.util.FormattedCharSequence = net.minecraft.network.chat.Component.literal(text).getVisualOrderText()
        val xOffset = -font.width(sequence) / 2f
        submitNodeCollector.submitText(
            matrices, xOffset, 0f, sequence,
            shadow, displayMode, LightCoordsUtil.FULL_BRIGHT, color, bgOpacity, 0
        )

        matrices.popPose()
    }

    fun renderBeaconBeam(x: Double, y: Double, z: Double, height: Int, color: Int, tickDelta: Float, seeThrough: Boolean = false) {
        val cam = cameraPos
        matrices.pushPose()
        matrices.translate(x - cam.x, y - cam.y, z - cam.z)
        val r = (color shr 16 and 0xFF) / 255f
        val g = (color shr 8 and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val a = 0.5f
        val size = 0.3f
        val renderType = if (seeThrough) RenderTypes.textSeeThrough(Identifier.fromNamespaceAndPath("starredheltix", "textures/misc/white.png")) else RenderTypes.lightning()

        submitNodeCollector.submitCustomGeometry(matrices, renderType) { pose, buffer ->
            val m = pose.pose()
            if (seeThrough) {
                addQuadSeeThrough(buffer, m, -size, 0f, -size, size, 0f, -size, size, 0f, size, -size, 0f, size, r, g, b, a)
                addQuadSeeThrough(buffer, m, -size, 0f, -size, -size, height.toFloat(), -size, size, height.toFloat(), -size, size, 0f, -size, r, g, b, a)
                addQuadSeeThrough(buffer, m, size, 0f, -size, size, height.toFloat(), -size, size, height.toFloat(), size, size, 0f, size, r, g, b, a)
                addQuadSeeThrough(buffer, m, size, 0f, size, size, height.toFloat(), size, -size, height.toFloat(), size, -size, 0f, size, r, g, b, a)
                addQuadSeeThrough(buffer, m, -size, 0f, size, -size, height.toFloat(), size, size, height.toFloat(), size, size, 0f, size, r, g, b, a)
            } else {
                addQuad(buffer, m, -size, 0f, -size, size, 0f, -size, size, 0f, size, -size, 0f, size, r, g, b, a)
                addQuad(buffer, m, -size, 0f, -size, -size, height.toFloat(), -size, size, height.toFloat(), -size, size, 0f, -size, r, g, b, a)
                addQuad(buffer, m, size, 0f, -size, size, height.toFloat(), -size, size, height.toFloat(), size, size, 0f, size, r, g, b, a)
                addQuad(buffer, m, size, 0f, size, size, height.toFloat(), size, -size, height.toFloat(), size, -size, 0f, size, r, g, b, a)
                addQuad(buffer, m, -size, 0f, size, -size, height.toFloat(), size, size, height.toFloat(), size, size, 0f, size, r, g, b, a)
            }
        }
        matrices.popPose()
    }

    fun blockPos(pos: BlockPos): Vec3 = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)

    /**
     * Круг в мире (из толстых двусторонних сегментов).
     * @param axis плоскость круга: "y" — горизонтальный (XZ), "x" — YZ, "z" — вертикальный (XY)
     */
    fun renderCircle(
        x: Double, y: Double, z: Double,
        radius: Double, color: Int = 0xFFFFFF,
        thickness: Float = 2f, segments: Int = 32, axis: String = "y"
    ) {
        if (radius <= 0 || segments < 3) return
        val cam = cameraPos
        val r = (color shr 16 and 0xFF) / 255f
        val g = (color shr 8 and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val a = ((color shr 24 and 0xFF) / 255f).coerceAtLeast(0.25f)
        val t = 0.01f * thickness

        val (u, v) = when (axis.lowercase()) {
            "x" -> Vec3(0.0, 1.0, 0.0) to Vec3(0.0, 0.0, 1.0)
            "z" -> Vec3(1.0, 0.0, 0.0) to Vec3(0.0, 1.0, 0.0)
            else -> Vec3(1.0, 0.0, 0.0) to Vec3(0.0, 0.0, 1.0)
        }
        // Нормаль плоскости — направление толщины сегментов
        val normal = u.cross(v).normalize().scale(t.toDouble())
        val center = Vec3(x, y, z)

        matrices.pushPose()
        matrices.translate(-cam.x, -cam.y, -cam.z)

        submitNodeCollector.submitCustomGeometry(matrices, RenderTypes.lightning()) { pose, buffer ->
            val m = pose.pose()
            val n = segments.coerceIn(3, 128)
            for (i in 0 until n) {
                val a0 = 2.0 * Math.PI * i / n
                val a1 = 2.0 * Math.PI * (i + 1) / n
                val p1 = center.add(
                    u.scale(radius * kotlin.math.cos(a0)).add(v.scale(radius * kotlin.math.sin(a0)))
                )
                val p2 = center.add(
                    u.scale(radius * kotlin.math.cos(a1)).add(v.scale(radius * kotlin.math.sin(a1)))
                )
                // Двусторонний квад по нормали плоскости
                addQuad(buffer, m,
                    (p1.x + normal.x).toFloat(), (p1.y + normal.y).toFloat(), (p1.z + normal.z).toFloat(),
                    (p2.x + normal.x).toFloat(), (p2.y + normal.y).toFloat(), (p2.z + normal.z).toFloat(),
                    (p2.x - normal.x).toFloat(), (p2.y - normal.y).toFloat(), (p2.z - normal.z).toFloat(),
                    (p1.x - normal.x).toFloat(), (p1.y - normal.y).toFloat(), (p1.z - normal.z).toFloat(),
                    r, g, b, a)
                addQuad(buffer, m,
                    (p2.x + normal.x).toFloat(), (p2.y + normal.y).toFloat(), (p2.z + normal.z).toFloat(),
                    (p1.x + normal.x).toFloat(), (p1.y + normal.y).toFloat(), (p1.z + normal.z).toFloat(),
                    (p1.x - normal.x).toFloat(), (p1.y - normal.y).toFloat(), (p1.z - normal.z).toFloat(),
                    (p2.x - normal.x).toFloat(), (p2.y - normal.y).toFloat(), (p2.z - normal.z).toFloat(),
                    r, g, b, a)
            }
        }
        matrices.popPose()
    }

    companion object {
        @Volatile
        var current: RenderContext? = null
    }
}
