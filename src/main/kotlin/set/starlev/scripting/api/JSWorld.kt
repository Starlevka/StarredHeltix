package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.LightLayer
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.ScoreHolder

/**
 * JS API: Обёртка сущности для World API.
 * Чтение состояния + безопасные клиентские флаги (glowing).
 */
class JSEntity(val mcValue: Entity) {
    fun getName(): String = mcValue.name.string

    fun getUUID(): String = mcValue.uuid.toString()

    /** Идентификатор типа сущности: "minecraft:zombie". */
    fun getType(): String = BuiltInRegistries.ENTITY_TYPE.getKey(mcValue.type).toString()

    fun isPlayer(): Boolean = mcValue is net.minecraft.world.entity.player.Player

    fun isLiving(): Boolean = mcValue is LivingEntity

    fun isArmorStand(): Boolean = mcValue is ArmorStand

    fun getX(): Double = mcValue.x

    fun getY(): Double = mcValue.y

    fun getZ(): Double = mcValue.z

    fun getBlockX(): Int = mcValue.blockX

    fun getBlockY(): Int = mcValue.blockY

    fun getBlockZ(): Int = mcValue.blockZ

    fun getDistanceToPlayer(): Double {
        val player = Minecraft.getInstance().player ?: return 0.0
        return mcValue.distanceTo(player).toDouble()
    }

    /** Расстояние до точки (евклидово). */
    fun distanceTo(x: Double, y: Double, z: Double): Double = kotlin.math.sqrt(mcValue.distanceToSqr(x, y, z))
    /** Квадрат расстояния (быстрее, без sqrt). */
    fun distanceToSqr(x: Double, y: Double, z: Double): Double = mcValue.distanceToSqr(x, y, z)

    fun getHealth(): Float = (mcValue as? LivingEntity)?.health ?: -1f

    fun getMaxHealth(): Float = (mcValue as? LivingEntity)?.maxHealth ?: -1f

    /** Глоссарий подсказки (имя над сущностью) — обычно содержит уровень моба на Heltix. */
    fun getDisplayName(): String = mcValue.displayName.string

    fun getYaw(): Float = mcValue.yRot

    fun getPitch(): Float = mcValue.xRot

    /** Состояния сущности (чтение). */
    fun isSprinting(): Boolean = mcValue.isSprinting

    fun isSneaking(): Boolean = mcValue.isShiftKeyDown

    fun isOnGround(): Boolean = mcValue.onGround()

    fun isInWater(): Boolean = mcValue.isInWater

    fun isInLava(): Boolean = mcValue.isInLava

    fun isOnFire(): Boolean = mcValue.isOnFire

    fun isInvisible(): Boolean = mcValue.isInvisible

    fun isGlowing(): Boolean = mcValue.hasGlowingTag()

    fun isSpectator(): Boolean = mcValue.isSpectator

    /** Имя команды сущности (если есть), иначе "". */
    fun getTeamName(): String = mcValue.getTeam()?.name ?: ""

    /** Клиентская подсветка (только локально, видна до перезахода/смерти сущности). */
    fun setGlowing(glowing: Boolean) {
        if (glowing) {
            val module = set.starlev.scripting.SHScriptEngine.currentModule ?: "unknown"
            set.starlev.scripting.ModuleGlowRegistry.setGlow(mcValue.id, module, 0xFFFFFFFF.toInt())
            // Без setGlowingTag — миксин делает glow по карте цветов, без кэша в энтити
        } else {
            set.starlev.scripting.ModuleGlowRegistry.clearGlow(mcValue.id)
            try { if (mcValue.hasGlowingTag()) mcValue.setGlowingTag(false) } catch (_: Exception) {}
        }
    }

    /** Клиентская подсветка с цветом ARGB (0xAARRGGBB). Сквозь стены. */
    @JvmOverloads
    fun setGlowing(glowing: Boolean, color: Double) {
        if (!glowing) {
            setGlowing(false)
            return
        }
        val module = set.starlev.scripting.SHScriptEngine.currentModule ?: "unknown"
        val c = color.toInt()
        set.starlev.scripting.ModuleGlowRegistry.setGlow(mcValue.id, module, c)
    }

