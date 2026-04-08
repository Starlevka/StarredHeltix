package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import set.starlev.injections.accessors.GuiAccessor

object TitleDetector {

    /**
     * Returns the currently displayed title text.
     * Returns an empty string if no title is visible.
     */
    fun getTitleText(): String {
        val client = Minecraft.getInstance()
        val gui = client.gui ?: return ""
        
        return try {
            val accessor = gui as GuiAccessor
            // Only return text if it's currently being displayed (time > 0)
            // or if title is not null. Usually titleTime manages visibility/fading.
            // But checking for null is safer.
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
     * Returns the currently displayed subtitle text.
     * Returns an empty string if no subtitle is visible.
     */
    fun getSubtitleText(): String {
        val client = Minecraft.getInstance()
        val gui = client.gui ?: return ""
        
        return try {
            val accessor = gui as GuiAccessor
            val subtitle = accessor.getSubtitle()
            // Subtitle usually accompanies title, checking titleTime is good practice
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
     * Checks if a title or subtitle is currently being displayed.
     */
    fun isTitleVisible(): Boolean {
        val client = Minecraft.getInstance()
        val gui = client.gui ?: return false
        
        return try {
            val accessor = gui as GuiAccessor
            accessor.getTitleTime() > 0
        } catch (e: Exception) {
            false
        }
    }
}
