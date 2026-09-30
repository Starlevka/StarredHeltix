package set.starlev.features.visual

import set.starlev.StarredHeltix

/**
 * Аксессор настройки блеска голов для Java-миксинов.
 */
object HeadGlint {
    fun isEnabled(): Boolean {
        return try {
            StarredHeltix.feature.visuals.headGlint
        } catch (_: Exception) {
            true
        }
    }
}
