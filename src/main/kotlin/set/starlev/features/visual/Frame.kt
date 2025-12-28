package set.starlev.features.visual

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import set.starlev.render.RenderEvents

/**
 * Функция-прикол: отрисовывает картинку в мире на координатах 2 71 -92 в биоме Overworld.
 * Аналог CatPicture из Skyblocker.
 */
object GhostFrameFeature {
    private val mc = Minecraft.getInstance()
    private val FRAME_ITEM by lazy { ItemStack(Items.ITEM_FRAME) }
    
    // Первая рамка
    private val TARGET_POS_1 = BlockPos(2, 71, -93)
    private val IMAGE_TEXTURE_1 = ResourceLocation.fromNamespaceAndPath("starredheltix", "textures/image.png")
    private val OVERWORLD_DIM_ID = "minecraft:overworld"
    
    // Вторая рамка
    private val TARGET_POS_2 = BlockPos(7, 100, 29)
    private val IMAGE_TEXTURE_2 = ResourceLocation.fromNamespaceAndPath("starredheltix", "textures/image2.png")
    private val TARGET_DIM_ID = "minecraft:7cbb1bc2-5556-4ab5-b79e-4da777a9294c"

    fun init() {
        RenderEvents.register { context ->
            val level = mc.level ?: return@register
            val config = set.starlev.StarredHeltix.feature.misc.newYear
            
            val currentDimId = level.dimension().location().toString()
            val isOverworld = currentDimId == OVERWORLD_DIM_ID || 
                             currentDimId.contains("overworld") || 
                             currentDimId.contains("spawn") ||
                             mc.currentServer?.ip?.contains("heltix") == true

            // Отрисовка первой рамки
            if (config.secretFrame && isOverworld) {
                renderFrame(context, TARGET_POS_1, IMAGE_TEXTURE_1, 0f)
            }

            // Отрисовка второй рамки
            if (config.secretFrame2 && currentDimId == TARGET_DIM_ID) {
                renderFrame(context, TARGET_POS_2, IMAGE_TEXTURE_2, 180f)
            }
        }
    }

    private fun renderFrame(context: set.starlev.render.RenderContext, pos: BlockPos, texture: ResourceLocation, rotation: Float) {
        // Отрисовываем саму картинку
        val offsetZ = if (rotation == 180f) -0.001 else 0.001
        
        context.renderImage(
            texture,
            pos.x.toDouble(),
            pos.y + 1.0,
            pos.z + offsetZ,
            width = 1.0f,
            height = -1.0f,
            rotationY = rotation
        )
    }
}
