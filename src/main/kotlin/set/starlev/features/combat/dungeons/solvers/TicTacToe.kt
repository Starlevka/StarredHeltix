package set.starlev.features.combat.dungeons.solvers

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.ButtonBlock
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.saveddata.maps.MapId
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import net.minecraft.world.phys.AABB
import org.slf4j.LoggerFactory
import set.starlev.StarredHeltix
import set.starlev.render.RenderEvents
import set.starlev.utils.detectors.DungeonDetector
import set.starlev.utils.tictactoe.BoardIndex
import set.starlev.utils.tictactoe.TicTacToeUtils

object TicTacToe {
    private val LOGGER = LoggerFactory.getLogger(TicTacToe::class.java)
    private val mc = Minecraft.getInstance()
    private var nextBestMove: BlockPos? = null

    fun init() {
        LOGGER.info("[StarredHeltix] Инициализация солвера Крестики-нолики")

        RenderEvents.register { context ->
            if (!StarredHeltix.feature.dungeons.solvers.ticTacToe) return@register
            val pos = nextBestMove ?: return@register
            try {
                context.renderBox(AABB(pos), 0f, 1f, 0f, 0.4f, fill = true)
                context.renderBoxThroughBlocks(AABB(pos).inflate(0.01), 0f, 1f, 0f, 1.0f, fill = false, thickness = 4f)
            } catch (_: Exception) {}
        }

        var lastUpdate = 0L
        RenderEvents.register {
            val now = System.currentTimeMillis()
            if (now - lastUpdate > 200) {
                tick()
                lastUpdate = now
            }
        }
    }

