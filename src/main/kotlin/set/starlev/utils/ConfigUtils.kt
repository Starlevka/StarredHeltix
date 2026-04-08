package set.starlev.utils

import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.hud.HudEditorScreen

object ConfigUtils {

    fun openEditor(editor: MoulConfigEditor<*>) {
        StarredHeltix.screenToOpen = MoulConfigScreenComponent(Component.empty(), GuiContext(GuiElementComponent(editor)), null)
    }

    /**
     * Открыть редактор HUD элементов
     */
    fun openHudEditor() {
        StarredHeltix.screenToOpen = HudEditorScreen()
    }

    fun String.asStructuredText(): StructuredText {
        return StructuredText.of(this)
    }

    fun toLegacyHex(component: net.minecraft.network.chat.Component): String {
        val sb = StringBuilder()
        component.visit({ style, text ->
            val color = style.color
            if (color != null) {
                val hex = String.format("%06X", color.value and 0xFFFFFF)
                sb.append("§x")
                for (c in hex) {
                    sb.append("§$c")
                }
            }
            if (style.isBold) sb.append("§l")
            if (style.isItalic) sb.append("§o")
            if (style.isUnderlined) sb.append("§n")
            if (style.isStrikethrough) sb.append("§m")
            if (style.isObfuscated) sb.append("§k")
            sb.append(text)
            java.util.Optional.empty<Any>()
        }, net.minecraft.network.chat.Style.EMPTY)
        return sb.toString()
    }
}
