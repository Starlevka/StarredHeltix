package set.starlev.features.visual.swing

import java.util.ArrayDeque
import java.util.function.LongSupplier

/** Псевдоним конфига анимации удара (используется редактором и историей). */
internal fun <T> java.util.ArrayDeque<T>.trimStartToSize(maximumSize: Int) {
    require(maximumSize >= 0) { "Maximum deque size must not be negative" }
    while (size > maximumSize) removeFirst()
}

// ---- Результат изменения ----

enum class ChangeResult {
    CHANGED,
    UNCHANGED,
    ;

    companion object {
        fun from(changed: Boolean): ChangeResult =
            if (changed) CHANGED else UNCHANGED
    }
}

// ---- История снимков (undo/redo) ----

class SnapshotHistory<S>(
    private val maximumSteps: Int,
    private val areEquivalent: (S, S) -> Boolean = { first, second -> first == second },
) {
    private val undo = ArrayDeque<Edit<S>>()
    private val redo = ArrayDeque<Edit<S>>()

    init {
        require(maximumSteps > 0) { "Snapshot history requires at least one step" }
    }

    val canUndo: Boolean get() = undo.isNotEmpty()
    val canRedo: Boolean get() = redo.isNotEmpty()

    fun record(before: S, after: S): ChangeResult {
        if (areEquivalent(before, after)) return ChangeResult.UNCHANGED
        undo.addLast(Edit(before, after))
        undo.trimStartToSize(maximumSteps)
        redo.clear()
        return ChangeResult.CHANGED
    }

    fun undo(restore: (S) -> Unit): ChangeResult {
        val edit = undo.pollLast() ?: return ChangeResult.UNCHANGED
        restore(edit.before)
        redo.addLast(edit)
        redo.trimStartToSize(maximumSteps)
        return ChangeResult.CHANGED
    }

    fun redo(restore: (S) -> Unit): ChangeResult {
        val edit = redo.pollLast() ?: return ChangeResult.UNCHANGED
        restore(edit.after)
        undo.addLast(edit)
        undo.trimStartToSize(maximumSteps)
        return ChangeResult.CHANGED
    }

    private data class Edit<S>(val before: S, val after: S)
}

// ---- Анимационные часы ----

class AnimationClock(
    nanoTime: LongSupplier = LongSupplier(System::nanoTime),
) {
    private val nanoTimeSupplier = nanoTime
    private var startedAtNanos = Long.MIN_VALUE

    fun restart() {
        startedAtNanos = nanoTimeSupplier.getAsLong()
    }

    fun stop() {
        startedAtNanos = Long.MIN_VALUE
    }

    fun hasStarted(): Boolean = startedAtNanos != Long.MIN_VALUE

    fun elapsedNanos(): Long =
        if (hasStarted()) (nanoTimeSupplier.getAsLong() - startedAtNanos).coerceAtLeast(0L) else Long.MAX_VALUE

    fun progress(durationMillis: Int): Float =
        progressNanos(durationMillis.toLong() * NANOS_PER_MILLISECOND)

    fun progressNanos(durationNanos: Long): Float {
        if (!hasStarted() || durationNanos <= 0L) return 1.0f
        return (elapsedNanos() / durationNanos.toFloat()).coerceAtMost(1.0f)
    }

    fun isCompleteNanos(durationNanos: Long): Boolean = progressNanos(durationNanos) >= 1.0f

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}

// ---- Сглаживание ----

object EasingUtilities {
    fun easeOutCubic(progress: Double): Double {
        val inverse = 1.0 - progress.coerceIn(0.0, 1.0)
        return 1.0 - inverse * inverse * inverse
    }

    fun easeOutCubic(progress: Float): Float = easeOutCubic(progress.toDouble()).toFloat()

    fun smoothStep(progress: Double): Double {
        val clamped = progress.coerceIn(0.0, 1.0)
        return clamped * clamped * (SMOOTHSTEP_QUADRATIC_COEFFICIENT - SMOOTHSTEP_CUBIC_COEFFICIENT * clamped)
    }

    fun smoothStep(progress: Float): Float = smoothStep(progress.toDouble()).toFloat()

    private const val SMOOTHSTEP_QUADRATIC_COEFFICIENT = 3.0
    private const val SMOOTHSTEP_CUBIC_COEFFICIENT = 2.0
}
