package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import org.mozilla.javascript.NativeObject
import java.util.concurrent.CopyOnWriteArrayList
import set.starlev.scripting.ScriptConsole

/**
 * JS API: Display / Text / Image — легковесные 2D примитивы.
 * Только Display/Text/Image, без полного Renderer.begin (Фаза 2).
 * Рендер через персистентный overlay (events.onOverlay) — 1 draw вызов на Display, без JS в hot-path лишнего.
 */
class JSText private constructor() {
    private var string: String = ""
    private var x: Int = 0
    private var y: Int = 0
    private var color: Long = 0xffffffff
    private var backgroundColor: Long = 0xff000000
    private var formatted: Boolean = true
    private var shadow: Boolean = false
    private var align: Align = Align.LEFT
    private var background: Boolean = false
    private var width: Int = 0
    private var maxWidth: Int = 0
    private var maxLines: Int = Int.MAX_VALUE
    private var scale: Float = 1f
    private val lines = mutableListOf<String>()

    constructor(string: String, x: Int = 0, y: Int = 0) : this() {
        setString(string); setX(x); setY(y)
    }
    constructor(string: String, config: NativeObject?) : this() {
        setString(string)
        if (config != null) {
            (config.get("color", config) as? Number)?.let { setColor(it.toLong()) }
            (config.get("formatted", config) as? Boolean)?.let { setFormatted(it) }
            (config.get("shadow", config) as? Boolean)?.let { setShadow(it) }
            (config.get("align", config) as? String)?.let { setAlign(it) }
            (config.get("background", config) as? Boolean)?.let { setBackground(it) }
            (config.get("backgroundColor", config) as? Number)?.let { setBackgroundColor(it.toLong()) }
            (config.get("x", config) as? Number)?.let { setX(it.toInt()) }
            (config.get("y", config) as? Number)?.let { setY(it.toInt()) }
            (config.get("maxLines", config) as? Number)?.let { setMaxLines(it.toInt()) }
            (config.get("scale", config) as? Number)?.let { setScale(it.toFloat()) }
            (config.get("maxWidth", config) as? Number)?.let { setMaxWidth(it.toInt()) }
        }
    }

    fun getString(): String = string
    fun setString(s: String): JSText { string = s; updateFormatting(); return this }
    fun getColor(): Long = color
    fun setColor(c: Long): JSText { color = c; return this }
    fun getFormatted(): Boolean = formatted
    fun setFormatted(f: Boolean): JSText { formatted = f; updateFormatting(); return this }
    fun getShadow(): Boolean = shadow
    fun setShadow(s: Boolean): JSText { shadow = s; return this }
    fun getAlign(): Align = align
    fun setAlign(a: Any): JSText { align = when(a){ is CharSequence -> try{ Align.valueOf(a.toString().uppercase()) }catch(_:Exception){ Align.LEFT }; is Align -> a; else -> Align.LEFT }; return this }
    fun getBackground(): Boolean = background
    fun setBackground(b: Boolean): JSText { background = b; return this }
    fun getBackgroundColor(): Long = backgroundColor
    fun setBackgroundColor(c: Long): JSText { backgroundColor = c; return this }
    fun getX(): Int = x
    fun setX(v: Int): JSText { x = v; return this }
    fun getY(): Int = y
    fun setY(v: Int): JSText { y = v; return this }
    fun getWidth(): Int = width
    fun getMaxWidth(): Int = maxWidth
    fun setMaxWidth(v: Int): JSText { maxWidth = v; updateFormatting(); return this }
    fun getMaxLines(): Int = maxLines
    fun setMaxLines(v: Int): JSText { maxLines = v; return this }
    fun getScale(): Float = scale
    fun setScale(v: Float): JSText { scale = v; return this }
    fun getHeight(): Float = if (lines.size > 1) lines.size.coerceAtMost(maxLines) * 10f else 10f
    fun exceedsMaxLines(): Boolean = lines.size > maxLines

