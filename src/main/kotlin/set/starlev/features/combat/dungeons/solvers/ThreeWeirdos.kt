package set.starlev.features.combat.dungeons.solvers

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import set.starlev.StarredHeltix
import set.starlev.utils.detectors.DungeonDetector
import set.starlev.utils.detectors.MobHeadDisplayDetector
import java.util.regex.Pattern
import org.slf4j.LoggerFactory

object ThreeWeirdos {
    private val LOGGER = LoggerFactory.getLogger(ThreeWeirdos::class.java)
    private val STRANGER_PATTERN = Pattern.compile("\\[Персонаж] (.+?):")
    private val statements = mutableMapOf<String, String>()
    private var lastCorrectStranger: String? = null
    
    // Публичные поля для ThreeWeirdosChest
    @Volatile
    var foundNpcPos: BlockPos? = null
        private set
    @Volatile
    var foundChestPos: BlockPos? = null
        private set

    fun init() {
        ClientReceiveMessageEvents.GAME.register(this::onChat)
    }
    
    /**
     * Проверка, решена ли уже головоломка (для ThreeWeirdosChest)
     */
    @Volatile
    var puzzleSolved: Boolean = false
        private set

    /**
     * Возвращает имя последнего правильного незнакомца.
     */
    fun getCorrectStranger(): String? = lastCorrectStranger
    
