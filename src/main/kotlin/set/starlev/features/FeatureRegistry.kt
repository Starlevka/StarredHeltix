package set.starlev.features

/**
 * @deprecated Используй [FeatureManager].
 */
@Deprecated("Заменён на FeatureManager", replaceWith = ReplaceWith("FeatureManager"))
object FeatureRegistry {
    fun initAll() {
        // FeatureManager.init() теперь вызывается напрямую из StarredHeltix.kt
    }
}