    /** Установить цвет glow (если уже glowing) или включить с цветом. */
    fun setGlowColor(color: Double) {
        val module = set.starlev.scripting.SHScriptEngine.currentModule ?: "unknown"
        set.starlev.scripting.ModuleGlowRegistry.setGlow(mcValue.id, module, color.toInt())
    }

    /** Снять подсветку модуля (alias). */
    fun clearGlow() = setGlowing(false)

    /** Текущий цвет модульного glow или -1 если нет. */
    fun getGlowColor(): Int = set.starlev.scripting.ModuleGlowRegistry.getGlowColor(mcValue.id)

    /** Цвет команды (строкой: "red", "gold"...) или "". */
    fun getTeamColor(): String {
        val team: PlayerTeam = mcValue.getTeam() ?: return ""
        val color = team.getColor()
        return if (color.isPresent) color.get().name.lowercase() else ""
    }

    // ==================== ctjs Entity-паритет доп. ====================

    /** Имя Java-класса сущности, например "Zombie". */
    fun getClassName(): String = mcValue.javaClass.simpleName

    /** Горит ли сущность (alias isOnFire для CTJS). */
    fun isBurning(): Boolean = mcValue.isOnFire

    /** Сталкивается ли хитбокс с другой коллизией. */
    fun isCollided(): Boolean {
        val lvl = Minecraft.getInstance().level ?: return false
        return lvl.getEntities(mcValue, mcValue.boundingBox.inflate(0.01)).isNotEmpty()
    }

    /** Пройденная дистанция (moveDist / 0.6). */
    fun getDistanceWalked(): Float = mcValue.moveDist / 0.6f

    /** Оставшиеся тики горения (0 — не горит, -20..0 — урон). */
    fun getFireResistance(): Int = mcValue.remainingFireTicks

    /** Может ли сущность сталкиваться. */
    fun canBeCollidedWith(): Boolean = mcValue.canBeCollidedWith(null)

    /** Может ли быть сдвинута поршнем/игроком. */
    fun canBePushed(): Boolean = mcValue.isPushable

    /** Вне границы мира. */
    fun isOutsideBorder(): Boolean {
        val lvl = Minecraft.getInstance().level ?: return false
        return !lvl.worldBorder.isWithinBounds(mcValue.blockPosition())
    }

    /** Позиция глаз с учётом partialTicks. */
    fun getEyePosition(partialTicks: Float = 0f): Map<String, Double> {
        val eye = mcValue.getEyePosition(partialTicks)
        return mapOf("x" to eye.x, "y" to eye.y, "z" to eye.z)
    }

    /** Максимальное время в портале (ticks). */
    fun getMaxInPortalTime(): Int = mcValue.portalCooldown

    /** Дистанция до другой сущности (евклидово). */
    fun distanceToEntity(other: JSEntity): Double = kotlin.math.sqrt(mcValue.distanceToSqr(other.mcValue))

    /** Квадрат дистанции до другой сущности. */
    fun distanceToSqrEntity(other: JSEntity): Double = mcValue.distanceToSqr(other.mcValue)

    // ==================== ctjs Entity-паритет ====================

    /** Мёртва ли сущность (обратно isAlive). */
    fun isDead(): Boolean = !mcValue.isAlive

    /** Движение за тик (deltaMovement): {x, y, z}. */
    fun getMotion(): Map<String, Double> {
        val d = mcValue.deltaMovement
        return mapOf("x" to d.x, "y" to d.y, "z" to d.z)
    }

    fun getMotionX(): Double = mcValue.deltaMovement.x
    fun getMotionY(): Double = mcValue.deltaMovement.y
    fun getMotionZ(): Double = mcValue.deltaMovement.z

    /** Позиция в прошлом тике (интерполяция рендера). */
    fun getLastX(): Double = mcValue.xOld
    fun getLastY(): Double = mcValue.yOld
    fun getLastZ(): Double = mcValue.zOld

    /** Позиция для отрисовки (lerp между прошлым и текущим тиком). */
    fun getRenderX(partialTicks: Float = 0f): Double = mcValue.xOld + (mcValue.x - mcValue.xOld) * partialTicks
    fun getRenderY(partialTicks: Float = 0f): Double = mcValue.yOld + (mcValue.y - mcValue.yOld) * partialTicks
    fun getRenderZ(partialTicks: Float = 0f): Double = mcValue.zOld + (mcValue.z - mcValue.zOld) * partialTicks

