package set.starlev.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4fc

object World3D {
    private val mc = Minecraft.getInstance()
    private const val LIGHT = 15728880

    private fun addQuad(buffer: VertexConsumer, m: Matrix4fc, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float, x4: Float, y4: Float, z4: Float, r: Float, g: Float, b: Float, a: Float) {
        buffer.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setUv2(LIGHT, LIGHT)
        buffer.addVertex(m, x2, y2, z2).setColor(r, g, b, a).setUv2(LIGHT, LIGHT)
        buffer.addVertex(m, x3, y3, z3).setColor(r, g, b, a).setUv2(LIGHT, LIGHT)
        buffer.addVertex(m, x4, y4, z4).setColor(r, g, b, a).setUv2(LIGHT, LIGHT)
    }

    private fun drawThickLine(buffer: VertexConsumer, m: Matrix4fc, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, r: Float, g: Float, b: Float, a: Float, t: Float) {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1

        if (Math.abs(dx) > 0.001f) {
            addQuad(buffer, m, x1, y1-t, z1, x2, y1-t, z1, x2, y1+t, z1, x1, y1+t, z1, r, g, b, a)
            addQuad(buffer, m, x1, y1, z1-t, x2, y1, z1-t, x2, y1, z1+t, x1, y1, z1+t, r, g, b, a)
        } else if (Math.abs(dy) > 0.001f) {
            addQuad(buffer, m, x1-t, y1, z1, x1+t, y1, z1, x1+t, y2, z1, x1-t, y2, z1, r, g, b, a)
            addQuad(buffer, m, x1, y1, z1-t, x1, y1, z1+t, x1, y2, z1+t, x1, y2, z1-t, r, g, b, a)
        } else {
            addQuad(buffer, m, x1-t, y1, z1, x1+t, y1, z1, x1+t, y1, z2, x1-t, y1, z2, r, g, b, a)
            addQuad(buffer, m, x1, y1-t, z1, x1, y1+t, z1, x1, y1+t, z2, x1, y1-t, z2, r, g, b, a)
        }
    }

    fun box(stack: PoseStack, collector: SubmitNodeCollector, box: AABB, color: Int, filled: Boolean = true) {
        val cam = mc.gameRenderer.mainCamera().position()
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val a = ((color shr 24) and 0xFF) / 255f
        val x1 = (box.minX - cam.x).toFloat()
        val y1 = (box.minY - cam.y).toFloat()
        val z1 = (box.minZ - cam.z).toFloat()
        val x2 = (box.maxX - cam.x).toFloat()
        val y2 = (box.maxY - cam.y).toFloat()
        val z2 = (box.maxZ - cam.z).toFloat()
        val renderType = RenderTypes.lightning()

        stack.pushPose()
        collector.submitCustomGeometry(stack, renderType) { pose, buffer ->
            val m = pose.pose()
            if (filled) {
                addQuad(buffer, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a)
                addQuad(buffer, m, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, b, a)
                addQuad(buffer, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, b, a)
                addQuad(buffer, m, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a)
                addQuad(buffer, m, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, b, a)
                addQuad(buffer, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a)
            } else {
                val t = 0.01f
                drawThickLine(buffer, m, x1, y1, z1, x2, y1, z1, r, g, b, a, t)
                drawThickLine(buffer, m, x2, y1, z1, x2, y1, z2, r, g, b, a, t)
                drawThickLine(buffer, m, x2, y1, z2, x1, y1, z2, r, g, b, a, t)
                drawThickLine(buffer, m, x1, y1, z2, x1, y1, z1, r, g, b, a, t)
                drawThickLine(buffer, m, x1, y2, z1, x2, y2, z1, r, g, b, a, t)
                drawThickLine(buffer, m, x2, y2, z1, x2, y2, z2, r, g, b, a, t)
                drawThickLine(buffer, m, x2, y2, z2, x1, y2, z2, r, g, b, a, t)
                drawThickLine(buffer, m, x1, y2, z2, x1, y2, z1, r, g, b, a, t)
                drawThickLine(buffer, m, x1, y1, z1, x1, y2, z1, r, g, b, a, t)
                drawThickLine(buffer, m, x2, y1, z1, x2, y2, z1, r, g, b, a, t)
                drawThickLine(buffer, m, x2, y1, z2, x2, y2, z2, r, g, b, a, t)
                drawThickLine(buffer, m, x1, y1, z2, x1, y2, z2, r, g, b, a, t)
            }
        }
        stack.popPose()
    }

    fun block(stack: PoseStack, collector: SubmitNodeCollector, pos: BlockPos, color: Int) {
        box(stack, collector, AABB(pos), color)
    }

    fun boxWorldCoords(stack: PoseStack, collector: SubmitNodeCollector, box: AABB, color: Int, filled: Boolean = true) {
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val a = ((color shr 24) and 0xFF) / 255f
        val renderType = RenderTypes.lightning()

        stack.pushPose()
        collector.submitCustomGeometry(stack, renderType) { pose, buffer ->
            val m = pose.pose()
            val x1 = box.minX.toFloat()
            val y1 = box.minY.toFloat()
            val z1 = box.minZ.toFloat()
            val x2 = box.maxX.toFloat()
            val y2 = box.maxY.toFloat()
            val z2 = box.maxZ.toFloat()
            if (filled) {
                addQuad(buffer, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a)
                addQuad(buffer, m, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, b, a)
                addQuad(buffer, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, b, a)
                addQuad(buffer, m, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a)
                addQuad(buffer, m, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, b, a)
                addQuad(buffer, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a)
            } else {
                val t = 0.01f
                drawThickLine(buffer, m, x1, y1, z1, x2, y1, z1, r, g, b, a, t)
                drawThickLine(buffer, m, x2, y1, z1, x2, y1, z2, r, g, b, a, t)
                drawThickLine(buffer, m, x2, y1, z2, x1, y1, z2, r, g, b, a, t)
                drawThickLine(buffer, m, x1, y1, z2, x1, y1, z1, r, g, b, a, t)
                drawThickLine(buffer, m, x1, y2, z1, x2, y2, z1, r, g, b, a, t)
                drawThickLine(buffer, m, x2, y2, z1, x2, y2, z2, r, g, b, a, t)
                drawThickLine(buffer, m, x2, y2, z2, x1, y2, z2, r, g, b, a, t)
                drawThickLine(buffer, m, x1, y2, z2, x1, y2, z1, r, g, b, a, t)
                drawThickLine(buffer, m, x1, y1, z1, x1, y2, z1, r, g, b, a, t)
                drawThickLine(buffer, m, x2, y1, z1, x2, y2, z1, r, g, b, a, t)
                drawThickLine(buffer, m, x2, y1, z2, x2, y2, z2, r, g, b, a, t)
                drawThickLine(buffer, m, x1, y1, z2, x1, y2, z2, r, g, b, a, t)
            }
        }
        stack.popPose()
    }

    fun line(stack: PoseStack, collector: SubmitNodeCollector, from: Vec3, to: Vec3, color: Int) {
        val cam = mc.gameRenderer.mainCamera().position()
        stack.pushPose()
        stack.translate(-cam.x, -cam.y, -cam.z)
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val a = ((color shr 24) and 0xFF) / 255f

        collector.submitCustomGeometry(stack, RenderTypes.lightning()) { pose, buffer ->
            val m = pose.pose()
            drawThickLine(buffer, m, from.x.toFloat(), from.y.toFloat(), from.z.toFloat(), to.x.toFloat(), to.y.toFloat(), to.z.toFloat(), r, g, b, a, 0.01f)
        }
        stack.popPose()
    }
}
