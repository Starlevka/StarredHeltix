package set.starlev.events

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import set.starlev.render.RenderContext

data class GuiOpenEvent(val screen: AbstractContainerScreen<*>)
data class GuiCloseEvent(val screen: AbstractContainerScreen<*>)
data class GuiRenderEvent(val graphics: GuiGraphics)
data class GuiForegroundEvent(val graphics: GuiGraphics, val mouseX: Int, val mouseY: Int, val screen: AbstractContainerScreen<*>)
data class GuiClickEvent(val mouseX: Double, val mouseY: Double, val button: Int, val screen: AbstractContainerScreen<*>)
data class ChatIncomingEvent(val message: String)
data class ChatOutgoingEvent(val message: String)

object StarredEvents {
    val guiOpen = TypedEvent<GuiOpenEvent>()
    val guiClose = TypedEvent<GuiCloseEvent>()
    val guiRender = TypedEvent<GuiRenderEvent>()
    val guiForeground = TypedEvent<GuiForegroundEvent>()
    val guiClick = TypedEvent<GuiClickEvent>()
    val worldRender = TypedEvent<RenderContext>()
    val chatIncoming = TypedEvent<ChatIncomingEvent>()
    val chatOutgoing = TypedEvent<ChatOutgoingEvent>()
}
