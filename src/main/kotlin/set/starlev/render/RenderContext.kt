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

    fun renderBox(aabb: AABB, r: Float, g: Float, b: Float, a: Float) {
        renderBox(aabb, r, g, b, a, false)
    }

    fun renderBox(aabb: AABB, r: Float, g: Float, b: Float, a: Float, fill: Boolean, thickness: Float = 0f) {
        val pos = camera.position
        
        matrices.pushPose()
        matrices.translate(aabb.minX - pos.x, aabb.minY - pos.y, aabb.minZ - pos.z)
        
        val box = aabb.move(-aabb.minX, -aabb.minY, -aabb.minZ)
        
        val renderType = net.minecraft.client.renderer.RenderType.debugQuads()
        
        val buffer = vertexConsumers.getBuffer(renderType)
        val m = matrices.last().pose()
        
        if (fill) {
            drawBoxFilled(buffer, m, box, r, g, b, a)
        } else {
            // Рисуем обводку через тонкие кватдраты
            // Это гарантирует видимость и корректный цвет (RenderType.lines имеет ограничения)
            val finalThickness = if (thickness > 0) {
                thickness * 0.005f // Масштабируем пользовательское значение (1-10) в реальные размеры
            } else {
                val centerX = (aabb.minX + aabb.maxX) * 0.5
                val centerY = (aabb.minY + aabb.maxY) * 0.5
                val centerZ = (aabb.minZ + aabb.maxZ) * 0.5
                val dx = (centerX - pos.x).toFloat()
                val dy = (centerY - pos.y).toFloat()
                val dz = (centerZ - pos.z).toFloat()
                val dist = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
                (0.01f + (dist / 96f).coerceIn(0f, 1.5f) * 0.04f).coerceIn(0.01f, 0.06f)
            }
            drawBoxLinesAsQuads(buffer, m, box, r, g, b, a, finalThickness)
        }
        
        matrices.popPose()
    }

    private fun drawBoxFilled(buffer: com.mojang.blaze3d.vertex.VertexConsumer, m: org.joml.Matrix4f, box: AABB, r: Float, g: Float, b: Float, a: Float) {
        val x1 = box.minX.toFloat()
        val y1 = box.minY.toFloat()
        val z1 = box.minZ.toFloat()
        val x2 = box.maxX.toFloat()
        val y2 = box.maxY.toFloat()
        val z2 = box.maxZ.toFloat()

        // Нижняя
        addQuad(buffer, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a)
        // Верхняя
        addQuad(buffer, m, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, b, a)
        // Северная
        addQuad(buffer, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, b, a)
        // Южная
        addQuad(buffer, m, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a)
        // Западная
        addQuad(buffer, m, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, b, a)
        // Восточная
        addQuad(buffer, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a)
    }

    private fun drawBoxLinesAsQuads(buffer: com.mojang.blaze3d.vertex.VertexConsumer, m: org.joml.Matrix4f, box: AABB, r: Float, g: Float, b: Float, a: Float, thickness: Float) {
        val x1 = box.minX.toFloat()
        val y1 = box.minY.toFloat()
        val z1 = box.minZ.toFloat()
        val x2 = box.maxX.toFloat()
        val y2 = box.maxY.toFloat()
        val z2 = box.maxZ.toFloat()
        
        val t = thickness
        
        // Рисуем каждую линию как два перпендикулярных кватадрата для видимости со всех сторон
        
        // Горизонтальные по X (нижние)
        drawThickLine(buffer, m, x1, y1, z1, x2, y1, z1, r, g, b, a, t)
        drawThickLine(buffer, m, x1, y1, z2, x2, y1, z2, r, g, b, a, t)
        // Горизонтальные по X (верхние)
        drawThickLine(buffer, m, x1, y2, z1, x2, y2, z1, r, g, b, a, t)
        drawThickLine(buffer, m, x1, y2, z2, x2, y2, z2, r, g, b, a, t)
        
        // Горизонтальные по Z (нижние)
        drawThickLine(buffer, m, x1, y1, z1, x1, y1, z2, r, g, b, a, t)
        drawThickLine(buffer, m, x2, y1, z1, x2, y1, z2, r, g, b, a, t)
        // Горизонтальные по Z (верхние)
        drawThickLine(buffer, m, x1, y2, z1, x1, y2, z2, r, g, b, a, t)
        drawThickLine(buffer, m, x2, y2, z1, x2, y2, z2, r, g, b, a, t)
        
        // Вертикальные по Y
        drawThickLine(buffer, m, x1, y1, z1, x1, y2, z1, r, g, b, a, t)
        drawThickLine(buffer, m, x2, y1, z1, x2, y2, z1, r, g, b, a, t)
        drawThickLine(buffer, m, x1, y1, z2, x1, y2, z2, r, g, b, a, t)
        drawThickLine(buffer, m, x2, y1, z2, x2, y2, z2, r, g, b, a, t)
    }

    private fun drawThickLine(buffer: com.mojang.blaze3d.vertex.VertexConsumer, m: org.joml.Matrix4f, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, r: Float, g: Float, b: Float, a: Float, t: Float) {
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

    private fun drawBoxLines(buffer: com.mojang.blaze3d.vertex.VertexConsumer, m: org.joml.Matrix4f, box: AABB, r: Float, g: Float, b: Float, a: Float) {
        val x1 = box.minX.toFloat()
        val y1 = box.minY.toFloat()
        val z1 = box.minZ.toFloat()
        val x2 = box.maxX.toFloat()
        val y2 = box.maxY.toFloat()
        val z2 = box.maxZ.toFloat()

        // Линии с нормалью вверх для корректного освещения/видимости
        addLine(buffer, m, x1, y1, z1, x2, y1, z1, r, g, b, a)
        addLine(buffer, m, x2, y1, z1, x2, y1, z2, r, g, b, a)
        addLine(buffer, m, x2, y1, z2, x1, y1, z2, r, g, b, a)
        addLine(buffer, m, x1, y1, z2, x1, y1, z1, r, g, b, a)

        addLine(buffer, m, x1, y2, z1, x2, y2, z1, r, g, b, a)
        addLine(buffer, m, x2, y2, z1, x2, y2, z2, r, g, b, a)
        addLine(buffer, m, x2, y2, z2, x1, y2, z2, r, g, b, a)
        addLine(buffer, m, x1, y2, z2, x1, y2, z1, r, g, b, a)

        addLine(buffer, m, x1, y1, z1, x1, y2, z1, r, g, b, a)
        addLine(buffer, m, x2, y1, z1, x2, y2, z1, r, g, b, a)
        addLine(buffer, m, x2, y1, z2, x2, y2, z2, r, g, b, a)
        addLine(buffer, m, x1, y1, z2, x1, y2, z2, r, g, b, a)
    }

    private fun addQuad(buffer: com.mojang.blaze3d.vertex.VertexConsumer, m: org.joml.Matrix4f, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float, x4: Float, y4: Float, z4: Float, r: Float, g: Float, b: Float, a: Float) {
        buffer.addVertex(m, x1, y1, z1).setColor(r, g, b, a)
        buffer.addVertex(m, x2, y2, z2).setColor(r, g, b, a)
        buffer.addVertex(m, x3, y3, z3).setColor(r, g, b, a)
        buffer.addVertex(m, x4, y4, z4).setColor(r, g, b, a)
    }

    private fun addLine(buffer: com.mojang.blaze3d.vertex.VertexConsumer, matrix: org.joml.Matrix4f, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, r: Float, g: Float, b: Float, a: Float) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setNormal(0f, 1f, 0f)
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setNormal(0f, 1f, 0f)
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
        
        // Используем lightning для совместимости с шейдерами
        val buf = vertexConsumers.getBuffer(net.minecraft.client.renderer.RenderType.lightning())
        val m = matrices.last().pose()
        
        val r = (color shr 16 and 0xFF) / 255f
        val g = (color shr 8 and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val a = (color shr 24 and 0xFF) / 255f
        
        val t = 0.01f * thickness // Толщина
        
        // Рисуем линию как очень вытянутый бокс (квадрат)
        val dir = end.subtract(start).normalize()
        val up = if (Math.abs(dir.y) < 0.9) net.minecraft.world.phys.Vec3(0.0, 1.0, 0.0) else net.minecraft.world.phys.Vec3(1.0, 0.0, 0.0)
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
        
        // Рисуем два перпендикулярных кватдрата для видимости со всех сторон
        addQuad(buf, m, p1.x.toFloat(), p1.y.toFloat(), p1.z.toFloat(), 
                       p2.x.toFloat(), p2.y.toFloat(), p2.z.toFloat(), 
                       p3.x.toFloat(), p3.y.toFloat(), p3.z.toFloat(), 
                       p4.x.toFloat(), p4.y.toFloat(), p4.z.toFloat(), r, g, b, a)
        
        addQuad(buf, m, p5.x.toFloat(), p5.y.toFloat(), p5.z.toFloat(), 
                       p6.x.toFloat(), p6.y.toFloat(), p6.z.toFloat(), 
                       p7.x.toFloat(), p7.y.toFloat(), p7.z.toFloat(), 
                       p8.x.toFloat(), p8.y.toFloat(), p8.z.toFloat(), r, g, b, a)
        
        // Дополнительные плоскости для "объема"
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

    fun renderText(text: String, x: Double, y: Double, z: Double, color: Int = 0xFFFFFFFF.toInt(), scale: Float = 1f, shadow: Boolean = true, seeThrough: Boolean = true) {
        val mc = net.minecraft.client.Minecraft.getInstance()
        val font = mc.font
        val pos = camera.position
        
        matrices.pushPose()
        // Смещаем на позицию в мире относительно камеры
        matrices.translate(x - pos.x, y - pos.y, z - pos.z)
        
        // Биллбординг: поворачиваем к камере
        // Используем углы поворота камеры напрямую для надежности
        matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-camera.yRot))
        matrices.mulPose(com.mojang.math.Axis.XP.rotationDegrees(camera.xRot))
        
        // Масштабирование
        val baseScale = -0.05f * scale // Увеличим базовый масштаб в 2 раза
        matrices.scale(baseScale, baseScale, 1.0f)
        
        val matrix = matrices.last().pose()
        val width = font.width(text).toFloat()
        val xOffset = -width / 2
        
        // Рисуем фон для текста чтобы его было лучше видно
        if (seeThrough) {
            val bgOpacity = (0.4f * 255).toInt() shl 24
            font.drawInBatch(
                text,
                xOffset,
                0f,
                color,
                shadow,
                matrix,
                vertexConsumers,
                net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH,
                bgOpacity,
                15728880
            )
        } else {
            font.drawInBatch(
                text,
                xOffset,
                0f,
                color,
                shadow,
                matrix,
                vertexConsumers,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL,
                0,
                15728880
            )
        }
        
        // Форсируем отрисовку текста немедленно
        if (vertexConsumers is net.minecraft.client.renderer.MultiBufferSource.BufferSource) {
            vertexConsumers.endBatch()
        }
        
        matrices.popPose()
    }

    fun renderBeaconBeam(x: Double, y: Double, z: Double, height: Int, color: Int, tickDelta: Float) {
        val mc = net.minecraft.client.Minecraft.getInstance()
        val pos = camera.position
        
        matrices.pushPose()
        matrices.translate(x - pos.x, y - pos.y, z - pos.z)
        
        // В 1.21.10 для просвечивания сквозь блоки и совместимости с шейдерами лучше всего подходит lightning()
        val buffer = vertexConsumers.getBuffer(net.minecraft.client.renderer.RenderType.lightning())
        
        val r = (color shr 16 and 0xFF) / 255f
        val g = (color shr 8 and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val a = 0.5f
        
        val m = matrices.last().pose()
        
        // Рисуем простой луч (4 грани)
        val size = 0.3f
        
        // Нижняя грань
        addQuad(buffer, m, -size, 0f, -size, size, 0f, -size, size, 0f, size, -size, 0f, size, r, g, b, a)
        // Стенки
        addQuad(buffer, m, -size, 0f, -size, -size, height.toFloat(), -size, size, height.toFloat(), -size, size, 0f, -size, r, g, b, a)
        addQuad(buffer, m, size, 0f, -size, size, height.toFloat(), -size, size, height.toFloat(), size, size, 0f, size, r, g, b, a)
        addQuad(buffer, m, size, 0f, size, size, height.toFloat(), size, -size, height.toFloat(), size, -size, 0f, size, r, g, b, a)
        addQuad(buffer, m, -size, 0f, size, -size, height.toFloat(), size, size, height.toFloat(), size, size, 0f, size, r, g, b, a)
        
        // Форсируем отрисовку луча
        if (vertexConsumers is net.minecraft.client.renderer.MultiBufferSource.BufferSource) {
            vertexConsumers.endBatch(net.minecraft.client.renderer.RenderType.lightning())
        }
        
        matrices.popPose()
    }
}