    private fun tick() {
        if (!StarredHeltix.feature.dungeons.solvers.ticTacToe) {
            nextBestMove = null
            return
        }
        val level = mc.level ?: return
        val player = mc.player ?: return
        if (!DungeonDetector.isInDungeon()) {
            nextBestMove = null
            return
        }

        try {
            val searchBox = AABB(
                player.x - 30, player.y - 10, player.z - 30,
                player.x + 30, player.y + 10, player.z + 30
            )
            val frames = level.getEntities(null, searchBox) { it is ItemFrame }
                .filterIsInstance<ItemFrame>()

            // Ищем кнопки на стенах в той же области
            val buttonPositions = mutableListOf<BlockPos>()
            for (x in (player.x - 30).toInt()..(player.x + 30).toInt()) {
                for (y in (player.y - 10).toInt()..(player.y + 10).toInt()) {
                    for (z in (player.z - 30).toInt()..(player.z + 30).toInt()) {
                        val pos = BlockPos(x, y, z)
                        val state = level.getBlockState(pos)
                        if (state.`is`(BlockTags.STONE_BUTTONS)) {
                            buttonPositions.add(pos)
                        }
                    }
                }
            }

            // Группируем рамки по плоскости
            data class PlaneKey(val dir: Direction, val wallCoord: Int)
            val groups = frames.groupBy { f ->
                val p = f.blockPosition()
                val wall = when (f.direction) {
                    Direction.NORTH, Direction.SOUTH -> p.z
                    Direction.EAST, Direction.WEST -> p.x
                    else -> p.y
                }
                PlaneKey(f.direction, wall)
            }

            // Ищем группу с 1+ рамками (может быть 1 рамка + 8 кнопок)
            val boardGroup = groups.entries
                .filter { it.value.size >= 1 }
                .maxByOrNull { it.value.size }

            if (boardGroup == null || boardGroup.value.size < 1) {
                nextBestMove = null
                return
            }

            val boardFrames = boardGroup.value
            val facing = boardGroup.key.dir

            // Получаем уникальные строки (Y) и столбцы (X/Z)
            // Если рамок мало (1-2), определяем границы по кнопкам
            val allPositions = boardFrames.map { it.blockPosition() }.toMutableList()
            // Добавляем позиции кнопок той же плоскости для определения границ
            for (buttonPos in buttonPositions) {
                val buttonDir = when (facing) {
                    Direction.NORTH, Direction.SOUTH -> buttonPos.z
                    Direction.EAST, Direction.WEST -> buttonPos.x
                    else -> buttonPos.y
                }
                if (buttonDir == boardGroup.key.wallCoord) {
                    allPositions.add(buttonPos)
                }
            }

            val uniqueRows = allPositions.map { it.y }.distinct().sortedDescending()
            val uniqueCols = allPositions.map { p ->
                when (facing) {
                    Direction.NORTH, Direction.SOUTH -> p.x
                    Direction.EAST, Direction.WEST -> p.z
                    else -> p.x
                }
            }.distinct().sorted()

            if (uniqueRows.size < 2 || uniqueCols.size < 2) {
                nextBestMove = null
                return
            }

            // Строим карту позиций и доску
            val positionMap = mutableMapOf<Pair<Int, Int>, BlockPos>()
            val board = Array(3) { CharArray(3) { '\u0000' } }
            var framesWithMaps = 0

            for (frame in boardFrames) {
                val pos = frame.blockPosition()
                val row = uniqueRows.indexOf(pos.y)
                val col = uniqueCols.indexOf(when (facing) {
                    Direction.NORTH, Direction.SOUTH -> pos.x
                    Direction.EAST, Direction.WEST -> pos.z
                    else -> pos.x
                })

                if (row < 0 || row >= uniqueRows.size || col < 0 || col >= uniqueCols.size) continue
                if (row > 2 || col > 2) continue

                positionMap[Pair(row, col)] = pos

                val item = frame.item
                if (!item.isEmpty && item.`is`(Items.FILLED_MAP)) {
                    framesWithMaps++
                    val mapId = item.getOrDefault(DataComponents.MAP_ID, MapId(-1)).id()
                    if (mapId != -1) {
                        val mapData = level.getMapData(MapId(mapId))
                        if (mapData != null) {
                            // Определяем X или O по цвету среднего пикселя карты
                            val middleColor = mapData.colors[8256].toInt() and 0xFF
                            if (middleColor == 114) {
                                board[row][col] = 'X'
                            } else if (middleColor == 33) {
                                board[row][col] = 'O'
                            }
                        }
                    }
                }
            }

            // Добавляем кнопки в positionMap как пустые клетки
            for (buttonPos in buttonPositions) {
                val row = uniqueRows.indexOf(buttonPos.y)
                val col = uniqueCols.indexOf(when (facing) {
                    Direction.NORTH, Direction.SOUTH -> buttonPos.x
                    Direction.EAST, Direction.WEST -> buttonPos.z
                    else -> buttonPos.x
                })

                if (row >= 0 && row < uniqueRows.size && col >= 0 && col < uniqueCols.size && row <= 2 && col <= 2) {
                    if (!positionMap.containsKey(Pair(row, col))) {
                        positionMap[Pair(row, col)] = buttonPos
                    }
                }
            }

            // Проверяем, что пазл не завершён и сейчас ход игрока
            // framesWithMaps == 0 — первый ход (сервер ещё не ходил)
            // framesWithMaps % 2 == 1 — сервер сходил, теперь ход игрока
            if (framesWithMaps == 9 || framesWithMaps % 2 == 0) {
                nextBestMove = null
                return
            }

            // Логируем состояние доски
            LOGGER.debug("[TicTacToe] Доска ({}x{}):", uniqueRows.size, uniqueCols.size)
            for (r in 0 until minOf(uniqueRows.size, 3)) {
                val line = (0 until minOf(uniqueCols.size, 3)).joinToString(" ") { c ->
                    val ch = board[r][c]
                    if (ch == '\u0000') "." else ch.toString()
                }
                LOGGER.debug("  $line")
            }

            // Проверяем пустые клетки
            val emptyCells = mutableListOf<Pair<Int, Int>>()
            for (r in 0 until minOf(uniqueRows.size, 3)) {
                for (c in 0 until minOf(uniqueCols.size, 3)) {
                    if (board[r][c] == '\u0000' && positionMap.containsKey(Pair(r, c))) {
                        emptyCells.add(Pair(r, c))
                    }
                }
            }

            if (emptyCells.isEmpty()) {
                nextBestMove = null
                return
            }

            // Минимакс считает ход для O, а игрок = X, поэтому меняем местами
            val swappedBoard = Array(3) { r ->
                CharArray(3) { c ->
                    when (board[r][c]) {
                        'X' -> 'O'
                        'O' -> 'X'
                        else -> '\u0000'
                    }
                }
            }
            val bestMove = TicTacToeUtils.getBestMove(swappedBoard)
            val targetPos = positionMap[Pair(bestMove.row, bestMove.column)]

            if (targetPos != null && board[bestMove.row][bestMove.column] == '\u0000') {
                nextBestMove = targetPos
                LOGGER.info("[TicTacToe] Лучший ход: row={}, col={}", bestMove.row, bestMove.column)
            } else {
                LOGGER.warn("[TicTacToe] Минимакс вернул невалидный ход, фоллбэк на пустые клетки")
                // Пробуем все пустые клетки по порядку
                for (cell in emptyCells) {
                    val pos = positionMap[cell]
                    if (pos != null) {
                        nextBestMove = pos
                        LOGGER.info("[TicTacToe] Фоллбэк ход: row={}, col={}", cell.first, cell.second)
                        break
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("[TicTacToe] Ошибка", e)
            nextBestMove = null
        }
    }
}