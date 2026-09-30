package set.starlev.scripting

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import set.starlev.scripting.ModuleLoader
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import java.util.concurrent.Executors

/**
 * Кастомные шрифты для JS-модулей: TTF/OTF из папки модуля -> атлас глифов
 * -> DynamicTexture в GPU (по образцу SHModuleTextures).
 *
 * ```js
 * render.loadFontFromFile("fonts/myfont.ttf", "myfont");     // один раз при загрузке
 * ctx.drawFontText("myfont", "Привет", 10, 10, 24, 0xFFFFFF, true);
 * var m = ctx.measureFontText("myfont", "Привет", 24);        // m[0]=ширина, m[1]=высота
 * render.removeFont("myfont"); // при выгрузке модуля чистится автоматически
 * ```
 *
 * Атлас печётся синхронно (единожды, ~30-60 мс), регистрация текстуры —
 * на рендер-потоке через client.execute. Отрисовка — обычный blit по глифам,
 * поэтому работает в overlay/hud/gui с трансформами и scissor без ограничений.
 */
object SHModuleFonts {
    private const val NAMESPACE = "starredheltix"
    private const val FONT_PREFIX = "module/font/"
    private const val TEXT_PREFIX = "module/text/"
    private const val MAX_FONT_BYTES = 4L * 1024 * 1024
    private const val MAX_ATLAS_DIM = 4096
    private const val DEFAULT_ATLAS_SIZE = 48

    /** key: "<module>:<id>" (lower) -> атлас. */
    private val registered = ConcurrentHashMap<String, FontAtlas>()

    /** Глобальный индекс по простому id (последний загруженный побеждает). */
    private val byPlainId = ConcurrentHashMap<String, FontAtlas>()

    /**
     * Метрики текста для JS (bean — Rhino запрещает доступ к примитивным массивам "[I").
     * Использование из JS: m.width, m.height.
     */
    class Metrics(val width: Int, val height: Int)

    class Glyph internal constructor(
        val u: Float, val v: Float,   // uv в ТЕКСЕЛЯХ атласа (blit сам делит на texW/texH)
        val w: Int, val h: Int,       // размер ячейки глифа (px, базовый размер)
        val advance: Int              // продвижение курсора (px)
    )

    class FontAtlas internal constructor(
        val textureId: Identifier,
        val texW: Int, val texH: Int,
        val baseSize: Int,            // кегль, в котором запечён атлас
        val cellW: Int, val cellH: Int,
        val lineHeightPx: Int,
        val glyphs: Map<Char, Glyph>,
        internal val awtFont: java.awt.Font   // для textToTexture (растеризации строк)
    )

    private fun nextPow2(v: Int): Int {
        var p = 64
        while (p < v) p = p shl 1
        return p
    }

    private fun charset(): CharArray {
        val list = ArrayList<Char>(256)
        for (c in ' '..'~') list.add(c)                       // ASCII
        for (c in 0x0410..0x044F) list.add(c.toChar())        // А-я
        list.add('Ё'); list.add('ё')
        for (code in intArrayOf(0x00AB, 0x00BB, 0x2013, 0x2014, 0x2026, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x00B7)) {
            list.add(code.toChar())
        }
        return list.toCharArray()
    }

    /**
     * Загрузить шрифт из папки модуля и запечь атлас глифов.
     * @param fileName путь внутри папки модуля (TTF/OTF)
     * @param id идентификатор шрифта (для ctx.drawFontText / measureFontText)
     * @param sizePx базовый кегль атласа (48 = хорошее качество до ~2x масштаба)
     */
    @JvmOverloads
    fun loadFromFile(moduleName: String, fileName: String, id: String, sizePx: Int = DEFAULT_ATLAS_SIZE): Boolean {
        val folder: File = ModuleLoader.findModuleFolder(moduleName)
            ?: run { ScriptConsole.error(moduleName, "loadFontFromFile: папка модуля не найдена"); return false }
        val file: File = try {
            val f = File(folder, fileName)
            if (!f.canonicalFile.path.startsWith(folder.canonicalFile.path)) {
                ScriptConsole.error(moduleName, "loadFontFromFile('$fileName'): доступ за пределами папки модуля запрещён")
                return false
            }
            f
        } catch (e: Exception) {
            ScriptConsole.error(moduleName, "loadFontFromFile('$fileName'): ${e.message}")
            return false
        }
        if (!file.exists() || !file.isFile) {
            ScriptConsole.error(moduleName, "loadFontFromFile('$fileName'): файл шрифта не найден")
            return false
        }
        if (file.length() > MAX_FONT_BYTES) {
            ScriptConsole.error(moduleName, "loadFontFromFile('$fileName'): шрифт больше ${MAX_FONT_BYTES / 1024 / 1024} МБ")
            return false
        }
        if (id.isBlank() || id.contains('/') || id.contains("..")) {
            ScriptConsole.error(moduleName, "loadFontFromFile: некорректный id")
            return false
        }
        val key = "${moduleName.lowercase()}:${id.lowercase()}"
        removeFont(moduleName, id) // перезагрузка с тем же id заменяет

        return try {
            val awtBase = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, file)
            val awtFont = awtBase.deriveFont(sizePx.toFloat())
            val probe = BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB)
            val pg = probe.graphics as Graphics2D
            val fm = pg.getFontMetrics(awtFont)
            val ascent = fm.maxAscent.coerceAtLeast(1)
            val descent = fm.maxDescent.coerceAtLeast(1)
            var maxAdvance = 4
            for (c in charset()) maxAdvance = maxOf(maxAdvance, fm.charWidth(c))
            pg.dispose()

