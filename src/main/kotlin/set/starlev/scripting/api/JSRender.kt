package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.mozilla.javascript.Callable
import set.starlev.render.RenderContext
import set.starlev.render.drawString
import set.starlev.render.drawCenteredString
import set.starlev.scripting.SHWorldRenderBatches

/**
 * JS API: Объект `render` — менеджер персистентных батчей мировых примитивов.
 *
 * Тяжёлые рендер-системы: наполни батч ОДИН РАЗ (waypoints, ESP, зоны),
 * а ядро рисует его каждый кадр нативно, без запуска JS в hot-path.
 *
 * ```js
 * var b = render.batch("waypoints");
 * b.addBox(100, 64, -200, 1, 1, 1, 0x80FF0000, true, true);
 * b.addBeacon(100, 64, -200, 256, 0xFF0000);
 * // при изменении данных:
 * b.clear(); b.addBox(...); ...
 * render.removeBatch("waypoints");
 * ```
 */
class JSRenderApi(private val moduleName: String) {

    fun batch(id: String): JSBatchHandle =
        JSBatchHandle(set.starlev.scripting.SHWorldRenderBatches.getOrCreate(id, moduleName))

    fun removeBatch(id: String) {
        set.starlev.scripting.SHWorldRenderBatches.remove(moduleName, id)
    }

    /** Удалить все батчи модуля. */
    fun clearBatches() {
        set.starlev.scripting.SHWorldRenderBatches.removeForModule(moduleName)
    }

    // ==================== Динамические текстуры по URL ====================

    /**
     * Скачать изображение по URL и загрузить в GPU (асинхронно).
     * После загрузки рисуется через ctx.drawTexture("starredheltix:module/<модуль>/<id>", ...).
     * Повторный вызов с тем же id заменяет текстуру. PNG/JPEG до 8 МБ.
     */
    fun loadImage(url: String, id: String): Boolean =
        set.starlev.scripting.SHModuleTextures.loadImage(moduleName, url, id)

    /**
     * Загрузить изображение из файла в папке модуля (PNG/JPEG до 8 МБ).
     * После загрузки рисуется через ctx.drawTexture("starredheltix:module/<модуль>/<id>", ...).
     */
    fun loadImageFromFile(fileName: String, id: String): Boolean =
        set.starlev.scripting.SHModuleTextures.loadImageFromFile(moduleName, fileName, id)

    /** Удалить загруженную текстуру модуля. */
    fun removeImage(id: String) {
        set.starlev.scripting.SHModuleTextures.removeImage(moduleName, id)
    }

    /** Удалить все текстуры модуля (автоматически при выгрузке модуля). */
    fun clearImages() {
        set.starlev.scripting.SHModuleTextures.removeForModule(moduleName)
    }

    // ==================== Кастомные шрифты (TTF/OTF из папки модуля) ====================

    /**
     * Загрузить шрифт из папки модуля и запечь атлас глифов (ASCII + кириллица).
     * После загрузки рисуется: ctx.drawFontText("myfont", "текст", x, y, size, color, shadow).
     * @param sizePx базовый кегль атласа (48 = качество до ~2x масштаба)
     */
    @JvmOverloads
    fun loadFontFromFile(fileName: String, id: String, sizePx: Int = 48): Boolean =
        set.starlev.scripting.SHModuleFonts.loadFromFile(moduleName, fileName, id, sizePx)

    /** Удалить шрифт модуля. */
    fun removeFont(id: String) {
        set.starlev.scripting.SHModuleFonts.removeFont(moduleName, id)
    }

    /** Удалить все шрифты модуля (автоматически при выгрузке модуля). */
    fun clearFonts() {
        set.starlev.scripting.SHModuleFonts.removeForModule(moduleName)
    }

    /**
     * Растеризовать целую строку в текстуру (для статичных надписей):
     * ctx.drawTexture("starredheltix:module/<модуль>/text/<id>", x, y, w, h).
     * @return SHModuleFonts.Metrics (m.width, m.height) или null
     */
    @JvmOverloads
    fun textToTexture(
        text: String, id: String, fontId: String,
        size: Int = 24, color: Double = 0xFFFFFF.toDouble(), shadow: Boolean = false
    ): set.starlev.scripting.SHModuleFonts.Metrics? = set.starlev.scripting.SHModuleFonts.textToTexture(
        moduleName, text, fontId, id, size, color.toInt(), shadow
    )
}

