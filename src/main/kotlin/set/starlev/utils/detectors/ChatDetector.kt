package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import set.starlev.mixin.accessors.ChatComponentAccessor

object ChatDetector {

    /**
     * Returns the last chat message received.
     * Returns an empty string if history is empty.
     */
    fun getLastChatMessage(): String {
        val client = Minecraft.getInstance()
        val chat = client.gui?.chat ?: return ""
        
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
     * Returns a list of the last N chat messages.
     */
    fun getChatHistory(limit: Int): List<String> {
        val client = Minecraft.getInstance()
        val chat = client.gui?.chat ?: return emptyList()
        
        return try {
            val accessor = chat as ChatComponentAccessor
            val messages = accessor.getAllMessages()
            
            // Messages are usually stored oldest to newest or vice versa?
            // Typically list adds to end, so newest is last.
            // Let's return newest first for convenience.
            
            messages.takeLast(limit).reversed().map { it.content.string }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
