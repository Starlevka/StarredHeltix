package set.starlev.features.visual

import net.minecraft.client.Camera
import net.minecraft.util.ARGB
import net.minecraft.world.level.material.FogType
import org.joml.Vector4f
import set.starlev.StarredHeltix
import set.starlev.utils.ColorUtils

/**
 * Цвет неба, горизонта и пустоты.
 *
 * Три точки входа, все через mixin'ы (логики тут нет):
 *  - `SkyRenderer.renderSkyDisc(int)`      — цвет купола неба;
 *  - `SkyRenderer.renderDarkDisc()`        — цвет «пустоты» (диск под миром);
 *  - `FogRenderer.computeFogColor(...)`    — цвет горизонта/тумана.
 *
 * Цвета берём из конфига строкой `"A:R:G:B"` (как остальные цвета мода) и подменяем
 * только при `enabled` — иначе возвращаем оригинал, чтобы выключенная фича была
 * полностью бесплатной. Цвет с прозрачностью 0% тоже считается «не задан»:
 * дефолты в конфиге именно такие, то есть из коробки небо остаётся ванильным.
 */
object SkyColor {
    private const val OPAQUE_ALPHA = 0xFF000000.toInt()
    private const val RGB_MASK = 0xFFFFFF

    /** Дефолт парсера: альфа 0 = «цвет не задан». */
    private const val TRANSPARENT = 0

    private val config
        get() = StarredHeltix.feature.visuals.skyColor

    /**
     * Разбирает цвет из конфига. `null` = перекрашивать нечего, отдаём ванильный.
     *
     * Альфа у всех трёх точек подмены и так отбрасывается (небо режется маской,
     * горизонт и пустота приводятся к непрозрачным), поэтому прозрачность 0% —
     * единственный свободный «выключатель» без ещё одного поля в конфиге.
     * Пустая строка и неразобранное значение дают тот же эффект.
     */
    private fun overrideColor(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val argb = try {
            ColorUtils.parseColor(raw, TRANSPARENT)
        } catch (t: Throwable) {
            TRANSPARENT
        }
        return if ((argb ushr 24) == 0) null else argb
    }

    /** Цвет неба. [original] — ванильный ARGB ( SkyRenderer.renderSkyDisc ). */
    @JvmStatic
    fun skyColor(original: Int): Int {
        val cfg = config
        if (!cfg.enabled) return original
        val argb = overrideColor(cfg.color) ?: return original
        return argb and RGB_MASK
    }

    /** Цвет пустоты — подменяем Vector4f, который создаётся внутри renderDarkDisc. */
    @JvmStatic
    fun voidColor(original: Vector4f): Vector4f {
        val cfg = config
        if (!cfg.enabled) return original
        val argb = overrideColor(cfg.voidColor) ?: return original
        return ARGB.vector4fFromARGB32(argb or OPAQUE_ALPHA)
    }

    /** Цвет горизонта — пишем в dest из FogRenderer.computeFogColor (TAIL). */
    @JvmStatic
    fun applyHorizonColor(camera: Camera, destination: Vector4f) {
        val cfg = config
        if (!cfg.enabled) return
        // В воде/лаве горизонт не рисуется — не тратим время на разбор цвета.
        if (camera.fluidInCamera != FogType.NONE) return
        val argb = overrideColor(cfg.horizonColor) ?: return
        destination.set(ARGB.vector4fFromARGB32(argb or OPAQUE_ALPHA))
    }
}
