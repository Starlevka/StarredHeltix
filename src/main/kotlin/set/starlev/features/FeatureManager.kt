package set.starlev.features

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files

/**
 * Менеджер фич — единственная точка регистрации и управления жизненным циклом.
 */
object FeatureManager {
    private val LOGGER = LoggerFactory.getLogger("StarredHeltix")
    private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    private val features = mutableListOf<Feature>()

    private var togglesFile: File? = null
    private val togglesCache = mutableMapOf<String, Boolean>()

    fun register(vararg newFeatures: Feature) {
        for (feature in newFeatures) {
            features.add(feature)
        }
    }

    fun init(configDir: File) {
        togglesFile = File(configDir, "feature-toggles.json")
        loadToggles()

        LOGGER.info("Инициализация ${features.size} фич...")
        for (feature in features) {
            try {
                val saved = togglesCache[feature.id]
                if (saved != null) {
                    feature.enabled = saved
                }
                feature.init()
                if (feature.enabled) {
                    feature.onEnable()
                }
                LOGGER.debug("Фича '${feature.name}' [${feature.category.name}] инициализирована (enabled=${feature.enabled})")
            } catch (e: Exception) {
                LOGGER.error("Ошибка инициализации фичи '${feature.name}': ${e.message}", e)
            }
        }
        LOGGER.info("Все фичи инициализированы (${features.count { it.enabled }} из ${features.size} включены)")
    }

    fun getAll(): List<Feature> = features.toList()
    fun getById(id: String): Feature? = features.find { it.id == id }
    fun getByName(name: String): Feature? = features.find { it.name == name }
    fun getByCategory(category: Category): List<Feature> = features.filter { it.category == category }
    fun getEnabled(): List<Feature> = features.filter { it.enabled }
    fun getDisabled(): List<Feature> = features.filter { !it.enabled }

    fun toggleById(id: String): Boolean {
        val feature = getById(id) ?: return false
        feature.toggle()
        saveToggles()
        return true
    }

    fun saveToggles() {
        val file = togglesFile ?: return
        try {
            togglesCache.clear()
            for (feature in features) {
                togglesCache[feature.id] = feature.enabled
            }
            Files.createDirectories(file.parentFile.toPath())
            file.writeText(GSON.toJson(togglesCache))
        } catch (e: Exception) {
            LOGGER.error("Ошибка сохранения toggle-состояний: ${e.message}", e)
        }
    }

    private fun loadToggles() {
        val file = togglesFile ?: return
        if (!file.exists()) return
        try {
            val type = object : TypeToken<Map<String, Boolean>>() {}.type
            val loaded: Map<String, Boolean> = GSON.fromJson(file.readText(), type)
            togglesCache.putAll(loaded)
        } catch (e: Exception) {
            LOGGER.warn("Ошибка загрузки toggle-состояний: ${e.message}")
        }
    }
}
