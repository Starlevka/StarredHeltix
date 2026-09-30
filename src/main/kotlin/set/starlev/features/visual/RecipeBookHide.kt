package set.starlev.features.visual

import set.starlev.StarredHeltix

/**
 * Аксессор скрытия книги рецептов для миксинов.
 */
object RecipeBookHide {
    fun isHidden(): Boolean {
        return try {
            StarredHeltix.feature.optimization.visualOptimizations.hideRecipeBook
        } catch (_: Exception) {
            false
        }
    }
}
