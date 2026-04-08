package set.starlev.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Camera
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.state.CameraRenderState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import set.starlev.utils.IWorldRenderer
import org.joml.Matrix4f
import net.minecraft.resources.ResourceLocation
import com.mojang.math.Axis
import net.minecraft.world.item.ItemStack
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.monster.MagmaCube
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.state.EntityRenderState

data class RenderContext(
    val matrices: PoseStack,
    val camera: Camera,
    val tickDelta: Float,
    val vertexConsumers: MultiBufferSource,
    val cameraRenderState: CameraRenderState? = null
) {
    private fun isVisible(aabb: AABB): Boolean {
        val mc = net.minecraft.client.Minecraft.getInstance()
        val worldRenderer = mc.levelRenderer as? IWorldRenderer ?: return true
        val frustum = worldRenderer.`starredheltix$getFrustum`() ?: return true
        return frustum.isVisible(aabb)
    }

    fun renderBox(aabb: AABB, r: Float, g: Float, b: Float, a: Float, fill: Boolean = false, thickness: Float = 0f) {
        renderBoxInternal(aabb, r, g, b, a, fill, thickness, false)
    }
    
    fun renderBoxThroughBlocks(aabb: AABB, r: Float, g: Float, b: Float, a: Float, fill: Boolean = false, thickness: Float = 0f) {
        renderBoxInternal(aabb, r, g, b, a, fill, thickness, true)
    }

    private fun renderBoxInternal(aabb: AABB, r: Float, g: Float, b: Float, a: Float, fill: Boolean, thickness: Float, skipVisibility: Boolean = false) {
        val pos = camera.position
        matrices.pushPose()
        matrices.translate(aabb.minX - pos.x, aabb.minY - pos.y, aabb.minZ - pos.z)
        
        val box = aabb.move(-aabb.minX, -aabb.minY, -aabb.minZ)
        
        val renderType = if (skipVisibility) net.minecraft.client.renderer.RenderType.lightning() else net.minecraft.client.renderer.RenderType.debugQuads()
        val buffer = vertexConsumers.getBuffer(renderType)
        val m = matrices.last().pose()
        
        if (fill) {
            drawBoxFilled(buffer, m, box, r, g, b, a)
        } else {
            val finalThickness = if (thickness > 0) {
                thickness * 0.005f
            } else {
                val dx = (aabb.minX + 0.5 - pos.x).toFloat()
                val dy = (aabb.minY + 0.5 - pos.y).toFloat()
                val dz = (aabb.minZ + 0.5 - pos.z).toFloat()
                val dist = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
                (0.01f + (dist / 96f).coerceIn(0f, 1.5f) * 0.04f).coerceIn(0.01f, 0.06f)
            }
            drawBoxLinesAsQuads(buffer, m, box, r, g, b, a, finalThickness)
        }
        
        matrices.popPose()
        
        if (skipVisibility && vertexConsumers is net.minecraft.client.renderer.MultiBufferSource.BufferSource) {
            vertexConsumers.endBatch(renderType)
        }
    }

    private fun drawBoxFilled(buffer: VertexConsumer, m: Matrix4f, box: AABB, r: Float, g: Float, b: Float, a: Float) {
        val x1 = box.minX.toFloat(); val y1 = box.minY.toFloat(); val z1 = box.minZ.toFloat()
        val x2 = box.maxX.toFloat(); val y2 = box.maxY.toFloat(); val z2 = box.maxZ.toFloat()
        addQuad(buffer, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a)
        addQuad(buffer, m, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, b, a)
        addQuad(buffer, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, b, a)
        addQuad(buffer, m, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a)
        addQuad(buffer, m, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, b, a)
        addQuad(buffer, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a)
    }

    private fun drawBoxLinesAsQuads(buffer: VertexConsumer, m: Matrix4f, box: AABB, r: Float, g: Float, b: Float, a: Float, t: Float) {
        val x1 = box.minX.toFloat(); val y1 = box.minY.toFloat(); val z1 = box.minZ.toFloat()
        val x2 = box.maxX.toFloat(); val y2 = box.maxY.toFloat(); val z2 = box.maxZ.toFloat()
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
        drawThickLine(buffer, m, x1, y1, z2, x1, y2, z2, r, g, b, a, t)
        drawThickLine(buffer, m, x2, y1, z2, x2, y2, z2, r, g, b, a, t)
    }

    private fun drawThickLine(buffer: VertexConsumer, m: Matrix4f, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, r: Float, g: Float, b: Float, a: Float, t: Float) {
        val dx = x2 - x1; val dy = y2 - y1; val dz = z2 - z1
        if (kotlin.math.abs(dx) > 0.001f) {
            addQuad(buffer, m, x1, y1-t, z1, x2, y1-t, z1, x2, y1+t, z1, x1, y1+t, z1, r, g, b, a)
            addQuad(buffer, m, x1, y1, z1-t, x2, y1, z1-t, x2, y1, z1+t, x1, y1, z1+t, r, g, b, a)
        } else if (kotlin.math.abs(dy) > 0.001f) {
            addQuad(buffer, m, x1-t, y1, z1, x1+t, y1, z1, x1+t, y2, z1, x1-t, y2, z1, r, g, b, a)
            addQuad(buffer, m, x1, y1, z1-t, x1, y1, z1+t, x1, y2, z1+t, x1, y2, z1-t, r, g, b, a)
        } else {
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

    private fun addLine(buffer: VertexConsumer, matrix: Matrix4f, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, r: Float, g: Float, b: Float, a: Float) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setNormal(0f, 1f, 0f)
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setNormal(0f, 1f, 0f)
    }

    fun renderItem(itemStack: ItemStack, x: Double, y: Double, z: Double, scale: Float = 1f, rotation: Float = 0f) {
        val mc = net.minecraft.client.Minecraft.getInstance()
        val pos = camera.position
        
        matrices.pushPose()
        matrices.translate(x - pos.x, y - pos.y, z - pos.z)
        matrices.scale(scale, scale, scale)
        matrices.mulPose(Axis.YP.rotationDegrees(rotation))
        
        // В 1.21.10 используем ItemStackRenderState и ItemModelResolver
        val tracking = ItemStackRenderState()
        mc.itemModelResolver.updateForTopItem(
            tracking, 
            itemStack, 
            ItemDisplayContext.FIXED, 
            mc.level, 
            mc.player, 
            0
        )
        
        // Пытаемся вызвать render через рефлексию, так как имя может отличаться в разных маппингах
        try {
            val renderMethod = tracking.javaClass.methods.find { it.parameterCount >= 4 && (it.name == "render" || it.name.startsWith("method_")) }
            if (renderMethod != null) {
                if (renderMethod.parameterCount == 4) {
                    renderMethod.invoke(tracking, matrices, vertexConsumers, 15728880, OverlayTexture.NO_OVERLAY)
                } else if (renderMethod.parameterCount == 5) {
                    renderMethod.invoke(tracking, matrices, vertexConsumers, 15728880, OverlayTexture.NO_OVERLAY, 0)
                }
            }
        } catch (e: Exception) {
            // Фолбэк если не удалось
        }
        
        matrices.popPose()
    }

    fun renderLine(start: Vec3, end: Vec3, color: Int, thickness: Float = 1f) {
        matrices.pushPose()
        val pos = camera.position
        matrices.translate(-pos.x, -pos.y, -pos.z)
        
        // Используем lightning для совместимости с шейдерами
        val renderType = net.minecraft.client.renderer.RenderType.lightning()
        val buf = vertexConsumers.getBuffer(renderType)
        val m = matrices.last().pose()
        
        val r = (color shr 16 and 0xFF) / 255f
        val g = (color shr 8 and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val a = (color shr 24 and 0xFF) / 255f
        
        val t = 0.01f * thickness // Толщина
        
        // Рисуем линию как вытянутый бокс для объема
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
        
        addQuad(buf, m, p1.x.toFloat(), p1.y.toFloat(), p1.z.toFloat(), 
                       p2.x.toFloat(), p2.y.toFloat(), p2.z.toFloat(), 
                       p3.x.toFloat(), p3.y.toFloat(), p3.z.toFloat(), 
                       p4.x.toFloat(), p4.y.toFloat(), p4.z.toFloat(), r, g, b, a)
        
        addQuad(buf, m, p5.x.toFloat(), p5.y.toFloat(), p5.z.toFloat(), 
                       p6.x.toFloat(), p6.y.toFloat(), p6.z.toFloat(), 
                       p7.x.toFloat(), p7.y.toFloat(), p7.z.toFloat(), 
                       p8.x.toFloat(), p8.y.toFloat(), p8.z.toFloat(), r, g, b, a)
        
        val p1_2 = start.add(side).add(perpendicular)
        val p4_2 = end.add(side).add(perpendicular)
        val p8_2 = end.add(side).subtract(perpendicular)
        val p5_2 = start.add(side).subtract(perpendicular)
        
        addQuad(buf, m, p1_2.x.toFloat(), p1_2.y.toFloat(), p1_2.z.toFloat(), 
                       p4_2.x.toFloat(), p4_2.y.toFloat(), p4_2.z.toFloat(), 
                       p8_2.x.toFloat(), p8_2.y.toFloat(), p8_2.z.toFloat(), 
                       p5_2.x.toFloat(), p5_2.y.toFloat(), p5_2.z.toFloat(), r, g, b, a)
        
        val p2_2 = start.subtract(side).add(perpendicular)
        val p3_2 = end.subtract(side).add(perpendicular)
        val p7_2 = end.subtract(side).subtract(perpendicular)
        val p6_2 = start.subtract(side).subtract(perpendicular)
        
        addQuad(buf, m, p2_2.x.toFloat(), p2_2.y.toFloat(), p2_2.z.toFloat(), 
                       p3_2.x.toFloat(), p3_2.y.toFloat(), p3_2.z.toFloat(), 
                       p7_2.x.toFloat(), p7_2.y.toFloat(), p7_2.z.toFloat(), 
                       p6_2.x.toFloat(), p6_2.y.toFloat(), p6_2.z.toFloat(), r, g, b, a)
        
        matrices.popPose()
        
        // Принудительно пушим буфер слоев, чтобы линия сразу отрендерилась
        if (vertexConsumers is net.minecraft.client.renderer.MultiBufferSource.BufferSource) {
            vertexConsumers.endBatch(renderType)
        }
    }

    fun renderImage(texture: ResourceLocation, x: Double, y: Double, z: Double, width: Float, height: Float, rotationY: Float = 0f) {
        renderAnimatedImage(texture, x, y, z, width, height, rotationY, 1, 1, 0f)
    }

    fun renderAnimatedImage(
        texture: ResourceLocation,
        x: Double,
        y: Double,
        z: Double,
        width: Float,
        height: Float,
        rotationY: Float = 0f,
        columns: Int = 1,
        rows: Int = 1,
        fps: Float = 20f
    ) {
        val pos = camera.position
        
        matrices.pushPose()
        matrices.translate(x - pos.x, y - pos.y, z - pos.z)
        matrices.mulPose(Axis.YP.rotationDegrees(rotationY))
        
        val buffer = vertexConsumers.getBuffer(RenderType.text(texture))
        val entry = matrices.last()
        val matrix = entry.pose()
        
        val light = 15728880 // Max light
 
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
        
        buffer.addVertex(matrix, 0f, height, 0f).setColor(1f, 1f, 1f, 1f).setUv(uMin, vMax).setLight(light)
        buffer.addVertex(matrix, width, height, 0f).setColor(1f, 1f, 1f, 1f).setUv(uMax, vMax).setLight(light)
        buffer.addVertex(matrix, width, 0f, 0f).setColor(1f, 1f, 1f, 1f).setUv(uMax, vMin).setLight(light)
        buffer.addVertex(matrix, 0f, 0f, 0f).setColor(1f, 1f, 1f, 1f).setUv(uMin, vMin).setLight(light)
        
        matrices.popPose()
    }

    @Suppress("UNCHECKED_CAST")
    fun renderEntity(entity: Entity, x: Double, y: Double, z: Double, yaw: Float, tickDelta: Float, alpha: Float = 1.0f) {
        val mc = net.minecraft.client.Minecraft.getInstance()
        val pos = camera.position
        
        matrices.pushPose()
        matrices.translate(x - pos.x, y - pos.y, z - pos.z)
        
        if (entity is MagmaCube) {
            val size = entity.size.toFloat()
            matrices.scale(size, size, size)
        }

        val dispatcher = mc.entityRenderDispatcher
        try {
            val renderMethod = dispatcher.javaClass.methods.find { 
                it.parameterCount == 9 && 
                it.parameterTypes[0].isAssignableFrom(entity.javaClass) &&
                it.parameterTypes[1] == Double::class.javaPrimitiveType
            }
            
            if (renderMethod != null) {
                renderMethod.invoke(
                    dispatcher, 
                    entity, 
                    0.0, 0.0, 0.0, 
                    yaw, 
                    tickDelta, 
                    matrices, 
                    vertexConsumers, 
                    15728880
                )
            } else {
                val renderer = dispatcher.getRenderer(entity) as EntityRenderer<Entity, EntityRenderState>
                val state = renderer.createRenderState()
                renderer.extractRenderState(entity, state, tickDelta)
                val entityRendererMethod = renderer.javaClass.methods.find { it.name == "render" && it.parameterCount == 4 }
                entityRendererMethod?.invoke(renderer, state, matrices, vertexConsumers, 15728880)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
         
         matrices.popPose()
    }

    fun renderText(text: String, x: Double, y: Double, z: Double, scale: Float = 1f, color: Int = 0xFFFFFFFF.toInt(), shadow: Boolean = true, seeThrough: Boolean = false) {
       val mc = net.minecraft.client.Minecraft.getInstance()
       val font = mc.font
       val cameraPos = camera.position
       matrices.pushPose()
       matrices.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z)
       // Биллбординг: поворачиваем к камере через отдельные оси (надёжнее чем camera.rotation())
       matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-camera.yRot))
       matrices.mulPose(com.mojang.math.Axis.XP.rotationDegrees(camera.xRot))
       val baseScale = -0.05f * scale
       matrices.scale(baseScale, baseScale, 1.0f)
       val matrix = matrices.last().pose()
       val width = font.width(text).toFloat()
       val xOffset = -width / 2
       if (seeThrough) {
             val bgOpacity = (0.4f * 255).toInt() shl 24
             font.drawInBatch(text, xOffset, 0f, color, shadow, matrix, vertexConsumers, net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, bgOpacity, 15728880)
       } else {
             font.drawInBatch(text, xOffset, 0f, color, shadow, matrix, vertexConsumers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 15728880)
       }
       if (vertexConsumers is net.minecraft.client.renderer.MultiBufferSource.BufferSource) {
             vertexConsumers.endBatch()
       }
       matrices.popPose()
    }

    fun renderBeaconBeam(x: Double, y: Double, z: Double, height: Int, color: Int, tickDelta: Float) {
        val pos = camera.position
        matrices.pushPose()
        matrices.translate(x - pos.x, y - pos.y, z - pos.z)
        val buffer = vertexConsumers.getBuffer(RenderType.lightning())
        val r = (color shr 16 and 0xFF) / 255f
        val g = (color shr 8 and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val a = 0.5f
        val m = matrices.last().pose()
        val size = 0.3f
        addQuad(buffer, m, -size, 0f, -size, size, 0f, -size, size, 0f, size, -size, 0f, size, r, g, b, a)
        addQuad(buffer, m, -size, 0f, -size, -size, height.toFloat(), -size, size, height.toFloat(), -size, size, 0f, -size, r, g, b, a)
        addQuad(buffer, m, size, 0f, -size, size, height.toFloat(), -size, size, height.toFloat(), size, size, 0f, size, r, g, b, a)
        addQuad(buffer, m, size, 0f, size, size, height.toFloat(), size, -size, height.toFloat(), size, -size, 0f, size, r, g, b, a)
        addQuad(buffer, m, -size, 0f, size, -size, height.toFloat(), size, size, height.toFloat(), size, size, 0f, size, r, g, b, a)
        if (vertexConsumers is net.minecraft.client.renderer.MultiBufferSource.BufferSource) {
            vertexConsumers.endBatch(RenderType.lightning())
        }
        matrices.popPose()
    }
}
