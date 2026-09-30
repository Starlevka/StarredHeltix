package set.starlev.scripting

import net.minecraft.resources.Identifier
import set.starlev.render.RenderContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Персистентный батч мировых примитивов для JS-модулей.
 *
 * Модуль наполняет батч ОДИН РАЗ при изменении данных (waypoints, ESP, зоны и т.д.),
 * а ядро рисует его каждый кадр напрямую через RenderContext — без запуска JS
 * в hot-path. Это и есть система батчинга/LOD для тяжёлых рендер-систем модулей.
 */
class SHRenderBatch internal constructor(
    val id: String,
    val ownerModule: String
) {
    companion object {
        /**
         * Мягкий лимит примитивов на один батч: кривой модуль, пихающий боксы каждый tick,
         * не должен съесть ОЗУ и FPS. Превышение — warn раз в минуту, добавление игнорируется.
         */
        const val MAX_PRIMITIVES = 20_000
        private const val WARN_INTERVAL_MS = 60_000L
    }

    @Volatile
    private var lastWarnMs = 0L

    private fun checkLimit(delta: Int): Boolean {
        if (size + delta <= MAX_PRIMITIVES) return true
        val now = System.currentTimeMillis()
        if (now - lastWarnMs > WARN_INTERVAL_MS) {
            lastWarnMs = now
            ScriptConsole.warn(
                ownerModule,
                "Батч '$id' превысил лимит $MAX_PRIMITIVES примитивов (${size}). Новые примитивы игнорируются — очистите батч: render.batch(\"$id\").clear()"
            )
        }
        return false
    }

    internal data class BoxPrim(
        val x: Double, val y: Double, val z: Double,
        val w: Double, val h: Double, val d: Double,
        val color: Int, val filled: Boolean,
        val throughBlocks: Boolean, val thickness: Float
    )

    internal data class LinePrim(
        val x1: Double, val y1: Double, val z1: Double,
        val x2: Double, val y2: Double, val z2: Double,
        val color: Int, val thickness: Float
    )

    internal data class TextPrim(
        val text: String,
        val x: Double, val y: Double, val z: Double,
        val scale: Float, val color: Int,
        val shadow: Boolean, val seeThrough: Boolean
    )

    internal data class BeaconPrim(val x: Double, val y: Double, val z: Double, val height: Int, val color: Int)

    internal data class CirclePrim(
        val x: Double, val y: Double, val z: Double,
        val radius: Double, val color: Int,
        val thickness: Float, val segments: Int, val axis: String
    )

    internal data class ImagePrim(
        val texture: Identifier,
        val x: Double, val y: Double, val z: Double,
        val width: Float, val height: Float, val rotationY: Float,
        val throughBlocks: Boolean
    )

    internal val boxes = ArrayList<BoxPrim>()
    internal val lines = ArrayList<LinePrim>()
    internal val texts = ArrayList<TextPrim>()
    internal val beacons = ArrayList<BeaconPrim>()
    internal val circles = ArrayList<CirclePrim>()
    internal val images = ArrayList<ImagePrim>()

    val size: Int get() = boxes.size + lines.size + texts.size + beacons.size + circles.size + images.size

    // ==== Добавление с защитой от неограниченного роста (потокобезопасно: JS-thread vs render-thread) ====

    @Synchronized internal fun addBoxPrim(p: BoxPrim) { if (checkLimit(1)) { boxes.add(p); version++ } }
    @Synchronized internal fun addLinePrim(p: LinePrim) { if (checkLimit(1)) { lines.add(p); version++ } }
    @Synchronized internal fun addTextPrim(p: TextPrim) { if (checkLimit(1)) { texts.add(p); version++ } }
    @Synchronized internal fun addBeaconPrim(p: BeaconPrim) { if (checkLimit(1)) { beacons.add(p); version++ } }
    @Synchronized internal fun addCirclePrim(p: CirclePrim) { if (checkLimit(1)) { circles.add(p); version++ } }
    @Synchronized internal fun addImagePrim(p: ImagePrim) { if (checkLimit(1)) { images.add(p); version++ } }

    @Synchronized internal fun clearAllPrimitives() {
        boxes.clear(); lines.clear(); texts.clear(); beacons.clear(); circles.clear(); images.clear()
        version++
    }

    // ==== Кэш снапшотов ====
    // Батч — персистентный (меняется редко), а render() бегает каждый кадр.
    // Снимали по 6 toList() на до 20k примитивов КАЖДЫЙ кадр — теперь перекопируем
    // только когда version изменился (add/clear), на стабильном батче — 0 аллокаций.
    @Volatile private var version = 0L
    private var snapVersion = -1L
    private var snapBoxes: List<BoxPrim> = emptyList()
    private var snapLines: List<LinePrim> = emptyList()
    private var snapTexts: List<TextPrim> = emptyList()
    private var snapBeacons: List<BeaconPrim> = emptyList()
    private var snapCircles: List<CirclePrim> = emptyList()
    private var snapImages: List<ImagePrim> = emptyList()

    internal fun render(ctx: RenderContext) {
        // Пересобираем снапшот только при изменении батча (под lock — без CME).
        val v = version
        if (v != snapVersion) {
            synchronized(this) {
                snapBoxes = boxes.toList(); snapLines = lines.toList(); snapTexts = texts.toList()
                snapBeacons = beacons.toList(); snapCircles = circles.toList(); snapImages = images.toList()
                snapVersion = version
            }
        }
        for (b in snapBoxes) {
            val aabb = net.minecraft.world.phys.AABB(b.x, b.y, b.z, b.x + b.w, b.y + b.h, b.z + b.d)
            if (b.throughBlocks) {
                ctx.renderBoxThroughBlocks(aabb, r(b.color), g(b.color), bl(b.color), a(b.color), b.filled, b.thickness)
            } else {
                ctx.renderBox(aabb, r(b.color), g(b.color), bl(b.color), a(b.color), b.filled, b.thickness)
            }
        }
        for (l in snapLines) ctx.renderLine(
            net.minecraft.world.phys.Vec3(l.x1, l.y1, l.z1),
            net.minecraft.world.phys.Vec3(l.x2, l.y2, l.z2),
            l.color, l.thickness
        )
        for (t in snapTexts) ctx.renderText(t.text, t.x, t.y, t.z, t.scale, t.color, t.shadow, t.seeThrough)
        for (b in snapBeacons) ctx.renderBeaconBeam(b.x, b.y, b.z, b.height, b.color, ctx.tickDelta)
        for (c in snapCircles) ctx.renderCircle(c.x, c.y, c.z, c.radius, c.color, c.thickness, c.segments, c.axis)
        for (i in snapImages) ctx.renderImage(i.texture, i.x, i.y, i.z, i.width, i.height, i.rotationY, i.throughBlocks)
    }

    private fun r(c: Int) = ((c shr 16) and 0xFF) / 255f
    private fun g(c: Int) = ((c shr 8) and 0xFF) / 255f
    private fun bl(c: Int) = (c and 0xFF) / 255f
    private fun a(c: Int) = (((c shr 24) and 0xFF) / 255f).takeIf { it > 0f } ?: 1f
}

/**
 * Реестр батчей модулей. Чистится при выгрузке модуля и выгрузке движка.
 */
object SHWorldRenderBatches {
    private val batches = ConcurrentHashMap<String, SHRenderBatch>()

    fun getOrCreate(id: String, ownerModule: String): SHRenderBatch {
        val key = ownerModule.lowercase() + ":" + id.lowercase()
        return batches.computeIfAbsent(key) { SHRenderBatch(id, ownerModule) }
    }

    fun remove(ownerModule: String, id: String) {
        batches.remove(ownerModule.lowercase() + ":" + id.lowercase())
    }

    fun removeForModule(moduleName: String) {
        batches.keys.removeIf { it.startsWith(moduleName.lowercase() + ":") }
    }

    fun clearAll() {
        batches.clear()
    }

    fun renderAll(ctx: RenderContext) {
        if (batches.isEmpty()) return
        for (batch in batches.values) batch.render(ctx)
    }
}
