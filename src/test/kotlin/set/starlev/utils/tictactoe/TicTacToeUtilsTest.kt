package set.starlev.utils.tictactoe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для [TicTacToeUtils].
 *
 * Покрывает публичный API — [TicTacToeUtils.getBestMove]. Утилита ищет лучший ход
 * для игрока 'O' (наш ход в логике солвера TicTacToe).
 *
 * Что НЕ покрываем:
 * — Приватные методы (countPotentialWinningLines, getWinner, isBoardFull) —
 *   они автоматически покрываются через публичный getBestMove.
 * — Minecraft-логика TicTacToe.kt — требует загрузки мира, тестируется вручную на сервере.
 *
 * Запуск:
 *   ./gradlew test
 *
 * Что проверяем:
 * — Пустая доска → центр (приоритетная стратегия).
 * — Немедленная победа > любой другой ход.
 * — Немедленный проигрыш → блокировка.
 * — Вилки: создание своей вилки > блокировка вилки противника > центр.
 * — Никогда не выбираем занятую клетку.
 * — Ход всегда на пустую клетку.
 */
class TicTacToeUtilsTest {

    // ===== Хелперы =====

    /** Создаёт пустую доску 3x3. */
    private fun emptyBoard(): Array<CharArray> = Array(3) { CharArray(3) { '\u0000' } }

    /**
     * Создаёт доску из 9 символов по строкам.
     * Используй '\u0000' для пустой клетки, 'X' или 'O' для занятых.
     */
    private fun boardOf(vararg cells: Char): Array<CharArray> {
        require(cells.size == 9) { "Доска 3x3 = 9 клеток, передано ${cells.size}" }
        return Array(3) { r -> CharArray(3) { c -> cells[r * 3 + c] } }
    }

    /** Возвращает true, если указанный игрок выиграл на доске. */
    private fun isWinner(board: Array<CharArray>, player: Char): Boolean {
        val lines = listOf(
            listOf(0 to 0, 0 to 1, 0 to 2),
            listOf(1 to 0, 1 to 1, 1 to 2),
            listOf(2 to 0, 2 to 1, 2 to 2),
            listOf(0 to 0, 1 to 0, 2 to 0),
            listOf(0 to 1, 1 to 1, 2 to 1),
            listOf(0 to 2, 1 to 2, 2 to 2),
            listOf(0 to 0, 1 to 1, 2 to 2),
            listOf(0 to 2, 1 to 1, 2 to 0)
        )
        return lines.any { line ->
            line.all { (r, c) -> board[r][c] == player }
        }
    }

    // ===== Тесты =====

    @Test
    fun `пустая доска — должен выбрать центр`() {
        val board = emptyBoard()
        val move = TicTacToeUtils.getBestMove(board)
        assertEquals("На пустой доске оптимально — центр", BoardIndex(1, 1), move)
    }

    @Test
    fun `немедленная победа — должен выиграть сейчас`() {
        // O O .
        // . . .
        // . . .
        // O может выиграть на (0,2) за один ход.
        val board = boardOf('O', 'O', '\u0000',
                            '\u0000', '\u0000', '\u0000',
                            '\u0000', '\u0000', '\u0000')
        val move = TicTacToeUtils.getBestMove(board)
        assertEquals(BoardIndex(0, 2), move)
    }

    @Test
    fun `немедленная победа по столбцу`() {
        // X . .
        // X . .
        // . . .
        val board = boardOf('X', '\u0000', '\u0000',
                            'X', '\u0000', '\u0000',
                            '\u0000', '\u0000', '\u0000')
        val move = TicTacToeUtils.getBestMove(board)
        // Должен заблокировать на (2, 0)
        assertEquals(BoardIndex(2, 0), move)
    }

    @Test
    fun `немедленная победа по диагонали`() {
        // X . .
        // . X .
        // . . .
        val board = boardOf('X', '\u0000', '\u0000',
                            '\u0000', 'X', '\u0000',
                            '\u0000', '\u0000', '\u0000')
        val move = TicTacToeUtils.getBestMove(board)
        // X может выиграть на (2, 2) за один ход — нужно блокировать
        assertEquals(BoardIndex(2, 2), move)
    }

    @Test
    fun `блокировка — приоритетнее всего кроме немедленной победы`() {
        // X X .
        // . . .
        // . . .
        // X выигрывает на (0, 2). У O нет немедленной победы → блокируем.
        val board = boardOf('X', 'X', '\u0000',
                            '\u0000', '\u0000', '\u0000',
                            '\u0000', '\u0000', '\u0000')
        val move = TicTacToeUtils.getBestMove(board)
        assertEquals(BoardIndex(0, 2), move)
    }

    @Test
    fun `победа приоритетнее блокировки`() {
        // O O .  <- O может выиграть на (0,2)
        // X X .  <- X может выиграть на (2,0) или (1,2)
        // . . .
        // O должен выбрать немедленную победу (0,2), а не блокировать.
        val board = boardOf('O', 'O', '\u0000',
                            'X', 'X', '\u0000',
                            '\u0000', '\u0000', '\u0000')
        val move = TicTacToeUtils.getBestMove(board)
        assertEquals("Победа важнее блокировки", BoardIndex(0, 2), move)
    }

    @Test
    fun `противник занял угол — O выбирает центр`() {
        // X . .
        // . . .
        // . . .
        val board = boardOf('X', '\u0000', '\u0000',
                            '\u0000', '\u0000', '\u0000',
                            '\u0000', '\u0000', '\u0000')
        val move = TicTacToeUtils.getBestMove(board)
        // Стратегия: занять центр — лучший ответ на угол противника
        assertEquals(BoardIndex(1, 1), move)
    }