/**
 * Хэндл персистентного батча. Все координаты — мировые (в блоках).
 */
class JSBatchHandle internal constructor(private val batch: set.starlev.scripting.SHRenderBatch) {

    @JvmOverloads
    fun addBox(
        x: Double, y: Double, z: Double,
        w: Double, h: Double, d: Double,
        color: Double = 0x55FF5555.toDouble(),
        filled: Boolean = false,
        throughBlocks: Boolean = true,
        thickness: Float = 0f
    ) {
        // Double + toInt(): JS-литералы 0xAARRGGBB с альфой >= 0x80 не помещаются в Int,
        // Rhino не смог бы сконвертировать их напрямую в java.lang.Integer.
        batch.addBoxPrim(set.starlev.scripting.SHRenderBatch.BoxPrim(x, y, z, w, h, d, color.toInt(), filled, throughBlocks, thickness))
    }

    @JvmOverloads
    fun addLine(
        x1: Double, y1: Double, z1: Double,
        x2: Double, y2: Double, z2: Double,
        color: Double = 0xFFFFFF.toDouble(),
        thickness: Float = 2f
    ) {
        batch.addLinePrim(set.starlev.scripting.SHRenderBatch.LinePrim(x1, y1, z1, x2, y2, z2, color.toInt(), thickness))
    }

    @JvmOverloads
    fun addText(
        text: String,
        x: Double, y: Double, z: Double,
        scale: Float = 1f,
        color: Double = 0xFFFFFF.toDouble(),
        shadow: Boolean = true,
        seeThrough: Boolean = true
    ) {
        batch.addTextPrim(set.starlev.scripting.SHRenderBatch.TextPrim(text, x, y, z, scale, color.toInt(), shadow, seeThrough))
    }

    @JvmOverloads
    fun addBeacon(x: Double, y: Double, z: Double, height: Int = 256, color: Double = 0xFF0000.toDouble()) {
        batch.addBeaconPrim(set.starlev.scripting.SHRenderBatch.BeaconPrim(x, y, z, height, color.toInt()))
    }

    /** Круг в мире: axis "y" — горизонтальный (XZ), "x"/"z" — вертикальные. */
    @JvmOverloads
    fun addCircle(
        x: Double, y: Double, z: Double,
        radius: Double, color: Double = 0xFFFFFF.toDouble(),
        thickness: Float = 2f, segments: Int = 32, axis: String = "y"
    ) {
        batch.addCirclePrim(set.starlev.scripting.SHRenderBatch.CirclePrim(x, y, z, radius, color.toInt(), thickness, segments, axis))
    }

    /**
     * Замкнутый полигон в мире по точкам: координаты тройками (x1,y1,z1, x2,y2,z2, ...).
     * Рисуется линиями по периметру. Минимум 3 точки.
     */
    fun addPolygon(vararg coords: Double) {
        if (coords.size < 9 || coords.size % 3 != 0) {
            set.starlev.scripting.ScriptConsole.warn(
                batch.ownerModule,
                "batch.addPolygon: нужно минимум 3 точки (9 координат), тройками x,y,z — получено ${coords.size}"
            )
            return
        }
        val count = coords.size / 3
        for (i in 0 until count) {
            val j = (i + 1) % count
            addLine(coords[i * 3], coords[i * 3 + 1], coords[i * 3 + 2], coords[j * 3], coords[j * 3 + 1], coords[j * 3 + 2])
        }
    }

    @JvmOverloads
    fun addImage(
        texture: String,
        x: Double, y: Double, z: Double,
        width: Float, height: Float,
        rotationY: Float = 0f,
        throughBlocks: Boolean = false
    ) {
        batch.addImagePrim(
            set.starlev.scripting.SHRenderBatch.ImagePrim(Identifier.parse(texture), x, y, z, width, height, rotationY, throughBlocks)
        )
    }

    fun clear() {
        batch.clearAllPrimitives()
    }

    fun getSize(): Int = batch.size
}

