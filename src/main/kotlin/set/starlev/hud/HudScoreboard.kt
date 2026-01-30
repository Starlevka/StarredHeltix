package set.starlev.hud

import net.minecraft.client.Minecraft
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.network.chat.Component

object HudScoreboard : HudElement("Scoreboard") {
    private val mc = Minecraft.getInstance()

    fun getAdjustedX(): Int {
        ensureInitialized()
        return x
    }

    fun getAdjustedY(): Int {
        ensureInitialized()
        return y
    }

    private fun calculateSize(includeSlayer: Boolean = true): Pair<Int, Int> {
        val scoreboard = mc.level?.scoreboard
        val objective = scoreboard?.getDisplayObjective(DisplaySlot.SIDEBAR)
        
        val padding = 4
        if (objective == null) {
            if (isEditing) {
                val lines = listOf("§6§lScoreboard", "§7(Placeholder)", "§eМасштаб: ${(scale * 100).toInt()}%")
                val width = lines.maxOf { mc.font.width(it) } + padding * 2
                val height = lines.size * 9 + padding * 2
                return width to height
            }
            return 120 to 160
        }

        var maxWidth = mc.font.width(objective.displayName)
        val scores = scoreboard.listPlayerScores(objective)
        
        for (score in scores) {
            val owner = score.owner
            val team = scoreboard.getPlayersTeam(owner)
            val lineComponent = score.display ?: if (team != null) {
                PlayerTeam.formatNameForTeam(team, Component.literal(owner))
            } else {
                Component.literal(owner)
            }
            maxWidth = maxOf(maxWidth, mc.font.width(lineComponent))
        }

        var totalHeight = (scores.size + 1) * 9 // +1 для заголовка
        
        // Учитываем Slayer HUD если он включен и запрошен
        if (includeSlayer && set.starlev.StarredHeltix.Companion.feature.slayer.slayerHud.slayerScoreboardHud) {
            val extraLines = set.starlev.features.combat.slayer.SlayerScoreboard.getExtraLines()
            if (extraLines.isNotEmpty()) {
                for (line in extraLines) {
                    maxWidth = maxOf(maxWidth, mc.font.width(line))
                }
                totalHeight += extraLines.size * 9
            }
        }
        
        return (maxWidth + padding * 2) to (totalHeight + padding * 2)
    }

    override fun render() {
        val config = set.starlev.StarredHeltix.Companion.feature.visuals.scoreboard
        if (!config.enabled && !isEditing) return

        val graphics = cachedGraphics ?: return
        val scoreboard = mc.level?.scoreboard ?: return
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR)
        
        val isSlayerEnabled = set.starlev.StarredHeltix.Companion.feature.slayer.slayerHud.slayerScoreboardHud
        val (width, height) = calculateSize(isSlayerEnabled)
        
        if (objective != null || isEditing) {
            // Рисуем фон худа
            this.showBackground = config.showBackground
            drawBackground(width, height, 0, shadow = false, shadowBottom = false)
            
            val padding = 4
            var currentY = y + padding
            
            if (objective != null) {
                // Заголовок
                var title = objective.displayName
                title = set.starlev.secret.features.SecretFunFeatures.processComponent(title, false)
                graphics.drawString(mc.font, title, x + (width - mc.font.width(title)) / 2, currentY, 0xFFFFFFFF.toInt())
                currentY += 9
                
                // Строки скорборда
                val scores = scoreboard.listPlayerScores(objective).sortedByDescending { it.value }
                for (score in scores) {
                    val owner = score.owner
                    val team = scoreboard.getPlayersTeam(owner)
                    var lineComponent = score.display ?: if (team != null) {
                        PlayerTeam.formatNameForTeam(team, Component.literal(owner))
                    } else {
                        Component.literal(owner)
                    }
                    
                    // Применяем эффекты текста (Starlev, MegaChromeX)
                    lineComponent = set.starlev.secret.features.SecretFunFeatures.processComponent(lineComponent, false)
                    
                    graphics.drawString(mc.font, lineComponent, x + padding, currentY, 0xFFFFFFFF.toInt())
                    currentY += 9

                    // Вставляем строки Slayer после строки с опытом
                    if (isSlayerEnabled) {
                        val text = lineComponent.string
                        if (text.contains("/") && (text.contains("опыта") || text.contains("XP") || text.contains("опыта Боя"))) {
                            val extraLines = set.starlev.features.combat.slayer.SlayerScoreboard.getExtraLines()
                            for (line in extraLines) {
                                val processedLine = set.starlev.secret.features.SecretFunFeatures.processComponent(line, false)
                                graphics.drawString(mc.font, processedLine, x + padding, currentY, 0xFFFFFFFF.toInt())
                                currentY += 9
                            }
                        }
                    }
                }
            } else if (isEditing) {
                val lines = listOf("§6§lScoreboard", "§7(Placeholder)", "§eМасштаб: ${(scale * 100).toInt()}%")
                for (line in lines) {
                    graphics.drawString(mc.font, line, x + padding, currentY, 0xFFFFFFFF.toInt())
                    currentY += 9
                }
            }
            
            // Больше не рисуем Slayer в конце, так как он вставляется в цикле выше
        }
    }

    override fun getWidth(): Int = calculateSize(true).first

    override fun getHeight(): Int = calculateSize(true).second

    override fun getDefaultX(): Int {
        val baseWidth = calculateSize(false).first
        return mc.window.guiScaledWidth - baseWidth - 3 + 4
    }

    override fun getDefaultY(): Int {
        val scoreboard = mc.level?.scoreboard
        val objective = scoreboard?.getDisplayObjective(DisplaySlot.SIDEBAR)
        val scoresCount = if (objective != null) scoreboard.listPlayerScores(objective).size else 15
        val totalHeight = (scoresCount + 1) * 9
        val startY = mc.window.guiScaledHeight / 2 + totalHeight / 3
        return startY - totalHeight - 1 + 4
    }

    fun init() {
        // Загрузка сохраненных координат происходит автоматически через HudManager
    }
}
