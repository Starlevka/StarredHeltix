package set.starlev.features.chat

object ChatEventsManager {
    private val outgoingListeners = mutableListOf<(String) -> Boolean>()
    private val incomingListeners = mutableListOf<(String) -> Unit>()

    fun registerOutgoing(listener: (String) -> Boolean) {
        outgoingListeners.add(listener)
    }

    fun registerIncoming(listener: (String) -> Unit) {
        incomingListeners.add(listener)
    }

    fun onOutgoingMessage(message: String): Boolean {
        return outgoingListeners.any { it(message) }
    }

    fun onIncomingMessage(message: String) {
        net.minecraft.client.Minecraft.getInstance().execute {
            incomingListeners.forEach { it(message) }
        }
    }
}
