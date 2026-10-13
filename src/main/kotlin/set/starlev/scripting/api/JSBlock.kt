package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.phys.Vec3

/**
 * JS API: Сторона/грань блока: "down" | "up" | "north" | "south" | "west" | "east".
 */
class JSBlockFace private constructor(private val name: String) {
    fun getName(): String = name

    /** Противоположная грань. */
    fun getOpposite(): JSBlockFace = wrap(when (name) {
        "down" -> Direction.UP; "up" -> Direction.DOWN
        "north" -> Direction.SOUTH; "south" -> Direction.NORTH
        "west" -> Direction.EAST; "east" -> Direction.WEST
        else -> Direction.DOWN
    })

    fun getOffsetX(): Int = mc().stepX
    fun getOffsetY(): Int = mc().stepY
    fun getOffsetZ(): Int = mc().stepZ

    private fun mc(): Direction = Direction.byName(name) ?: Direction.DOWN

    override fun toString(): String = name

    companion object {
        val DOWN = JSBlockFace("down")
        val UP = JSBlockFace("up")
        val NORTH = JSBlockFace("north")
        val SOUTH = JSBlockFace("south")
        val WEST = JSBlockFace("west")
        val EAST = JSBlockFace("east")

        fun wrap(direction: Direction): JSBlockFace = when (direction) {
            Direction.DOWN -> DOWN; Direction.UP -> UP
            Direction.NORTH -> NORTH; Direction.SOUTH -> SOUTH
            Direction.WEST -> WEST; Direction.EAST -> EAST
        }

        /** Разбор строки: "north" или "minecraft:north". */
        fun parse(name: String): JSBlockFace? {
            val n = name.lowercase().trim().removePrefix("minecraft:")
            return when (n) {
                "down" -> DOWN; "up" -> UP
                "north" -> NORTH; "south" -> SOUTH
                "west" -> WEST; "east" -> EAST
                else -> null
            }
        }
    }
}

/**
 * JS API: Тип блока (реестровый блок, не позиция в мире).
 */
class JSBlockType private constructor(val mcValue: net.minecraft.world.level.block.Block) {

    /** Числовой ID в реестре. */
    fun getID(): Int = BuiltInRegistries.BLOCK.getId(mcValue)

    /** Регистровое имя: "minecraft:oak_planks". */
    fun getRegistryName(): String = BuiltInRegistries.BLOCK.getKey(mcValue).toString()

    /** Ключ перевода: "block.minecraft.oak_planks". */
    fun getTranslationKey(): String = mcValue.descriptionId

    /** Локализованное название ("Дубовые доски"). */
    fun getName(): String = mcValue.name.string

    /** Светимость блока (0-15). */
    fun getLightValue(): Int = mcValue.defaultBlockState().lightEmission

    /** Является ли источником redstone-сигнала. */
    fun canProvidePower(): Boolean = mcValue.defaultBlockState().isSignalSource

    /** Состояние по умолчанию (для проверок). */
    fun getDefaultState(): net.minecraft.world.level.block.state.BlockState = mcValue.defaultBlockState()

    /** Блок по умолчанию твёрдый (не прозрачный)? */
    fun isSolid(): Boolean = mcValue.defaultBlockState().canOcclude()

    override fun toString(): String = "BlockType{${getRegistryName()}}"

    companion object {
        private val cache = java.util.concurrent.ConcurrentHashMap<String, JSBlockType?>()

        /** Из регистрового имени ("minecraft:stone") или null. */
        fun of(blockName: String): JSBlockType? {
            val key = blockName.lowercase().trim()
            return cache.computeIfAbsent(key) {
                val id = net.minecraft.resources.Identifier.tryParse(it) ?: return@computeIfAbsent null
                val block = BuiltInRegistries.BLOCK.getValue(id)
                if (block === net.minecraft.world.level.block.Blocks.AIR &&
                    id != net.minecraft.resources.Identifier.withDefaultNamespace("air")) null
                else JSBlockType(block)
            }
        }

        /** Из MC-блока (внутреннее использование). */
        fun wrap(block: net.minecraft.world.level.block.Block): JSBlockType =
            of(BuiltInRegistries.BLOCK.getKey(block).toString()) ?: JSBlockType(block)

        /** Из числового ID реестра (BuiltInRegistries.BLOCK) или null. */
        fun fromID(id: Int): JSBlockType? {
            if (id < 0 || id >= BuiltInRegistries.BLOCK.size()) return null
            return wrap(BuiltInRegistries.BLOCK.byId(id))
        }
    }
}

