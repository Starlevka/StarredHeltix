package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import set.starlev.mixin.accessors.GuiAccessor

object HeldItemDetector {

    /**
     * Returns the text of the currently displayed held item tooltip.
     * Returns an empty string if the tooltip is not visible or empty.
     */
    fun getTooltipText(): String {
        val client = Minecraft.getInstance()
        val gui = client.gui ?: return ""
        
        return try {
            val accessor = gui as GuiAccessor
            if (accessor.getToolHighlightTimer() > 0) {
                val stack = accessor.getLastToolHighlight()
                if (stack != null && !stack.isEmpty) {
                    return stack.hoverName.string
                }
            }
            ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Checks if the held item tooltip is currently visible (timer > 0).
     */
    fun isTooltipVisible(): Boolean {
        val client = Minecraft.getInstance()
        val gui = client.gui ?: return false
        
        return try {
            val accessor = gui as GuiAccessor
            accessor.getToolHighlightTimer() > 0
        } catch (e: Exception) {
            false
        }
    }
}
