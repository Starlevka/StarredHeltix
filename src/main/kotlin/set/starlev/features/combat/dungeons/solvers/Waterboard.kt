package set.starlev.features.combat.dungeons.solvers

import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LeverBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.slf4j.LoggerFactory
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.render.RenderEngine
import set.starlev.render.RenderEvents
import set.starlev.render.drawCenteredString
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.DungeonDetector

/**
 * Waterboard / Водяной — солвер для Heltix (1 тип).
 *
 * Паттерн решения (со слов заказчика):
 * 1) Включить воду (рычаг) и дождаться стечения (~13с, настраивается waterFlowDelaySec).
 * 2) Нажимаем рычаги изумрудного > алмазного > золотого > кварцевого блоков (порядок настраивается leverOrder).
 * 3) В это время где-то течёт вода по центру и нужно в тайминг нажать на изумрудный блок-рычага, чтобы открыть последний проход.
 *
 * Заготовка: вся логика — скан рычагов вокруг игрока, подсветка следующего рычага и таймеры.
 * Тайминги вынесены в конфиг WaterboardConfig — можно подкрутить без пересборки.
 */
object Waterboard {
    private val LOGGER = LoggerFactory.getLogger(Waterboard::class.java)
    private val mc = Minecraft.getInstance()

    enum class LeverType(val block: Block?, val color: Int, val displayName: String) {
        EMERALD(Blocks.EMERALD_BLOCK, 0x55FF55, "ИЗУМРУД"),
        DIAMOND(Blocks.DIAMOND_BLOCK, 0x55FFFF, "АЛМАЗ"),
        GOLD(Blocks.GOLD_BLOCK, 0xFFAA00, "ЗОЛОТО"),
        QUARTZ(Blocks.QUARTZ_BLOCK, 0xFFFFFF, "КВАРЦ"),
        WATER(null, 0x5555FF, "ВОДА");

        companion object {
            fun fromBlock(block: Block?): LeverType? {
                if (block == null) return null
                return entries.find { it.block == block }
            }
            fun fromName(name: String): LeverType? {
                return entries.find { it.name.equals(name.trim(), ignoreCase = true) }
            }
        }
    }

    private enum class PuzzleState {
        IDLE, WAIT_FLOW, SEQUENCE, FINAL_WAIT, FINAL_WINDOW, DONE
    }

    private var puzzleState = PuzzleState.IDLE
    private var waterStartMillis = 0L
    private var sequenceIndex = 0
    private var finalWindowStartMillis = 0L
    private var doneStartMillis = 0L
    private var lastScanMillis = 0L
    private var cachedLevers: Map<LeverType, BlockPos> = emptyMap()
    private var waterLeverPos: BlockPos? = null
    private var puzzleCenter: Vec3? = null
    private var noLeversSince: Long = 0L
    private var orderedTypes: List<LeverType> = listOf(LeverType.EMERALD, LeverType.DIAMOND, LeverType.GOLD, LeverType.QUARTZ)