/**
 * JS API: Конкретный блок в мире — тип + позиция + грань.
 */
class JSBlock private constructor(val type: JSBlockType, val pos: JSBlockPos, val face: JSBlockFace?) {
    val x: Int get() = pos.x
    val y: Int get() = pos.y
    val z: Int get() = pos.z

    fun withType(type: JSBlockType) = JSBlock(type, pos, face)

    fun withPos(pos: JSBlockPos) = JSBlock(type, pos, face)

    fun withFace(face: JSBlockFace) = JSBlock(type, pos, face)

    private fun level(): net.minecraft.world.level.Level? = Minecraft.getInstance().level

    private fun state(): net.minecraft.world.level.block.state.BlockState? =
        level()?.getBlockState(BlockPos(x, y, z))

    /** BlockState в этой позиции (или null вне мира). */
    fun getState(): net.minecraft.world.level.block.state.BlockState? = state()

    /** Регистровое имя блока в этой позиции ("minecraft:stone" или null вне мира). */
    fun getBlockName(): String? = JSWorld().getBlock(x, y, z)

    /** Испускает ли блок redstone-сигнал (в сторону [face] или вообще). */
    fun isEmittingPower(face: JSBlockFace? = null): Boolean {
        val level = level() ?: return false
        val p = BlockPos(x, y, z)
        return if (face != null) {
            level.hasSignal(p, Direction.byName(face.getName()) ?: Direction.DOWN)
        } else {
            listOf(JSBlockFace.DOWN, JSBlockFace.UP, JSBlockFace.NORTH, JSBlockFace.SOUTH, JSBlockFace.WEST, JSBlockFace.EAST)
                .any { isEmittingPower(it) }
        }
    }

    /** Сила сигнала, испускаемого блоком (0-15). */
    fun getEmittingPower(face: JSBlockFace? = null): Int {
        val level = level() ?: return 0
        val p = BlockPos(x, y, z)
        return if (face != null) {
            level.getSignal(p, Direction.byName(face.getName()) ?: Direction.DOWN)
        } else {
            listOf(JSBlockFace.DOWN, JSBlockFace.UP, JSBlockFace.NORTH, JSBlockFace.SOUTH, JSBlockFace.WEST, JSBlockFace.EAST)
                .map { getEmittingPower(it) }.firstOrNull { it != 0 } ?: 0
        }
    }

    /** Получает ли блок сигнал от соседей. */
    fun isReceivingPower(): Boolean {
        val level = level() ?: return false
        return level.hasNeighborSignal(BlockPos(x, y, z))
    }

    /** Максимальный сигнал от соседей (0-15). */
    fun getReceivingPower(): Int {
        val level = level() ?: return 0
        return level.getBestNeighborSignal(BlockPos(x, y, z))
    }

    /** Твёрдость блока (скорость ломания; -1 — неразрушим/вне мира). */
    fun getHardness(): Float {
        val level = level() ?: return -1f
        return state()?.getDestroySpeed(level, BlockPos(x, y, z)) ?: -1f
    }

    /** Жидкость (вода/лава)? */
    @Suppress("DEPRECATION")
    fun isLiquid(): Boolean = state()?.liquid() ?: false

    /** Полный непрозрачный блок? */
    fun isSolid(): Boolean = state()?.canOcclude() ?: false

