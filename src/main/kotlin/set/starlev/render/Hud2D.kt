package set.starlev.render

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack

object Hud2D {
    private val mc = Minecraft.getInstance()

    fun text(graphics: GuiGraphics, text: String, x: Float, y: Float, color: Int = -1, shadow: Boolean = true) {
        if (shadow) graphics.drawString(mc.font, text, x.toInt(), y.toInt(), color, true)
        else graphics.drawString(mc.font, text, x.toInt(), y.toInt(), color, false)
    }

    fun item(graphics: GuiGraphics, stack: ItemStack, x: Float, y: Float) {
        graphics.renderItem(stack, x.toInt(), y.toInt())
    }

    fun rect(graphics: GuiGraphics, x: Int, y: Int, w: Int, h: Int, color: Int) {
        graphics.fill(x, y, x + w, y + h, color)
    }
}
