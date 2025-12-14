package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import set.starlev.mixin.accessors.GuiAccessor

object ActionBarDetector {

    /**
     * Получить текст из экшн бара (над хотбаром)
     * @return Текст из экшн бара или пустая строка
     */
    fun getActionBarText(): String {
        val client = Minecraft.getInstance()
        return try {
            val gui = client.gui ?: return ""
            val accessor = gui as GuiAccessor
            val message = accessor.getOverlayMessageString()
            message?.string ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
