package set.starlev.features.combat.slayer

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.hud.HudElement
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.ActionBarDetector
import set.starlev.utils.detectors.EntityDeathDetector
import set.starlev.utils.detectors.ScoreboardDetector
import set.starlev.utils.detectors.SkillXpDetector
import set.starlev.utils.PersonalBestManager
import java.util.Locale
import kotlin.math.ceil

object SlayerHud : HudElement("SlayerHud") {
    private val mc = Minecraft.getInstance()
    private var slayerInfo: List<String> = emptyList()
    private var lastUpdate = 0L

    // Состояние для расчета мобов
    private var lastCombatXpPerMob = 50.0 // Дефолтное значение
    private var isFirstKill = true
    private var lastCurrentXp = 0.0
    private var lastTargetXp = 0.0
    private var mobsRemaining = -1
    private var lastWarningMobs = -1
    
    // Тип текущего слеера для фильтрации мобов
    private var currentSlayerType = ""
    private var currentSlayerTier = ""

    // Таймер босса
    private var bossStartTime = 0L
    private var isBossTimerRunning = false
    private var lastBossPhase = ""

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
        
        SkillXpDetector.registerListener { info ->
            if (info.skill.contains("Бой", ignoreCase = true) && currentSlayerType.isNotEmpty()) {
                if (isFirstKill) {
                    lastCombatXpPerMob = info.gained
                    isFirstKill = false
                } else {
                    if (info.gained < 700.0) {
                        lastCombatXpPerMob = info.gained
                    }
                }
                lastCurrentXp = info.current
                lastTargetXp = info.target
            }
        }
    }

    fun init() {
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
            lines.add("§fОсталось мобов: §b~40")
            progressValue = 0.5f
            progressText = "50,000 / 100,000"
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

        val actionBar = ActionBarDetector.getActionBarText()
        val skillInfo = SkillXpDetector.detectFromText(actionBar)
        
        if (skillInfo != null && skillInfo.skill.contains("Бой", ignoreCase = true)) {
            if (skillInfo.gained < 700.0) {
                lastCombatXpPerMob = skillInfo.gained
            }
            lastCurrentXp = skillInfo.current
            lastTargetXp = skillInfo.target
        }

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
            isFirstKill = true
            progressValue = 0f
            progressText = ""
            isBossTimerRunning = false
            slayerInfo = emptyList() // Явно очищаем
            return
        }    

        val config = StarredHeltix.feature.slayer.slayerHud
        if (config.bossTimer) {
            val hasKillBossMsg = scoreboard.any { it.contains("Убейте босса!", ignoreCase = true) || it.contains("Kill the boss!", ignoreCase = true) }
            val hasBossKilledMsg = scoreboard.any { it.contains("Босс убит!", ignoreCase = true) || it.contains("Boss slain!", ignoreCase = true) }

            if (hasKillBossMsg && !isBossTimerRunning) {
                bossStartTime = System.currentTimeMillis()
                isBossTimerRunning = true
            } else if (hasBossKilledMsg && isBossTimerRunning) {
                handleBossKill()
                isBossTimerRunning = false
            } else if (!hasKillBossMsg && !hasBossKilledMsg && isBossTimerRunning) {
                // Если нет сообщения об убийстве или активном боссе, но таймер идет - проверяем, есть ли вообще слеер
                if (!foundSlayer) {
                    isBossTimerRunning = false
                }
            }
            
            if (isBossTimerRunning) {
                val dur = (System.currentTimeMillis() - bossStartTime) / 1000.0
                val pb = PersonalBestManager.getPB(currentSlayerType, currentSlayerTier)
                val pbText = if (pb != null) " §7(PB: §6${String.format(Locale.US, "%.3f", pb)}s§7)" else ""
                result.add("§cТаймер: §c${String.format(Locale.US, "%.3f", dur)}s$pbText")
            }
        }

        val progressLine = scoreboard.find { it.contains("/") && (it.contains("опыта", ignoreCase = true) || it.contains("XP", ignoreCase = true)) }
        
        if (progressLine != null) {
            val cleanProgress = progressLine.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").replace(",", "")
            val xpMatch = Regex("(\\d+)/(\\d+)").find(cleanProgress)
            
            if (xpMatch != null) {
                val currentXp = xpMatch.groupValues[1].toDouble()
                val targetXp = xpMatch.groupValues[2].toDouble()
                
                progressValue = (currentXp / targetXp).toFloat()
                progressText = "${String.format("%,.0f", currentXp)} / ${String.format("%,.0f", targetXp)}"
                
                val remaining = targetXp - currentXp
                if (remaining > 0) {
                    val mobs = ceil(remaining / lastCombatXpPerMob).toInt()
                    result.add("§cМоб: §c~$mobs §7(по §e${lastCombatXpPerMob.toInt()}§7)")
                    
                    if (mobs in 1..4 && mobs != lastWarningMobs) {
                        sendBossWarning(mobs)
                        lastWarningMobs = mobs
                    }
                } else {
                    result.add("§a✔ Босс готов!")
                    lastWarningMobs = -1
                }
            }
        } else if (scoreboard.any { it.contains("spawned", ignoreCase = true) || it.contains("появился", ignoreCase = true) }) {
            result.add("§c☠ Босс появился!")
            lastWarningMobs = -1
            progressValue = 1f
        }
        
        // Если остался только заголовок, значит полезной инфы нет - скрываем худ
        slayerInfo = if (result.size > 1) result else emptyList()
    }

    private fun handleBossKill() {
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

    private fun sendBossWarning(mobs: Int) {
        mc.execute {
            mc.gui.setTimes(10, 40, 10)
            mc.gui.setTitle(Component.literal("§6§lВнимание!"))
            mc.gui.setSubtitle(Component.literal("§eДо босса осталось мобов: §c$mobs"))
        }
    }

    override fun getWidth(): Int {
        val title = if (isEditing && slayerInfo.isEmpty()) "Zombie T4" else if (currentSlayerType.isNotEmpty()) "$currentSlayerType $currentSlayerTier" else "Slayer"
        val titleWidth = mc.font.width("§lSLAYER §7($title)")
        
        val lines = if (isEditing && slayerInfo.isEmpty()) {
            listOf("§fОсталось мобов: §b~40")
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