/**
 * JS API: 3D Render Context обёртка для `events.onWorldRender(function(ctx) { ... })`
 */
class JSRenderContext(private val nativeCtx: RenderContext) {

    /** Позиция камеры (для LOD/каллинга решений). */
    val cameraX: Double get() = nativeCtx.cameraPos.x
    val cameraY: Double get() = nativeCtx.cameraPos.y
    val cameraZ: Double get() = nativeCtx.cameraPos.z

    /** Проверка фрустума: попадает ли бокс в кадр (для LOD-каллинга). */
    fun isVisible(x: Double, y: Double, z: Double, w: Double, h: Double, d: Double): Boolean =
        nativeCtx.isBoxVisible(AABB(x, y, z, x + w, y + h, z + d))

    /** Дистанция от камеры до точки (квадрат — без sqrt, для сравнения). */
    fun distanceSq(x: Double, y: Double, z: Double): Double {
        val dx = x - nativeCtx.cameraPos.x
        val dy = y - nativeCtx.cameraPos.y
        val dz = z - nativeCtx.cameraPos.z
        return dx * dx + dy * dy + dz * dz
    }

    /** Доля текущего тика (0..1) — для интерполяции позиций сущностей: entity.getRenderX(ctx.tickDelta). */
    val tickDelta: Float get() = nativeCtx.tickDelta