    fun init() {
        UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
            handleUseBlock(player, world, hand, hitResult)
        }
        RenderEvents.register { context ->
            val cfg = StarredHeltix.feature.dungeons.solvers.waterboard
            if (!cfg.enabled) return@register
            if (!DungeonDetector.isInDungeon()) return@register
            val nextPos = getNextLeverPos() ?: return@register
            try {
                val isWaiting = puzzleState == PuzzleState.WAIT_FLOW || puzzleState == PuzzleState.FINAL_WAIT
                val colorInt = if (isWaiting) ColorUtils.parseColor(cfg.waitingColorV2, 0xFFFF5555.toInt())
                else ColorUtils.parseColor(cfg.activeColorV2, 0xFF55FF55.toInt())
                // Темный цвет: затемняем исходный по требованию ТЗ
                val darkFactor = 0.45f
                val rRaw = (colorInt shr 16 and 0xFF) / 255f
                val gRaw = (colorInt shr 8 and 0xFF) / 255f
                val bRaw = (colorInt and 0xFF) / 255f
                val r = (rRaw * darkFactor).coerceIn(0f, 1f)
                val g = (gRaw * darkFactor).coerceIn(0f, 1f)
                val b = (bRaw * darkFactor).coerceIn(0f, 1f)
                val aFill = 0.35f
                val aLine = 1f
                val box = AABB(nextPos)
                // Не показываем сквозь стены — всегда обычный renderBox (depth-test включен)
                context.renderBox(box, r, g, b, aFill, true)
                context.renderBox(box, r, g, b, aLine, false)
                if (cfg.showTimer) {
                    val text = getHintText()
                    val timerText = getTimerText()
                    val full = if (timerText != null) "$text §7($timerText)" else text
                    val dist = mc.player?.position()?.distanceTo(Vec3.atCenterOf(nextPos)) ?: 10.0
                    val scale = (1.6f + (dist / 40f).toFloat()).coerceIn(1.2f, 4f)
                    context.renderText(full, nextPos.x + 0.5, nextPos.y + 1.8, nextPos.z + 0.5, scale = scale, seeThrough = false)
                }
                if (puzzleState == PuzzleState.FINAL_WINDOW) {
                    val center = findWaterCenter()
                    if (center != null) {
                        context.renderBeaconBeam(center.x + 0.5, center.y.toDouble(), center.z + 0.5, 8, 0x00FFFF, context.tickDelta, seeThrough = false)
                        context.renderText("§b§lВОДА В ЦЕНТРЕ — ЖМИ!", center.x + 0.5, center.y + 2.5, center.z + 0.5, scale = 3f, seeThrough = false)
                    }
                }
            } catch (e: Exception) {
            }
        }
        var lastTick = 0L
        RenderEvents.register {
            val now = System.currentTimeMillis()
            if (now - lastTick < 50) return@register
            lastTick = now
            tick(now)
        }

