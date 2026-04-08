package set.starlev.features.visual

import set.starlev.StarredHeltix

object Fullbright {
    fun isEnabled(): Boolean {
        return StarredHeltix.feature.optimization.visualOptimizations.fullbright
    }

    fun init() {
    }
}
