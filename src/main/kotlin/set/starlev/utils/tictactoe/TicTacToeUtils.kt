package set.starlev.utils.tictactoe

/**
 * Утилиты для расчета оптимального хода в крестиках-ноликах
 * Улучшенный минимакс с приоритетами: победа > блок > вилка > блок вилки > центр > углы
 */
object TicTacToeUtils {

    // Все возможные выигрышные линии
    private val WIN_LINES = listOf(
        // Строки
        listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2)),
        listOf(Pair(1, 0), Pair(1, 1), Pair(1, 2)),
        listOf(Pair(2, 0), Pair(2, 1), Pair(2, 2)),
        // Столбцы
        listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0)),
        listOf(Pair(0, 1), Pair(1, 1), Pair(2, 1)),
        listOf(Pair(0, 2), Pair(1, 2), Pair(2, 2)),
        // Диагонали
        listOf(Pair(0, 0), Pair(1, 1), Pair(2, 2)),
        listOf(Pair(0, 2), Pair(1, 1), Pair(2, 0))
    )

    /**
     * Находит лучший ход для игрока O (наш ход)
     * Приоритеты: победа > блок > создание вилки > блок вилки > центр > углы > стороны
     */
    fun getBestMove(board: Array<CharArray>): BoardIndex {
        // 1. Проверяем, можем ли мы выиграть сейчас
        val winMove = findWinningMove(board, 'O')
        if (winMove != null) return winMove

        // 2. Проверяем, нужно ли блокировать победу противника
        val blockMove = findWinningMove(board, 'X')
        if (blockMove != null) return blockMove

        // 3. Проверяем, можем ли создать вилку (две выигрышные линии одновременно)
        val forkMove = findForkMove(board, 'O')
        if (forkMove != null) return forkMove

        // 4. Проверяем, нужно ли блокировать вилку противника
        val blockForkMove = findBlockForkMove(board, 'X')
        if (blockForkMove != null) return blockForkMove

        // 5. Центр — лучший ход если свободен
        if (board[1][1] == '\u0000') return BoardIndex(1, 1)

        // 6. Создать угрозу победы (линия с 2 из 3)
        val createThreat = findCreateThreat(board, 'O')
        if (createThreat != null) return createThreat

        // 7. Блокировать угрозу противника (уже сделано в шаге 2, но на случай если пропустили)
        val blockThreat = findBlockThreat(board, 'X')
        if (blockThreat != null) return blockThreat

        // 8. Противоположный угол от занятого угла противника
        val oppositeCorner = findOppositeCorner(board)
        if (oppositeCorner != null) return oppositeCorner

        // 9. Углы предпочтительнее сторон, но только если не создают угрозу противнику
        val safeCorner = findSafeCorner(board)
        if (safeCorner != null) return safeCorner

        // 10. Используем минимакс для оставшихся позиций
        return minimaxBestMove(board)
    }

    /**
     * Найти ход, который приведёт к победе
     */
    private fun findWinningMove(board: Array<CharArray>, player: Char): BoardIndex? {
        for (row in 0..2) {
            for (col in 0..2) {
                if (board[row][col] == '\u0000') {
                    board[row][col] = player
                    val isWin = getWinner(board) == player
                    board[row][col] = '\u0000'
                    if (isWin) return BoardIndex(row, col)
                }
            }
        }
        return null
    }

    /**
     * Найти ход, создающий вилку (две линии одновременно) для игрока
     */
    private fun findForkMove(board: Array<CharArray>, player: Char): BoardIndex? {
        for (row in 0..2) {
            for (col in 0..2) {
                if (board[row][col] == '\u0000') {
                    board[row][col] = player
                    val winningLines = countPotentialWinningLines(board, player)
                    board[row][col] = '\u0000'
                    if (winningLines >= 2) return BoardIndex(row, col)
                }
            }
        }
        return null
    }

    /**
     * Найти ход, блокирующий вилку противника.
     * Стратегия: если противник может создать вилку, нужно либо:
     * - Создать свою угрозу, чтобы заставить противника защищаться
     * - Блокировать одну из линий вилки
     */
    private fun findBlockForkMove(board: Array<CharArray>, opponent: Char): BoardIndex? {
        // Проверяем все пустые клетки — может ли противник создать вилку
        val forkPositions = mutableListOf<Pair<Int, Int>>()
        for (row in 0..2) {
            for (col in 0..2) {
                if (board[row][col] == '\u0000') {
                    board[row][col] = opponent
                    val winningLines = countPotentialWinningLines(board, opponent)
                    board[row][col] = '\u0000'
                    if (winningLines >= 2) {
                        forkPositions.add(Pair(row, col))
                    }
                }
            }
        }

        if (forkPositions.isEmpty()) return null

        // Стратегия блокирования: пытаемся создать свою угрозу
        val player = if (opponent == 'X') 'O' else 'X'
        for (row in 0..2) {
            for (col in 0..2) {
                if (board[row][col] == '\u0000' && Pair(row, col) !in forkPositions) {
                    board[row][col] = player
                    val createsThreat = countPotentialWinningLines(board, player) >= 1
                    board[row][col] = '\u0000'
                    if (createsThreat) return BoardIndex(row, col)
                }
            }
        }

        // Если не удалось создать угрозу, блокируем первую позицию вилки
        // но только если она не даёт противнику выиграть
        for (pos in forkPositions) {
            // Блокируем, создавая свою линию
            board[pos.first][pos.second] = player
            val createsLine = countPotentialWinningLines(board, player) >= 1
            board[pos.first][pos.second] = '\u0000'
            if (createsLine) return BoardIndex(pos.first, pos.second)
        }

        // Фоллбэк: занимаем центр или угол
        if (board[1][1] == '\u0000') return BoardIndex(1, 1)
        val corners = listOf(Pair(0, 0), Pair(0, 2), Pair(2, 0), Pair(2, 2))
        for ((row, col) in corners) {
            if (board[row][col] == '\u0000' && Pair(row, col) !in forkPositions) {
                return BoardIndex(row, col)
            }
        }

        // Последний вариант — блокируем первую вилку
        val firstFork = forkPositions.firstOrNull()
        if (firstFork != null) return BoardIndex(firstFork.first, firstFork.second)

        return null
    }

    /**
     * Найти ход, создающий угрозу победы (2 из 3 в линии)
     */
    private fun findCreateThreat(board: Array<CharArray>, player: Char): BoardIndex? {
        for (row in 0..2) {
            for (col in 0..2) {
                if (board[row][col] == '\u0000') {
                    board[row][col] = player
                    val threats = countPotentialWinningLines(board, player)
                    board[row][col] = '\u0000'
                    if (threats >= 1) return BoardIndex(row, col)
                }
            }
        }
        return null
    }

    /**
     * Найти ход, блокирующий угрозу противника (когда у противника 2 из 3)
     * Это дополнительный блок на случай если шаг 2 пропустил что-то
     */
    private fun findBlockThreat(board: Array<CharArray>, opponent: Char): BoardIndex? {
        for (row in 0..2) {
            for (col in 0..2) {
                if (board[row][col] == '\u0000') {
                    board[row][col] = opponent
                    val threats = countPotentialWinningLines(board, opponent)
                    board[row][col] = '\u0000'
                    if (threats >= 1) return BoardIndex(row, col)
                }
            }
        }
        return null
    }

    /**
     * Найти безопасный угол, который не создаёт угрозу для противника
     * (т.е. угол, после которого противник не сможет создать вилку)
     */
    private fun findSafeCorner(board: Array<CharArray>): BoardIndex? {
        val corners = listOf(Pair(0, 0), Pair(0, 2), Pair(2, 0), Pair(2, 2))
        val opponent = 'X'
        val player = 'O'
        
        for ((row, col) in corners) {
            if (board[row][col] == '\u0000') {
                // Проверяем, не создаёт ли этот угол угрозу для противника
                board[row][col] = player
                val opponentThreats = countPotentialWinningLines(board, opponent)
                board[row][col] = '\u0000'
                
                // Если не создаёт угрозу для противника (он не сможет выиграть следующим ходом)
                if (opponentThreats == 0) {
                    return BoardIndex(row, col)
                }
            }
        }
        return null
    }

    /**
     * Найти противоположный угол от занятого угла противника
     */
    private fun findOppositeCorner(board: Array<CharArray>): BoardIndex? {
        val player = 'O'
        val opponent = 'X'
        
        // Если противник занял угол, а центр свободен — занимаем противоположный угол
        val cornerOpposites = mapOf(
            Pair(0, 0) to Pair(2, 2),
            Pair(0, 2) to Pair(2, 0),
            Pair(2, 0) to Pair(0, 2),
            Pair(2, 2) to Pair(0, 0)
        )

        for ((corner, opposite) in cornerOpposites) {
            if (board[corner.first][corner.second] == opponent && 
                board[opposite.first][opposite.second] == '\u0000') {
                return BoardIndex(opposite.first, opposite.second)
            }
        }
        return null
    }

    /**
     * Подсчитать количество потенциально выигрышных линий (2 из 3 заполнены, третья пуста)
     */
    private fun countPotentialWinningLines(board: Array<CharArray>, player: Char): Int {
        var count = 0
        for (line in WIN_LINES) {
            val cells = line.map { (r, c) -> board[r][c] }
            val playerCount = cells.count { it == player }
            val emptyCount = cells.count { it == '\u0000' }
            if (playerCount == 2 && emptyCount == 1) count++
        }
        return count
    }

    /**
     * Минимакс для оставшихся позиций
     */
    private fun minimaxBestMove(board: Array<CharArray>): BoardIndex {
        var bestScore = Int.MIN_VALUE
        var bestMove = BoardIndex(1, 1)

        for (row in 0..2) {
            for (col in 0..2) {
                if (board[row][col] == '\u0000') {
                    board[row][col] = 'O'
                    val score = minimax(board, depth = 0, isMaximizing = false)
                    board[row][col] = '\u0000'
                    if (score > bestScore) {
                        bestScore = score
                        bestMove = BoardIndex(row, col)
                    }
                }
            }
        }
        return bestMove
    }

    /**
     * Минимакс с улучшенными эвристиками
     */
    private fun minimax(board: Array<CharArray>, depth: Int, isMaximizing: Boolean): Int {
        val winner = getWinner(board)
        if (winner == 'O') return 10 - depth  // быстрая победа лучше
        if (winner == 'X') return depth - 10  // медленное поражение лучше
        if (isBoardFull(board)) return 0       // ничья

        if (isMaximizing) {
            var best = Int.MIN_VALUE
            for (r in 0..2) {
                for (c in 0..2) {
                    if (board[r][c] == '\u0000') {
                        board[r][c] = 'O'
                        best = maxOf(best, minimax(board, depth + 1, false))
                        board[r][c] = '\u0000'
                    }
                }
            }
            return best
        } else {
            var best = Int.MAX_VALUE
            for (r in 0..2) {
                for (c in 0..2) {
                    if (board[r][c] == '\u0000') {
                        board[r][c] = 'X'
                        best = minOf(best, minimax(board, depth + 1, true))
                        board[r][c] = '\u0000'
                    }
                }
            }
            return best
        }
    }

    /**
     * Возвращает победителя ('O', 'X') или null
     */
    private fun getWinner(board: Array<CharArray>): Char? {
        for (line in WIN_LINES) {
            val (a, b, c) = line
            val cellA = board[a.first][a.second]
            val cellB = board[b.first][b.second]
            val cellC = board[c.first][c.second]
            if (cellA != '\u0000' && cellA == cellB && cellB == cellC) {
                return cellA
            }
        }
        return null
    }

    private fun isBoardFull(board: Array<CharArray>): Boolean {
        return board.all { row -> row.all { it != '\u0000' } }
    }
}