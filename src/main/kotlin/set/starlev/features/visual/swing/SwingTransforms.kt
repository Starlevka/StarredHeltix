package set.starlev.features.visual.swing

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import kotlin.math.tan
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.joml.Matrix4f
import org.joml.Matrix4fc
import set.starlev.StarredHeltix
import set.starlev.registry.ItemRegistry

// ---- Право на кастомизацию ----

object SwingCustomization {
    @JvmStatic
    fun isEligible(itemStack: ItemStack): Boolean = isEligible(itemStack.item)

    fun isEligible(item: Item): Boolean = item != Items.MAP && item != Items.FILLED_MAP
}

// ---- Применение трансформации предмета ----

object SwingTransforms {
    @JvmStatic
    fun apply(itemStack: ItemStack, poseStack: PoseStack) {
        if (!SwingCustomization.isEligible(itemStack)) return
        val config = StarredHeltix.feature.visuals.animations.swingAnimation
        if (!config.enabled) return
        val transform = effectiveTransform(itemStack)
        if (!transform.hasRenderChanges()) return

        applyPosition(poseStack, transform, RenderSystem.getModelViewStack())
        applyRotation(poseStack, transform)
        if (transform.scale != 1f) poseStack.scale(transform.scale, transform.scale, transform.scale)
    }

    fun currentItem(): ItemStack {
        val player = Minecraft.getInstance().player ?: return ItemStack.EMPTY
        return player.mainHandItem.takeUnless(ItemStack::isEmpty) ?: player.offhandItem
    }

    fun itemId(itemStack: ItemStack): String? {
        if (itemStack.isEmpty) return null
        ItemRegistry.getSkyblockId(itemStack)?.let { return it }
        // Фолбэк для ванильных предметов без skyblock id — используем registry id
        return try {
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(itemStack.item).toString()
        } catch (_: Exception) { null }
    }

    fun effectiveTransform(itemId: String?): SwingTransformConfig =
        StarredHeltix.feature.visuals.animations.swingAnimation.transformFor(itemId)

    fun effectiveTransform(itemStack: ItemStack): SwingTransformConfig {
        SwingEditorScreen.previewTransform(itemStack)?.let { return it }
        val config = StarredHeltix.feature.visuals.animations.swingAnimation
        return if (config.itemTransforms.isEmpty()) config.global else config.transformFor(itemId(itemStack))
    }

    fun applyPosition(
        poseStack: PoseStack,
        transform: SwingTransformConfig,
        viewTransform: Matrix4fc,
    ) {
        val referenceDepth = SwingPositionMath.referenceDepth(transform.z)
        val screenTransform = Matrix4f()
            .m20(-transform.x / referenceDepth)
            .m21(-transform.y / referenceDepth)
            .translate(0f, 0f, transform.z)
        val cameraSpaceTransform = Matrix4f(viewTransform)
            .invert()
            .mul(screenTransform)
            .mul(viewTransform)
        poseStack.last().pose().mulLocal(cameraSpaceTransform)
    }

    fun applyRotation(poseStack: PoseStack, transform: SwingTransformConfig) {
        if (transform.rotationX != 0f) poseStack.mulPose(Axis.XP.rotationDegrees(transform.rotationX))
        if (transform.rotationY != 0f) poseStack.mulPose(Axis.YP.rotationDegrees(transform.rotationY))
        if (transform.rotationZ != 0f) poseStack.mulPose(Axis.ZP.rotationDegrees(transform.rotationZ))
    }
}

object SwingPositionMath {
    fun referenceDepth(depthOffset: Float): Float =
        (DEFAULT_ITEM_DEPTH - depthOffset).coerceAtLeast(MIN_ITEM_DEPTH)

    fun unitsPerPixel(guiHeight: Int, depthOffset: Float): Float {
        require(guiHeight > 0)
        return (2.0 * referenceDepth(depthOffset) * tan(HALF_HUD_FOV_RADIANS) / guiHeight).toFloat()
    }

    private const val DEFAULT_ITEM_DEPTH = 0.72f
    private const val MIN_ITEM_DEPTH = 0.1f
    private val HALF_HUD_FOV_RADIANS = Math.toRadians(Camera.BASE_HUD_FOV.toDouble() / 2.0)
}