package set.starlev.secret.features.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.secret.config.SecretMenuManager
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CompletableFuture

object LmStudioClient {
    private val gson = Gson()

    fun generateResponse(prompt: String, sender: String): CompletableFuture<String> {
        return CompletableFuture.supplyAsync {
            val config = SecretMenuManager.secretConfig.lmStudio
            if (!config.enabled) return@supplyAsync ""

            try {
                val url = URL("${config.apiUrl.removeSuffix("/")}/chat/completions")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 30000 // 30 seconds
                connection.readTimeout = 60000    // 60 seconds
                connection.doOutput = true

                val systemPrompt = "Ты - помощник в игре Minecraft на сервере Heltix Skyblock. Твое имя - StarredHeltix AI. Его создатель Starlev (Лёва) не сервера, а мода." +
                        "Ты общаешься с игроком по нику $sender. Обязательно обращайся к нему! Напиши его ник в начале. Будь дружелюбным и ОЧЕНЬ кратким (строго не более 150 символов!)." +
                        "Пиши только одной строкой. НИКАКИХ переносов строк (\\n), никаких табуляций (\\t), эмодзи или иероглифов." +
                        "Используй только русский алфавит, цифры и базовые знаки (. , ! ? - :)." +
                        "Твой ответ будет отправлен напрямую в чат игры, поэтому любые запрещенные символы приведут к кику игрока." +
                        "Если в сообщении есть [!], ты отвечаешь в глобальный чат и ОБЯЗАТЕЛЬНО начни ответ с символа ! (восклицательный знак), иначе его никто не увидит."

                val requestBody = JsonObject().apply {
                    addProperty("model", if (config.modelId.isBlank()) "local-model" else config.modelId)
                    add("messages", gson.toJsonTree(listOf(
                        mapOf("role" to "system", "content" to systemPrompt),
                        mapOf("role" to "user", "content" to prompt)
                    )))
                    addProperty("temperature", config.temperature.toDoubleOrNull() ?: 0.7)
                    addProperty("max_tokens", 80)
                }

                connection.outputStream.use { it.write(gson.toJson(requestBody).toByteArray()) }

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = gson.fromJson(response, JsonObject::class.java)
                    val content = jsonResponse.getAsJsonArray("choices")
                        .get(0).asJsonObject
                        .getAsJsonObject("message")
                        .get("content").asString
                    
                    return@supplyAsync content.trim()
                } else {
                    val errorDetail = try {
                        connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Нет деталей"
                    } catch (e: Exception) {
                        "Ошибка чтения ошибки"
                    }
                    
                    Minecraft.getInstance().execute {
                        Minecraft.getInstance().player?.displayClientMessage(
                            Component.literal("§6[LM Studio] §cОшибка API: ${connection.responseCode} ($errorDetail)"),
                            false
                        )
                    }
                }
            } catch (e: Exception) {
                Minecraft.getInstance().execute {
                    Minecraft.getInstance().player?.displayClientMessage(
                        Component.literal("§6[LM Studio] §cОшибка подключения! Проверьте LM Studio."),
                        false
                    )
                }
                e.printStackTrace()
            }
            ""
        }
    }
}
