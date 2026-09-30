package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import set.starlev.injections.accessors.ChatComponentAccessor
import set.starlev.utils.CacheManager

object ChatDetector {

    private const val COLOR_PATTERN = "(?i)§[0-9a-fk-orlnmxz]"

    /**
     * Возвращает последнее полученное сообщение чата.
     * Возвращает пустую строку, если история пуста.
     */
    fun getLastChatMessage(): String {
        val client = Minecraft.getInstance()
        val chat = client.gui.hud.chat
        
        return try {
            val accessor = chat as ChatComponentAccessor
            val messages = accessor.getAllMessages()
            if (messages.isNotEmpty()) {
                messages[messages.size - 1].content.string
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Очищает текст от цветовых кодов.
     */
    fun cleanColor(text: String): String {
        return text.replace(CacheManager.getRegex(COLOR_PATTERN), "")
    }

    /**
     * Возвращает список последних N сообщений чата.
     */
    fun getChatHistory(limit: Int): List<String> {
        val client = Minecraft.getInstance()
        val chat = client.gui.hud.chat
        
        return try {
            val accessor = chat as ChatComponentAccessor
            val messages = accessor.getAllMessages()
            
            // Сообщения обычно хранятся от старых к новым или наоборот?
            // Как правило, список пополняется в конец, значит новейшие — в конце.
            // Для удобства возвращаем новейшие первыми.
            
            messages.takeLast(limit).reversed().map { it.content.string }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
