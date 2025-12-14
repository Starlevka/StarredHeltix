package set.starlev.features.visual

import set.starlev.StarredHeltix

object SwingAnimation {
    fun isEnabled(): Boolean = StarredHeltix.feature.visuals.swingAnimation.enabled
    fun getSwingX(): Double = StarredHeltix.feature.visuals.swingAnimation.swingX
    fun getSwingY(): Double = StarredHeltix.feature.visuals.swingAnimation.swingY
    fun getSwingZ(): Double = StarredHeltix.feature.visuals.swingAnimation.swingZ
    fun isSwingSpeedEnabled(): Boolean = StarredHeltix.feature.visuals.swingAnimation.swingSpeedEnabled
    fun getSwingSpeed(): Float = StarredHeltix.feature.visuals.swingAnimation.swingSpeed
}
