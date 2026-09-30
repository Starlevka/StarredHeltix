package set.starlev.features.combat.dungeons.solvers

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import org.joml.Vector3f
import set.starlev.StarredHeltix
import set.starlev.utils.detectors.DungeonDetector
import set.starlev.utils.detectors.MobHeadDisplayDetector
import java.util.regex.Pattern
import org.slf4j.LoggerFactory

/**
 * Солвер «Три незнакомца» по образцу Odin WeirdosSolver / Skyblocker ThreeWeirdos:
 * - реакция на каждое сообщение: совпало с пулом решений — подсветку сразу (ждать все
 *   три реплики не нужно, игра гарантирует ровно одну «решающую» фразу на комнату);
 * - пул фраз двуязычный (RU-перевод Heltix + английские оригиналы Hypixel), сравнение
 *   нормализованное (§-коды, ё=е, пробелы, регистр, хвостовая пунктуация);
 * - РЕТРАИ: реплика приходит раньше, чем синхронизируются энтити НПС — поиск повторяется
 *   каждые 5 тиков до 15 секунд (tick() из MinecraftMixin), раньше был один выстрел;
 * - НПС: сначала точное совпадение имени, затем подстрока (подстрока сразу давала
 *   матчи на похожие имена соседей);
 * - сундук: предпочтение «за спиной» (против направления взгляда НПС), но с фоллбэком
 *   на ближайший — при «взгляде в стену» раньше отбраковывались все три кандидата.
 */
object ThreeWeirdos {
    private val LOGGER = LoggerFactory.getLogger(ThreeWeirdos::class.java)
    private val STRANGER_PATTERN = Pattern.compile("^\\[(?:npc|персонаж)\\]\\s*(.+?):\\s*(.+)$")

    // Утверждения, произнёсший которые — владелец награды (6 фраз Hypixel, RU + EN)
    private val SOLUTIONS = listOf(
        // RU
        "в моем сундуке находится награда",                    // My chest has the reward and I'm telling the truth!
        "они оба говорят правду. также",                       // Both of them are telling the truth. Also, X has the reward...
        "по крайней мере один из них лжет, и награды нет",     // At least one of them is lying, and the reward is not in X's chest!
        "награды нет ни в одном из сундуков",                  // The reward isn't in any of our chests.
        "награда не в моем сундуке",                           // The reward is not in my chest!
        "в моем сундуке нет награды. мы все говорим правду",   // My chest doesn't have the reward. We are all telling the truth.
        // EN (Heltix может слать оригинал)
        "my chest has the reward and i'm telling the truth",
        "both of them are telling the truth. also",
        "at least one of them is lying, and the reward is not in",
        "the reward isn't in any of our chests",
        "the reward is not in my chest",
        "my chest doesn't have the reward. we are all telling the truth",
    )

    // Ловушки лжецов: «решающая» фраза, содержащая одну из этих — не решение
    private val TRAPS = listOf(
        // RU
        "они оба лгут",
        "все лгут",
        "один из нас говорит правду",
        "по крайней мере один из других",
        // EN
        "they are both lying",
        "one of us is telling the truth",
        "at least one of the others",
        "they are all lying",
    )

    private fun norm(s: String): String =
        (ChatFormatting.stripFormatting(s) ?: s)
            .replace('ё', 'е')
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
            .trim('!', '.', ' ')

    // Публичные поля для ThreeWeirdosChest
    @Volatile
    var foundNpcPos: BlockPos? = null
        private set
    @Volatile
    var foundChestPos: BlockPos? = null
        private set

    // Очередь ретраев: кого ищем и до какого момента
    private var pendingName: String? = null
    private var pendingUntil = 0L
    private var tickCounter = 0

    fun init() {
        ClientReceiveMessageEvents.GAME.register(this::onChat)
    }

    /**
     * Проверка, решена ли уже головоломка (для ThreeWeirdosChest)
     */
    @Volatile
    var puzzleSolved: Boolean = false
        private set

    private var lastCorrectStranger: String? = null

    /**
     * Возвращает имя последнего правильного незнакомца.
     */
    fun getCorrectStranger(): String? = lastCorrectStranger

    private fun onChat(message: Component, overlay: Boolean) {
        if (overlay) return
        if (!StarredHeltix.feature.dungeons.solvers.threeWeirdos) return
        if (!DungeonDetector.isInDungeon()) return

        val text = norm(message.string)

        // Сообщения о решении/провале головоломки
        if (text.contains("головоломка решена") || text.contains("выбрал правильный сундук")) {
            puzzleSolved = true
            resetFound()
            LOGGER.info("[ThreeWeirdos] Головоломка решена, сброс подсветки")
            return
        }
        if (text.contains("головоломка провалена") || text.contains("обхитрил")) {
            puzzleSolved = true
            resetFound()
            LOGGER.info("[ThreeWeirdos] Головоломка провалена, сброс подсветки")
            return
        }

        val matcher = STRANGER_PATTERN.matcher(text)
        if (!matcher.find()) return
        val name = matcher.group(1).trim()
        val statement = matcher.group(2).trim()
        if (name.isEmpty() || statement.isEmpty()) return

        // Новая головоломка после решённой (детект по строке мог не сработать на Heltix)
        if (puzzleSolved) {
            puzzleSolved = false
            fullReset()
        }

        val isSolution = SOLUTIONS.any { statement.contains(it) } &&
            TRAPS.none { statement.contains(it) }
        if (!isSolution) return

        puzzleSolved = false
        lastCorrectStranger = name
        LOGGER.info("[ThreeWeirdos] Решение: '$name' («$statement»)")
        Minecraft.getInstance().player?.sendSystemMessage(Component.literal("§a§l[Три незнакомца] §aНаграда в сундуке: §e$name"))

        // Первая попытка сразу + ретраи в tick() (энтити НПС могут ещё не долететь)
        pendingName = name
        pendingUntil = System.currentTimeMillis() + 15000
        tryLocate()
    }

