package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import set.starlev.injections.accessors.GuiAccessor

object HeldItemDetector {

    /**
     * Возвращает текст тултипа предмета, который сейчас отображается.
     * Возвращает пустую строку, если тултип не виден или пуст.
     */
    fun getTooltipText(): String {
        val client = Minecraft.getInstance()
        val gui = client.gui.hud
        
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
     * Проверяет, отображается ли сейчас тултип предмета в руке (таймер > 0).
     */
    fun isTooltipVisible(): Boolean {
        val client = Minecraft.getInstance()
        val gui = client.gui.hud
        
        return try {
            val accessor = gui as GuiAccessor
            accessor.getToolHighlightTimer() > 0
        } catch (e: Exception) {
            false
        }
    }
}
