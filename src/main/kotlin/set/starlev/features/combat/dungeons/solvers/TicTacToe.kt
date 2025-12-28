package set.starlev.features.combat.solvers.dungeons

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.phys.AABB
import set.starlev.StarredHeltix
import set.starlev.render.RenderEvents

/**
 * TicTacToeSolver - Сольвер для головоломки крестики-нолики в подземельях.
 * Адаптировано из Skyblocker-master.
 */
object TicTacToeSolver {
    private val mc = Minecraft.getInstance()
    private var nextBestMoveToMake: AABB? = null
    
    // Цвета для рендеринга
    private val GREEN_COLOR = 0x8000FF00.toInt() // Полупрозрачный зеленый

    fun init() {
        RenderEvents.register { context ->
            if (!StarredHeltix.feature.dungeons.solvers.ticTacToe) return@register
            
            val box = nextBestMoveToMake ?: return@register
            context.renderBox(box, 0f, 1f, 0f, 0.5f, true)
            context.renderBox(box, 0f, 1f, 0f, 1f, false)
        }
        
        // Регистрируем тик для обновления логики
        // В данном проекте тики можно обрабатывать через RenderEvents или создать отдельный TickHandler
        // Для простоты будем обновлять логику в RenderEvents, но с ограничением по времени
        var lastUpdate = 0L
        RenderEvents.register {
            val now = System.currentTimeMillis()
            if (now - lastUpdate > 100) { // Каждые 100мс
                update()
                lastUpdate = now
            }
        }
    }