    /** Ширина/высота хитбокса. */
    fun getWidth(): Float = mcValue.bbWidth
    fun getHeight(): Float = mcValue.bbHeight

    /** Высота глаз. */
    fun getEyeHeight(): Float = mcValue.eyeHeight

    /** Сколько тиков сущность существует. */
    fun getTicksExisted(): Int = mcValue.tickCount

    /** Запас воздуха (у игроков/мобов под водой). */
    fun getAir(): Int = mcValue.airSupply

    /** Наездник сверху (если сущность едет на другой) или null. */
    fun getRiding(): JSEntity? = mcValue.vehicle?.let { JSEntity(it) }

    /** Пассажиры сверху (кто едет НА этой сущности). */
    fun getRiders(): List<JSEntity> = mcValue.passengers.map { JSEntity(it) }

    /** Иммунна к огню. */
    fun isImmuneToFire(): Boolean = mcValue.fireImmune()

    /** Мокрая (вода или дождь). */
    fun isWet(): Boolean = mcValue.isInWaterOrRain

    /** Молчит (без звуков). */
    fun isSilent(): Boolean = mcValue.isSilent

    /** Сквозь стены (noPhysics, обычно false). */
    fun hasNoClip(): Boolean = mcValue.noPhysics

    /** Высота шага (подъёма на блок). */
    fun getStepHeight(): Float = mcValue.maxUpStep()

    /** Вектор взгляда: {x, y, z}. */
    fun getLookVector(): Map<String, Double> {
        val look = mcValue.getLookAngle()
        return mapOf("x" to look.x, "y" to look.y, "z" to look.z)
    }

    /** Идентификатор измерения, где находится сущность: "minecraft:overworld". */
    fun getDimension(): String =
        mcValue.level().dimension().identifier().toString()

    /** Пинг игрока из таб-листа (-1 — не игрок/не найден). */
    fun getPing(): Int {
        val player = mcValue as? net.minecraft.world.entity.player.Player ?: return -1
        return Minecraft.getInstance().connection?.getPlayerInfo(player.uuid)?.latency ?: -1
    }

    override fun toString(): String =
        "Entity{type=${getType()}, name=${getName()}, pos=(${getX()}, ${getY()}, ${getZ()})}"
}

/**
 * JS API: Объект `world` — доступ к миру, сущностям, блокам, скорборду, таб-листу.
 * Аналог World + Scoreboard + TabList API из ctjs.
 */
class JSWorld {
    private val client: Minecraft get() = Minecraft.getInstance()

    /** Мир загружен. */
    fun isLoaded(): Boolean = client.level != null

    /** Все отрисовываемые сущности в мире. */
    fun getAllEntities(): List<JSEntity> {
        val level = client.level ?: return emptyList()
        return level.entitiesForRendering().map { JSEntity(it) }
    }

    /** Сущности по имени (совпадение без учёта регистра, частичное). */
    fun getEntitiesByName(name: String): List<JSEntity> =
        getAllEntities().filter { it.getName().contains(name, ignoreCase = true) }

    /** Сущности по идентификатору типа, например "minecraft:zombie". */
    fun getEntitiesOfType(type: String): List<JSEntity> =
        getAllEntities().filter {
            it.getType().equals(type, ignoreCase = true) ||
                it.getType().endsWith(":$type", ignoreCase = true)
        }

    /** Все игроки (кроме себя) в радиусе [radius] блоков. */
    fun getPlayers(radius: Double = 128.0): List<JSEntity> =
        getAllEntities().filter { it.isPlayer() && it.getDistanceToPlayer() <= radius }

    /** Игрок по нику (загруженные чанки), или null. */
    fun getPlayerByName(name: String): JSEntity? =
        getAllEntities().firstOrNull { it.isPlayer() && it.getName().equals(name, ignoreCase = true) }

    /** Есть ли игрок с таким ником рядом. */
    fun hasPlayer(name: String): Boolean = getPlayerByName(name) != null

