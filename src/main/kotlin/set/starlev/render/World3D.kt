package set.starlev.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f

object World3D {
    private val mc = Minecraft.getInstance()

    fun box(stack: PoseStack, buffer: MultiBufferSource, box: AABB, color: Int, filled: Boolean = true) {
        val cam = mc.gameRenderer.mainCamera.position
        
        // Смещаем координаты бокса относительно камеры
        val x1 = (box.minX - cam.x).toFloat()
        val y1 = (box.minY - cam.y).toFloat()
        val z1 = (box.minZ - cam.z).toFloat()
        val x2 = (box.maxX - cam.x).toFloat()
        val y2 = (box.maxY - cam.y).toFloat()
        val z2 = (box.maxZ - cam.z).toFloat()
        
        stack.pushPose()
        
        if (filled) {
            val a = ((color shr 24) and 0xFF) / 255f
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8) and 0xFF) / 255f
            val b = (color and 0xFF) / 255f
            val buf = buffer.getBuffer(RenderType.lightning())
            val m = stack.last().pose()
            
            // Bottom
            addQuad(buf, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a)
            // Top
            addQuad(buf, m, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, b, a)
            // North
            addQuad(buf, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, b, a)
            // South
            addQuad(buf, m, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a)
            // West
            addQuad(buf, m, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, b, a)
            // East
            addQuad(buf, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a)
        } else {
            val buf = buffer.getBuffer(RenderType.lightning())
            val m = stack.last().pose()
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8) and 0xFF) / 255f
            val b = (color and 0xFF) / 255f
            val a = ((color shr 24) and 0xFF) / 255f
            val t = 0.01f
            
            // Bottom face
            drawThickLine(buf, m, x1, y1, z1, x2, y1, z1, r, g, b, a, t)
            drawThickLine(buf, m, x2, y1, z1, x2, y1, z2, r, g, b, a, t)
            drawThickLine(buf, m, x2, y1, z2, x1, y1, z2, r, g, b, a, t)
            drawThickLine(buf, m, x1, y1, z2, x1, y1, z1, r, g, b, a, t)
            // Top face
            drawThickLine(buf, m, x1, y2, z1, x2, y2, z1, r, g, b, a, t)
            drawThickLine(buf, m, x2, y2, z1, x2, y2, z2, r, g, b, a, t)
            drawThickLine(buf, m, x2, y2, z2, x1, y2, z2, r, g, b, a, t)
            drawThickLine(buf, m, x1, y2, z2, x1, y2, z1, r, g, b, a, t)
            // Vertical edges
            drawThickLine(buf, m, x1, y1, z1, x1, y2, z1, r, g, b, a, t)
            drawThickLine(buf, m, x2, y1, z1, x2, y2, z1, r, g, b, a, t)
            drawThickLine(buf, m, x2, y1, z2, x2, y2, z2, r, g, b, a, t)
            drawThickLine(buf, m, x1, y1, z2, x1, y2, z2, r, g, b, a, t)
        }
        stack.popPose()
    }

    private fun drawThickLine(buffer: VertexConsumer, m: Matrix4f, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, r: Float, g: Float, b: Float, a: Float, t: Float) {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1
        
        // Выбираем перпендикулярные векторы в зависимости от направления линии
        if (Math.abs(dx) > 0.001f) { // Линия вдоль X
            addQuad(buffer, m, x1, y1-t, z1, x2, y1-t, z1, x2, y1+t, z1, x1, y1+t, z1, r, g, b, a)
            addQuad(buffer, m, x1, y1, z1-t, x2, y1, z1-t, x2, y1, z1+t, x1, y1, z1+t, r, g, b, a)
        } else if (Math.abs(dy) > 0.001f) { // Линия вдоль Y
            addQuad(buffer, m, x1-t, y1, z1, x1+t, y1, z1, x1+t, y2, z1, x1-t, y2, z1, r, g, b, a)
            addQuad(buffer, m, x1, y1, z1-t, x1, y1, z1+t, x1, y2, z1+t, x1, y2, z1-t, r, g, b, a)
        } else { // Линия вдоль Z
            addQuad(buffer, m, x1-t, y1, z1, x1+t, y1, z1, x1+t, y1, z2, x1-t, y1, z2, r, g, b, a)
            addQuad(buffer, m, x1, y1-t, z1, x1, y1+t, z1, x1, y1+t, z2, x1, y1-t, z2, r, g, b, a)
        }
    }

    private fun addQuad(buffer: VertexConsumer, m: Matrix4f, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float, x4: Float, y4: Float, z4: Float, r: Float, g: Float, b: Float, a: Float) {
        buffer.addVertex(m, x1, y1, z1).setColor(r, g, b, a)
        buffer.addVertex(m, x2, y2, z2).setColor(r, g, b, a)
        buffer.addVertex(m, x3, y3, z3).setColor(r, g, b, a)
        buffer.addVertex(m, x4, y4, z4).setColor(r, g, b, a)
    }

    fun block(stack: PoseStack, buffer: MultiBufferSource, pos: BlockPos, color: Int) {
        box(stack, buffer, AABB(pos), color)
    }

    fun boxWorldCoords(stack: PoseStack, buffer: MultiBufferSource, box: AABB, color: Int, filled: Boolean = true) {
        stack.pushPose()
        
        if (filled) {
            val a = ((color shr 24) and 0xFF) / 255f
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8) and 0xFF) / 255f
            val b = (color and 0xFF) / 255f
            val buf = buffer.getBuffer(RenderType.lightning())
            val m = stack.last().pose()
            val x1 = box.minX.toFloat()
            val y1 = box.minY.toFloat()
            val z1 = box.minZ.toFloat()
            val x2 = box.maxX.toFloat()
            val y2 = box.maxY.toFloat()
            val z2 = box.maxZ.toFloat()

            // Bottom
            addQuad(buf, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a)
            // Top
            addQuad(buf, m, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, b, a)
            // North
            addQuad(buf, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, b, a)
            // South
            addQuad(buf, m, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a)
            // West
            addQuad(buf, m, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, b, a)
            // East
            addQuad(buf, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a)
        } else {
            val buf = buffer.getBuffer(RenderType.lightning())
            val m = stack.last().pose()
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8) and 0xFF) / 255f
            val b = (color and 0xFF) / 255f
            val a = ((color shr 24) and 0xFF) / 255f
            val t = 0.01f
            val x1 = box.minX.toFloat()
            val y1 = box.minY.toFloat()
            val z1 = box.minZ.toFloat()
            val x2 = box.maxX.toFloat()
            val y2 = box.maxY.toFloat()
            val z2 = box.maxZ.toFloat()

            // Bottom face
            drawThickLine(buf, m, x1, y1, z1, x2, y1, z1, r, g, b, a, t)
            drawThickLine(buf, m, x2, y1, z1, x2, y1, z2, r, g, b, a, t)
            drawThickLine(buf, m, x2, y1, z2, x1, y1, z2, r, g, b, a, t)
            drawThickLine(buf, m, x1, y1, z2, x1, y1, z1, r, g, b, a, t)
            // Top face
            drawThickLine(buf, m, x1, y2, z1, x2, y2, z1, r, g, b, a, t)
            drawThickLine(buf, m, x2, y2, z1, x2, y2, z2, r, g, b, a, t)
            drawThickLine(buf, m, x2, y2, z2, x1, y2, z2, r, g, b, a, t)
            drawThickLine(buf, m, x1, y2, z2, x1, y2, z1, r, g, b, a, t)
            // Vertical edges
            drawThickLine(buf, m, x1, y1, z1, x1, y2, z1, r, g, b, a, t)
            drawThickLine(buf, m, x2, y1, z1, x2, y2, z1, r, g, b, a, t)
            drawThickLine(buf, m, x2, y1, z2, x2, y2, z2, r, g, b, a, t)
            drawThickLine(buf, m, x1, y1, z2, x1, y2, z2, r, g, b, a, t)
        }
        stack.popPose()
    }

    fun line(stack: PoseStack, buffer: MultiBufferSource, from: Vec3, to: Vec3, color: Int) {
        val cam = mc.gameRenderer.mainCamera.position
        stack.pushPose()
        stack.translate(-cam.x, -cam.y, -cam.z)
        val buf = buffer.getBuffer(RenderType.lightning())
        val m = stack.last().pose()
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val a = ((color shr 24) and 0xFF) / 255f
        
        drawThickLine(buf, m, from.x.toFloat(), from.y.toFloat(), from.z.toFloat(), to.x.toFloat(), to.y.toFloat(), to.z.toFloat(), r, g, b, a, 0.01f)
        stack.popPose()
    }

    private fun buildCube(matrix: Matrix4f, buf: VertexConsumer, color: Int) {
        buildFace(matrix, buf, 0f, 0f, 0f, 1f, 0f, 0f, color)
        buildFace(matrix, buf, 0f, 0f, 1f, 1f, 0f, 1f, color)
        buildFace(matrix, buf, 0f, 1f, 0f, 1f, 1f, 0f, color)
        buildFace(matrix, buf, 0f, 1f, 1f, 1f, 1f, 1f, color)
        buildFace(matrix, buf, 0f, 0f, 0f, 0f, 1f, 0f, color)
        buildFace(matrix, buf, 1f, 0f, 0f, 1f, 1f, 0f, color)
        buildFace(matrix, buf, 0f, 0f, 1f, 0f, 1f, 1f, color)
        buildFace(matrix, buf, 1f, 0f, 1f, 1f, 1f, 1f, color)
        buildFace(matrix, buf, 0f, 0f, 0f, 0f, 0f, 1f, color)
        buildFace(matrix, buf, 1f, 0f, 0f, 1f, 0f, 1f, color)
        buildFace(matrix, buf, 0f, 1f, 0f, 0f, 1f, 1f, color)
        buildFace(matrix, buf, 1f, 1f, 0f, 1f, 1f, 1f, color)
    }

    private fun buildFace(m: Matrix4f, b: VertexConsumer, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, c: Int) {
        val normal = Vector3f(x2 - x1, y2 - y1, z2 - z1).normalize()
        b.addVertex(m, x1, y1, z1).setColor(c).setNormal(normal.x, normal.y, normal.z)
        b.addVertex(m, x2, y2, z2).setColor(c).setNormal(normal.x, normal.y, normal.z)
    }
}
