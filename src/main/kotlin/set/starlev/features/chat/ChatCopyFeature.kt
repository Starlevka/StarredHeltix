package set.starlev.features.chat

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.ChatFormatting
import net.minecraft.client.GuiMessage
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import org.lwjgl.glfw.GLFW
import set.starlev.StarredHeltix
import set.starlev.mixin.accessors.ChatComponentAccessor
import set.starlev.mixin.accessors.WindowAccessor
import java.awt.Color
import kotlin.math.floor

object ChatCopyFeature {
    private val mc = Minecraft.getInstance()

    private fun isShiftDown(): Boolean {
        return try {
            val handle = (mc.window as WindowAccessor).windowHandle
            GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS || 
            GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS
        } catch (e: Exception) {
            // Fallback: use InputConstants with Window object
            val window = mc.window
            InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || 
            InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT)
        }
    }

    fun handleRender(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (!StarredHeltix.feature.chat.chatCopy.enabled) return
        if (!isShiftDown()) return
        
        val chat = mc.gui?.chat ?: return
        val accessor = chat as? ChatComponentAccessor ?: return
        
        val lineIndex = getLineIndexAt(mouseX.toDouble(), mouseY.toDouble(), accessor)
        
        if (lineIndex != -1) {
            val lines = accessor.getTrimmedMessages()
            if (lineIndex < lines.size) {
                drawHighlight(graphics, lineIndex, accessor, mouseY.toDouble())
            }
        }
    }
    


    fun handleMouseClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!StarredHeltix.feature.chat.chatCopy.enabled) return false
        if (!isShiftDown()) return false
        if (button != 0) return false // Only left click

        val chat = mc.gui.chat
        val accessor = chat as? ChatComponentAccessor ?: return false
        val allMessages = accessor.getAllMessages()

        if (allMessages.isNotEmpty()) {
            val lastMessage = allMessages[0]
            val text = extractText(lastMessage.content)
            if (text.isNotEmpty()) {
                mc.keyboardHandler.clipboard = text
                mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
                return true
            }
        }
        return false
    }

    private fun getLineHeight(): Double {
        return 9.0 * (mc.options.chatLineSpacing().get() + 1.0)
    }

    private fun getLineIndexAt(x: Double, y: Double, accessor: ChatComponentAccessor): Int {
        try {
            val chatY = accessor.invokeScreenToChatY(y)
            if (chatY < 0) return -1
            
            val lineHeight = getLineHeight()
            val lineIndex = floor(chatY / lineHeight).toInt()
            val visibleLines = accessor.getTrimmedMessages().size

            if (lineIndex in 0..<visibleLines) {
                return lineIndex
            }
        } catch (e: Exception) {
            StarredHeltix.LOGGER.error("Error in getLineIndexAt", e)
        }
        return -1
    }
    
    private fun drawHighlight(graphics: GuiGraphics, lineIndex: Int, accessor: ChatComponentAccessor, currentMouseY: Double) {
        if (lineIndex < 0) return

        val chat = mc.gui.chat ?: return
        val lineHeight = getLineHeight()
        
        val lineTopChatY = lineIndex * lineHeight
        val lineBottomChatY = (lineIndex + 1) * lineHeight
        
        val mouseChatY = accessor.invokeScreenToChatY(currentMouseY)
        val screenOffset = currentMouseY - mouseChatY
        
        val lineTopScreen = lineTopChatY + screenOffset
        val lineBottomScreen = lineBottomChatY + screenOffset
        
        val x = 4.0
        val width = chat.width.toDouble()
        
        val colorStr = StarredHeltix.feature.chat.chatCopy.highlightColor
        val color = parseColorString(colorStr)
        
        renderBox(graphics, x.toInt(), lineTopScreen.toInt(), (x + width).toInt(), lineBottomScreen.toInt(), color)
    }
    
    private fun renderBox(graphics: GuiGraphics, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        // Top
        graphics.fill(x1, y1, x2, y1 + 1, color)
        // Bottom
        graphics.fill(x1, y2 - 1, x2, y2, color)
        // Left
        graphics.fill(x1, y1, x1 + 1, y2, color)
        // Right
        graphics.fill(x2 - 1, y1, x2, y2, color)
    }

    private fun extractText(content: Component): String {
        return try {
            content.string
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun extractFormattedText(line: GuiMessage.Line): String {
          try {
             val content = line.content

             val sb = StringBuilder()
             content.accept { _, style, codePoint ->
                 // Reconstruct basic formatting
                 if (style.isBold) sb.append("&l")
                 if (style.isItalic) sb.append("&o")
                 if (style.isUnderlined) sb.append("&n")
                 if (style.isStrikethrough) sb.append("&m")
                 if (style.isObfuscated) sb.append("&k")

                 val color = style.color
                 if (color != null) {
                     // Try to find standard formatting code
                     val formatting = ChatFormatting.getByName(color.serialize())
                     if (formatting != null) {
                         sb.append(formatting.toString().replace('§', '&'))
                     } else {
                         // Hex color - keep as &#hex
                         val hex = color.value
                         sb.append("&#").append(Integer.toHexString(hex))
                     }
                 }

                 sb.append(codePoint.toChar())
                 true
             }
             return sb.toString()
         } catch (e: Exception) {
             return ""
         }
    }


    private fun parseColorString(colorString: String): Int {
        return try {
            val parts = colorString.split(":")
            if (parts.size < 5) return 0xFFFFFFFF.toInt()

            val chroma = parts[0].toInt()
            val a = parts[1].toInt().coerceIn(0, 255)
            val r = parts[2].toInt().coerceIn(0, 255)
            val g = parts[3].toInt().coerceIn(0, 255)
            val b = parts[4].toInt().coerceIn(0, 255)

            if (chroma != 0) {
                val invertedChroma = (256 - chroma).coerceIn(1, 255)
                val periodInMillis = (invertedChroma / 255.0) * 60000.0
                if (periodInMillis <= 0) {
                    return (a shl 24) or (r shl 16) or (g shl 8) or b
                }

                val hue = (System.currentTimeMillis() % periodInMillis.toLong()) / periodInMillis.toFloat()

                val rainbowRgb = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f)
                (a shl 24) or (rainbowRgb and 0x00FFFFFF)
            } else {
                (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        } catch (e: NumberFormatException) {
            0xFFFFFFFF.toInt()
        }
    }
}