    private fun update() {
        if (!StarredHeltix.feature.dungeons.solvers.ticTacToe) {
            nextBestMoveToMake = null
            return
        }

        val player = mc.player ?: return
        val level = mc.level ?: return
        
        // Проверка на нахождение в подземельях
        if (!level.dimension().location().toString().startsWith("minecraft:dungeon_")) {
            nextBestMoveToMake = null
            return
        }
        
        nextBestMoveToMake = null

        // Поиск всех рамок в радиусе 21 блока (и с картами, и без)
        val searchBox = player.boundingBox.inflate(21.0)
        val allFrames = level.getEntitiesOfClass(ItemFrame::class.java, searchBox)
        
        if (allFrames.isEmpty()) return

        // Фильтруем только те рамки, которые образуют 3x3 сетку
        // Обычно они все смотрят в одну сторону и находятся на одной плоскости
        val firstFrame = allFrames.first()
        val facing = firstFrame.direction
        val boardFrames = allFrames.filter { it.direction == facing }
            .filter { 
                if (facing.axis.isHorizontal) {
                    if (facing.axis == net.minecraft.core.Direction.Axis.X) {
                        Math.abs(it.x - firstFrame.x) < 0.1
                    } else {
                        Math.abs(it.z - firstFrame.z) < 0.1
                    }
                } else true
            }

        if (boardFrames.size < 1) return

        val itemFramesWithMaps = boardFrames.filter { it.item.`is`(net.minecraft.world.item.Items.FILLED_MAP) }

        try {
            // В Skyblocker: (itemFrames.size != 9 && (itemFrames.size & 1) == 1)
            // Это означает, что сейчас ход игрока (нечетное количество рамок с картами, и их не 9)
            if (itemFramesWithMaps.size != 9 && (itemFramesWithMaps.size % 2) != 0) {
                val board = Array(3) { CharArray(3) { '\u0000' } }
                
                // Находим границы всех рамок на стене для точного позиционирования
                val minX = boardFrames.minOf { it.x }
                val minY = boardFrames.minOf { it.y }
                val minZ = boardFrames.minOf { it.z }
                val maxX = boardFrames.maxOf { it.x }
                val maxY = boardFrames.maxOf { it.y }
                val maxZ = boardFrames.maxOf { it.z }
                
                // Центр сетки — это среднее арифметическое координат всех рамок
                val gridCenterX = boardFrames.map { it.x }.average()
                val gridCenterY = boardFrames.map { it.y }.average()
                val gridCenterZ = boardFrames.map { it.z }.average()

                val facing = boardFrames.first().direction

                for (frame in itemFramesWithMaps) {
                    val mapId = frame.item.get(net.minecraft.core.component.DataComponents.MAP_ID) ?: continue
                    val mapState = level.getMapData(mapId) ?: continue
                    
                    // Ряды (Y): сверху вниз (0, 1, 2)
                    // Используем порог 0.5 для надежного определения ряда
                    val row = when {
                        frame.y > gridCenterY + 0.5 -> 0
                        frame.y < gridCenterY - 0.5 -> 2
                        else -> 1
                    }
                    
                    // Колонки (X/Z): слева направо для игрока, смотрящего на доску
                    val col = when (facing) {
                        net.minecraft.core.Direction.NORTH -> { // Доска на севере, игрок смотрит на СЕВЕР (Z уменьшается)
                            // Для игрока, смотрящего на север: право — это +X, лево — это -X
                            when {
                                frame.x < gridCenterX - 0.5 -> 0 // Лево
                                frame.x > gridCenterX + 0.5 -> 2 // Право
                                else -> 1
                            }
                        }
                        net.minecraft.core.Direction.SOUTH -> { // Доска на юге, игрок смотрит на ЮГ (Z увеличивается)
                            // Для игрока, смотрящего на юг: право — это -X, лево — это +X
                            when {
                                frame.x > gridCenterX + 0.5 -> 0 // Лево
                                frame.x < gridCenterX - 0.5 -> 2 // Право
                                else -> 1
                            }
                        }
                        net.minecraft.core.Direction.WEST -> { // Доска на западе, игрок смотрит на ЗАПАД (X уменьшается)
                            // Для игрока, смотрящего на запад: право — это -Z, лево — это +Z
                            when {
                                frame.z > gridCenterZ + 0.5 -> 0 // Лево
                                frame.z < gridCenterZ - 0.5 -> 2 // Право
                                else -> 1
                            }
                        }
                        net.minecraft.core.Direction.EAST -> { // Доска на востоке, игрок смотрит на ВОСТОК (X увеличивается)
                            // Для игрока, смотрящего на восток: право — это +Z, лево — это -Z
                            when {
                                frame.z < gridCenterZ - 0.5 -> 0 // Лево
                                frame.z > gridCenterZ + 0.5 -> 2 // Право
                                else -> 1
                            }
                        }
                        else -> 1
                    }
                    
                    if (row !in 0..2 || col !in 0..2) continue

                    val middleColor = mapState.colors[8256].toInt() and 0xFF
                    if (middleColor == 114) board[row][col] = 'X'
                    else if (middleColor == 33) board[row][col] = 'O'
                }

                val bestMove = getBestMove(board) ?: return
                
                // Находим рамку, соответствующую лучшему ходу
                // Это гораздо надежнее, чем вычислять координаты вручную
                val targetFrame = boardFrames.find { frame ->
                    val r = when {
                        frame.y > gridCenterY + 0.5 -> 0
                        frame.y < gridCenterY - 0.5 -> 2
                        else -> 1
                    }
                    val c = when (facing) {
                        net.minecraft.core.Direction.NORTH -> when {
                            frame.x < gridCenterX - 0.5 -> 0
                            frame.x > gridCenterX + 0.5 -> 2
                            else -> 1
                        }
                        net.minecraft.core.Direction.SOUTH -> when {
                            frame.x > gridCenterX + 0.5 -> 0
                            frame.x < gridCenterX - 0.5 -> 2
                            else -> 1
                        }
                        net.minecraft.core.Direction.WEST -> when {
                            frame.z > gridCenterZ + 0.5 -> 0
                            frame.z < gridCenterZ - 0.5 -> 2
                            else -> 1
                        }
                        net.minecraft.core.Direction.EAST -> when {
                            frame.z < gridCenterZ - 0.5 -> 0
                            frame.z > gridCenterZ + 0.5 -> 2
                            else -> 1
                        }
                        else -> 1
                    }
                    r == bestMove.row && c == bestMove.col
                }

                if (targetFrame != null) {
                    nextBestMoveToMake = targetFrame.boundingBox.inflate(0.05)
                } else {
                    // Fallback на ручной расчет если рамка не найдена (не должно случаться)
                    val targetY = gridCenterY + (1 - bestMove.row)
                    var targetX = gridCenterX
                    var targetZ = gridCenterZ
                    
                    when (facing) {
                        net.minecraft.core.Direction.NORTH -> targetX = gridCenterX - (1 - bestMove.col)
                        net.minecraft.core.Direction.SOUTH -> targetX = gridCenterX + (1 - bestMove.col)
                        net.minecraft.core.Direction.WEST -> targetZ = gridCenterZ + (1 - bestMove.col)
                        net.minecraft.core.Direction.EAST -> targetZ = gridCenterZ - (1 - bestMove.col)
                        else -> {}
                    }
                    nextBestMoveToMake = AABB(targetX - 0.45, targetY - 0.45, targetZ - 0.45, targetX + 0.45, targetY + 0.45, targetZ + 0.45)
                }
            }
        } catch (e: Exception) {
            // Ошибка в логике
        }
    }

