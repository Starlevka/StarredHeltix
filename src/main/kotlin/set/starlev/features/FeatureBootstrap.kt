package set.starlev.features

/**
 * @deprecated Используй [FeatureManager].
 */
@Deprecated("Заменён на FeatureManager", replaceWith = ReplaceWith("FeatureManager"))
object FeatureBootstrap {
    fun registerAll() {
        // FeatureManager.register() теперь вызывается напрямую из StarredHeltix.kt
    }
}
