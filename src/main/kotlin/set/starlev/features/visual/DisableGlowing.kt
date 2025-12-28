package set.starlev.features.visual

import set.starlev.StarredHeltix

/**
 * DisableGlowing - Управляет состоянием эффекта свечения (Glowing) у сущностей.
 */
object DisableGlowing {

    /**
     * Проверяет, должен ли эффект свечения быть отключен.
     * Вызывается из MixinEntity (isGlowing) или аналогичного места.
     */
    fun shouldDisable(): Boolean {
        return StarredHeltix.feature.optimization.visualOptimizations.disableGlowing
    }
}
