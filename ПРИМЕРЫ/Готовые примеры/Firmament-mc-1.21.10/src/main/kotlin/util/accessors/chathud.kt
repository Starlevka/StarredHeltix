package moe.nea.firmament.util.accessors

import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.GuiMessage
import moe.nea.firmament.mixins.accessor.AccessorChatHud

val ChatComponent.messages: MutableList<GuiMessage>
	get() = (this as AccessorChatHud).messages_firmament