    /** Соседний блок по грани (или null вне мира). */
    fun getNeighbor(face: JSBlockFace): JSBlock? {
        val n = pos.offset(face, 1)
        val name = JSWorld().getBlock(n.x, n.y, n.z) ?: return null
        return JSBlock.create(JSBlockType.of(name) ?: return null, n, null)
    }

    /** Все 6 соседей (загруженные). */
    fun getNeighbors(): List<JSBlock> =
        listOf(JSBlockFace.DOWN, JSBlockFace.UP, JSBlockFace.NORTH, JSBlockFace.SOUTH, JSBlockFace.WEST, JSBlockFace.EAST)
            .mapNotNull { getNeighbor(it) }

    /** Сломается ли этот блок предметом в руке (canHarvest). */
    fun canBeHarvested(): Boolean {
        val player = Minecraft.getInstance().player ?: return false
        val st = state() ?: return false
        return player.mainHandItem.isCorrectToolForDrops(st)
    }

    override fun toString(): String = "Block{type=$type, pos=($x, $y, $z)}"

    companion object {
        /** Блок в мире по координатам (или null вне мира). */
        @JvmStatic
        fun of(x: Int, y: Int, z: Int): JSBlock? {
            val name = JSWorld().getBlock(x, y, z) ?: return null
            return JSBlock.create(JSBlockType.of(name) ?: return null, JSBlockPos(x, y, z), null)
        }

        /** Блок в мире по позиции. */
        @JvmStatic
        fun of(pos: JSBlockPos): JSBlock? = of(pos.x, pos.y, pos.z)

        internal fun create(type: JSBlockType, pos: JSBlockPos, face: JSBlockFace?): JSBlock =
            JSBlock(type, pos, face)
    }
}

/**
 * JS API: Позиция блока в мире с операторами (+, -, up/down/north/...).
 */
class JSBlockPos(val x: Int, val y: Int, val z: Int) {

    // getX()/getY()/getZ() генерируются автоматически из val-свойств выше

    fun up(n: Int = 1) = JSBlockPos(x, y + n, z)
    fun down(n: Int = 1) = JSBlockPos(x, y - n, z)
    fun north(n: Int = 1) = JSBlockPos(x, y, z - n)
    fun south(n: Int = 1) = JSBlockPos(x, y, z + n)
    fun east(n: Int = 1) = JSBlockPos(x + n, y, z)
    fun west(n: Int = 1) = JSBlockPos(x - n, y, z)

    /** Смещение по грани. */
    fun offset(face: JSBlockFace, n: Int = 1) =
        JSBlockPos(x + face.getOffsetX() * n, y + face.getOffsetY() * n, z + face.getOffsetZ() * n)

    fun plus(other: JSBlockPos) = JSBlockPos(x + other.x, y + other.y, z + other.z)

    fun minus(other: JSBlockPos) = JSBlockPos(x - other.x, y - other.y, z - other.z)

    /** Расстояние до другой позиции. */
    fun distanceTo(other: JSBlockPos): Double {
        val dx = (x - other.x).toDouble()
        val dy = (y - other.y).toDouble()
        val dz = (z - other.z).toDouble()
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    /** Расстояние до игрока. */
    fun distanceToPlayer(): Double {
        val p = Minecraft.getInstance().player ?: return -1.0
        return kotlin.math.sqrt(
            (x + 0.5 - p.x).let { it * it } + (y + 0.5 - p.y).let { it * it } + (z + 0.5 - p.z).let { it * it }
        )
    }

    /** Внутренний MC BlockPos. */
    fun toMC(): BlockPos = BlockPos(x, y, z)

    /** Блок в этой позиции (или null вне мира). */
    fun getBlock(): JSBlock? = JSBlock.of(x, y, z)

    fun toVec3d(): Vec3 = Vec3(x.toDouble(), y.toDouble(), z.toDouble())

    override fun toString(): String = "BlockPos($x, $y, $z)"

    override fun equals(other: Any?): Boolean =
        other is JSBlockPos && other.x == x && other.y == y && other.z == z

    override fun hashCode(): Int = (x * 73856093) xor (y * 19349663) xor (z * 83492791)
}
