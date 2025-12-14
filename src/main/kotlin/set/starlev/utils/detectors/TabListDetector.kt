package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.mixin.accessors.PlayerTabOverlayAccessor

object TabListDetector {

    /**
     * Получить табулятор лист (список игроков)
     * @return Header из таб листа
     */
    fun getTabListHeader(): String {
        val client = Minecraft.getInstance()
        val tabList = client.gui?.tabList ?: return ""
        
        return try {
            val accessor = tabList as PlayerTabOverlayAccessor
            val header = accessor.getHeader()
            componentToString(header)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Получить footer из таб листа
     * @return Footer строка
     */
    fun getTabListFooter(): String {
        val client = Minecraft.getInstance()
        val tabList = client.gui?.tabList ?: return ""
        
        return try {
            val accessor = tabList as PlayerTabOverlayAccessor
            val footer = accessor.getFooter()
            componentToString(footer)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Конвертировать Component в строку
     */
    private fun componentToString(component: Component?): String {
        return component?.getString() ?: ""
    }
}
