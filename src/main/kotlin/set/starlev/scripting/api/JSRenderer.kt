package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Quaternionf
import set.starlev.render.RenderContext

/**
 * JS API: глобальный `Renderer` — immediate-mode 2D/3D-отрисовка.
 * Используется в `registerRenderOverlay` / `registerRenderWorld`:
 *   Renderer.drawRect(0xff000000, 10,10,100,20)
 *   Renderer.drawString("hi", 10,10)
 * В SH реализован как тонкая обёртка над текущим GuiGraphicsExtractor / RenderContext,
 * которые прокидываются через ThreadLocal во время fireTrigger("overlay"/"worldRender").
 */
object JSRenderer {
    private val guiCtx = ThreadLocal<GuiGraphicsExtractor>()
    private val worldCtx = ThreadLocal<RenderContext>()

    internal fun setGuiContext(ctx: GuiGraphicsExtractor?) {
        if (ctx == null) guiCtx.remove() else guiCtx.set(ctx)
    }
    internal fun setWorldContext(ctx: RenderContext?) {
        if (ctx == null) worldCtx.remove() else worldCtx.set(ctx)
    }

    // Цвета
    @JvmStatic val BLACK = 0xFF000000L
    @JvmStatic val DARK_BLUE = 0xFF0000AAL
    @JvmStatic val GREEN = 0xFF00AA00L
    @JvmStatic val AQUA = 0xFF00AAAAL
    @JvmStatic val RED = 0xFFFF5555L
    @JvmStatic val PURPLE = 0xFFAA00AAL
    @JvmStatic val GOLD = 0xFFFFAA00L
    @JvmStatic val GRAY = 0xFFAAAAAAL
    @JvmStatic val DARK_GRAY = 0xFF555555L
    @JvmStatic val BLUE = 0xFF5555FFL
    @JvmStatic val LIGHT_PURPLE = 0xFFFF55FFL
    @JvmStatic val YELLOW = 0xFFFFFF55L
    @JvmStatic val WHITE = 0xFFFFFFFFL

    @JvmStatic fun color(code: String): Long {
        return try {
            net.minecraft.ChatFormatting.valueOf(code.uppercase())
            WHITE
        } catch (_: Exception) { WHITE }
    }
    @JvmStatic fun getColor(r: Int, g: Int, b: Int, a: Int = 255): Long = (a.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    @JvmStatic fun getRainbow(step: Int, speed: Double = 1.0): Long {
        val hue = ((System.currentTimeMillis() / 1000.0 * speed + step) % 360 / 360.0).toFloat()
        val rgb = java.awt.Color.getHSBColor(hue, 1f, 1f).rgb
        return (0xFF000000L or (rgb and 0xFFFFFF).toLong())
    }

    @JvmStatic fun getStringWidth(text: String): Int = Minecraft.getInstance().font.width(text)
    @JvmStatic fun getFontRenderer(): net.minecraft.client.gui.Font = Minecraft.getInstance().font

    // 2D
    @JvmStatic fun drawRect(color: Long, x: Int, y: Int, w: Int, h: Int) {
        val g = guiCtx.get() ?: return
        g.fill(x, y, x + w, y + h, color.toInt())
    }
    @JvmStatic fun drawLine(color: Long, x1: Int, y1: Int, x2: Int, y2: Int, thickness: Float = 1f) {
        val g = guiCtx.get() ?: return
        // простая линия через fill (толщину GL-линий имитируем стеком заливок)
        if (x1 == x2) g.fill(x1, minOf(y1, y2), x1 + thickness.toInt().coerceAtLeast(1), maxOf(y1, y2), color.toInt())
        else g.fill(minOf(x1, x2), y1, maxOf(x1, x2), y1 + thickness.toInt().coerceAtLeast(1), color.toInt())
    }
    @JvmStatic fun drawCircle(color: Long, x: Int, y: Int, radius: Int, steps: Int = 16) {
        val g = guiCtx.get() ?: return
        val r2 = radius * radius
        for (dx in -radius..radius) {
            val half = kotlin.math.sqrt((r2 - dx*dx).toDouble()).toInt()
            g.fill(x+dx, y-half, x+dx+1, y+half+1, color.toInt())
        }
    }
    @JvmStatic fun drawString(text: String, x: Int, y: Int) {
        val g = guiCtx.get() ?: return
        g.text(Minecraft.getInstance().font, text, x, y, 0xFFFFFFFF.toInt(), true)
    }
    @JvmStatic fun drawStringWithShadow(text: String, x: Int, y: Int) = drawString(text, x, y)
    @JvmStatic fun drawImage(image: Any?, x: Int, y: Int, w: Int, h: Int) {
        // image is JSImage or Identifier string
        val g = guiCtx.get() ?: return
        val id = when(image) {
            is JSImage -> try { image.getIdOrRegister() } catch(_:Exception){ return }
            is String -> try { net.minecraft.resources.Identifier.parse(image) } catch(_:Exception){ return }
            else -> return
        }
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, w, h, w, h)
    }

    // Matrix
    @JvmStatic fun pushMatrix() { guiCtx.get()?.pose()?.pushMatrix() }
    @JvmStatic fun popMatrix() { guiCtx.get()?.pose()?.popMatrix() }
    @JvmStatic fun translate(x: Float, y: Float, z: Float = 0f) { guiCtx.get()?.pose()?.translate(x, y) }
    @JvmStatic fun scale(x: Float, y: Float, z: Float = 1f) { guiCtx.get()?.pose()?.scale(x, y) }
    @JvmStatic fun rotate(angle: Float, x: Float, y: Float, z: Float) {
        val q = Quaternionf().rotateAxis(angle * (Math.PI.toFloat()/180f), x, y, z)
        // GuiGraphicsExtractor pose is Matrix3x2fStack (2D), can't rotate 3D — no-op for 2D
    }

    // 3D (world)
    @JvmStatic fun drawBox(color: Long, x: Double, y: Double, z: Double, w: Double, h: Double, d: Double, filled: Boolean = false) {
        val ctx = worldCtx.get() ?: return
        val aabb = net.minecraft.world.phys.AABB(x, y, z, x+w, y+h, z+d)
        val a = ((color shr 24) and 0xFF) / 255f; val r = ((color shr 16) and 0xFF)/255f
        val g = ((color shr 8) and 0xFF)/255f; val b = (color and 0xFF)/255f
        if (filled) ctx.renderBoxThroughBlocks(aabb, r,g,b,a, true, 0f) else ctx.renderBox(aabb, r,g,b,a, false, 1f)
    }
}