    /** Ближайшая сущность по имени (или null). */
    fun getClosestEntityByName(name: String): JSEntity? =
        getEntitiesByName(name).minByOrNull { it.getDistanceToPlayer() }

    /** Сущность, в которую смотрит игрок (в пределах [maxDistance]). */
    fun getLookedAtEntity(maxDistance: Double = 6.0): JSEntity? {
        val hit = client.hitResult ?: return null
        if (hit.type != net.minecraft.world.phys.HitResult.Type.ENTITY) return null
        val entity = (hit as net.minecraft.world.phys.EntityHitResult).entity
        if (entity.distanceTo(client.player ?: return null) > maxDistance) return null
        return JSEntity(entity)
    }

    // ==================== Блоки и свет ====================

    /** Идентификатор блока по координатам, например "minecraft:diamond_ore" (или null вне мира). */
    fun getBlock(x: Int, y: Int, z: Int): String? {
        val level = client.level ?: return null
        val state = level.getBlockState(BlockPos(x, y, z))
        return BuiltInRegistries.BLOCK.getKey(state.block).toString()
    }

    fun getBlockName(x: Int, y: Int, z: Int): String? = getBlock(x, y, z)

    /** Блок как объект (JSBlock): тип + позиция + redstone/твёрдость. null вне мира. */
    fun getBlockAt(x: Int, y: Int, z: Int): JSBlock? = JSBlock.of(x, y, z)

    /** Тип блока по регистровому имени: world.getBlockType("minecraft:beacon"). */
    fun getBlockType(name: String): JSBlockType? = JSBlockType.of(name)

    /** Тип блока по координатам. */
    fun getBlockType(x: Int, y: Int, z: Int): JSBlockType? = getBlock(x, y, z)?.let { JSBlockType.of(it) }

    /** Сущности по идентификатору типа (алиас getEntitiesOfType). */
    fun getEntitiesByType(type: String): List<JSEntity> = getEntitiesOfType(type)

    /** Сущности в радиусе от точки. */
    fun getEntitiesInRadius(x: Double, y: Double, z: Double, radius: Double): List<JSEntity> =
        getAllEntities().filter {
            val dx = it.getX() - x; val dy = it.getY() - y; val dz = it.getZ() - z
            dx * dx + dy * dy + dz * dz <= radius * radius
        }

    /** Ближайший игрок к точке (или null). */
    fun getNearestPlayerTo(x: Double, y: Double, z: Double): JSEntity? =
        getAllEntities().filter { it.isPlayer() }.minByOrNull {
            val dx = it.getX() - x; val dy = it.getY() - y; val dz = it.getZ() - z
            dx * dx + dy * dy + dz * dz
        }

