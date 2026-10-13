package set.starlev.utils

import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.render.HudEditorScreen

object ConfigUtils {

    /**
     * Корневой компонент, который перенаправляет onAfterClose() в редактор.
     *
     * Без него конфиг не сохраняется: `MoulConfigEditor` объявляет `onAfterClose()` (там
     * вызывается `config.saveNow()`), но `GuiElementComponent` — это final-обёртка (@Value),
     * которая НЕ имплементирует `CloseEventListener`. `GuiContext.onAfterClose()` идёт
     * `foldRecursive` по дереву и находит слушателя только если он есть в корне, поэтому
     * saveNow() молча не вызывался ни в основном меню, ни в секретном.
     */
    private class CloseForwardingRoot(private val editor: MoulConfigEditor<*>) :
        io.github.notenoughupdates.moulconfig.gui.GuiComponent(),
        io.github.notenoughupdates.moulconfig.gui.CloseEventListener {

        private val delegate = GuiElementComponent(editor)

        override fun getWidth(): Int = delegate.width
        override fun getHeight(): Int = delegate.height
        override fun render(context: io.github.notenoughupdates.moulconfig.gui.GuiImmediateContext) =
            delegate.render(context)

        override fun mouseEvent(
            mouseEvent: io.github.notenoughupdates.moulconfig.gui.MouseEvent,
            context: io.github.notenoughupdates.moulconfig.gui.GuiImmediateContext
        ): Boolean = delegate.mouseEvent(mouseEvent, context)

        override fun keyboardEvent(
            event: io.github.notenoughupdates.moulconfig.gui.KeyboardEvent,
            context: io.github.notenoughupdates.moulconfig.gui.GuiImmediateContext
        ): Boolean = delegate.keyboardEvent(event, context)

        override fun onAfterClose() = editor.onAfterClose()
    }

    fun openEditor(editor: MoulConfigEditor<*>) {
        StarredHeltix.screenToOpen =
            MoulConfigScreenComponent(Component.empty(), GuiContext(CloseForwardingRoot(editor)), null)
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