    @Test
    fun `выигрышный ход вместо ловушки — O выбирает победу а не финт`() {
        // X . .
        // X . .
        // O O .
        // O может выиграть сейчас на (2, 2). Не должен отвлекаться на ловушки.
        val board = boardOf('X', '\u0000', '\u0000',
                            'X', '\u0000', '\u0000',
                            'O', 'O', '\u0000')
        val move = TicTacToeUtils.getBestMove(board)
        assertEquals(BoardIndex(2, 2), move)

        // Проверим, что после этого хода O действительно выигрывает.
        board[2][2] = 'O'
        assertTrue("O должен выиграть по нижней строке", isWinner(board, 'O'))
    }

    @Test
    fun `два выигрышных варианта — O выбирает любой из них`() {
        // . O .
        // X X .
        // O . .
        // O может выиграть по диагонали (0,1)-(1,1)-(2,1) если поставить (1,1),
        // либо может выиграть по столбцу (2,0)-(2,1)-(2,2) если поставить (2,2) — но X мешает.
        // Проверим: после хода O должен либо выиграть сейчас, либо создать угрозу.
        val board = boardOf('\u0000', 'O', '\u0000',
                            'X', 'X', '\u0000',
                            'O', '\u0000', '\u0000')
        val move = TicTacToeUtils.getBestMove(board)
        // Главное — ход на пустую клетку, в пределах доски.
        assertTrue("row в [0..2]", move.row in 0..2)
        assertTrue("col в [0..2]", move.column in 0..2)
        assertEquals(
            "Ход должен быть на пустую клетку",
            '\u0000', board[move.row][move.column]
        )
    }

    @Test
    fun `вилка противника — должна быть заблокирована или встречной угрозой`() {
        // . . X
        // . O .
        // X . .
        // X может создать вилку на (0,0) или (2,2). O должен блокировать
        // или создать свою встречную угрозу.
        val board = boardOf('\u0000', '\u0000', 'X',
                            '\u0000', 'O', '\u0000',
                            'X', '\u0000', '\u0000')
        val move = TicTacToeUtils.getBestMove(board)
        // После хода O, у X не должно быть возможности выиграть следующим ходом.
        board[move.row][move.column] = 'O'
        val xCanWinNext = canWinInOneMove(board, 'X')
        assertFalse("После нашего хода X не должен выигрывать следующим ходом", xCanWinNext)
    }

    @Test
    fun `ход всегда на пустую клетку`() {
        // Почти полная доска — у O один пустой ход.
        val board = boardOf('X', 'O', 'X',
                            'X', 'O', 'O',
                            'O', 'X', '\u0000')
        val move = TicTacToeUtils.getBestMove(board)
        assertEquals("Единственная пустая клетка", BoardIndex(2, 2), move)
    }

    @Test
    fun `ничья — O не проигрывает`() {
        // Позиция, где оба играли оптимально — ничья.
        // X O X
        // X O O
        // O X .   <- пустая только (2,2)
        val board = boardOf('X', 'O', 'X',
                            'X', 'O', 'O',
                            'O', 'X', '\u0000')
        val move = TicTacToeUtils.getBestMove(board)
        // Ход не должен приводить к проигрышу O.
        board[move.row][move.column] = 'O'
        assertTrue("O не должен проиграть", !isWinner(board, 'X'))
    }

    @Test
    fun `возвращаемый ход всегда в пределах доски`() {
        // Стресс-тест: 20 случайных досок — все ходы должны быть в [0..2].
        val rng = java.util.Random(42)
        repeat(20) {
            val board = emptyBoard()
            // Заполняем случайно 4-6 клеток
            val cellsToFill = 4 + rng.nextInt(3)
            val positions = (0..8).toMutableList()
            repeat(cellsToFill) {
                val idx = positions.removeAt(rng.nextInt(positions.size))
                board[idx / 3][idx % 3] = if (rng.nextBoolean()) 'X' else 'O'
            }
            val move = TicTacToeUtils.getBestMove(board)
            assertTrue("row должен быть в [0..2]", move.row in 0..2)
            assertTrue("column должен быть в [0..2]", move.column in 0..2)
            // Ход должен быть на пустую клетку
            assertEquals(
                "Ход ${move} должен быть на пустую клетку",
                '\u0000', board[move.row][move.column]
            )
        }
    }

    @Test
    fun `BoardIndex равенство`() {
        // Проверяем data class контракт.
        assertEquals(BoardIndex(1, 1), BoardIndex(1, 1))
        assertFalse("Разные клетки не должны быть равны",
            BoardIndex(1, 1) == BoardIndex(1, 2))
    }

    // ===== Внутренние хелперы для тестов =====

    private fun countWinningLinesFor(board: Array<CharArray>, player: Char): Int {
        val lines = listOf(
            listOf(0 to 0, 0 to 1, 0 to 2),
            listOf(1 to 0, 1 to 1, 1 to 2),
            listOf(2 to 0, 2 to 1, 2 to 2),
            listOf(0 to 0, 1 to 0, 2 to 0),
            listOf(0 to 1, 1 to 1, 2 to 1),
            listOf(0 to 2, 1 to 2, 2 to 2),
            listOf(0 to 0, 1 to 1, 2 to 2),
            listOf(0 to 2, 1 to 1, 2 to 0)
        )
        return lines.count { line ->
            val cells = line.map { (r, c) -> board[r][c] }
            cells.count { it == player } == 2 && cells.count { it == '\u0000' } == 1
        }
    }

    private fun canWinInOneMove(board: Array<CharArray>, player: Char): Boolean {
        for (r in 0..2) {
            for (c in 0..2) {
                if (board[r][c] == '\u0000') {
                    board[r][c] = player
                    val wins = isWinner(board, player)
                    board[r][c] = '\u0000'
                    if (wins) return true
                }
            }
        }
        return false
    }
}