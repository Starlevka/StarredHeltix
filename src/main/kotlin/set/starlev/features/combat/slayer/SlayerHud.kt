package set.starlev.features.combat.slayer

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.features.chat.ChatEventsManager
import set.starlev.hud.HudElement
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.EntityDeathDetector
import set.starlev.utils.detectors.ScoreboardDetector
import set.starlev.utils.PersonalBestManager
import java.util.Locale

object SlayerHud : HudElement("SlayerHud") {
    private val mc = Minecraft.getInstance()
    private var slayerInfo: List<String> = emptyList()
    private var lastUpdate = 0L

    // Таймер босса
    
    // Тип текущего слеера для фильтрации мобов
    private var currentSlayerType = ""
    private var currentSlayerTier = ""

    // Таймер босса
    private var bossStartTime = 0L
    private var isBossTimerRunning = false
    private var lastBossPhase = ""
    private var detectedMiniBosses = mutableSetOf<Int>()

    // Данные для рендеринга
    private var progressValue = 0f
    private var progressText = ""
    private var bossTimerText = ""

    init {
        EntityDeathDetector.registerListener { entity ->
            if (currentSlayerType.isNotEmpty() && EntityDeathDetector.isRelevantForSlayer(entity, currentSlayerType)) {
                // Моб убит
            }
        }
        
        }

    fun init() {
        ChatEventsManager.registerIncoming { message ->
            val clean = message.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
            if (clean.contains("МИНИБОСС") || clean.contains("МИНИ-БОСС") || clean.uppercase().contains("MINIBOSS")) {
                mc.execute {
                    mc.gui.setTitle(Component.literal("§d§lМИНИБОСС!"))
                    mc.gui.setSubtitle(Component.literal("§fВ чате замечен: §e$clean"))
                    mc.gui.setTimes(10, 100, 30)
                }
            }
        }
    }

    private fun getHudTextColor(): Int {
        return 0xFFFFFFFF.toInt()
    }

    private fun parseColor(colorStr: String, default: Int): Int {
        return ColorUtils.parseColor(colorStr, default)
    }

    override fun render() {
        val config = StarredHeltix.feature.slayer.slayerHud
        if (!config.enabled) return

        updateSlayerInfo()

        if (slayerInfo.isEmpty() && !isEditing) return

        val title = if (isEditing && slayerInfo.isEmpty()) "Zombie T4" else if (currentSlayerType.isNotEmpty()) "$currentSlayerType $currentSlayerTier" else "Slayer"
        val lines = mutableListOf<String>()
        
        if (isEditing && slayerInfo.isEmpty()) {
            lines.add("§c☠ Босс появился!")
        } else {
            slayerInfo.forEachIndexed { index, line ->
                if (index > 0) lines.add(line) // Пропускаем заголовок, он пойдет в drawBackground
            }
        }

        // Рендеринг в стиле SkyHanni
        val padding = 4
        val titleWidth = mc.font.width("§lSLAYER §7($title)")
        val contentWidth = if (lines.isEmpty()) titleWidth else maxOf(titleWidth, lines.maxOf { mc.font.width(it) })
        val finalWidth = maxOf(contentWidth, 100) // Минимальная ширина для полоски прогресса
        
        val rowHeight = mc.font.lineHeight + 2
        val totalHeight = rowHeight * (lines.size + 1) // +1 для заголовка
        
        // Синхронизируем состояние фона с конфигом
        this.showBackground = config.showBackground
        
        drawBackground(finalWidth, totalHeight, padding)
        
        var currentY = y
        val textColor = getHudTextColor()

        // 1. Заголовок
        cachedGraphics?.drawString(mc.font, "§d§lSLAYER §7($title)", x, currentY, textColor, true)
        currentY += rowHeight
        
        // 2. Остальные строки (мобы, таймер и т.д.)
        lines.forEach { line ->
            cachedGraphics?.drawString(mc.font, line, x, currentY, textColor, true)
            currentY += rowHeight
        }
    }

