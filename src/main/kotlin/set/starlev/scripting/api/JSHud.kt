package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.mozilla.javascript.Callable
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import set.starlev.render.HudElement
import set.starlev.render.HudManager
import set.starlev.scripting.SHScriptEngine
import set.starlev.scripting.ScriptConsole
import set.starlev.render.drawString
import set.starlev.render.drawCenteredString
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * JS API: Управление кастомными HUD-элементами
 */
class JSHud(private val moduleName: String) {

    fun register(
        id: String,
        width: Int,
        height: Int,
        renderCallable: Callable
    ): JSHudElement {
        val fullId = "$moduleName-$id"
        val element = JSHudElement(fullId, width, height, renderCallable, moduleName)
        HudManager.registerElement(element)
        moduleElements.computeIfAbsent(moduleName.lowercase()) { Collections.synchronizedList(mutableListOf()) }
            .add(element)
        return element
    }

    companion object {
        private val moduleElements = ConcurrentHashMap<String, MutableList<JSHudElement>>()

        /** Снять все HUD-элементы модуля (при выгрузке модуля). */
        fun removeForModule(moduleName: String) {
            val list = moduleElements.remove(moduleName.lowercase()) ?: return
            list.toList().forEach { HudManager.unregisterElement(it) }
        }

        /** Снять все JS HUD-элементы (при выгрузке движка). */
        fun removeAll() {
            moduleElements.keys.toList().forEach { removeForModule(it) }
        }
    }
}

class JSHudElement(
    id: String,
    private var elementWidth: Int,
    private var elementHeight: Int,
    private val renderCallable: Callable,
    private val moduleName: String
) : HudElement(id) {

    override fun getWidth(): Int = customWidth.takeIf { it > 0 } ?: elementWidth
    override fun getHeight(): Int = customHeight.takeIf { it > 0 } ?: elementHeight

    override fun render() {
        val graphics = cachedGraphics ?: return
        SHScriptEngine.withContext { cx ->
            try {
                val ctxObj = JSHudGraphicsContext(graphics, x, y, getWidth(), getHeight())
                val jsCtx = Context.javaToJS(ctxObj, SHScriptEngine.globalScope)
                renderCallable.call(cx, SHScriptEngine.globalScope, SHScriptEngine.globalScope, arrayOf(jsCtx))
            } catch (e: Throwable) {
                ScriptConsole.error(moduleName, "Ошибка в render() HUD-элемента '$id': ${e.message}", e)
            }
        }
    }
}

