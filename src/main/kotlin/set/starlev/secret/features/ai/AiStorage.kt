package set.starlev.secret.features.ai

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import set.starlev.config.ConfigManager
import java.io.*
import java.nio.charset.StandardCharsets

object AiStorage {
    private val storageFile = File("config/starredheltix/others/ai_memory.json")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun save() {
        try {
            storageFile.parentFile.mkdirs()
            val writer = BufferedWriter(OutputStreamWriter(FileOutputStream(storageFile), StandardCharsets.UTF_8))
            
            // Очистка памяти перед сохранением: убираем мусорные ключи и дубликаты
            val cleanContexts = AiContext.contextStore.filterKeys { key ->
                key.isNotEmpty() && key.all { it.isLetterOrDigit() || it == '_' }
            }
            
            val cleanEntities = AiContext.worldEntities.filterKeys { key ->
                key.isNotEmpty() && key.all { it.isLetterOrDigit() || it == '_' }
            }
            
            // Чистка выученных фраз от мусора
            val cleanLearned = AiLeksikon.BRAIN.filter { intent ->
                intent.source != "base" && 
                intent.keywords.all { it.length > 2 && it.any { c -> c.isLetter() } } &&
                intent.trustScore > 0.5
            }

            val data = mapOf(
                "users" to cleanContexts,
                "entities" to cleanEntities,
                "learned" to cleanLearned
            )
            val json = gson.toJson(data)
            writer.use { it.write(json) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun load() {
        if (!storageFile.exists()) return
        try {
            val reader = BufferedReader(InputStreamReader(FileInputStream(storageFile), StandardCharsets.UTF_8))
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val rawData: Map<String, Any> = gson.fromJson(reader.readText(), type)
            
            // Загрузка пользователей
            val usersJson = gson.toJson(rawData["users"])
            val usersType = object : TypeToken<MutableMap<String, UserContext>>() {}.type
            val loadedUsers: MutableMap<String, UserContext>? = gson.fromJson(usersJson, usersType)
            if (loadedUsers != null) {
                AiContext.contextStore.clear()
                AiContext.contextStore.putAll(loadedUsers)
            }

            // Загрузка сущностей
            val entitiesJson = gson.toJson(rawData["entities"])
            val entitiesType = object : TypeToken<MutableMap<String, WorldEntity>>() {}.type
            val loadedEntities: MutableMap<String, WorldEntity>? = gson.fromJson(entitiesJson, entitiesType)
            if (loadedEntities != null) {
                AiContext.worldEntities.clear()
                AiContext.worldEntities.putAll(loadedEntities)
            }

            // Загрузка выученных интентов
            val learnedJson = gson.toJson(rawData["learned"])
            val learnedType = object : TypeToken<List<Intent>>() {}.type
            val loadedLearned: List<Intent>? = gson.fromJson(learnedJson, learnedType)
            if (loadedLearned != null) {
                // Добавляем только те, которых нет в базе
                loadedLearned.forEach { intent ->
                    if (AiLeksikon.BRAIN.none { it.keywords == intent.keywords }) {
                        AiLeksikon.BRAIN.add(intent)
                    }
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