    fun draw(ctx: GuiGraphicsExtractor, x: Int? = null, y: Int? = null): JSText = draw(ctx, x, y, null, null)
    internal fun draw(ctx: GuiGraphicsExtractor, x: Int?, y: Int?, backgroundX: Int?, backgroundWidth: Int?): JSText {
        ctx.pose().pushMatrix()
        ctx.pose().scale(scale, scale)
        var longest = lines.maxOfOrNull { Minecraft.getInstance().font.width(it) * scale } ?: 0f
        if (maxWidth != 0) longest = longest.coerceAtMost(maxWidth.toFloat())
        width = longest.toInt()
        var yHolder = y ?: this.y
        val xHolder = when(align){
            Align.CENTER -> (x ?: this.x) - width/2
            Align.RIGHT -> (x ?: this.x) - width
            else -> x ?: this.x
        }
        if (background) {
            val ox = backgroundX ?: xHolder
            ctx.fill(ox, yHolder, ox + (backgroundWidth ?: width), yHolder + getHeight().toInt(), backgroundColor.toInt())
        }
        for(i in 0 until maxLines) {
            if(i >= lines.size) break
            ctx.text(Minecraft.getInstance().font, lines[i], xHolder, yHolder, color.toInt(), shadow)
            yHolder += 10
        }
        ctx.pose().popMatrix()
        return this
    }
    private fun updateFormatting() {
        var s = string
        // ChatLib.addColor already handles &, but we keep simple
        s = s.replace('&','§')
        lines.clear()
        s.split("\n").forEach { line ->
            if (maxWidth > 0) {
                val split = Minecraft.getInstance().font.splitter.splitLines(line, maxWidth, net.minecraft.network.chat.Style.EMPTY).map{ it.string }
                lines.addAll(split)
            } else lines.add(line)
        }
    }
    enum class Align { LEFT, CENTER, RIGHT }
}

class JSDisplay {
    private var lines = CopyOnWriteArrayList<JSText>()
    private var x = 0; private var y = 0
    private var order = Order.NORMAL
    private var backgroundColor: Long = 0x50000000
    private var textColor: Long = 0xffffffff
    private var background = Background.NONE
    private var align = JSText.Align.LEFT
    private var minWidth = 0
    private var width = 0; private var height = 0
    /** Модуль-владелец (для автоматической очистки при выгрузке). null — вне модуля. */
    val owner: String? = set.starlev.scripting.SHScriptEngine.currentModule

    constructor() {
        DisplayManager.register(this)
    }
    constructor(config: NativeObject?) : this() {
        if (config != null) {
            (config.get("backgroundColor", config) as? Number)?.let { backgroundColor = it.toLong() }
            (config.get("textColor", config) as? Number)?.let { textColor = it.toLong() }
            (config.get("background", config) as? String)?.let { setBackground(it) }
            (config.get("align", config) as? String)?.let { setAlign(it) }
            (config.get("order", config) as? String)?.let { setOrder(it) }
            (config.get("x", config) as? Number)?.let { x = it.toInt() }
            (config.get("y", config) as? Number)?.let { y = it.toInt() }
            (config.get("minWidth", config) as? Number)?.let { minWidth = it.toInt() }
        }
    }
    fun destroy(){ DisplayManager.unregister(this) }
    fun remove(){ destroy() }

    fun getTextColor(): Long = textColor
    fun setTextColor(c: Long): JSDisplay { textColor = c; return this }
    fun getAlign(): JSText.Align = align
    fun setAlign(a: Any): JSDisplay { align = when(a){ is CharSequence -> try{ JSText.Align.valueOf(a.toString().uppercase()) }catch(_:Exception){ JSText.Align.LEFT }; is JSText.Align -> a; else -> JSText.Align.LEFT }; return this }
    fun getOrder(): Order = order
    fun setOrder(o: Any): JSDisplay { order = when(o){ is CharSequence -> try{ Order.valueOf(o.toString().uppercase()) }catch(_:Exception){ Order.NORMAL }; is Order -> o; else -> Order.NORMAL }; return this }
    fun getBackground(): Background = background
    fun setBackground(b: Any): JSDisplay { background = when(b){ is CharSequence -> try{ Background.valueOf(b.toString().uppercase().replace(" ","_")) }catch(_:Exception){ Background.NONE }; is Background -> b; else -> Background.NONE }; return this }
    fun getBackgroundColor(): Long = backgroundColor
    fun setBackgroundColor(c: Long): JSDisplay { backgroundColor = c; return this }
    fun getX(): Int = x
    fun setX(v: Int): JSDisplay { x = v; return this }
    fun getY(): Int = y
    fun setY(v: Int): JSDisplay { y = v; return this }
    fun getMinWidth(): Int = minWidth
    fun setMinWidth(v: Int): JSDisplay { minWidth = v; return this }
    fun getWidth(): Int = width
    fun getHeight(): Int = height
    fun setLine(index: Int, line: Any): JSDisplay {
        while (lines.size -1 < index) lines.add(JSText(""))
        when(line){
            is CharSequence -> lines[index].setString(line.toString())
            is JSText -> lines[index] = line
            else -> lines[index] = JSText("")
        }
        return this
    }
    fun getLine(index: Int): JSText = lines[index]
    fun getLines(): List<JSText> = lines
    fun setLines(l: MutableList<JSText>): JSDisplay { lines = CopyOnWriteArrayList(l); return this }
    fun addLine(line: Any): JSDisplay { setLine(lines.size, line); return this }
    fun addLines(vararg l: Any): JSDisplay { l.forEach{ addLine(it) }; return this }
    fun removeLine(index: Int): JSDisplay { lines.removeAt(index); return this }
    fun clearLines(): JSDisplay { lines.clear(); return this }

