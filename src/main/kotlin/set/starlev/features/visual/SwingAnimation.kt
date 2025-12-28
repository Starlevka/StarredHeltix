package set.starlev.features.visual

import set.starlev.StarredHeltix

object SwingAnimation {
    fun isEnabled(): Boolean = StarredHeltix.feature.visuals.animations.swingAnimation.enabled
    
    fun getSwingSpeed(): Int = StarredHeltix.feature.visuals.animations.swingAnimation.swingSpeed.toInt()
    fun isNoEquipEnabled(): Boolean = StarredHeltix.feature.visuals.animations.swingAnimation.noEquipAnimation

    fun getOffX(): Double = StarredHeltix.feature.visuals.animations.swingAnimation.offX
    fun getOffY(): Double = StarredHeltix.feature.visuals.animations.swingAnimation.offY
    fun getOffZ(): Double = StarredHeltix.feature.visuals.animations.swingAnimation.offZ

    fun getSwingX(): Double = StarredHeltix.feature.visuals.animations.swingAnimation.swingX
    fun getSwingY(): Double = StarredHeltix.feature.visuals.animations.swingAnimation.swingY
    fun getSwingZ(): Double = StarredHeltix.feature.visuals.animations.swingAnimation.swingZ
}
