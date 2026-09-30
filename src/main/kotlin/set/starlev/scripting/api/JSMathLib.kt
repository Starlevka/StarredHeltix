package set.starlev.scripting.api

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * JS API: Объект `math` — математические помощники.
 * Аналог MathLib из ctjs + полезные дополнения.
 */
class JSMathLib {
    /** Перевод значения из одного диапазона в другой. */
    fun map(value: Double, inMin: Double, inMax: Double, outMin: Double, outMax: Double): Double =
        (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin

    /** Ограничение числа диапазоном. */
    fun clamp(value: Double, min: Double, max: Double): Double = value.coerceIn(min, max)

    /** Линейная интерполяция. */
    fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t.coerceIn(0.0, 1.0)

    /** Расстояние по 2 осям. */
    fun dist2D(x1: Double, z1: Double, x2: Double, z2: Double): Double =
        sqrt((x2 - x1) * (x2 - x1) + (z2 - z1) * (z2 - z1))

    /** Расстояние по 3 осям. */
    fun dist3D(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double =
        sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1))

    /** Абсолютная разность. */
    fun diff(a: Double, b: Double): Double = abs(a - b)

    /** Минимум/максимум из произвольного числа аргументов. */
    fun min(a: Double, b: Double): Double = kotlin.math.min(a, b)

    fun max(a: Double, b: Double): Double = kotlin.math.max(a, b)

    /** Округление до N знаков. */
    fun round(value: Double, decimals: Int = 0): Double {
        val factor = Math.pow(10.0, decimals.toDouble())
        return Math.round(value * factor) / factor
    }

    /** Плавный переход 0..1 (smoothstep). */
    fun smoothstep(t: Double): Double {
        val x = t.coerceIn(0.0, 1.0)
        return x * x * (3.0 - 2.0 * x)
    }
}
