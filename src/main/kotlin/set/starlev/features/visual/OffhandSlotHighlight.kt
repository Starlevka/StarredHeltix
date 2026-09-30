package set.starlev.features.visual

import set.starlev.StarredHeltix

/**
 * Аксессор настройки подсветки слота второй руки.
 */
object OffhandSlotHighlight {
    fun isHidden(): Boolean {
        return try {
            StarredHeltix.feature.visuals.hideOffhandSlotHighlight
        } catch (_: Exception) {
            false
        }
    }
}
