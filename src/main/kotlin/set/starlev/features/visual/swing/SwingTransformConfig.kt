package set.starlev.features.visual.swing

import com.google.gson.annotations.Expose

// ---- Стиль анимации удара ----

enum class SwingStyle {
    VANILLA,
    ITEM_ONLY,
}

// ---- Ограничения значений ----

object SwingTransformLimits {
    const val MIN_X = -2f
    const val MAX_X = 2f
    const val MIN_Y = -2f
    const val MAX_Y = 2f
    const val MIN_Z = -1.5f
    const val MAX_Z = 0.6f
    const val MIN_SCALE = 0.25f
    const val MAX_SCALE = 3f
    const val MIN_SWING_SPEED = 0.25f
    const val MAX_SWING_SPEED = 3f
    const val MIN_ROTATION = -180f
    const val MAX_ROTATION = 180f
}

// ---- Трансформация предмета ----

class SwingTransformConfig(
    @Expose var x: Float = 0f,
    @Expose var y: Float = 0f,
    @Expose var z: Float = 0f,
    @Expose var scale: Float = 1f,
    @Expose var swingSpeed: Float = 1f,
    @Expose var swingStyle: SwingStyle = SwingStyle.VANILLA,
    @Expose var rotationX: Float = 0f,
    @Expose var rotationY: Float = 0f,
    @Expose var rotationZ: Float = 0f,
) {
    fun copyValues(): SwingTransformConfig = SwingTransformConfig(
        x = x,
        y = y,
        z = z,
        scale = scale,
        swingSpeed = swingSpeed,
        swingStyle = swingStyle,
        rotationX = rotationX,
        rotationY = rotationY,
        rotationZ = rotationZ,
    )

    fun reset() {
        x = 0f
        y = 0f
        z = 0f
        scale = 1f
        swingSpeed = 1f
        swingStyle = SwingStyle.VANILLA
        rotationX = 0f
        rotationY = 0f
        rotationZ = 0f
    }

    fun repairLoadedValues() {
        x = x.coerceIn(SwingTransformLimits.MIN_X, SwingTransformLimits.MAX_X)
        y = y.coerceIn(SwingTransformLimits.MIN_Y, SwingTransformLimits.MAX_Y)
        z = z.coerceIn(SwingTransformLimits.MIN_Z, SwingTransformLimits.MAX_Z)
        scale = scale.coerceIn(SwingTransformLimits.MIN_SCALE, SwingTransformLimits.MAX_SCALE)
        swingSpeed = swingSpeed.coerceIn(
            SwingTransformLimits.MIN_SWING_SPEED,
            SwingTransformLimits.MAX_SWING_SPEED,
        )
        rotationX = rotationX.coerceIn(SwingTransformLimits.MIN_ROTATION, SwingTransformLimits.MAX_ROTATION)
        rotationY = rotationY.coerceIn(SwingTransformLimits.MIN_ROTATION, SwingTransformLimits.MAX_ROTATION)
        rotationZ = rotationZ.coerceIn(SwingTransformLimits.MIN_ROTATION, SwingTransformLimits.MAX_ROTATION)
    }

    fun hasRenderChanges(): Boolean =
        x != 0f || y != 0f || z != 0f || scale != 1f || rotationX != 0f || rotationY != 0f || rotationZ != 0f

    fun isDefault(): Boolean =
        !hasRenderChanges() && swingSpeed == 1f && swingStyle == SwingStyle.VANILLA
}

// ---- Снимки для undo/redo ----

data class SwingCustomizationSnapshot(
    val transform: SwingTransformSnapshot?,
)

data class SwingTransformSnapshot(
    val x: Float,
    val y: Float,
    val z: Float,
    val scale: Float,
    val swingSpeed: Float,
    val swingStyle: SwingStyle,
    val rotationX: Float,
    val rotationY: Float,
    val rotationZ: Float,
)

fun SwingTransformConfig.snapshot(): SwingTransformSnapshot = SwingTransformSnapshot(
    x = x,
    y = y,
    z = z,
    scale = scale,
    swingSpeed = swingSpeed,
    swingStyle = swingStyle,
    rotationX = rotationX,
    rotationY = rotationY,
    rotationZ = rotationZ,
)

fun SwingTransformSnapshot.toConfig(): SwingTransformConfig = SwingTransformConfig(
    x = x,
    y = y,
    z = z,
    scale = scale,
    swingSpeed = swingSpeed,
    swingStyle = swingStyle,
    rotationX = rotationX,
    rotationY = rotationY,
    rotationZ = rotationZ,
)