    /** Расстояние от игрока до точки. */
    fun getDistanceToPlayer(x: Double, y: Double, z: Double): Double {
        val p = client.player ?: return -1.0
        val dx = x - p.x; val dy = y - p.y; val dz = z - p.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    /** Записи скорборда: {owner, value, display} — сырые данные Sidebar. */
    fun getScoreboardEntries(): List<Map<String, Any>> {
        val scoreboard = client.level?.scoreboard ?: return emptyList()
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return emptyList()
        return scoreboard.listPlayerScores(objective).map { entry ->
            mapOf(
                "owner" to entry.owner(),
                "value" to entry.value(),
                "display" to (entry.display()?.string ?: entry.ownerName().string)
            )
        }
    }

    /** Уровень небесного света (0-15) на позиции. */
    fun getSkyLightLevel(x: Int, y: Int, z: Int): Int =
        client.level?.getBrightness(LightLayer.SKY, BlockPos(x, y, z)) ?: 0

    /** Уровень света от блоков (факелы и т.п., 0-15) на позиции. */
    fun getBlockLightLevel(x: Int, y: Int, z: Int): Int =
        client.level?.getBrightness(LightLayer.BLOCK, BlockPos(x, y, z)) ?: 0

    // ==================== Редактирование скорборда (ctjs Scoreboard) ====================
    // Клиентские правки: видны только вам, сбрасываются серверным пакетом.

    private fun sidebarObjective() = client.level?.scoreboard?.getDisplayObjective(DisplaySlot.SIDEBAR)

    /** Установить заголовок скорборда (клиентски). */
    fun setScoreboardTitle(title: String) {
        sidebarObjective()?.displayName = net.minecraft.network.chat.Component.literal(title.replace('&', '§'))
    }

    /**
     * Заменить отображаемую строку с заданным значением (score).
     * @param override true — удалить все строки с этим score и вставить новую
     */
    fun setScoreboardLine(score: Int, line: String, override: Boolean = false) {
        val scoreboard = client.level?.scoreboard ?: return
        val objective = sidebarObjective() ?: return
        val text = net.minecraft.network.chat.Component.literal(line.replace('&', '§'))
        if (override) {
            removeScoreboardLines(score)
            val holder = ScoreHolder.forNameOnly(Math.random().toString())
            val access = scoreboard.getOrCreatePlayerScore(holder, objective, true)
            access.display(text)
            access.set(score)
            return
        }
        for (entry in scoreboard.listPlayerScores(objective)) {
            if (entry.value() == score) {
                val access = scoreboard.getOrCreatePlayerScore(
                    ScoreHolder.forNameOnly(entry.owner()), objective, true
                )
                access.display(text)
            }
        }
    }

    /** Добавить строку со своим score. */
    fun addScoreboardLine(score: Int, line: String) {
        val scoreboard = client.level?.scoreboard ?: return
        val objective = sidebarObjective() ?: return
        val holder = ScoreHolder.forNameOnly(Math.random().toString())
        val access = scoreboard.getOrCreatePlayerScore(holder, objective, true)
        access.display(net.minecraft.network.chat.Component.literal(line.replace('&', '§')))
        access.set(score)
    }

    /** Удалить все строки со значением score. */
    fun removeScoreboardLines(score: Int) {
        val scoreboard = client.level?.scoreboard ?: return
        val objective = sidebarObjective() ?: return
        scoreboard.listPlayerScores(objective)
            .filter { it.value() == score }
            .forEach { entry ->
                scoreboard.resetSinglePlayerScore(
                    ScoreHolder.forNameOnly(entry.owner()), objective
                )
            }
    }

    // ==================== Редактирование таб-листа (ctjs TabList) ====================

    /** Установить шапку таб-листа (клиентски). */
    fun setTabListHeader(header: String) {
        client.gui.hud.tabList.setHeader(
            net.minecraft.network.chat.Component.literal(header.replace('&', '§'))
        )
    }

    /** Установить подвал таб-листа (клиентски). */
    fun setTabListFooter(footer: String) {
        client.gui.hud.tabList.setFooter(
            net.minecraft.network.chat.Component.literal(footer.replace('&', '§'))
        )
    }

    /** Очистить шапку таб-листа (вернуть серверную). */
    fun clearTabListHeader() {
        client.gui.hud.tabList.setHeader(null)
    }

    /** Очистить подвал таб-листа. */
    fun clearTabListFooter() {
        client.gui.hud.tabList.setFooter(null)
    }

    /** Отображаемое имя игрока в таб-листе (с командой/цветом). */
    fun getPlayerTabName(name: String): String? {
        val info = client.connection?.getPlayerInfo(name) ?: return null
        return info.tabListDisplayName?.string
            ?: info.team?.let { net.minecraft.world.scores.PlayerTeam.formatNameForTeam(it, Component.literal(info.profile.name)).string }
            ?: info.profile.name
    }

    // ==================== Блок-энтити (ctjs BlockEntity) ====================

    /** Тип блок-энтити на позиции: "minecraft:chest" или null (не энтити/вне мира). */
    fun getBlockEntityType(x: Int, y: Int, z: Int): String? {
        val level = client.level ?: return null
        val be = level.getBlockEntity(BlockPos(x, y, z)) ?: return null
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.type).toString()
    }

