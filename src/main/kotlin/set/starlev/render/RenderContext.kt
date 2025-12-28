package set.starlev.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.client.renderer.debug.DebugRenderer
import net.minecraft.client.renderer.state.CameraRenderState
import net.minecraft.world.phys.AABB

data class RenderContext(
    val matrices: PoseStack,
    val camera: Camera,
    val tickDelta: Float,
    val vertexConsumers: MultiBufferSource,
    val cameraRenderState: CameraRenderState? = null
) {

    fun renderBox(box: AABB, red: Float, green: Float, blue: Float, alpha: Float) {
        renderBox(box, red, green, blue, alpha, false)
    }

    fun renderBox(box: AABB, red: Float, green: Float, blue: Float, alpha: Float, filled: Boolean) {
        val pos = camera.position
        matrices.pushPose()
        matrices.translate(-pos.x, -pos.y, -pos.z)

        if (filled) {
            DebugRenderer.renderFilledBox(matrices, vertexConsumers, box, red, green, blue, alpha)
        } else {
            ShapeRenderer.renderLineBox(matrices.last(), vertexConsumers.getBuffer(net.minecraft.client.renderer.RenderType.lines()), box, red, green, blue, alpha)
        }
        matrices.popPose()
    }

    fun renderItem(itemStack: net.minecraft.world.item.ItemStack, x: Double, y: Double, z: Double, scale: Float = 1f, rotation: Float = 0f) {
        val mc = net.minecraft.client.Minecraft.getInstance()
        val pos = camera.position
        
        matrices.pushPose()
        matrices.translate(x - pos.x, y - pos.y, z - pos.z)
        matrices.scale(scale, scale, scale)
        matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation))
        
        // В 1.21.10 используем ItemStackRenderState и ItemModelResolver
        val tracking = net.minecraft.client.renderer.item.ItemStackRenderState()
        mc.itemModelResolver.updateForTopItem(
            tracking, 
            itemStack, 
            net.minecraft.world.item.ItemDisplayContext.FIXED, 
            mc.level, 
            mc.player, 
            0
        )
        
        // Пытаемся вызвать render через рефлексию, так как имя может отличаться в разных маппингах
        try {
            val renderMethod = tracking.javaClass.methods.find { it.parameterCount >= 4 && (it.name == "render" || it.name.startsWith("method_")) }
            if (renderMethod != null) {
                if (renderMethod.parameterCount == 4) {
                    renderMethod.invoke(tracking, matrices, vertexConsumers, 15728880, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                } else if (renderMethod.parameterCount == 5) {
                    renderMethod.invoke(tracking, matrices, vertexConsumers, 15728880, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0)
                }
            }
        } catch (e: Exception) {
            // Фолбэк если не удалось
        }
        
        matrices.popPose()
    }

    fun renderLine(start: net.minecraft.world.phys.Vec3, end: net.minecraft.world.phys.Vec3, color: Int, thickness: Float = 1f) {
        matrices.pushPose()
        val pos = camera.position
        matrices.translate(-pos.x, -pos.y, -pos.z)
        
        val buf = vertexConsumers.getBuffer(net.minecraft.client.renderer.RenderType.lines())
        val m = matrices.last().pose()
        
        buf.addVertex(m, start.x.toFloat(), start.y.toFloat(), start.z.toFloat()).setColor(color).setNormal(0f, 1f, 0f)
        buf.addVertex(m, end.x.toFloat(), end.y.toFloat(), end.z.toFloat()).setColor(color).setNormal(0f, 1f, 0f)
        
        matrices.popPose()
    }

    fun renderImage(texture: net.minecraft.resources.ResourceLocation, x: Double, y: Double, z: Double, width: Float, height: Float, rotationY: Float = 0f) {
        renderAnimatedImage(texture, x, y, z, width, height, rotationY, 1, 1, 0f)
    }

    fun renderAnimatedImage(
        texture: net.minecraft.resources.ResourceLocation,
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
        matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotationY))
        
        val buffer = vertexConsumers.getBuffer(net.minecraft.client.renderer.RenderType.text(texture))
        val entry = matrices.last()
        val matrix = entry.pose()
        
        val light = 15728880 // Max light

        // Вычисляем текущий кадр
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
        
        // Рисуем квадрат (2 треугольника) с учетом UV текущего кадра
        buffer.addVertex(matrix, 0f, height, 0f).setColor(0xFFFFFFFF.toInt()).setUv(uMin, vMax).setLight(light)
        buffer.addVertex(matrix, width, height, 0f).setColor(0xFFFFFFFF.toInt()).setUv(uMax, vMax).setLight(light)
        buffer.addVertex(matrix, width, 0f, 0f).setColor(0xFFFFFFFF.toInt()).setUv(uMax, vMin).setLight(light)
        buffer.addVertex(matrix, 0f, 0f, 0f).setColor(0xFFFFFFFF.toInt()).setUv(uMin, vMin).setLight(light)
        
        matrices.popPose()
    }

    @Suppress("UNCHECKED_CAST")
    fun renderEntity(entity: net.minecraft.world.entity.Entity, x: Double, y: Double, z: Double, yaw: Float, tickDelta: Float, alpha: Float = 1.0f) {
        val mc = net.minecraft.client.Minecraft.getInstance()
        val pos = camera.position
        
        matrices.pushPose()
        matrices.translate(x - pos.x, y - pos.y, z - pos.z)
        
        if (entity is net.minecraft.world.entity.monster.MagmaCube) {
            val size = entity.size.toFloat()
            matrices.scale(size, size, size)
        }

        val dispatcher = mc.entityRenderDispatcher
        try {
            // В 1.21.10 метод render часто имеет маппинг method_3954 или подобный
            // Используем рефлексию как самый надежный способ для разных окружений (dev/prod)
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
                // Если через рефлексию не нашли, пробуем через рендерер напрямую
                val renderer = dispatcher.getRenderer(entity) as net.minecraft.client.renderer.entity.EntityRenderer<net.minecraft.world.entity.Entity, net.minecraft.client.renderer.entity.state.EntityRenderState>
                val state = renderer.createRenderState()
                renderer.extractRenderState(entity, state, tickDelta)
                // В 1.21.10 у EntityRenderer есть метод render(S state, PoseStack poseStack, MultiBufferSource bufferSource, int light)
                val entityRendererMethod = renderer.javaClass.methods.find { it.name == "render" && it.parameterCount == 4 }
                entityRendererMethod?.invoke(renderer, state, matrices, vertexConsumers, 15728880)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
         
         matrices.popPose()
     }
}