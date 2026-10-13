package set.starlev.features.visual

import set.starlev.StarredHeltix

/**
 * Аксессор для настроек анимации удара, используемых из Java-миксинов.
 * Основная настройка — через отдельное окно-редактор (SwingEditorScreen).
 */
object SwingAnimation {
    fun isNoEquipEnabled(): Boolean =
        StarredHeltix.feature.visuals.animations.swingAnimation.noEquipAnimation
}