    /** Все блок-энтити в чанках вокруг игрока (эффективно, без перебора каждого блока). */
    fun getAllBlockEntities(limit: Int = 512): List<Map<String, Any>> {
        val level = client.level ?: return emptyList()
        val p = client.player ?: return emptyList()
        val out = mutableListOf<Map<String, Any>>()
        val chunkRadius = 8 // ~128 блоков
        val pcx = p.blockX shr 4
        val pcz = p.blockZ shr 4
        for (dx in -chunkRadius..chunkRadius) {
            for (dz in -chunkRadius..chunkRadius) {
                if (out.size >= limit) return out
                val blockPos = BlockPos((pcx + dx) * 16, p.blockY, (pcz + dz) * 16)
                val chunk = try { level.getChunkAt(blockPos) } catch (_: Exception) { continue }
                val beMap = try { (chunk as? net.minecraft.world.level.chunk.LevelChunk)?.getBlockEntities() } catch (_: Exception) { null } ?: continue
                for ((pos, be) in beMap) {
                    if (out.size >= limit) break
                    val type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.type).toString()
                    out.add(mapOf("type" to type, "x" to pos.x, "y" to pos.y, "z" to pos.z))
                }
            }
        }
        return out
    }

    /** Блок-энтити в радиусе от точки (фильтр по дистанции). */
    fun getBlockEntitiesInRadius(x: Double, y: Double, z: Double, radius: Double, limit: Int = 128): List<Map<String, Any>> {
        val all = getAllBlockEntities(limit * 4)
        if (all.isEmpty()) return emptyList()
        val r2 = radius * radius
        val out = mutableListOf<Map<String, Any>>()
        for (e in all) {
            if (out.size >= limit) break
            val bx = e["x"] as Int; val by = e["y"] as Int; val bz = e["z"] as Int
            val dx = bx + 0.5 - x; val dy = by + 0.5 - y; val dz = bz + 0.5 - z
            if (dx*dx + dy*dy + dz*dz <= r2) out.add(e)
        }
        // Фолбэк для маленьких радиусов вне чанкового кэша — прямой скан куба (cheap, radius <=16)
        if (out.isEmpty() && radius <= 16) {
            val level = client.level ?: return out
            val ir = radius.toInt().coerceIn(1, 16)
            for (dx in -ir..ir) for (dy in -ir..ir) for (dz in -ir..ir) {
                if (out.size >= limit) return out
                val bx = (x + dx).toInt(); val by = (y + dy).toInt(); val bz = (z + dz).toInt()
                val ddx = bx + 0.5 - x; val ddy = by + 0.5 - y; val ddz = bz + 0.5 - z
                if (ddx*ddx + ddy*ddy + ddz*ddz > r2) continue
                val be = level.getBlockEntity(BlockPos(bx, by, bz)) ?: continue
                val type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.type).toString()
                // deduplicate
                if (out.any { it["x"] == bx && it["y"] == by && it["z"] == bz }) continue
                out.add(mapOf("type" to type, "x" to bx, "y" to by, "z" to bz))
            }
        }
        return out
    }

    /** Все сущности заданного класса Java (например "net.minecraft.world.entity.monster.Zombie") — аналог CTJS getAllEntitiesOfType(Class). */
    fun getAllEntitiesOfType(clazzName: String): List<JSEntity> {
        return try {
            val clazz = Class.forName(clazzName)
            getAllEntities().filter { clazz.isInstance(it.mcValue) }
        } catch (_: Exception) {
            getEntitiesOfType(clazzName)
        }
    }

    /** Чанк на позиции (инфо). */
    fun getChunk(x: Int, y: Int, z: Int): Map<String, Any>? {
        val level = client.level ?: return null
        val chunk = level.getChunk(BlockPos(x, y, z)) ?: return null
        val pos = chunk.pos
        return mapOf("x" to pos.x, "z" to pos.z)
    }

    // ==================== Состояние мира ====================

    /** IP текущего сервера (или null в главном меню). */
    fun getServerIP(): String? = client.connection?.connection?.remoteAddress?.toString()

    /** Идентификатор текущего измерения, например "minecraft:overworld" (или null вне мира). */
    fun getDimension(): String? = client.level?.dimension()?.identifier()?.toString()

    /** Количество загруженных чанков (для систем оптимизации/мониторинга). */
    fun getLoadedChunkCount(): Int = client.level?.chunkSource?.getLoadedChunksCount() ?: -1

    /** Позиция камеры (глаза игрока) — для LOD-решений. */
    fun getCameraPos(): Map<String, Double> {
        val camera = client.gameRenderer.mainCamera()
        val pos = camera.position()
        return mapOf("x" to pos.x, "y" to pos.y, "z" to pos.z)
    }

    /** Идёт ли дождь. */
    fun isRaining(): Boolean = client.level?.isRaining ?: false

    /** Сила дождя (0.0-1.0). */
    fun getRainingStrength(): Float = client.level?.getRainLevel(1.0f) ?: -1f

    /** Игровое время мира (тики). */
    fun getTime(): Long = client.level?.gameTime ?: -1L

    /** Сложность мира: "peaceful" | "easy" | "normal" | "hard" | "". */
    fun getDifficulty(): String {
        val level = client.level ?: return ""
        return level.difficulty.name.lowercase()
    }

    /** TPS мира (обычно 20). */
    fun getTicksPerSecond(): Int {
        val mpt = client.level?.tickRateManager()?.millisecondsPerTick() ?: return 20
        return (1000.0 / mpt).toInt()
    }

    /** Затемнение неба (0-15, ночью выше). */
    fun getSkyDarken(): Int = client.level?.skyDarken ?: 0

    /** Границы мира (world border): {centerX, centerZ, size, targetSize, timeUntilTarget}. */
    fun getBorder(): Map<String, Any> {
        val border = client.level?.worldBorder
            ?: return mapOf("centerX" to 0.0, "centerZ" to 0.0, "size" to 0.0, "targetSize" to 0.0, "timeUntilTarget" to 0L)
        return mapOf(
            "centerX" to border.centerX,
            "centerZ" to border.centerZ,
            "size" to border.size,
            "targetSize" to border.lerpTarget,
            "timeUntilTarget" to border.lerpTime
        )
    }

    /** Точка возрождения мира: {x, y, z, dimension}. */
    fun getSpawn(): Map<String, Any> {
        val data = client.level?.respawnData
            ?: return mapOf("x" to 0, "y" to 0, "z" to 0, "dimension" to "")
        return mapOf(
            "x" to data.pos().x,
            "y" to data.pos().y,
            "z" to data.pos().z,
            "dimension" to data.dimension().identifier().toString()
        )
    }
    // ==================== Скорборд (Sidebar) ====================

    /** Заголовок скорборда (Sidebar). */
    fun getScoreboardTitle(): String = set.starlev.utils.detectors.ScoreboardDetector.getScoreboardTitle()

    /** Строки скорборда. */
    fun getScoreboardLines(): List<String> = set.starlev.utils.detectors.ScoreboardDetector.getScoreboardText()

    /** Текст экшн-бара (сообщение над хотбаром). */
    fun getActionBar(): String = set.starlev.utils.detectors.ActionBarDetector.getActionBarText()

    // ==================== Таб-лист ====================

    /** Верхняя и нижняя части Tab-листа. */
    fun getTabListHeader(): String = set.starlev.utils.detectors.TabListDetector.getTabListHeader()

    fun getTabListFooter(): String = set.starlev.utils.detectors.TabListDetector.getTabListFooter()

    fun getTabListLines(): List<String> = set.starlev.utils.detectors.TabListDetector.getAllTabListLines()

    /** Текст заголовка (title) на экране. */
    fun getTitle(): String = set.starlev.utils.detectors.TitleDetector.getTitleText()

    fun getSubtitle(): String = set.starlev.utils.detectors.TitleDetector.getSubtitleText()

    /**
     * Блок, в который смотрит игрок (raycast).
     * @return {block, x, y, z, hitX, hitY, hitZ} или null (нет блока в радиусе)
     */
    fun getLookedAtBlock(maxDistance: Double = 6.0): Map<String, Any?>? {
        val player = client.player ?: return null
        val hit = player.pick(maxDistance, 0f, false)
        if (hit.type != net.minecraft.world.phys.HitResult.Type.BLOCK) return null
        val bhr = hit as net.minecraft.world.phys.BlockHitResult
        val pos = bhr.blockPos
        val loc = hit.location
        return mapOf(
            "block" to getBlock(pos.x, pos.y, pos.z),
            "x" to pos.x, "y" to pos.y, "z" to pos.z,
            "hitX" to loc.x, "hitY" to loc.y, "hitZ" to loc.z
        )
    }
}
