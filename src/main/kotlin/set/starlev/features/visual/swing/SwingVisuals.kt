package set.starlev.features.visual.swing

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import kotlin.math.sin
import kotlin.math.sqrt
import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SwingAnimationType
import set.starlev.StarredHeltix

// ---- Скорость анимации удара ----

object SwingDuration {
    @JvmStatic
    fun duration(entity: LivingEntity, vanillaDuration: Int): Int {
        val minecraft = Minecraft.getInstance()
        if (entity !== minecraft.player) return vanillaDuration

        val hand = entity.swingingArm ?: InteractionHand.MAIN_HAND
        val itemStack = entity.getItemInHand(hand)
        if (!SwingCustomization.isEligible(itemStack)) return vanillaDuration

        val config = StarredHeltix.feature.visuals.animations.swingAnimation
        if (!config.enabled) return vanillaDuration

        val transform = SwingTransforms.effectiveTransform(itemStack)
        return adjustedDuration(
            vanillaDuration = vanillaDuration,
            baseDuration = itemStack.swingAnimation.duration(),
            speed = transform.swingSpeed,
            ignoresMiningEffects = config.ignoresMiningEffects,
        )
    }

    fun adjustedDuration(
        vanillaDuration: Int,
        baseDuration: Int,
        speed: Float,
        ignoresMiningEffects: Boolean,
    ): Int {
        require(speed > 0f)
        if (speed == 1f && !ignoresMiningEffects) return vanillaDuration
        val duration = if (ignoresMiningEffects) baseDuration else vanillaDuration
        return (duration / speed).toInt().coerceAtLeast(MIN_VISIBLE_DURATION)
    }

    private const val MIN_VISIBLE_DURATION = 2
}

// ---- Превью удара ----

object SwingPreview {
    fun play() {
        val player = Minecraft.getInstance().player ?: return
        player.swing(InteractionHand.MAIN_HAND, false)
    }
}

// ---- Стиль удара "только предмет" ----

object SwingVisuals {
    private var itemOnlySwing: ItemOnlySwing? = null

    @JvmStatic
    fun begin(itemStack: ItemStack, attack: Float, arm: HumanoidArm) {
        if (!SwingCustomization.isEligible(itemStack)) {
            itemOnlySwing = null
            return
        }
        val config = StarredHeltix.feature.visuals.animations.swingAnimation
        val transform = SwingTransforms.effectiveTransform(itemStack)
        itemOnlySwing = ItemOnlySwing(itemStack, attack, arm).takeIf {
            config.enabled &&
                transform.swingStyle == SwingStyle.ITEM_ONLY &&
                itemStack.swingAnimation.type() == SwingAnimationType.WHACK
        }
    }

    @JvmStatic
    fun replaceVanillaSwing(): SwingReplacementResult {
        val swing = itemOnlySwing ?: return SwingReplacementResult.UNCHANGED
        swing.isVanillaSwingReplaced = true
        return SwingReplacementResult.REPLACED
    }

    @JvmStatic
    fun apply(itemStack: ItemStack, poseStack: PoseStack) {
        val swing = itemOnlySwing?.takeIf {
            it.itemStack === itemStack && it.isVanillaSwingReplaced
        } ?: return
        applyItemOnlySwing(poseStack, swing.attack, swing.arm)
    }

    @JvmStatic
    fun end() {
        itemOnlySwing = null
    }

    fun applyItemOnlySwing(poseStack: PoseStack, attack: Float, arm: HumanoidArm) {
        if (attack <= 0f) return
        val direction = if (arm == HumanoidArm.RIGHT) 1f else -1f
        val arc = sin(sqrt(attack.toDouble()) * Math.PI).toFloat()
        val twist = sin(attack * attack * Math.PI).toFloat()
        poseStack.mulPose(Axis.YP.rotationDegrees(direction * twist * SWING_Y_DEGREES))
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * arc * SWING_Z_DEGREES))
        poseStack.mulPose(Axis.XP.rotationDegrees(arc * SWING_X_DEGREES))
    }

    private data class ItemOnlySwing(
        val itemStack: ItemStack,
        val attack: Float,
        val arm: HumanoidArm,
        var isVanillaSwingReplaced: Boolean = false,
    )

    private const val SWING_X_DEGREES = -80f
    private const val SWING_Y_DEGREES = -20f
    private const val SWING_Z_DEGREES = -20f
}

enum class SwingReplacementResult {
    REPLACED,
    UNCHANGED,
}