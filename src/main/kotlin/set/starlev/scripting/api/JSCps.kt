package set.starlev.scripting.api

import net.minecraft.client.Minecraft

/**
 * JS API: Объект `cps` — клики в секунду (CPS).
 * Отслеживает нажатия кнопок мыши за последнюю секунду.
 */
class JSCps {
    companion object {
        // Окна кликов (в тиках, 20 тиков = 1 секунда): List<Int> — остаток жизни клика
        private val leftClicks = mutableListOf<Int>()
        private val rightClicks = mutableListOf<Int>()
        private val middleClicks = mutableListOf<Int>()

        private var leftMax = 0
        private var rightMax = 0
        private var middleMax = 0

        private val leftAverages = ArrayDeque<Int>()
        private val rightAverages = ArrayDeque<Int>()
        private val middleAverages = ArrayDeque<Int>()

        /** Вызывать раз в тик из движка (END_CLIENT_TICK): затухание кликов + средние. */
        fun tick() {
            val mc = Minecraft.getInstance()
            track(mc.mouseHandler.isLeftPressed, leftClicks, 0)
            track(mc.mouseHandler.isRightPressed, rightClicks, 1)
            track(mc.mouseHandler.isMiddlePressed, middleClicks, 2)

            leftMax = updateMax(leftMax, leftClicks, leftAverages)
            rightMax = updateMax(rightMax, rightClicks, rightAverages)
            middleMax = updateMax(middleMax, middleClicks, middleAverages)
        }

        private var leftWasDown = false
        private var rightWasDown = false
        private var middleWasDown = false

        private fun track(down: Boolean, list: MutableList<Int>, id: Int) {
            val was = when (id) {
                0 -> leftWasDown
                1 -> rightWasDown
                else -> middleWasDown
            }
            if (down && !was) {
                list.add(20) // клик живёт 1 секунду (20 тиков)
            }
            when (id) {
                0 -> leftWasDown = down
                1 -> rightWasDown = down
                else -> middleWasDown = down
            }
        }

        private fun updateMax(currentMax: Int, clicks: MutableList<Int>, averages: ArrayDeque<Int>): Int {
            for (i in clicks.indices) clicks[i] = clicks[i] - 1
            clicks.removeAll { it <= 0 }

            averages.addLast(clicks.size)
            if (averages.size > 100) averages.removeFirst()

            // decay max если последние 20 тиков (1с) без кликов — сброс пика
            if (averages.size >= 20 && averages.takeLast(20).all { it == 0 }) {
                averages.clear()
                return 0
            }
            return maxOf(currentMax, clicks.size)
        }
    }

    /** Текущие клики ЛКМ за секунду. */
    fun getLeftClicks(): Int = leftClicks.size

    /** Текущие клики ПКМ за секунду. */
    fun getRightClicks(): Int = rightClicks.size

    /** Текущие клики СКМ за секунду. */
    fun getMiddleClicks(): Int = middleClicks.size

    /** Пик кликов ЛКМ. */
    fun getLeftClicksMax(): Int = leftMax

    /** Пик кликов ПКМ. */
    fun getRightClicksMax(): Int = rightMax

    /** Пик кликов СКМ. */
    fun getMiddleClicksMax(): Int = middleMax

    /** Средние клики ЛКМ за последние ~5 секунд. */
    fun getLeftClicksAverage(): Int = average(leftAverages)

    /** Средние клики ПКМ. */
    fun getRightClicksAverage(): Int = average(rightAverages)

    /** Средние клики СКМ. */
    fun getMiddleClicksAverage(): Int = average(middleAverages)

    private fun average(deque: ArrayDeque<Int>): Int =
        if (deque.isNotEmpty()) deque.average().toInt() else 0
}
