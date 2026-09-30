package set.starlev.features.visual

import set.starlev.StarredHeltix

/**
 * Аксессор настройки «Скрытие второй руки» для Java-миксинов.
 * Порт HeldItemRendererMixin из heltix-mod: полностью убирает рендер
 * OFF_HAND от первого лица (предмет + рука + рука карты).
 */
object HideOffhand {
    fun isEnabled(): Boolean = StarredHeltix.feature.visuals.hideOffhand
}