    /** Вызывается из MinecraftMixin.tick(): повторяет поиск, пока НПС/сундук не найдены. */
    fun tick() {
        val name = pendingName ?: return
        if (foundChestPos != null) {
            pendingName = null
            return
        }
        if (System.currentTimeMillis() > pendingUntil) {
            LOGGER.warn("[ThreeWeirdos] Истекло время поиска НПС '$name' — сундук так и не найден")
            pendingName = null
            Minecraft.getInstance().player?.sendSystemMessage(Component.literal("§e§l[Три незнакомца] §eНе удалось найти сундук незнакомца"))
            return
        }
        tickCounter++
        if (tickCounter % 5 != 0) return
        tryLocate()
    }

    private fun tryLocate() {
        val name = pendingName ?: return
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val player = mc.player ?: return
        findNpcAndChest(level, player, name)
    }

    /**
     * Найти НПС по имени (текст над головой / customName) и сундук за ним.
     * @return true, если сундук найден и подсвечен.
     */
    private fun findNpcAndChest(level: net.minecraft.world.level.Level, player: net.minecraft.world.entity.player.Player, correctName: String): Boolean {
        // Радиус поиска НПС от игрока
        val searchBox = AABB(
            player.x - 15, player.y - 10, player.z - 15,
            player.x + 15, player.y + 10, player.z + 15
        )

        val entities = level.getEntities(null, searchBox) { e ->
            e is LivingEntity || e is ArmorStand
        }

        var npcPos: BlockPos? = null
        var npcLook: Vector3f? = null
        val target = norm(correctName)

        // Два прохода: сначала точное совпадение имени, только потом подстрока —
        // иначе короткое имя матчилось на длинное похожее имя соседа
        for (exactPass in 0..1) {
            for (entity in entities) {
                val displayName = when (entity) {
                    is LivingEntity -> {
                        val head = MobHeadDisplayDetector.getHeadDisplays(entity).textDisplays.maxByOrNull { it.string.length }
                        head?.string ?: entity.customName?.string
                    }
                    is ArmorStand -> entity.customName?.string
                    else -> null
                } ?: continue
                val cleanName = norm(displayName)
                val hit = if (exactPass == 0) cleanName == target
                    else cleanName.contains(target) || target.contains(cleanName)
                if (hit) {
                    npcPos = entity.blockPosition()
                    if (entity is LivingEntity) {
                        val v = entity.lookAngle
                        npcLook = Vector3f(v.x.toFloat(), 0f, v.z.toFloat())
                    }
                    LOGGER.info("[ThreeWeirdos] Найден НПС '$displayName' (${if (exactPass == 0) "точно" else "по подстроке"}) на $npcPos")
                    break
                }
            }
            if (npcPos != null) break
        }

        if (npcPos == null) {
            LOGGER.debug("[ThreeWeirdos] НПС '$correctName' ещё не найден (энтити не синхронизированы?), ждём ретрай")
            return false
        }

        foundNpcPos = npcPos

        // Сундуки стоят за спинами NPC (те смотрят на игрока): кандидаты «впереди по взгляду»
        // оштрафованы, но НЕ отброшены — если все варианты «впереди» (взгляд в стену), берём ближайший.
        var best: BlockPos? = null
        var bestScore = Double.MAX_VALUE

        val searchRadius = 5
        for (x in npcPos.x - searchRadius..npcPos.x + searchRadius) {
            for (y in npcPos.y - 1..npcPos.y + 2) {
                for (z in npcPos.z - searchRadius..npcPos.z + searchRadius) {
                    val pos = BlockPos(x, y, z)
                    val block = level.getBlockState(pos).block
                    if (block != Blocks.CHEST && block != Blocks.TRAPPED_CHEST && block != Blocks.BARREL) continue
                    var score = pos.distSqr(npcPos).toDouble()
                    npcLook?.let { look ->
                        if (look.lengthSquared() > 0.01f) {
                            look.normalize()
                            val dx = (pos.x - npcPos.x).toDouble()
                            val dz = (pos.z - npcPos.z).toDouble()
                            val len = Math.sqrt(dx * dx + dz * dz).coerceAtLeast(1e-3)
                            val dot = (dx / len) * look.x + (dz / len) * look.z // >0 — впереди НПС
                            if (dot > 0.3) score += 20.0 // штраф, но не запрет
                        }
                    }
                    if (score < bestScore) {
                        bestScore = score
                        best = pos
                    }
                }
            }
        }

        if (best != null) {
            val wasNull = foundChestPos == null
            foundChestPos = best
            pendingName = null
            LOGGER.info("[ThreeWeirdos] Найден сундук на позиции $best, дистанция: ${String.format("%.2f", Math.sqrt(bestScore))} блоков")
            if (wasNull) {
                player.sendSystemMessage(Component.literal("§c§l[Три незнакомца] §cСундук подсвечен!"))
            }
            return true
        }

        LOGGER.debug("[ThreeWeirdos] Сундук рядом с $npcPos пока не виден, ждём ретрай")
        return false
    }

    /**
     * Сброс найденных позиций
     */
    fun resetFound() {
        foundNpcPos = null
        foundChestPos = null
        pendingName = null
    }

    /**
     * Полный сброс (для ThreeWeirdosChest.reset())
     */
    fun fullReset() {
        resetFound()
        puzzleSolved = false
    }
}
