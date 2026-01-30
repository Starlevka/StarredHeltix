package set.starlev.features.chat

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.network.chat.contents.PlainTextContents
import set.starlev.StarredHeltix

object ChatFormatting {

    @JvmStatic
    fun processComponent(component: Component): Component {
        try {
            val text = component.string
            if (text.contains("&z") || text.contains("&f") || text.contains("§z") || text.contains("§f") || text.contains("&s") || text.contains("§s")) {
                return processRecursive(component)
            }
        } catch (e: Exception) {
            StarredHeltix.LOGGER.error("Error in ChatFormatting", e)
        }
        
        return component
    }

    private fun processRecursive(component: Component): Component {
        val contents = component.contents
        var result: MutableComponent? = null

        if (contents is PlainTextContents) {
            val text = contents.text()
            if (text.contains("&z") || text.contains("&f") || text.contains("§z") || text.contains("§f")) {
                result = parseFormatting(text, component.style)
            }
        } else if (contents is net.minecraft.network.chat.contents.TranslatableContents) {
            val args = contents.args
            var argsChanged = false
            val newArgs = arrayOfNulls<Any>(args.size)
            
            for (i in args.indices) {
                val arg = args[i]
                if (arg is Component) {
                    val modifiedArg = processRecursive(arg)
                    if (modifiedArg !== arg) {
                        argsChanged = true
                        newArgs[i] = modifiedArg
                    } else {
                        newArgs[i] = arg
                    }
                } else {
                    newArgs[i] = arg
                }
            }
            
            if (argsChanged) {
                result = Component.translatable(contents.key, *newArgs).withStyle(component.style)
            }
        }

        // Обрабатываем вложенные компоненты (siblings)
        val siblings = component.siblings
        var siblingsChanged = false
        val newSiblings = ArrayList<Component>(siblings.size)
        
        for (sibling in siblings) {
            val modifiedSibling = processRecursive(sibling)
            if (modifiedSibling !== sibling) {
                siblingsChanged = true
            }
            newSiblings.add(modifiedSibling)
        }
        
        if (result == null && !siblingsChanged) {
            return component
        }

        // Собираем итоговый компонент
        val finalComponent = result ?: MutableComponent.create(contents).withStyle(component.style)
        
        if (siblingsChanged || result != null) {
            newSiblings.forEach { finalComponent.append(it) }
        }

        return finalComponent
    }

    private fun parseFormatting(text: String, baseStyle: Style): MutableComponent {
        val root = Component.empty()
        var currentPart = StringBuilder()
        var currentStyle = baseStyle
        
        // В text_effects_utils.glsl: 
        // if (colorMatchMain(c, 255, 255) && c.b >= 243 && c.b <= 254) { effectID = 255 - c.b; ... }
        
        // Rainbow (ID 3): 255 - 3 = 252 (FC) -> 0xFFFFFC
        val rainbowTriggerColor = TextColor.fromRgb(0xFFFFFC)
        
        // Delayed Spin (ID 8): 255 - 8 = 247 (F7) -> 0xFFFFF7
        val spinTriggerColor = TextColor.fromRgb(0xFFFFF7)
        
        val whiteColor = TextColor.fromRgb(0xFFFFFF)

        var i = 0
        while (i < text.length) {
            val c = text[i]
            if ((c == '&' || c == '§') && i + 1 < text.length) {
                val code = text[i + 1].lowercaseChar()
                if (code == 'z' || code == 'f' || code == 's') {
                    if (currentPart.isNotEmpty()) {
                        root.append(Component.literal(currentPart.toString()).withStyle(currentStyle))
                        currentPart = StringBuilder()
                    }
                    
                    when (code) {
                        'z' -> currentStyle = currentStyle.withColor(rainbowTriggerColor)
                        's' -> currentStyle = currentStyle.withColor(spinTriggerColor)
                        'f' -> currentStyle = currentStyle.withColor(whiteColor)
                    }
                    i += 2
                    continue
                }
            }
            currentPart.append(c)
            i++
        }

        if (currentPart.isNotEmpty()) {
            root.append(Component.literal(currentPart.toString()).withStyle(currentStyle))
        }

        return root
    }
}
