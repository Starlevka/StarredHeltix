package set.starlev.utils.tictactoe

/**
 * Решатель крестиков-ноликов для комнаты-головоломки.
 *
 * Игрок ходит за **O** (крестики на рамках ставит сервер), поэтому [getBestMove]
 * возвращает ход за O на доске без каких-либо перестановок — перестановка X/O
 * перед минимаксом считала ход за сервер и подсвечивала проигрышную клетку.
 *
 * Алгоритм — полный альфа-бета перебор всех продолжений (9 клеток, ~362 000
 * вариантов, держится в памяти без проблем). При равной оценке победа предпочтительнее
 * ничьей, ничья — поражения; среди равноценных ходов берётся первый в порядке обхода
 * (строки сверху вниз, столбцы слева направо) — подсветка не «прыгает» между клетками.
 */
object TicTacToeUtils {

    /** Все выигрышные линии индексами в плоском массиве 0..8 (row * 3 + column). */
    private val WIN_LINES = arrayOf(
        intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
        intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
        intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
    )

    /** Победа/поражение с весом по глубине: быстрая победа лучше медленной. */
    private const val WIN_SCORE = 10
    private const val INF = 1_000_000

    /** Пустая клетка — NUL, тот же, каким солвер заполняет доску. */
    private const val EMPTY = Char.MIN_VALUE

    /** Наш знак — O. */
    private const val PLAYER = 'O'

    /** Знак сервера — X. */
    private const val OPPONENT = 'X'

    /**
     * Лучший ход для игрока O.
     * @param board доска 3x3: NUL — пусто, 'O' — наш знак, 'X' — сервер.
     */
    fun getBestMove(board: Array<CharArray>): BoardIndex {
        val cells = flatten(board)
        var bestIndex = -1
        var bestScore = Int.MIN_VALUE
        for (i in cells.indices) {
            if (cells[i] != EMPTY) continue
            cells[i] = PLAYER
            val score = alphabeta(cells, depth = 1, maximizing = false, alpha = -INF, beta = INF)
            cells[i] = EMPTY
            if (score > bestScore) {
                bestScore = score
                bestIndex = i
            }
        }
        // Все клетки заняты — вызывающий код всё равно прекратит отрисовку,
        // но вернуть валидальный индекс лучше, чем бросать.
        if (bestIndex < 0) bestIndex = cells.indexOfFirst { it == EMPTY }
        if (bestIndex < 0) bestIndex = 4
        return BoardIndex(bestIndex / 3, bestIndex % 3)
    }

    /** Раскладывает доску в плоский массив — рекурсии дешевле и без вложенных массивов. */
    private fun flatten(board: Array<CharArray>): CharArray {
        val out = CharArray(9)
        for (r in 0..2) {
            for (c in 0..2) {
                out[r * 3 + c] = board[r][c]
            }
        }
        return out
    }

    /** Оценка позиции: чья победа (знак для игрока) с поправкой на глубину. */
    private fun score(cells: CharArray, depth: Int): Int {
        for (line in WIN_LINES) {
            val a = cells[line[0]]
            if (a != EMPTY && a == cells[line[1]] && a == cells[line[2]]) {
                return if (a == PLAYER) WIN_SCORE - depth else depth - WIN_SCORE
            }
        }
        return 0
    }

    private fun hasEmpty(cells: CharArray): Boolean {
        for (cell in cells) if (cell == EMPTY) return true
        return false
    }

    /**
     * Альфа-бета. [maximizing] = true — ход игрока (O), false — ход сервера (X).
     */
    private fun alphabeta(cells: CharArray, depth: Int, maximizing: Boolean, alpha: Int, beta: Int): Int {
        val current = score(cells, depth)
        if (current != 0) return current
        if (!hasEmpty(cells)) return 0

        val mark = if (maximizing) PLAYER else OPPONENT
        var localAlpha = alpha
        var localBeta = beta
        var result = if (maximizing) -INF else INF

        for (i in cells.indices) {
            if (cells[i] != EMPTY) continue
            cells[i] = mark
            val value = alphabeta(cells, depth + 1, !maximizing, localAlpha, localBeta)
            cells[i] = EMPTY
            if (maximizing) {
                if (value > result) result = value
                if (result > localAlpha) localAlpha = result
            } else {
                if (value < result) result = value
                if (result < localBeta) localBeta = result
            }
            if (localBeta <= localAlpha) break
        }
        return result
    }
}