    fun draw(ctx: GuiGraphicsExtractor) {
        width = lines.maxOfOrNull{ it.getWidth() }?.coerceAtLeast(minWidth) ?: minWidth
        val bgW = when(background){ Background.FULL, Background.PER_LINE -> width; Background.NONE -> null }
        var curH = 0
        val linesX = when(align){ JSText.Align.CENTER -> x + width/2; JSText.Align.RIGHT -> x + width; else -> x }
        val toDraw = when(order){ Order.NORMAL -> lines; Order.REVERSED -> lines.asReversed() }
        toDraw.forEach{
            if (background == Background.FULL) it.setBackground(true).setBackgroundColor(backgroundColor)
            it.setColor(textColor).setAlign(align).draw(ctx, linesX, y+curH, x, bgW)
            curH += it.getHeight().toInt()
        }
        height = curH
    }

    fun shouldRender(): Boolean = true

    enum class Background { NONE, FULL, PER_LINE }
    enum class Order { REVERSED, NORMAL }
}

object DisplayManager {
    private val active = CopyOnWriteArrayList<JSDisplay>()
    fun register(d: JSDisplay){ if(!active.contains(d)) active.add(d) }
    fun unregister(d: JSDisplay){ active.remove(d) }
    fun clear(){ active.clear() }
    /** Удалить все Display, созданные модулем (при выгрузке). */
    fun removeForModule(moduleName: String) {
        val it = active.iterator()
        while (it.hasNext()) {
            val d = it.next()
            if (d.owner?.equals(moduleName, ignoreCase = true) == true) it.remove()
        }
    }
    fun renderAll(ctx: GuiGraphicsExtractor){
        for(d in active) d.draw(ctx)
    }
}

