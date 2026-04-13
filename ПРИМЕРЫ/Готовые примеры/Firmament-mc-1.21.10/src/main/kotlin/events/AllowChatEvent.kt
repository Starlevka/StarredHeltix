

package moe.nea.firmament.events

import net.minecraft.network.chat.Component
import moe.nea.firmament.util.unformattedString

/**
 * Filter whether the user should see a chat message altogether. May or may not be called for every chat packet sent by
 * the server. When that quality is desired, consider [ProcessChatEvent] instead.
 */
data class AllowChatEvent(val text: Component) : FirmamentEvent.Cancellable() {
    val unformattedString = text.unformattedString

    companion object : FirmamentEventBus<AllowChatEvent>()
}
