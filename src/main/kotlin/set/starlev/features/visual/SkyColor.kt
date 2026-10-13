package set.starlev.features.visual

import net.minecraft.client.Camera
import net.minecraft.util.ARGB
import net.minecraft.world.level.material.FogType
import org.joml.Vector4f
import set.starlev.StarredHeltix
import set.starlev.utils.ColorUtils

/**
 * Цвет неба, горизонта и пустоты (порт из Skysoft — `features/misc/SkyColor.kt`).
 *
 * Три точки входа, все через mixin'ы (логики тут нет):
 *  - `SkyRenderer.renderSkyDisc(int)`      — цвет купола неба;
 *  - `SkyRenderer.renderDarkDisc()`        — цвет «пустоты» (диск под миром);
 *  - `FogRenderer.computeFogColor(...)`    — цвет горизонта/тумана.
 *
 * Цвета берём из конфига строкой `"A:R:G:B"` (как остальные цвета мода) и подменяем
 * только при `enabled` — иначе возвращаем оригинал, чтобы выключенная фича была
 * полностью бесплатной.
 */
object SkyColor {
    private const val OPAQUE_ALPHA = 0xFF000000.toInt()
    private const val RGB_MASK = 0xFFFFFF

    private val config
        get() = StarredHeltix.feature.visuals.skyColor

    /** Цвет неба. [original] — ванильный ARGB ( SkyRenderer.renderSkyDisc ). */
    @JvmStatic
    fun skyColor(original: Int): Int {
        val cfg = config
        if (!cfg.enabled) return original
        return try {
            ColorUtils.parseColor(cfg.color, original) and RGB_MASK
        } catch (t: Throwable) {
            original
        }
    }

    /** Цвет пустоты — подменяем Vector4f, который создаётся внутри renderDarkDisc. */
    @JvmStatic
    fun voidColor(original: Vector4f): Vector4f {
        val cfg = config
        if (!cfg.enabled) return original
        return try {
            ARGB.vector4fFromARGB32(ColorUtils.parseColor(cfg.voidColor, OPAQUE_ALPHA) or OPAQUE_ALPHA)
        } catch (t: Throwable) {
            original
        }
    }

    /** Цвет горизонта — пишем в dest из FogRenderer.computeFogColor (TAIL). */
    @JvmStatic
    fun applyHorizonColor(camera: Camera, destination: Vector4f) {
        val cfg = config
        if (!cfg.enabled) return
        // В воде/лаве горизонт не рисуется — не тратим время на разбор цвета.
        if (camera.fluidInCamera != FogType.NONE) return
        try {
            destination.set(ARGB.vector4fFromARGB32(ColorUtils.parseColor(cfg.horizonColor, OPAQUE_ALPHA) or OPAQUE_ALPHA))
        } catch (t: Throwable) {
            // Цвет не распарсился — оставляем ванильный
        }
    }
}
