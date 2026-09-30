package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import set.starlev.injections.accessors.GuiAccessor

object TitleDetector {

    /**
     * Возвращает текст title, отображаемого сейчас.
     * Возвращает пустую строку, если title не виден.
     */
    fun getTitleText(): String {
        val client = Minecraft.getInstance()
        val gui = client.gui.hud
        
        return try {
            val accessor = gui as GuiAccessor
            // Возвращаем текст только если он сейчас отображается (время > 0)
            // или title не null. Обычно видимость/затухание управляется titleTime,
            // но проверка на null безопаснее.
            val title = accessor.getTitle()
            if (title != null && accessor.getTitleTime() > 0) {
                title.string
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Возвращает текст subtitle, отображаемого сейчас.
     * Возвращает пустую строку, если subtitle не виден.
     */
    fun getSubtitleText(): String {
        val client = Minecraft.getInstance()
        val gui = client.gui.hud
        
        return try {
            val accessor = gui as GuiAccessor
            val subtitle = accessor.getSubtitle()
            // Subtitle обычно идёт вместе с title — проверять titleTime — хорошая практика
            if (subtitle != null && accessor.getTitleTime() > 0) {
                subtitle.string
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Проверяет, отображается ли сейчас title или subtitle.
     */
    fun isTitleVisible(): Boolean {
        val client = Minecraft.getInstance()
        val gui = client.gui.hud
        
        return try {
            val accessor = gui as GuiAccessor
            accessor.getTitleTime() > 0
        } catch (e: Exception) {
            false
        }
    }
}
