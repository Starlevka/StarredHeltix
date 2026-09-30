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
        // Событие для JS-модулей (events.onChatOutgoing) и внутреннего транспорта
        set.starlev.events.StarredEvents.chatOutgoing.post(
            set.starlev.events.ChatOutgoingEvent(message)
        )
        return outgoingListeners.any { it(message) }
    }

    fun onIncomingMessage(message: String) {
        net.minecraft.client.Minecraft.getInstance().execute {
            // Событие для JS-модулей (events.onChat) — только на главном потоке
            set.starlev.events.StarredEvents.chatIncoming.post(
                set.starlev.events.ChatIncomingEvent(message)
            )
            // Изоляция: упавший слушатель не должен глушить остальных (PartyCommands,
            // Waypoints, DungeonMapHud регистрируются в один список).
            for (listener in incomingListeners) {
                try {
                    listener(message)
                } catch (e: Exception) {
                    set.starlev.StarredHeltix.LOGGER.error("[ChatEvents] Ошибка в incoming-слушателе: ${e.message}", e)
                }
            }
        }
    }
}
