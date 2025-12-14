package set.starlev.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.client.renderer.debug.DebugRenderer
import net.minecraft.world.phys.AABB

data class RenderContext(val matrices: PoseStack, val camera: Camera, val tickDelta: Float, val vertexConsumers: MultiBufferSource) {

    fun renderBox(box: AABB, red: Float, green: Float, blue: Float, alpha: Float) {
        renderBox(box, red, green, blue, alpha, false)
    }

    fun renderBox(box: AABB, red: Float, green: Float, blue: Float, alpha: Float, filled: Boolean) {
        matrices.pushPose()
        val pos = camera.position
        matrices.translate(-pos.x, -pos.y, -pos.z)

        if (filled) {
            DebugRenderer.renderFilledBox(matrices, vertexConsumers, box, red, green, blue, alpha)
        } else {
            ShapeRenderer.renderLineBox(matrices.last(), vertexConsumers.getBuffer(net.minecraft.client.renderer.RenderType.lines()), box, red, green, blue, alpha)
        }
        matrices.popPose()
    }
}