    data class Move(val row: Int, val col: Int)

    private fun getBestMove(board: Array<CharArray>): Move? {
        var bestVal = -1000
        var bestMove: Move? = null

        for (i in 0..2) {
            for (j in 0..2) {
                if (board[i][j] == '\u0000') {
                    board[i][j] = 'O'
                    val moveVal = minimax(board, 0, false)
                    board[i][j] = '\u0000'
                    if (moveVal > bestVal) {
                        bestMove = Move(i, j)
                        bestVal = moveVal
                    }
                }
            }
        }
        return bestMove
    }

    private fun minimax(board: Array<CharArray>, depth: Int, isMax: Boolean): Int {
        val score = evaluate(board)
        if (score == 10) return score - depth
        if (score == -10) return score + depth
        if (!isMovesLeft(board)) return 0

        return if (isMax) {
            var best = -1000
            for (i in 0..2) {
                for (j in 0..2) {
                    if (board[i][j] == '\u0000') {
                        board[i][j] = 'O'
                        best = maxOf(best, minimax(board, depth + 1, !isMax))
                        board[i][j] = '\u0000'
                    }
                }
            }
            best
        } else {
            var best = 1000
            for (i in 0..2) {
                for (j in 0..2) {
                    if (board[i][j] == '\u0000') {
                        board[i][j] = 'X'
                        best = minOf(best, minimax(board, depth + 1, !isMax))
                        board[i][j] = '\u0000'
                    }
                }
            }
            best
        }
    }

    private fun evaluate(b: Array<CharArray>): Int {
        for (row in 0..2) {
            if (b[row][0] == b[row][1] && b[row][1] == b[row][2]) {
                if (b[row][0] == 'O') return 10
                else if (b[row][0] == 'X') return -10
            }
        }
        for (col in 0..2) {
            if (b[0][col] == b[1][col] && b[1][col] == b[2][col]) {
                if (b[0][col] == 'O') return 10
                else if (b[0][col] == 'X') return -10
            }
        }
        if (b[0][0] == b[1][1] && b[1][1] == b[2][2]) {
            if (b[0][0] == 'O') return 10
            else if (b[0][0] == 'X') return -10
        }
        if (b[0][2] == b[1][1] && b[1][1] == b[2][0]) {
            if (b[0][2] == 'O') return 10
            else if (b[0][2] == 'X') return -10
        }
        return 0
    }

    private fun isMovesLeft(board: Array<CharArray>): Boolean {
        for (i in 0..2) for (j in 0..2) if (board[i][j] == '\u0000') return true
        return false
    }
}