            val cellW = maxAdvance + 2
            val cellH = ascent + descent + 2
            val chars = charset()
            val cols = 16
            val rows = (chars.size + cols - 1) / cols
            val texW = nextPow2(cols * cellW).coerceAtMost(MAX_ATLAS_DIM)
            val texH = nextPow2(rows * cellH).coerceAtMost(MAX_ATLAS_DIM)

            val img = BufferedImage(texW, texH, BufferedImage.TYPE_INT_ARGB)
            val g = img.graphics as Graphics2D
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.font = awtFont
            g.color = java.awt.Color(255, 255, 255, 255)

            val glyphs = HashMap<Char, Glyph>(chars.size)
            for (i in chars.indices) {
                val col = i % cols
                val row = i / cols
                if ((row + 1) * cellH > texH) break // лимит атласа
                val ox = col * cellW + 1
                val oy = row * cellH + 1
                val c = chars[i]
                g.drawString(c.toString(), ox, oy + ascent)
                val advance = if (c == ' ') (sizePx / 3).coerceAtLeast(4) else fm.charWidth(c)
                // u/v — пиксельные координаты в атласе (blit нормализует на texW/texH)
                glyphs[c] = Glyph(ox.toFloat(), oy.toFloat(), cellW, cellH, advance)
            }
            g.dispose()