        // Таймер на прицеле — HUD в центре экрана
        RenderEngine.registerHud(object : RenderEngine.HudRenderer {
            override fun render(graphics: net.minecraft.client.gui.GuiGraphicsExtractor, delta: Float) {
                val cfg = StarredHeltix.feature.dungeons.solvers.waterboard
                if (!cfg.enabled) return
                if (!cfg.showTimer) return
                if (!DungeonDetector.isInDungeon()) return
                if (mc.gui.screen() != null) return
                if (puzzleState == PuzzleState.IDLE || puzzleState == PuzzleState.DONE) return
                val timer = getTimerText() ?: return
                val hint = getHintText()
                val full = "$hint §7$timer"
                if (full.isBlank()) return
                try {
                    val w = graphics.guiWidth()
                    val h = graphics.guiHeight()
                    val cx = w / 2
                    val cy = h / 2 + 12
                    val textWidth = mc.font.width(Component.literal(full))
                    val bgPad = 4
                    graphics.fill(cx - textWidth / 2 - bgPad, cy - 2, cx + textWidth / 2 + bgPad, cy + mc.font.lineHeight + 2, 0x80000000.toInt())
                    graphics.drawCenteredString(mc.font, Component.literal(full), cx, cy, 0xFFFFFFFF.toInt())
                } catch (_: Exception) {
                }
            }
        })
    }

    private fun getHintText(): String {
        return when (puzzleState) {
            PuzzleState.IDLE -> "§b§lВКЛЮЧИ ВОДУ §7(рычаг)"
            PuzzleState.WAIT_FLOW -> "§e§lЖДИ СТЕЧЕНИЯ ВОДЫ"
            PuzzleState.SEQUENCE -> {
                val type = orderedTypes.getOrNull(sequenceIndex) ?: return "§a§lЖМИ"
                "§a§lЖМИ ${type.displayName} §7(${sequenceIndex + 1}/${orderedTypes.size})"
            }
            PuzzleState.FINAL_WAIT -> "§e§lЖДИ ФИНАЛЬНОГО ОКНА"
            PuzzleState.FINAL_WINDOW -> "§a§lЖМИ ИЗУМРУД §c§lТАЙМИНГ!"
            PuzzleState.DONE -> "§a§lГОТОВО!"
        }
    }

    private fun getTimerText(): String? {
        val cfg = StarredHeltix.feature.dungeons.solvers.waterboard
        val now = System.currentTimeMillis()
        return when (puzzleState) {
            PuzzleState.WAIT_FLOW -> {
                val elapsed = (now - waterStartMillis) / 1000.0
                val remain = cfg.waterFlowDelaySec - elapsed
                if (remain > 0) String.format("%.1fs", remain) else "0.0s"
            }
            PuzzleState.FINAL_WAIT -> {
                val elapsed = (now - finalWindowStartMillis) / 1000.0
                val remain = cfg.finalWindowOpenSec - elapsed
                if (remain > 0) String.format("%.1fs", remain) else "0.0s"
            }
            PuzzleState.FINAL_WINDOW -> {
                val elapsed = (now - finalWindowStartMillis) / 1000.0
                val totalOpen = cfg.finalWindowOpenSec + cfg.finalWindowDurationSec
                val remain = totalOpen - elapsed
                if (remain > 0) String.format("%.1fs", remain) else "0.0s"
            }
            PuzzleState.DONE -> {
                val elapsed = (now - doneStartMillis) / 1000.0
                String.format("%.1fs до сброса", (5.0 - elapsed).coerceAtLeast(0.0))
            }
            else -> null
        }
    }

    private fun getNextLeverPos(): BlockPos? {
        val now = System.currentTimeMillis()
        if (now - lastScanMillis > 200) {
            refreshLevers()
            lastScanMillis = now
        }
        return when (puzzleState) {
            // IDLE: подсвечиваем ТОЛЬКО водный рычаг. Если он не найден — не гадаем
            // (раньше fallback на первый цветной рычаг и давил «емerald не там»).
            PuzzleState.IDLE -> waterLeverPos
            PuzzleState.WAIT_FLOW -> waterLeverPos
            PuzzleState.SEQUENCE -> {
                val type = orderedTypes.getOrNull(sequenceIndex) ?: return null
                cachedLevers[type]
            }
            PuzzleState.FINAL_WAIT, PuzzleState.FINAL_WINDOW -> cachedLevers[LeverType.EMERALD]
            PuzzleState.DONE -> null
        }
    }

    private fun distXZ(a: BlockPos, b: BlockPos): Double {
        val dx = (a.x - b.x).toDouble(); val dz = (a.z - b.z).toDouble()
        return kotlin.math.sqrt(dx * dx + dz * dz)
    }

    /**
     * Сканируем только в радиусе комнаты (~20 блоков). Пазл идентифицируется
     * КЛАСТЕРОМ цветных рычагов: >=3 цветных, все близко друг к другу и по одной
     * высоте (рычаги Water Board стоят двумя группами на стенах).
     * Рычаги из соседних комнат в кластер не попадают и игнорируются.
     */
    private fun refreshLevers() {
        val level = mc.level ?: return
        val player = mc.player ?: return
        try {
            val cfgOrder = StarredHeltix.feature.dungeons.solvers.waterboard.leverOrder
            val parsed = cfgOrder.split(",").mapNotNull { LeverType.fromName(it.trim()) }.filter { it != LeverType.WATER }
            if (parsed.isNotEmpty()) orderedTypes = parsed
        } catch (_: Exception) {}
        val radius = 20
        val px = player.blockPosition().x
        val py = player.blockPosition().y
        val pz = player.blockPosition().z
        val allLevers = mutableListOf<BlockPos>()
        for (x in px - radius..px + radius) {
            for (y in py - 10..py + 10) {
                for (z in pz - radius..pz + radius) {
                    val pos = BlockPos(x, y, z)
                    val bs: BlockState = try { level.getBlockState(pos) } catch (_: Exception) { continue }
                    if (bs.`is`(Blocks.LEVER)) allLevers.add(pos)
                }
            }
        }

        // Классификация СТРОГО по блоку, к которому прикреплён рычаг (стена/пол/потолок).
        // Раньше сканировались все 6 соседей — рычаг чужого пазла рядом с изумрудным
        // блоком ошибочно считался «изумрудным».
        val colored: List<Pair<BlockPos, LeverType>> = allLevers.mapNotNull { pos ->
            val bs = try { level.getBlockState(pos) } catch (_: Exception) { return@mapNotNull null }
            LeverType.fromBlock(attachedBlock(pos, bs, level))?.let { pos to it }
        }

        // Кластер: для каждого семенного рычага берём все цветные в радиусе 16 по XZ и ±6 по Y.
        val best = colored.maxByOrNull { seed ->
            colored.count { distXZ(it.first, seed.first) <= 16.0 && kotlin.math.abs(it.first.y - seed.first.y) <= 6 }
        }?.let { seed ->
            colored.filter { distXZ(it.first, seed.first) <= 16.0 && kotlin.math.abs(it.first.y - seed.first.y) <= 6 }
        } ?: emptyList()

        val distinctTypes = best.map { it.second }.distinct()
        if (best.size >= 3 && distinctTypes.size >= 2) {
            val cx = best.map { it.first.x }.average()
            val cy = best.map { it.first.y }.average()
            val cz = best.map { it.first.z }.average()
            val center = Vec3(cx + 0.5, cy + 0.5, cz + 0.5)
            puzzleCenter = center
            // По одному рычагу каждого типа — ближайший к центру кластера
            val map = LinkedHashMap<LeverType, BlockPos>()
            for (t in distinctTypes) {
                val candidates = best.filter { it.second == t }.map { it.first }
                val nearest = candidates.minByOrNull { (it.x - cx) * (it.x - cx) + (it.y - cy) * (it.y - cy) + (it.z - cz) * (it.z - cz) }
                if (nearest != null) map[t] = nearest
            }
            cachedLevers = map
            // Водный рычаг = рычаг на ПОЛИРОВАННОМ АНДЕЗИТЕ на полу (относительно пола) — как в ТЗ Heltix.
            // Ищем рычаг, прикреплённый к POLISHED_ANDESITE, который стоит на полу (y ≈ полу).
            val centerPos = BlockPos(
                cx.toInt(),
                cy.toInt(),
                cz.toInt(),
            )
            val coloredSet = best.map { it.first }.toHashSet()
            // Пол — минимальный Y цветных рычагов (или центра), водный рычаг должен быть на полу/рядом с полом
            val floorY = best.minOfOrNull { it.first.y } ?: centerPos.y
            val andesiteCandidates = allLevers
                .filter { it !in coloredSet && distXZ(it, centerPos) <= 18.0 && kotlin.math.abs(it.y - centerPos.y) <= 8 }
                .filter {
                    try {
                        val bs2 = level.getBlockState(it)
                        val attached = attachedBlock(it, bs2, level)
                        val isAndesite = attached == Blocks.POLISHED_ANDESITE
                        // Относительно пола: рычаг на полу (y == floorY+1 и блок под ним андезит) или на стене андезита у пола (y <= floorY+2)
                        val nearFloor = it.y <= floorY + 2 && it.y >= floorY - 1
                        // Дополнительно: блок под рычагом или прикреплённый — андезит
                        val belowIsAndesite = try { level.getBlockState(it.below()).`is`(Blocks.POLISHED_ANDESITE) } catch (_: Exception) { false }
                        isAndesite && nearFloor || belowIsAndesite && nearFloor
                    } catch (_: Exception) { false }
                }
            waterLeverPos = andesiteCandidates.minByOrNull { distXZ(it, centerPos) }
                ?: allLevers
                    .filter { it !in coloredSet && distXZ(it, centerPos) <= 18.0 && kotlin.math.abs(it.y - centerPos.y) <= 8 }
                    .filter {
                        try { level.getBlockState(it).let { bs2 -> attachedBlock(it, bs2, level) == Blocks.POLISHED_ANDESITE } } catch (_: Exception) { false }
                    }.minByOrNull { distXZ(it, centerPos) }
                ?: allLevers
                    .filter { it !in coloredSet && distXZ(it, centerPos) <= 18.0 && kotlin.math.abs(it.y - centerPos.y) <= 8 }
                    .minByOrNull { distXZ(it, centerPos) }
        } else {
            // Кластера нет — не подсвечиваем вообще ничего (в чужих комнатах рычаги не наши)
            cachedLevers = emptyMap()
            waterLeverPos = null
            puzzleCenter = null
        }
    }

    /** Блок, к которому прикреплён рычаг (стена/пол/потолок). */
    private fun attachedBlock(leverPos: BlockPos, state: BlockState, level: Level): Block? {
        return try {
            val face = state.getValue(LeverBlock.FACE).toString()
            val facing = state.getValue(LeverBlock.FACING)
            val attachedPos = when (face) {
                "WALL" -> leverPos.relative(facing.opposite)
                "FLOOR" -> leverPos.below()
                "CEILING" -> leverPos.above()
                else -> leverPos.relative(facing.opposite)
            }
            level.getBlockState(attachedPos).block
        } catch (_: Exception) {
            null
        }
    }

    /** Тип рычага по его креплению; null — обычный (не цветной) блок. */
    private fun classifyLever(leverPos: BlockPos, state: BlockState, level: Level): LeverType? =
        LeverType.fromBlock(attachedBlock(leverPos, state, level))

    private fun findWaterCenter(): BlockPos? {
        val level = mc.level ?: return null
        val player = mc.player ?: return null
        val centerX = cachedLevers.values.map { it.x }.average().takeIf { !it.isNaN() }?.toInt() ?: player.blockPosition().x
        val centerZ = cachedLevers.values.map { it.z }.average().takeIf { !it.isNaN() }?.toInt() ?: player.blockPosition().z
        val centerY = player.blockPosition().y
        for (dx in -6..6) {
            for (dz in -6..6) {
                for (dy in -3..3) {
                    val pos = BlockPos(centerX + dx, centerY + dy, centerZ + dz)
                    try {
                        if (level.getBlockState(pos).`is`(Blocks.WATER)) return pos
                    } catch (_: Exception) {}
                }
            }
        }
        return null
    }

    private fun handleUseBlock(player: Player, world: Level, hand: InteractionHand, hitResult: BlockHitResult): InteractionResult {
        val cfg = StarredHeltix.feature.dungeons.solvers.waterboard
        if (!cfg.enabled) return InteractionResult.PASS
        if (!DungeonDetector.isInDungeon()) return InteractionResult.PASS
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS
        val pos = hitResult.blockPos
        val bs = try { world.getBlockState(pos) } catch (_: Exception) { return InteractionResult.PASS }
        if (!bs.`is`(Blocks.LEVER)) return InteractionResult.PASS
        // Refresh, чтобы waterLeverPos/cachedLevers были актуальны на момент клика
        refreshLevers()
        lastScanMillis = System.currentTimeMillis()
        val type = classifyLever(pos, bs, world)
        when (puzzleState) {
            PuzzleState.IDLE -> {
                // Старт пазла — только клик по ВОДНОМУ рычагу из найденного кластера.
                // (раньше IDLE съедал клик по любому рычагу, включая изумрудный)
                if (pos == waterLeverPos) {
                    waterStartMillis = System.currentTimeMillis()
                    puzzleState = PuzzleState.WAIT_FLOW
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§b[Водяной] §7Вода включена, ждём ${cfg.waterFlowDelaySec}с..."))
                }
            }
            PuzzleState.SEQUENCE -> {
                val expected = orderedTypes.getOrNull(sequenceIndex) ?: return InteractionResult.PASS
                val isCorrect = (type == expected) || (pos == cachedLevers[expected])
                if (isCorrect) {
                    sequenceIndex++
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[Водяной] §7${expected.displayName} OK (${sequenceIndex}/${orderedTypes.size})"))
                    if (sequenceIndex >= orderedTypes.size) {
                        puzzleState = PuzzleState.FINAL_WAIT
                        finalWindowStartMillis = System.currentTimeMillis()
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e[Водяной] §7Ждём финального окна..."))
                    }
                } else if (type != null || pos == waterLeverPos) {
                    // Кликнули по рычагу ЭТОГО пазла, но не тому, что нужен
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[Водяной] §7Ожидается ${expected.displayName}, а нажат ${type?.displayName ?: "водный"}"))
                }
            }
            PuzzleState.FINAL_WINDOW -> {
                val expected = LeverType.EMERALD
                val isCorrect = (type == expected) || (pos == cachedLevers[expected])
                if (isCorrect) {
                    puzzleState = PuzzleState.DONE
                    doneStartMillis = System.currentTimeMillis()
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[Водяной] §lФИНАЛЬНЫЙ ТАЙМИНГ OK! Проход открыт."))
                }
            }
            else -> {}
        }
        return InteractionResult.PASS
    }

    private fun tick(now: Long) {
        val cfg = StarredHeltix.feature.dungeons.solvers.waterboard
        if (!cfg.enabled) {
            if (puzzleState != PuzzleState.IDLE) reset()
            return
        }
        if (!DungeonDetector.isInDungeon()) {
            if (puzzleState != PuzzleState.IDLE) reset()
            return
        }
        // Ушли из комнаты Водяного (кластер цветных рычагов больше не виден) —
        // не тащим состояние и подсветку через весь данж.
        if (puzzleState != PuzzleState.IDLE && cachedLevers.isEmpty()) {
            if (this.noLeversSince == 0L) this.noLeversSince = now
            else if (now - this.noLeversSince > 5000) {
                mc.player?.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[Водяной] §7Комната не найдена — сброс"))
                reset()
                return
            }
        } else {
            this.noLeversSince = 0L
        }
        when (puzzleState) {
            PuzzleState.WAIT_FLOW -> {
                val elapsed = (now - waterStartMillis) / 1000.0
                if (elapsed >= cfg.waterFlowDelaySec) {
                    puzzleState = PuzzleState.SEQUENCE
                    sequenceIndex = 0
                    mc.player?.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[Водяной] §7Вода стекла! Жми ${orderedTypes.firstOrNull()?.displayName ?: "рычаги"}"))
                }
            }
            PuzzleState.FINAL_WAIT -> {
                val elapsed = (now - finalWindowStartMillis) / 1000.0
                if (elapsed >= cfg.finalWindowOpenSec) {
                    puzzleState = PuzzleState.FINAL_WINDOW
                    finalWindowStartMillis = now
                    mc.player?.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[Водяной] §lОКНО ОТКРЫТО — ЖМИ ИЗУМРУД!"))
                }
            }
            PuzzleState.FINAL_WINDOW -> {
                val elapsed = (now - finalWindowStartMillis) / 1000.0
                if (elapsed >= cfg.finalWindowDurationSec) {
                    if (elapsed >= cfg.finalWindowDurationSec + 2) {
                        mc.player?.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[Водяной] §7Окно закрылось, сброс."))
                        reset()
                    }
                }
            }
            PuzzleState.DONE -> {
                val elapsed = (now - doneStartMillis) / 1000.0
                if (elapsed >= 5) {
                    reset()
                }
            }
            else -> {}
        }
    }

    fun reset() {
        puzzleState = PuzzleState.IDLE
        waterStartMillis = 0L
        sequenceIndex = 0
        finalWindowStartMillis = 0L
        doneStartMillis = 0L
        this.noLeversSince = 0L
    }

    fun debugStatus(): String {
        return "state=$puzzleState seq=$sequenceIndex/${orderedTypes.size} waterStart=$waterStartMillis levers=$cachedLevers"
    }
}