    private fun updateSlayerInfo() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdate < 100) return 
        lastUpdate = currentTime

        val scoreboard = ScoreboardDetector.getScoreboardText()
        val result = mutableListOf<String>()
        
        val slayerTypes = listOf("Мститель", "Тарантул", "Свен", "Revenant", "Tarantula", "Sven", "Voidgloom", "Inferno", "Riftstalker")
        
        var foundSlayer = false
        var slayerType = ""
        var slayerTier = ""

        for (line in scoreboard) {
            val cleanLine = line.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
            for (type in slayerTypes) {
                if (cleanLine.contains(type, ignoreCase = true)) {
                    slayerType = type
                    val tierMatch = Regex("\\d+").find(cleanLine)
                    slayerTier = tierMatch?.value ?: ""
                    foundSlayer = true
                    break
                }
            }
            if (foundSlayer) break
        }

        if (foundSlayer) {
            currentSlayerType = slayerType
            currentSlayerTier = slayerTier
            result.add("§d§l$slayerType $slayerTier:")
        } else {
            currentSlayerType = ""
            currentSlayerTier = ""
            progressValue = 0f
            progressText = ""
            isBossTimerRunning = false
            slayerInfo = emptyList()
            return
        }    

        val config = StarredHeltix.feature.slayer.slayerHud
        if (config.bossTimer) {
            val hasKillBossMsg = scoreboard.any { it.contains("Убейте босса!", ignoreCase = true) || it.contains("Kill the boss!", ignoreCase = true) }
            val hasBossKilledMsg = scoreboard.any { it.contains("Босс убит!", ignoreCase = true) || it.contains("Boss slain!", ignoreCase = true) }

            if (hasKillBossMsg && !isBossTimerRunning) {
                bossStartTime = System.currentTimeMillis()
                isBossTimerRunning = true
            } else if ((hasBossKilledMsg || !hasKillBossMsg) && isBossTimerRunning) {
                if (hasBossKilledMsg) {
                    handleBossKill()
                }
                isBossTimerRunning = false
            }
            
            if (isBossTimerRunning) {
                val dur = (System.currentTimeMillis() - bossStartTime) / 1000.0
                val pb = PersonalBestManager.getPB(currentSlayerType, currentSlayerTier)
                val pbText = if (pb != null) " §7(PB: §6${String.format(Locale.US, "%.3f", pb)}s§7)" else ""
                result.add("§cТаймер: §c${String.format(Locale.US, "%.3f", dur)}s$pbText")
            }
        }

        if (scoreboard.any { it.contains("spawned", ignoreCase = true) || it.contains("появился", ignoreCase = true) }) {
            result.add("§c☠ Босс появился!")
            progressValue = 1f
        }
        
        // Детект мини-боссов
        detectMiniBosses()
        
        // Если остался только заголовок, значит полезной инфы нет - скрываем худ
        slayerInfo = if (result.size > 1) result else emptyList()
    }

    private fun detectMiniBosses() {
        if (currentSlayerType.isEmpty()) {
            detectedMiniBosses.clear()
            return
        }

        val level = mc.level ?: return
        val miniBossNames = listOf(
            "Revenant Sycophant", "Revenant Champion", "Deformed Revenant",
            "Tarantula Vermin", "Tarantula Beast", "Mutant Tarantula",
            "Pack Spirit", "Howling Spirit",
            "Slayer Mini-Boss", "Мститель-прислужник", "Мститель-чемпион", "Деформированный мститель",
            "Тарантул-вредитель", "Тарантул-зверь", "Мутант-тарантул",
            "Дух стаи", "Воющий дух"
        )

        for (entity in level.entitiesForRendering()) {
            if (entity is net.minecraft.world.entity.LivingEntity && !detectedMiniBosses.contains(entity.id)) {
                val name = entity.customName?.string ?: ""
                val cleanName = name.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
                
                if (miniBossNames.any { cleanName.contains(it, ignoreCase = true) }) {
                    detectedMiniBosses.add(entity.id)
                    mc.execute {
                        mc.player?.displayClientMessage(
                            Component.literal("§d§l[StarredHeltix] §fПоявился §e§lМИНИ-БОСС §f- §6$cleanName!"),
                            false
                        )
                        // Добавляем Title
                        mc.gui.setTitle(Component.literal("§d§lМИНИБОСС!"))
                        mc.gui.setSubtitle(Component.literal("§fРядом замечен §e$cleanName"))
                        mc.gui.setTimes(10, 100, 30)
                        mc.soundManager.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 1.2f))
                    }
                }
            }
        }
        
        // Очистка старых ID (которых больше нет в мире)
        if (detectedMiniBosses.size > 20) {
            val currentIds = level.entitiesForRendering().map { it.id }.toSet()
            detectedMiniBosses.retainAll { currentIds.contains(it) }
        }
    }

    private fun handleBossKill() {
        detectedMiniBosses.clear()
        val durationMs = System.currentTimeMillis() - bossStartTime
        val durationSec = durationMs / 1000.0
        val localeUS = Locale.US
        val formattedTime = String.format(localeUS, "%.3f", durationSec)
        
        val bossName = if (currentSlayerType.isEmpty()) "Босса" else currentSlayerType
        val bossTier = currentSlayerTier
        
        val config = StarredHeltix.feature.slayer.slayerHud
        var message = "§7[§dStarredHeltix§7] §fВы одолели босса §d$bossName $bossTier §fза §b$formattedTime §fсекунд!"
        
        if (config.personalBests) {
            val (isNewPB, oldPB) = PersonalBestManager.updatePB(bossName, bossTier, durationSec)
            if (isNewPB) {
                message += " §6§l(НОВЫЙ ЛИЧНЫЙ РЕКОРД!)"
            } else if (oldPB != null) {
                val formattedOldPB = String.format(localeUS, "%.3f", oldPB)
                message += " §7(Прошлый личный рекорд - $formattedOldPB секунд)"
            }
        }
        
        val finalMessage = message
        mc.execute {
            mc.player?.displayClientMessage(Component.literal(finalMessage), false)
        }
    }

    override fun getWidth(): Int {
        val title = if (isEditing && slayerInfo.isEmpty()) "Zombie T4" else if (currentSlayerType.isNotEmpty()) "$currentSlayerType $currentSlayerTier" else "Slayer"
        val titleWidth = mc.font.width("§lSLAYER §7($title)")
        
        val lines = if (isEditing && slayerInfo.isEmpty()) {
            listOf("§c☠ Босс появился!")
        } else {
            slayerInfo.drop(1)
        }
        
        val contentWidth = if (lines.isEmpty()) titleWidth else maxOf(titleWidth, lines.maxOf { mc.font.width(it) })
        return maxOf(contentWidth, 100)
    }

    override fun getHeight(): Int {
        val lines = if (isEditing && slayerInfo.isEmpty()) {
            listOf("dummy")
        } else {
            slayerInfo.drop(1)
        }
        return (mc.font.lineHeight + 2) * (lines.size + 2) + 4
    }

    override fun getDefaultScale(): Float = 1.5000001f
    override fun getDefaultX(): Int = 150
    override fun getDefaultY(): Int = 181
}
