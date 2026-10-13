package set.starlev.features

import org.slf4j.LoggerFactory

/**
 * Лёгкий реестр фич — только хранение и поиск по [Category]/id.
 * Раньше здесь была своя file-toggle система (feature-toggles.json), но теперь
 * единый источник правды — MoulConfig [set.starlev.config.Features] через
 * [set.starlev.StarredHeltix.feature]. Включение/выключение идёт через конфиг,
 * а не через отдельное поле [Feature.enabled].
 *
 * Bootstrap — напрямую в [set.starlev.StarredHeltix.onInitializeClient]:
 * каждая фича вызывает `init()` вручную, порядок важен (детекторы → фичи → HUD).
 * Этот менеджер сохраняется для будущего единого SH-экрана «Все фичи по категориям»
 * (использует [getByCategory] + [getAll] + SH-kit).
 */
object FeatureManager {
    private val LOGGER = LoggerFactory.getLogger("StarredHeltix")
    private val features = mutableListOf<Feature>()

    fun register(vararg newFeatures: Feature) {
        for (f in newFeatures) if (features.none { it.id == f.id }) features.add(f)
    }

    /** Вызвать `init()` у всех зарегистрированных (если когда-то перейдём на регистрацию через этот менеджер). */
    fun initAll() {
        if (features.isEmpty()) return
        for (f in features) {
            try {
                f.init()
            } catch (e: Exception) {
                LOGGER.error("Ошибка init '${f.name}': ${e.message}", e)
            }
        }
    }

    fun getAll(): List<Feature> = features.toList()
    fun getById(id: String): Feature? = features.find { it.id == id }
    fun getByName(name: String): Feature? = features.find { it.name == name }
    fun getByCategory(category: Category): List<Feature> = features.filter { it.category == category }
}