    @JvmOverloads
    fun drawBox(
        x: Double, y: Double, z: Double,
        w: Double = 1.0, h: Double = 1.0, d: Double = 1.0,
        colorHex: Double = 0x5500FF00.toDouble(),
        fill: Boolean = false,
        throughBlocks: Boolean = false,
        thickness: Float = 0f
    ) {
        val color = colorHex.toInt()
        val a = ((color ushr 24) and 0xFF) / 255f
        val r = ((color ushr 16) and 0xFF) / 255f
        val g = ((color ushr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val actualA = if (a == 0f && color > 0) 1f else a

        val aabb = AABB(x, y, z, x + w, y + h, z + d)
        if (throughBlocks) {
            nativeCtx.renderBoxThroughBlocks(aabb, r, g, b, actualA, fill, thickness)
        } else {
            nativeCtx.renderBox(aabb, r, g, b, actualA, fill, thickness)
        }
    }

    @JvmOverloads
    fun drawText(
        text: String,
        x: Double, y: Double, z: Double,
        scale: Float = 1.0f,
        color: Double = 0xFFFFFF.toDouble(),
        shadow: Boolean = true,
        seeThrough: Boolean = true
    ) {
        nativeCtx.renderText(text, x, y, z, scale, color.toInt(), shadow, seeThrough)
    }

    @JvmOverloads
    fun drawLine(
        x1: Double, y1: Double, z1: Double,
        x2: Double, y2: Double, z2: Double,
        color: Double = 0xFFFFFF.toDouble(),
        thickness: Float = 2.0f
    ) {
        nativeCtx.renderLine(Vec3(x1, y1, z1), Vec3(x2, y2, z2), color.toInt(), thickness)
    }

    @JvmOverloads
    fun drawBeacon(
        x: Double, y: Double, z: Double,
        height: Int = 256,
        color: Double = 0x00FF00.toDouble()
    ) {
        nativeCtx.renderBeaconBeam(x, y, z, height, color.toInt(), nativeCtx.tickDelta)
    }

    /** Круг в мире: axis "y" — горизонтальный (XZ), "x"/"z" — вертикальные плоскости. */
    @JvmOverloads
    fun drawCircle(
        x: Double, y: Double, z: Double,
        radius: Double, color: Double = 0xFFFFFF.toDouble(),
        thickness: Float = 2f, segments: Int = 32, axis: String = "y"
    ) {
        nativeCtx.renderCircle(x, y, z, radius, color.toInt(), thickness, segments, axis)
    }

    /** Текстурированная картинка в мире (текстура — через render.loadImage / loadImageFromFile).
     *  throughBlocks=true — картинка видна сквозь стены (без depth-test). */
    @JvmOverloads
    fun drawImage(
        texture: String, x: Double, y: Double, z: Double,
        width: Float, height: Float, rotationY: Float = 0f,
        throughBlocks: Boolean = false
    ) {
        nativeCtx.renderImage(net.minecraft.resources.Identifier.parse(texture), x, y, z, width, height, rotationY, throughBlocks)
    }

    /** Анимированный спрайт-лист: columns×rows кадров, fps кадров в секунду.
     *  throughBlocks=true — картинка видна сквозь стены (без depth-test). */
    @JvmOverloads
    fun drawAnimatedImage(
        texture: String, x: Double, y: Double, z: Double,
        width: Float, height: Float, rotationY: Float = 0f,
        columns: Int = 1, rows: Int = 1, fps: Float = 20f,
        throughBlocks: Boolean = false
    ) {
        nativeCtx.renderAnimatedImage(
            net.minecraft.resources.Identifier.parse(texture), x, y, z, width, height, rotationY, columns, rows, fps, throughBlocks
        )
    }
}

/**
 * JS API: 2D overlay-контекст для `events.onOverlay(function(ctx) { ... })`.
 * Отрисовка поверх HUD/экранов: текст, прямоугольники, текстуры, градиенты,
 * scissor-области, матричные трансформы, предметы.
 */
class JSOverlayContext(private val graphics: GuiGraphicsExtractor) {
    private val font get() = Minecraft.getInstance().font
    val screenWidth: Int get() = graphics.guiWidth()
    val screenHeight: Int get() = graphics.guiHeight()

    private fun colorize(text: String): String = text.replace('&', '§')

    /**
     * 26.2 отбрасывает текст с alpha=0 (проверка `ARGB.alpha(color) != 0` в GuiGraphicsExtractor.text).
     * Цвет без альфа-байта (например, дефолтный 0xFFFFFF) трактуем как непрозрачный.
     */
    private fun opaqueText(color: Int): Int =
        if (color and 0xFF000000.toInt() == 0) color or 0xFF000000.toInt() else color

    @JvmOverloads
    fun drawString(text: String, x: Int, y: Int, color: Double = 0xFFFFFF.toDouble(), shadow: Boolean = true) {
        graphics.drawString(font, colorize(text), x, y, opaqueText(color.toInt()), shadow)
    }

    @JvmOverloads
    fun drawCenteredString(text: String, x: Int, y: Int, color: Double = 0xFFFFFF.toDouble(), shadow: Boolean = true) {
        graphics.drawCenteredString(font, colorize(text), x, y, opaqueText(color.toInt()), shadow)
    }

    fun drawRect(x: Int, y: Int, width: Int, height: Int, color: Double) {
        graphics.fill(x, y, x + width, y + height, color.toInt())
    }

    /**
     * Заливка круга (сегментами-столбиками, без сглаживания).
     * Полезно для маркеров, точек, кулдаунов.
     */
    @JvmOverloads
    fun drawCircle(centerX: Int, centerY: Int, radius: Int, color: Double, outlineColor: Double = color) {
        val col = color.toInt()
        val outline = outlineColor.toInt()
        if (radius <= 0) return
        val r2 = radius.toDouble() * radius
        for (dx in -radius..radius) {
            val half = kotlin.math.sqrt(r2 - dx * dx).toInt()
            if (dx * dx > r2) continue
            graphics.fill(centerX + dx, centerY - half, centerX + dx + 1, centerY + half + 1, col)
        }
        // Тонкая окружность сверху (точки по периметру)
        val steps = (radius * 8).coerceIn(16, 256)
        for (i in 0 until steps) {
            val a = 2.0 * Math.PI * i / steps
            val px = centerX + (radius * kotlin.math.cos(a)).toInt()
            val py = centerY + (radius * kotlin.math.sin(a)).toInt()
            graphics.fill(px, py, px + 1, py + 1, outline)
        }
    }

    /** Вертикальный градиент между двумя цветами. */
    fun drawGradientRect(x: Int, y: Int, width: Int, height: Int, colorTop: Double, colorBottom: Double) {
        graphics.fillGradient(x, y, x + width, y + height, colorTop.toInt(), colorBottom.toInt())
    }

    /** Горизонтальная линия толщиной 1px. */
    fun drawHLine(x1: Int, x2: Int, y: Int, color: Double) {
        graphics.fill(minOf(x1, x2), y, maxOf(x1, x2), y + 1, color.toInt())
    }

    /** Вертикальная линия толщиной 1px. */
    fun drawVLine(x: Int, y1: Int, y2: Int, color: Double) {
        graphics.fill(x, minOf(y1, y2), x + 1, maxOf(y1, y2), color.toInt())
    }

    /** Полая рамка толщиной 1px. */
    fun drawBorder(x: Int, y: Int, width: Int, height: Int, color: Double) {
        val col = color.toInt()
        graphics.fill(x, y, x + width, y + 1, col)
        graphics.fill(x, y + height - 1, x + width, y + height, col)
        graphics.fill(x, y, x + 1, y + height, col)
        graphics.fill(x + width - 1, y, x + width, y + height, col)
    }

    /**
     * Нарисовать PNG-текстуру (например, из assets модуля: "starredheltix:gui/panel.png").
     * uv-координаты позволяют вырезать часть текстуры (спрайт-листы).
     */
    @JvmOverloads
    fun drawTexture(
        texture: String,
        x: Int, y: Int, width: Int, height: Int,
        u: Float = 0f, v: Float = 0f,
        textureWidth: Int = 256, textureHeight: Int = 256,
        color: Double = 0xFFFFFF.toDouble()
    ) {
        val id = Identifier.parse(texture)
        graphics.blit(
            RenderPipelines.GUI_TEXTURED, id,
            x, y, u, v, width, height, textureWidth, textureHeight, color.toInt()
        )
    }

    /**
     * Текст кастомным шрифтом (render.loadFontFromFile). Блит по глифам —
     * работает с трансформами и scissor. Цвет ARGB; §-коды не поддерживаются
     * (кастомный шрифт красится целиком через color).
     */
    @JvmOverloads
    fun drawFontText(
        fontId: String, text: String, x: Int, y: Int,
        size: Int = 24, color: Double = 0xFFFFFF.toDouble(), shadow: Boolean = false
    ): Boolean {
        return set.starlev.scripting.SHModuleFonts.drawText(
            fontId, text, x, y, size, color.toInt(), shadow
        ) { tid, dx, dy, u, v, w, h, tw, th, col ->
            graphics.blit(RenderPipelines.GUI_TEXTURED, tid, dx, dy, u, v, w, h, tw, th, col)
        }
    }

    /** Ширина/высота строки кастомным шрифтом: SHModuleFonts.Metrics (m.width, m.height) или null. */
    fun measureFontText(fontId: String, text: String, size: Int = 24): set.starlev.scripting.SHModuleFonts.Metrics? {
        return set.starlev.scripting.SHModuleFonts.measureText(fontId, text, size)
    }

    // ==================== Scissor (отсечение — панели со скроллом) ====================

    /** Включить отсечение рисования внутри прямоугольника. */
    fun pushClip(x: Int, y: Int, width: Int, height: Int) {
        graphics.enableScissor(x, y, x + width, y + height)
    }

    fun popClip() {
        graphics.disableScissor()
    }

    // ==================== Матричные трансформы (анимации, повороты, зум) ====================

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

    /** Поворот вокруг точки (для анимированных элементов). */
    fun rotateAround(angleRad: Float, px: Float, py: Float) {
        val pose = graphics.pose()
        pose.translate(px, py)
        pose.rotate(angleRad)
        pose.translate(-px, -py)
    }

    // ==================== Предметы ====================

    /** Нарисовать предмет по идентификатору, например "minecraft:diamond". */
    fun drawItem(itemId: String, x: Int, y: Int) {
        val id = Identifier.parse(itemId)
        val item = BuiltInRegistries.ITEM.getValue(id)
        if (item === net.minecraft.world.item.Items.AIR) return
        graphics.item(ItemStack(item), x, y)
    }

    /** Текст количества рядом с предметом (как в инвентаре). */
    fun drawItemWithCount(itemId: String, x: Int, y: Int, count: Int) {
        val id = Identifier.parse(itemId)
        val item = BuiltInRegistries.ITEM.getValue(id)
        if (item === net.minecraft.world.item.Items.AIR) return
        val stack = ItemStack(item, count)
        graphics.item(stack, x, y)
        graphics.itemDecorations(font, stack, x, y)
    }
}
