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
            val buf = buffer.getBuffer(RenderType.debugFilledBox())
            val m = stack.last()
            buf.addVertex(m, x1, y1, z1).setColor(r, g, b, a)
            buf.addVertex(m, x1, y1, z2).setColor(r, g, b, a)
            buf.addVertex(m, x1, y2, z1).setColor(r, g, b, a)
            buf.addVertex(m, x1, y2, z2).setColor(r, g, b, a)
            buf.addVertex(m, x2, y2, z2).setColor(r, g, b, a)
            buf.addVertex(m, x1, y1, z2).setColor(r, g, b, a)
            buf.addVertex(m, x2, y1, z2).setColor(r, g, b, a)
            buf.addVertex(m, x1, y1, z1).setColor(r, g, b, a)
            buf.addVertex(m, x2, y1, z1).setColor(r, g, b, a)
            buf.addVertex(m, x1, y2, z1).setColor(r, g, b, a)
            buf.addVertex(m, x2, y2, z1).setColor(r, g, b, a)
            buf.addVertex(m, x2, y2, z2).setColor(r, g, b, a)
            buf.addVertex(m, x2, y1, z1).setColor(r, g, b, a)
            buf.addVertex(m, x2, y1, z2).setColor(r, g, b, a)
        } else {
            val buf = buffer.getBuffer(RenderType.lines())
            val m = stack.last().pose()
            // Bottom face
            drawLine(m, buf, x1, y1, z1, x2, y1, z1, color)
            drawLine(m, buf, x2, y1, z1, x2, y1, z2, color)
            drawLine(m, buf, x2, y1, z2, x1, y1, z2, color)
            drawLine(m, buf, x1, y1, z2, x1, y1, z1, color)
            // Top face
            drawLine(m, buf, x1, y2, z1, x2, y2, z1, color)
            drawLine(m, buf, x2, y2, z1, x2, y2, z2, color)
            drawLine(m, buf, x2, y2, z2, x1, y2, z2, color)
            drawLine(m, buf, x1, y2, z2, x1, y2, z1, color)
            // Vertical edges
            drawLine(m, buf, x1, y1, z1, x1, y2, z1, color)
            drawLine(m, buf, x2, y1, z1, x2, y2, z1, color)
            drawLine(m, buf, x2, y1, z2, x2, y2, z2, color)
            drawLine(m, buf, x1, y1, z2, x1, y2, z2, color)
        }
        stack.popPose()
    }

    private fun drawLine(m: Matrix4f, buf: VertexConsumer, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, color: Int) {
        buf.addVertex(m, x1, y1, z1).setColor(color).setNormal(0f, 1f, 0f)
        buf.addVertex(m, x2, y2, z2).setColor(color).setNormal(0f, 1f, 0f)
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
            val buf = buffer.getBuffer(RenderType.debugFilledBox())
            val m = stack.last()
            buf.addVertex(m, box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).setColor(r, g, b, a)
            buf.addVertex(m, box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).setColor(r, g, b, a)
            buf.addVertex(m, box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).setColor(r, g, b, a)
            buf.addVertex(m, box.minX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).setColor(r, g, b, a)
            buf.addVertex(m, box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).setColor(r, g, b, a)
            buf.addVertex(m, box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).setColor(r, g, b, a)
            buf.addVertex(m, box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).setColor(r, g, b, a)
            buf.addVertex(m, box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).setColor(r, g, b, a)
            buf.addVertex(m, box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).setColor(r, g, b, a)
            buf.addVertex(m, box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).setColor(r, g, b, a)
            buf.addVertex(m, box.maxX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).setColor(r, g, b, a)
            buf.addVertex(m, box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).setColor(r, g, b, a)
            buf.addVertex(m, box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).setColor(r, g, b, a)
            buf.addVertex(m, box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).setColor(r, g, b, a)
        } else {
            val buf = buffer.getBuffer(RenderType.lines())
            val m = stack.last().pose()
            // Bottom face
            drawLine(m, buf, box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat(), box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat(), color)
            drawLine(m, buf, box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat(), box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat(), color)
            drawLine(m, buf, box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat(), box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat(), color)
            drawLine(m, buf, box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat(), box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat(), color)
            // Top face
            drawLine(m, buf, box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat(), box.maxX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat(), color)
            drawLine(m, buf, box.maxX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat(), box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat(), color)
            drawLine(m, buf, box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat(), box.minX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat(), color)
            drawLine(m, buf, box.minX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat(), box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat(), color)
            // Vertical edges
            drawLine(m, buf, box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat(), box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat(), color)
            drawLine(m, buf, box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat(), box.maxX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat(), color)
            drawLine(m, buf, box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat(), box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat(), color)
            drawLine(m, buf, box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat(), box.minX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat(), color)
        }
        stack.popPose()
    }

    fun line(stack: PoseStack, buffer: MultiBufferSource, from: Vec3, to: Vec3, color: Int) {
        val cam = mc.gameRenderer.mainCamera.position
        stack.pushPose()
        stack.translate(-cam.x, -cam.y, -cam.z)
        val buf = buffer.getBuffer(RenderType.lines())
        val normal = Vector3f(to.x.toFloat(), to.y.toFloat(), to.z.toFloat())
            .sub(from.x.toFloat(), from.y.toFloat(), from.z.toFloat()).normalize()
        buf.addVertex(stack.last(), from.x.toFloat(), from.y.toFloat(), from.z.toFloat())
            .setColor(color).setNormal(stack.last(), normal)
        buf.addVertex(stack.last(), to.x.toFloat(), to.y.toFloat(), to.z.toFloat())
            .setColor(color).setNormal(stack.last(), normal)
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
