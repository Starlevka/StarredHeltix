package set.starlev.features.combat.solvers.dungeons

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import set.starlev.StarredHeltix
import set.starlev.render.RenderEvents
import org.joml.Intersectiond

/**
 * CreeperBeamsSolver - Сольвер для головоломки с лучами криперов в подземельях.
 * Исправленная версия с учетом оригинальной логики Skyblocker.
 */
object CreeperBeamsSolver {
    private val MC = Minecraft.getInstance()
    private val list = mutableListOf<Beam>()
    private var base: BlockPos? = null

    private const val FLOOR_Y = 68
    private const val BASE_Y = 74
    private const val TARGET_SEARCH_XZ = 16
    private const val TARGET_SEARCH_Y_MIN = 68
    private const val TARGET_SEARCH_Y_MAX = 86

    private val COLORS = arrayOf(
        0xFF00FFFF.toInt(), // LIGHT_BLUE
        0xFF00FF00.toInt(), // LIME
        0xFFFFFF00.toInt(), // YELLOW
        0xFFFF00FF.toInt(), // MAGENTA
        0xFFFFC0CB.toInt()  // PINK
    )
    private val GREEN = 0xFF00FF00.toInt()

    fun init() {
        RenderEvents.register { context ->
            if (!StarredHeltix.feature.dungeons.solvers.creeperBeams) return@register
            
            list.forEachIndexed { index, beam ->
                val color = if (beam.toDo) COLORS[index % COLORS.size] else GREEN
                val alpha = if (beam.toDo) 0.6f else 0.2f
                
                // Рендерим боксы на концах (с отключенным тестом глубины)
                context.renderBox(beam.outlineOne, (color shr 16 and 0xFF) / 255f, (color shr 8 and 0xFF) / 255f, (color and 0xFF) / 255f, alpha * 0.4f, true)
                context.renderBox(beam.outlineTwo, (color shr 16 and 0xFF) / 255f, (color shr 8 and 0xFF) / 255f, (color and 0xFF) / 255f, alpha * 0.4f, true)
                
                // Рендерим линию
                context.renderLine(beam.lineStart, beam.lineEnd, color, if (beam.toDo) 3f else 1.5f)
            }
        }

        var lastUpdate = 0L
        RenderEvents.register {
            val now = System.currentTimeMillis()
            if (now - lastUpdate > 200) {
                update()
                lastUpdate = now
            }
        }
    }

    private fun update() {
        if (!StarredHeltix.feature.dungeons.solvers.creeperBeams) {
            list.clear()
            base = null
            return
        }

        val player = MC.player ?: return
        val level = MC.level ?: return

        // Проверка на нахождение в подземельях
        if (!level.dimension().location().toString().startsWith("minecraft:dungeon_")) {
            list.clear()
            base = null
            return
        }

        // Попытка найти базу если она еще не найдена
        if (base == null) {
            base = findCreeperBase(level)
            if (base != null) {
                StarredHeltix.LOGGER.info("CreeperBeams: Found base at $base")
                val creeperPos = Vec3(base!!.x + 0.5, BASE_Y + 1.75, base!!.z + 0.5)
                val targets = findTargets(level, base!!)
                if (targets.isNotEmpty()) {
                    StarredHeltix.LOGGER.info("CreeperBeams: Found ${targets.size} targets")
                    solve(creeperPos, targets)
                }
            }
        }

        if (base != null) {
            // Обновляем состояние лучей
            list.forEach { it.updateState(level) }

            // Сброс если игрок ушел далеко или база больше не валидна
            if (!player.blockPosition().closerThan(base!!, 50.0) || !isTarget(level, base!!)) {
                list.clear()
                base = null
            }
        }
    }

    private fun findCreeperBase(level: Level): BlockPos? {
        val player = MC.player ?: return null
        val creepers = level.getEntitiesOfClass(Creeper::class.java, player.boundingBox.inflate(50.0))
        
        for (creeper in creepers) {
            val pos = creeper.position()
            val potentialBase = BlockPos.containing(pos.x, BASE_Y.toDouble(), pos.z)
            if (isTarget(level, potentialBase)) {
                return potentialBase
            }
        }
        return null
    }

    private fun findTargets(level: Level, basePos: BlockPos): List<BlockPos> {
        val targets = mutableListOf<BlockPos>()
        val start = BlockPos(basePos.x - TARGET_SEARCH_XZ, TARGET_SEARCH_Y_MIN, basePos.z - TARGET_SEARCH_XZ)
        val end = BlockPos(basePos.x + TARGET_SEARCH_XZ, TARGET_SEARCH_Y_MAX, basePos.z + TARGET_SEARCH_XZ)

        for (pos in BlockPos.betweenClosed(start, end)) {
            if (isTarget(level, pos)) {
                targets.add(pos.immutable())
            }
        }
        return targets
    }

    private fun solve(creeperPos: Vec3, targets: List<BlockPos>) {
        list.clear()
        
        val allLines = mutableListOf<Pair<Beam, Double>>()

        // Генерируем все возможные комбинации лучей (в одну сторону)
        for (i in targets.indices) {
            for (j in i + 1 until targets.size) {
                val beam = Beam(targets[i], targets[j])
                val dist = Intersectiond.distancePointLine(
                    creeperPos.x, creeperPos.y, creeperPos.z,
                    beam.lineStart.x, beam.lineStart.y, beam.lineStart.z,
                    beam.lineEnd.x, beam.lineEnd.y, beam.lineEnd.z
                )
                allLines.add(beam to dist)
            }
        }

        // Сортируем по расстоянию до крипера (выбираем те, что проходят ближе всего)
        allLines.sortBy { it.second }

        val result = mutableListOf<Beam>()
        val usedBlocks = mutableSetOf<BlockPos>()

        for (pair in allLines) {
            if (result.size >= 5) break
            val beam = pair.first
            if (beam.blockOne !in usedBlocks && beam.blockTwo !in usedBlocks) {
                result.add(beam)
                usedBlocks.add(beam.blockOne)
                usedBlocks.add(beam.blockTwo)
            }
        }

        if (result.size < 5) {
            StarredHeltix.LOGGER.warn("CreeperBeams: Only found ${result.size}/5 beams")
        }
        list.addAll(result)
    }

    private fun isTarget(level: Level, pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        return state.`is`(Blocks.SEA_LANTERN) || state.`is`(Blocks.PRISMARINE)
    }

    private class Beam(val blockOne: BlockPos, val blockTwo: BlockPos) {
        val lineStart = Vec3.atCenterOf(blockOne)
        val lineEnd = Vec3.atCenterOf(blockTwo)
        val outlineOne = AABB(blockOne)
        val outlineTwo = AABB(blockTwo)
        var toDo = true
        
        fun updateState(level: Level) {
            // Луч считается выполненным, если ОБА блока стали призмирином
            toDo = !(level.getBlockState(blockOne).`is`(Blocks.PRISMARINE) && 
                     level.getBlockState(blockTwo).`is`(Blocks.PRISMARINE))
        }
    }
}