class JSHudGraphicsContext(
    private val graphics: GuiGraphicsExtractor,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    private val font get() = Minecraft.getInstance().font

    /** 26.2 отбрасывает текст с alpha=0; цвет без альфа-байта трактуем как непрозрачный. */
    private fun opaqueText(color: Int): Int =
        if (color and 0xFF000000.toInt() == 0) color or 0xFF000000.toInt() else color

    @JvmOverloads
    fun drawString(text: String, relX: Int, relY: Int, color: Double = 0xFFFFFF.toDouble(), shadow: Boolean = true) {
        val colored = text.replace('&', '§')
        // String-перегрузка: §-коды парсятся (Component.literal — нет)
        graphics.drawString(font, colored, x + relX, y + relY, opaqueText(color.toInt()), shadow)
    }

    @JvmOverloads
    fun drawCenteredString(text: String, relX: Int, relY: Int, color: Double = 0xFFFFFF.toDouble(), shadow: Boolean = true) {
        val colored = text.replace('&', '§')
        graphics.drawCenteredString(font, colored, x + relX, y + relY, opaqueText(color.toInt()), shadow)
    }

    fun drawRect(relX: Int, relY: Int, w: Int, h: Int, color: Double) {
        graphics.fill(x + relX, y + relY, x + relX + w, y + relY + h, color.toInt())
    }

    /** Горизонтальная линия толщиной 1px (координаты относительные). */
    fun drawHLine(x1: Int, x2: Int, relY: Int, color: Double) {
        val xa = x + minOf(x1, x2)
        val xb = x + maxOf(x1, x2)
        graphics.fill(xa, y + relY, xb, y + relY + 1, color.toInt())
    }

    /** Вертикальная линия толщиной 1px (координаты относительные). */
    fun drawVLine(relX: Int, y1: Int, y2: Int, color: Double) {
        val ya = y + minOf(y1, y2)
        val yb = y + maxOf(y1, y2)
        graphics.fill(x + relX, ya, x + relX + 1, yb, color.toInt())
    }

    /** Полая рамка толщиной 1px (координаты относительные). */
    fun drawBorder(relX: Int, relY: Int, w: Int, h: Int, color: Double) {
        val col = color.toInt()
        val ax = x + relX
        val ay = y + relY
        graphics.fill(ax, ay, ax + w, ay + 1, col)
        graphics.fill(ax, ay + h - 1, ax + w, ay + h, col)
        graphics.fill(ax, ay, ax + 1, ay + h, col)
        graphics.fill(ax + w - 1, ay, ax + w, ay + h, col)
    }

    /**
     * Круг: заливка секторами + опциональная обводка (координаты относительные).
     */
    @JvmOverloads
    fun drawCircle(relX: Int, relY: Int, radius: Int, fillColor: Double = 0x80FFFFFF.toDouble(), outlineColor: Double = fillColor, segments: Int = 32) {
        val fill = fillColor.toInt()
        val outline = outlineColor.toInt()
        if (radius <= 0) return
        val pose = graphics.pose()
        val cx = x + relX
        val cy = y + relY
        val n = segments.coerceIn(6, 96)
        if (fill != 0) {
            pose.pushMatrix()
            for (i in 0 until n) {
                val angle = (Math.PI * 2 * (i + 0.5) / n).toFloat()
                pose.translate(cx.toFloat(), cy.toFloat())
                pose.rotate(angle)
                graphics.fill(0, 0, radius + 1, 1, fill)
                pose.translate(-cx.toFloat(), -cy.toFloat())
            }
            pose.popMatrix()
        }
        if (outline != 0 && radius > 1) {
            pose.pushMatrix()
            for (i in 0 until n) {
                val angle = (Math.PI * 2 * i / n).toFloat()
                pose.translate(cx.toFloat(), cy.toFloat())
                pose.rotate(angle)
                val arcLen = (Math.PI * 2 * radius / n).toInt() + 2
                graphics.fill(radius - 1, -1, radius + arcLen / 2, 1, outline)
                pose.translate(-cx.toFloat(), -cy.toFloat())
            }
            pose.popMatrix()
        }
    }

    fun drawGradientRect(relX: Int, relY: Int, w: Int, h: Int, colorTop: Double, colorBottom: Double) {
        graphics.fillGradient(x + relX, y + relY, x + relX + w, y + relY + h, colorTop.toInt(), colorBottom.toInt())
    }

    @JvmOverloads
    fun drawBackground(padding: Int = 4) {
        graphics.fill(x - padding, y - padding, x + width + padding, y + height + padding, 0x80000000.toInt())
    }

    /** Текстура в координатах худ-элемента (см. drawTexture из overlay-контекста). */
    @JvmOverloads
    fun drawTexture(
        texture: String,
        relX: Int, relY: Int, w: Int, h: Int,
        u: Float = 0f, v: Float = 0f,
        textureWidth: Int = 256, textureHeight: Int = 256,
        color: Double = 0xFFFFFF.toDouble()
    ) {
        val id = net.minecraft.resources.Identifier.parse(texture)
        graphics.blit(
            net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, id,
            x + relX, y + relY, u, v, w, h, textureWidth, textureHeight, color.toInt()
        )
    }

    /** Текст кастомным шрифтом (координаты относительные, см. ctx.drawFontText из overlay). */
    @JvmOverloads
    fun drawFontText(
        fontId: String, text: String, relX: Int, relY: Int,
        size: Int = 24, color: Double = 0xFFFFFF.toDouble(), shadow: Boolean = false
    ): Boolean {
        return set.starlev.scripting.SHModuleFonts.drawText(
            fontId, text, x + relX, y + relY, size, color.toInt(), shadow
        ) { tid, dx, dy, u, v, w, h, tw, th, col ->
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tid, dx, dy, u, v, w, h, tw, th, col)
        }
    }

    /** Ширина/высота строки кастомным шрифтом: SHModuleFonts.Metrics (m.width, m.height) или null. */
    @JvmOverloads
    fun measureFontText(fontId: String, text: String, size: Int = 24): set.starlev.scripting.SHModuleFonts.Metrics? {
        return set.starlev.scripting.SHModuleFonts.measureText(fontId, text, size)
    }

    /** Scissor-отсечение в пределах худ-элемента (координаты относительные). */
    fun pushClip(relX: Int, relY: Int, w: Int, h: Int) {
        graphics.enableScissor(x + relX, y + relY, x + relX + w, y + relY + h)
    }

    fun popClip() {
        graphics.disableScissor()
    }

    fun pushTransform() {
        graphics.pose().pushMatrix()
    }

    fun popTransform() {
        graphics.pose().popMatrix()
    }

    fun translate(dx: Float, dy: Float) {
        graphics.pose().translate(dx, dy)
    }

    fun scale(sx: Float, sy: Float) {
        graphics.pose().scale(sx, sy)
    }
}