    private fun onChat(message: Component, overlay: Boolean) {
        if (!StarredHeltix.feature.dungeons.solvers.threeWeirdos) {
            return
        }
        
        val text = message.string
        
        // Проверяем сообщение о решении или провале головоломки
        if (text.contains("ГОЛОВОЛОМКА РЕШЕНА") || text.contains("выбрал правильный сундук")) {
            puzzleSolved = true
            resetFound()
            LOGGER.info("[ThreeWeirdos] Головоломка решена, сброс подсветки")
            return
        }
        
        // Проверяем сообщение о провале головоломки
        if (text.contains("ГОЛОВОЛОМКА ПРОВАЛЕНА") || text.contains("обхитрил")) {
            puzzleSolved = true
            resetFound()
            LOGGER.info("[ThreeWeirdos] Головоломка провалена, сброс подсветки")
            return
        }
        
        // Check if this is a stranger message
        val matcher = STRANGER_PATTERN.matcher(text)
        if (matcher.find()) {
            val name = matcher.group(1)
            val statement = text.substring(matcher.end()).trim()
            
            // Store the statement
            statements[name] = statement
            
            // Check if we have exactly 3 statements to solve
            if (statements.size == 3) {
                // Wait a bit to ensure all messages are processed, then solve
                Thread {
                    try {
                        Thread.sleep(500) // Wait 0.5 seconds
                        Minecraft.getInstance().execute(this::solve)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }.start()
            }
        }
    }
    
    private fun solve() {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val player = mc.player ?: return
        
        // Проверка на нахождение в подземельях
        if (!DungeonDetector.isInDungeon()) {
            statements.clear()
            return
        }

        var correct: String? = null
        
        for ((name, statement) in statements) {
            // Проверяем все верные случаи
            if (statement.contains("В моем сундуке находится награда, и я говорю правду!") ||
                statement.startsWith("Они оба говорят правду. Также") ||
                statement.startsWith("По крайней мере один из них лжёт, и награды нет в сундуке") ||
                statement == "Награды нет ни в одном из сундуков." ||
                statement == "Награда не в моём сундуке!") {
                correct = name
                break
            }
        }
        if (correct != null) {
            puzzleSolved = false // Сбрасываем флаг для новой головоломки
            lastCorrectStranger = correct
            mc.player!!.displayClientMessage(Component.literal("§a§l[Три незнакомца] §aНаграда в сундуке: §e$correct"), false)
            
            // Сразу ищем НПС и сундук после решения
            findNpcAndChest(level, player, correct)
        }

        // Clear statements for next puzzle
        statements.clear()
    }
    
    /**
     * Найти НПС по имени (через MobHeadDisplayDetector) и ближайший сундук
     */
    private fun findNpcAndChest(level: net.minecraft.world.level.Level, player: net.minecraft.world.entity.player.Player, correctName: String) {
        // Радиус поиска НПС от игрока
        val searchBox = AABB(
            player.x - 15, player.y - 10, player.z - 15,
            player.x + 15, player.y + 10, player.z + 15
        )
        
        val entities = level.getEntities(null, searchBox) { e ->
            e is LivingEntity || e is ArmorStand
        }
        
        var npcPos: BlockPos? = null
        val normalizedName = correctName.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
        
        LOGGER.info("[ThreeWeirdos] Ищем НПС '$correctName' (нормализованное: '$normalizedName') среди ${entities.size} сущностей")
        
        // 1. Ищем НПС через MobHeadDisplayDetector (текст над головой)
        for (entity in entities) {
            if (entity !is LivingEntity) continue
            
            val headDisplays = MobHeadDisplayDetector.getHeadDisplays(entity)
            for (displayComp in headDisplays.textDisplays) {
                val cleanName = displayComp.string.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
                
                if (cleanName.equals(normalizedName, ignoreCase = true) || 
                    cleanName.contains(normalizedName, ignoreCase = true) ||
                    normalizedName.contains(cleanName, ignoreCase = true)) {
                    npcPos = entity.blockPosition()
                    LOGGER.info("[ThreeWeirdos] Найден НПС через MobHeadDisplay '$cleanName' на позиции $npcPos")
                    break
                }
            }
            if (npcPos != null) break
        }
        
        // 2. Если не нашли, ищем по customName
        if (npcPos == null) {
            for (entity in entities) {
                val nameTag = when (entity) {
                    is LivingEntity -> entity.customName?.string
                    is ArmorStand -> entity.customName?.string
                    else -> null
                } ?: continue
                
                val cleanName = nameTag.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
                
                if (cleanName.equals(normalizedName, ignoreCase = true) || 
                    cleanName.contains(normalizedName, ignoreCase = true) ||
                    normalizedName.contains(cleanName, ignoreCase = true)) {
                    npcPos = entity.blockPosition()
                    LOGGER.info("[ThreeWeirdos] Найден НПС по customName '$cleanName' на позиции $npcPos")
                    break
                }
            }
        }
        
        // 3. Фоллбэк: ищем любого NPC с текстом над головой, содержащим имя
        if (npcPos == null) {
            LOGGER.warn("[ThreeWeirdos] НПС '$correctName' не найден точно, ищем любого NPC с текстом...")
            for (entity in entities) {
                if (entity !is LivingEntity) continue
                
                val headDisplays = MobHeadDisplayDetector.getHeadDisplays(entity)
                if (headDisplays.totalDisplays > 0) {
                    npcPos = entity.blockPosition()
                    val names = headDisplays.textDisplays.joinToString(", ") { it.string }
                    LOGGER.info("[ThreeWeirdos] Используем НПС с текстом '$names' на позиции $npcPos")
                    break
                }
            }
        }
        
        if (npcPos == null) {
            LOGGER.warn("[ThreeWeirdos] Не удалось найти НПС")
            return
        }
        
        foundNpcPos = npcPos
        
        // Ищем ближайший сундук/бочку к НПС в радиусе 3 блоков (маленький радиус!)
        var nearestChest: BlockPos? = null
        var nearestDist = Double.MAX_VALUE

        val searchRadius = 3
        for (x in npcPos.x - searchRadius..npcPos.x + searchRadius) {
            for (y in npcPos.y - 2..npcPos.y + 2) {
                for (z in npcPos.z - searchRadius..npcPos.z + searchRadius) {
                    val pos = BlockPos(x, y, z)
                    val state = level.getBlockState(pos)
                    val block = state.block
                    if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL) {
                        val dist = pos.distSqr(npcPos)
                        if (dist < nearestDist) {
                            nearestDist = dist
                            nearestChest = pos
                        }
                    }
                }
            }
        }
        
        if (nearestChest != null) {
            foundChestPos = nearestChest
            LOGGER.info("[ThreeWeirdos] Найден сундук на позиции $nearestChest, дистанция: ${String.format("%.2f", Math.sqrt(nearestDist))} блоков")
            player.displayClientMessage(Component.literal("§c§l[Три незнакомца] §cСундук подсвечен!"), false)
        } else {
            LOGGER.warn("[ThreeWeirdos] Сундук не найден в радиусе 3 блоков от НПС")
            player.displayClientMessage(Component.literal("§e§l[Три незнакомца] §eСундук не найден рядом с НПС"), false)
        }
    }
    
    /**
     * Сброс найденных позиций
     */
    fun resetFound() {
        foundNpcPos = null
        foundChestPos = null
    }
    
    /**
     * Полный сброс (для ThreeWeirdosChest.reset())
     */
    fun fullReset() {
        foundNpcPos = null
        foundChestPos = null
        puzzleSolved = false
    }
}