class JSImage private constructor(private var textureId: Identifier?, private var w: Int, private var h: Int){
    companion object {
        private val nextId = java.util.concurrent.atomic.AtomicInteger(0)
        private fun nextIdentifier(): Identifier = Identifier.fromNamespaceAndPath("starredheltix", "sh_img_${nextId.incrementAndGet()}_${System.nanoTime()}")

        /** id текстур, созданных каждым модулем (для релиза GPU-памяти при выгрузке). */
        private val ownedTextures = java.util.concurrent.ConcurrentHashMap<String, MutableSet<Identifier>>()
        /** Модуль, чей код сейчас исполняется (атрибуция новых Image). */
        private fun currentOwnerLower(): String? =
            set.starlev.scripting.SHScriptEngine.currentModule?.lowercase()

        fun fromFile(file: String): JSImage = fromFile(java.io.File(file))
        fun fromFile(file: java.io.File): JSImage {
            if(!file.exists()) throw IllegalArgumentException("File not found: ${file.absolutePath}")
            val bi = javax.imageio.ImageIO.read(file) ?: throw IllegalArgumentException("Invalid image: ${file.absolutePath}")
            return fromBufferedImage(bi)
        }
        fun fromAsset(name: String): JSImage {
            // Общая папка ассетов игрока; модульные ассеты — modulesDir/assets
            val f = java.io.File("config/starredheltix/assets", name)
            if (f.exists()) return fromFile(f)
            throw IllegalArgumentException("Asset not found: $name")
        }
        fun fromUrl(url: String, cachedName: String? = null): JSImage {
            if (cachedName != null) {
                val f = java.io.File("config/starredheltix/assets", cachedName)
                if (f.exists()) return fromFile(f)
            }
            val conn = java.net.URI(url).toURL().openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"; conn.connectTimeout = 7000; conn.readTimeout = 15000
            conn.setRequestProperty("User-Agent", "StarredHeltix")
            val bi = try {
                if (conn.responseCode !in 200..299) throw IllegalStateException("HTTP ${conn.responseCode} for $url")
                javax.imageio.ImageIO.read(conn.inputStream) ?: throw IllegalArgumentException("Invalid image URL: $url")
            } finally {
                try { conn.disconnect() } catch (_: Exception) {}
            }
            if (cachedName != null) {
                val out = java.io.File("config/starredheltix/assets", cachedName)
                out.parentFile?.mkdirs()
                javax.imageio.ImageIO.write(bi, "png", out)
            }
            return fromBufferedImage(bi)
        }
        fun fromBufferedImage(bi: java.awt.image.BufferedImage): JSImage {
            val id = nextIdentifier()
            val bytes = java.io.ByteArrayOutputStream().use { out -> javax.imageio.ImageIO.write(bi, "png", out); out.toByteArray() }
            val native = try {
                com.mojang.blaze3d.platform.NativeImage.read(bytes.inputStream())
            } catch (e: Exception) {
                throw IllegalStateException("Failed to decode image: ${e.message}", e)
            }
            try {
                val dyn = net.minecraft.client.renderer.texture.DynamicTexture({ "sh_img" }, native)
                Minecraft.getInstance().textureManager.register(id, dyn)
            } catch (e: Exception) {
                // native передан в DynamicTexture только при успехе; при падении закрываем сами.
                try { native.close() } catch (_: Exception) {}
                throw IllegalStateException("Failed to register image: ${e.message}", e)
            }
            val img = JSImage(id, bi.width, bi.height)
            currentOwnerLower()?.let { owner ->
                ownedTextures.computeIfAbsent(owner) { java.util.concurrent.ConcurrentHashMap.newKeySet() }.add(id)
            }
            return img
        }

        /** Освободить все текстуры, созданные модулем (при выгрузке). */
        fun releaseForModule(moduleName: String) {
            val ids = ownedTextures.remove(moduleName.lowercase()) ?: return
            val tm = Minecraft.getInstance().textureManager
            ids.forEach { id ->
                try { tm.getTexture(id).close() } catch (_: Exception) {}
                try { tm.release(id) } catch (_: Exception) {}
            }
        }
    }
    fun getTextureWidth(): Int = w
    fun getTextureHeight(): Int = h
    fun getTexture(): net.minecraft.client.renderer.texture.AbstractTexture? = textureId?.let{ Minecraft.getInstance().textureManager.getTexture(it) }
    fun getIdOrRegister(): Identifier = textureId ?: throw IllegalStateException("Image destroyed")
    fun draw(x: Float, y: Float, width: Float? = null, height: Float? = null): JSImage {
        val (dw, dh) = when {
            width == null && height == null -> w.toFloat() to h.toFloat()
            width == null -> height!! / (h.toFloat()/w) to height
            height == null -> width to width * (h.toFloat()/w)
            else -> width to height
        }
        ImageDrawQueue.add(this, x, y, dw, dh)
        return this
    }
    fun destroy(){
        textureId?.let{ id ->
            try{ Minecraft.getInstance().textureManager.getTexture(id).close() }catch(_:Exception){}
            try{ Minecraft.getInstance().textureManager.release(id) }catch(_:Exception){}
            ownedTextures[currentOwnerLower()]?.remove(id)
        }
        textureId = null
    }
}

object ImageDrawQueue {
    private data class Entry(val img: JSImage, val x: Float, val y: Float, val w: Float, val h: Float)
    private val queue = CopyOnWriteArrayList<Entry>()
    fun add(img: JSImage, x: Float, y: Float, w: Float, h: Float){ queue.add(Entry(img,x,y,w,h)) }
    fun renderAll(ctx: GuiGraphicsExtractor){
        for(e in queue) {
            try {
                val id = e.img.getIdOrRegister()
                ctx.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, id, e.x.toInt(), e.y.toInt(), 0f, 0f, e.w.toInt(), e.h.toInt(), e.w.toInt(), e.h.toInt())
            } catch(_:Exception){}
        }
        queue.clear()
    }
}