            val bos = ByteArrayOutputStream()
            ImageIO.write(img, "png", bos)
            val bytes = bos.toByteArray()
            val atlas = FontAtlas(
                Identifier.fromNamespaceAndPath(NAMESPACE, FONT_PREFIX + moduleName.lowercase() + "/" + id.lowercase()),
                texW, texH, sizePx, cellW, cellH, cellH, glyphs, awtFont
            )
            // Карты обновляем СИНХРОННО: скрипт может обратиться к шрифту сразу после
            // loadFontFromFile, а текстура регистрируется на рендер-потоке чуть позже.
            registered[key] = atlas
            byPlainId[id.lowercase()] = atlas
            Minecraft.getInstance().execute {
                try {
                    val tm = Minecraft.getInstance().textureManager
                    val dynamic = DynamicTexture({ "SHModuleFont/$key" }, com.mojang.blaze3d.platform.NativeImage.read(bytes))
                    tm.register(atlas.textureId, dynamic)
                } catch (e: Exception) {
                    ScriptConsole.error(moduleName, "loadFontFromFile('$id'): регистрация не удалась: ${e.message}")
                }
            }
            ScriptConsole.info(moduleName, "Шрифт '$id' загружен (${chars.size} глифов, атлас ${texW}x${texH})")
            true
        } catch (e: Exception) {
            ScriptConsole.error(moduleName, "loadFontFromFile('$fileName'): не удалось прочитать TTF/OTF: ${e.message}")
            false
        }
    }

    /**
     * Отрисовать строку кастомным шрифтом. Блит по глифам, работает в любом 2D-контексте.
     * @param blit колбэк отрисовки (graphics.blit конкретного контекста)
     */
    fun drawText(
        fontId: String, text: String, x: Int, y: Int, size: Int, color: Int, shadow: Boolean,
        blit: (Identifier, Int, Int, Float, Float, Int, Int, Int, Int, Int) -> Unit
    ): Boolean {
        val atlas = byPlainId[fontId.lowercase()] ?: return false
        val scale = size.toDouble() / atlas.baseSize
        val wScaled = Math.max(1, Math.round(atlas.cellW * scale)).toInt()
        val hScaled = Math.max(1, Math.round(atlas.cellH * scale)).toInt()
        val alpha = (color ushr 24) and 0xFF
        val shadowColor = alpha shl 24 // чёрная тень с той же альфой
        var cursor = x
        for (c in text) {
            val gl = atlas.glyphs[c] ?: atlas.glyphs['?'] ?: continue
            if (shadow) {
                blit(atlas.textureId, cursor + 1, y + 1, gl.u, gl.v, wScaled, hScaled, atlas.texW, atlas.texH, shadowColor)
            }
            blit(atlas.textureId, cursor, y, gl.u, gl.v, wScaled, hScaled, atlas.texW, atlas.texH, color)
            cursor += Math.max(1, Math.round(gl.advance * scale)).toInt()
        }
        return true
    }

    /** Размер строки кастомным шрифтом: Metrics(width, height) или null (шрифт не загружен). */
    fun measureText(fontId: String, text: String, size: Int): Metrics? {
        val atlas = byPlainId[fontId.lowercase()] ?: return null
        val scale = size.toDouble() / atlas.baseSize
        var w = 0
        for (c in text) {
            val gl = atlas.glyphs[c] ?: atlas.glyphs['?'] ?: continue
            w += Math.max(1, Math.round(gl.advance * scale)).toInt()
        }
        val h = Math.max(1, Math.round(atlas.cellH * scale)).toInt()
        return Metrics(w, h)
    }

    /**
     * Растеризовать ЦЕЛУЮ строку в отдельную текстуру (для статичных надписей HUD).
     * Рисуется через ctx.drawTexture("starredheltix:module/<модуль>/text/<id>", x, y, w, h).
     * @return Metrics(width, height) или null при ошибке
     */
    @JvmOverloads
    fun textToTexture(
        moduleName: String, text: String, fontId: String, id: String,
        size: Int = 24, color: Int = 0xFFFFFF, shadow: Boolean = false, padding: Int = 2
    ): Metrics? {
        val atlas = byPlainId[fontId.lowercase()] ?: run {
            ScriptConsole.error(moduleName, "textToTexture: шрифт '$fontId' не загружен")
            return null
        }
        if (id.isBlank() || id.contains('/') || id.contains("..")) return null
        val key = "${moduleName.lowercase()}:text:${id.lowercase()}"
        return try {
            val awtFont = atlas.awtFont.deriveFont(size.toFloat())
            val probe = BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB)
            val pg = probe.graphics as Graphics2D
            pg.font = awtFont
            val fm = pg.fontMetrics
            pg.dispose()
            val w = fm.stringWidth(text) + padding * 2 + 2
            val h = fm.maxAscent + fm.maxDescent + padding * 2
            val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            val g = img.graphics as Graphics2D
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.font = awtFont
            if (shadow) {
                g.color = java.awt.Color(0, 0, 0, (color ushr 24) and 0xFF)
                g.drawString(text, padding + 2, padding + fm.maxAscent)
            }
            g.color = java.awt.Color(color, true)
            g.drawString(text, padding, padding + fm.maxAscent)
            g.dispose()
            val bos = ByteArrayOutputStream()
            ImageIO.write(img, "png", bos)
            val bytes = bos.toByteArray()
            Minecraft.getInstance().execute {
                try {
                    val textureId = Identifier.fromNamespaceAndPath(NAMESPACE, TEXT_PREFIX + moduleName.lowercase() + "/" + id.lowercase())
                    val tm = Minecraft.getInstance().textureManager
                    registered[key]?.let { old ->
                        tm.getTexture(old.textureId).close()
                        tm.release(old.textureId)
                    }
                    val dynamic = DynamicTexture({ "SHModuleText/$key" }, com.mojang.blaze3d.platform.NativeImage.read(bytes))
                    tm.register(textureId, dynamic)
                    registered[key] = FontAtlas(textureId, w, h, size, w, h, h, emptyMap(), awtFont)
                } catch (e: Exception) {
                    ScriptConsole.error(moduleName, "textToTexture('$id'): регистрация не удалась: ${e.message}")
                }
            }
            Metrics(w, h)
        } catch (e: Exception) {
            ScriptConsole.error(moduleName, "textToTexture('$id'): ${e.message}")
            null
        }
    }

    /** Удалить шрифт или текстовую текстуру модуля. */
    fun removeFont(moduleName: String, id: String) {
        val key = "${moduleName.lowercase()}:${id.lowercase()}"
        val atlas = registered.remove(key) ?: return
        byPlainId.entries.removeIf { it.value === atlas }
        Minecraft.getInstance().execute {
            try {
                val tm = Minecraft.getInstance().textureManager
                tm.getTexture(atlas.textureId).close()
                tm.release(atlas.textureId)
            } catch (_: Exception) {
            }
        }
    }

    /** Удалить ВСЕ шрифты и текстовые текстуры модуля (при выгрузке). */
    fun removeForModule(moduleName: String) {
        val prefix = "${moduleName.lowercase()}:"
        registered.keys.filter { it.startsWith(prefix) }.toList().forEach { key ->
            removeFont(moduleName, key.removePrefix(prefix))
        }
    }

    /** Удалить шрифты всех модулей (выгрузка движка). */
    fun clearAll() {
        registered.keys.toList().forEach { key ->
            removeFont(key.substringBefore(':'), key.substringAfter(':'))
        }
    }
}
