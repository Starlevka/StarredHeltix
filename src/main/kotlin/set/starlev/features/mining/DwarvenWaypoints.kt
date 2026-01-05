package set.starlev.features.mining

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import set.starlev.StarredHeltix
import set.starlev.render.RenderEvents
import set.starlev.utils.detectors.ScoreboardDetector
import java.awt.Color

object DwarvenWaypoints {
    private val mc = Minecraft.getInstance()
    
    private val WAYPOINTS = mapOf(
        "Верхние шахты" to WaypointData(BlockPos(-122, 172, -71), listOf("Верхние шахты", "Upper Mines", "Верхние пещеры", "Upper Caves", "Верхние")),
        "Лавовый источник" to WaypointData(BlockPos(61, 199, -17), listOf("Лавовый источник", "Lava Springs", "Лава", "Lava")),
        "Горные рудники" to WaypointData(BlockPos(6, 129, 47), listOf("Горные рудники", "Cliffside Veins", "Cliffside", "Клифсайд")),
        "Карьер" to WaypointData(BlockPos(-88, 147, 1), listOf("Карьер", "Quarry", "Карьер")),
        "Королевские шахты" to WaypointData(BlockPos(153, 152, 35), listOf("Королевские шахты", "Royal mines", "Королевские", "Royal")),
    )

    private data class WaypointData(val pos: BlockPos, val aliases: List<String>)

    fun init() {
        RenderEvents.register { context ->


            val config = StarredHeltix.feature.mining.commissions
            if (!config.waypointsEnabled) return@register
            
            val level = mc.level ?: return@register
            
            // Проверка на нахождение в Dwarven Mines
            if (!isInDwarvenMines()) return@register
            
            val currentArea = getCurrentArea()
            val activeCommissions = CommissionsHud.getActiveCommissions()

            WAYPOINTS.forEach { (name, data) ->
                val hasCommission = activeCommissions.any { commission ->
                    data.aliases.any { alias -> 
                        commission.contains(alias, ignoreCase = true)
                    }
                }

                val isInside = data.aliases.any { alias -> 
                    currentArea.contains(alias, ignoreCase = true) 
                }

                if (hasCommission && !isInside) {
                    renderWaypoint(context, name, data.pos)
                }
            }
        }
    }
    
    private fun getCurrentArea(): String {
        val title = ScoreboardDetector.getScoreboardTitle()
        val lines = ScoreboardDetector.getScoreboardText()
        
        // 1. Ищем строку с символом локации или ключевым словом
        val locationLine = lines.find { 
            it.contains("⏣") || 
            it.contains("Локация", ignoreCase = true) || 
            it.contains("Area", ignoreCase = true) ||
            it.contains("Зона", ignoreCase = true)
        }
        
        if (locationLine != null) {
            return locationLine
                .replace("⏣", "")
                .replace("Локация", "", ignoreCase = true)
                .replace("Area", "", ignoreCase = true)
                .replace("Зона", "", ignoreCase = true)
                .replace(":", "")
                .trim()
        }
        
        // 2. Проверка заголовка
        if (title.isNotEmpty() && !title.contains("HELTIX", ignoreCase = true)) {
            return title
        }
        
        // 3. Проверка по алиасам в первых строках
        for (line in lines.take(8)) {
            val cleanLine = line.trim()
            if (cleanLine.isEmpty()) continue
            
            for (waypoint in WAYPOINTS.values) {
                for (alias in waypoint.aliases) {
                    if (cleanLine.contains(alias, ignoreCase = true)) {
                        return alias
                    }
                }
            }
        }
        
        return title
    }
    
    private fun isInDwarvenMines(): Boolean {
        val level = mc.level ?: return false
        val dimension = level.dimension().location().toString()
        
        // Базовая проверка по измерению
        if (dimension.contains("dwarven", ignoreCase = true)) return true
        
        val area = getCurrentArea()
        val title = ScoreboardDetector.getScoreboardTitle()
        
        val dwarvenKeywords = listOf("Гномьи", "Dwarven", "Mines", "Шахты")
        
        val matchesKeyword = dwarvenKeywords.any { 
            area.contains(it, ignoreCase = true) || title.contains(it, ignoreCase = true) 
        }
        
        if (matchesKeyword) return true
        
        // Если мы находимся в одной из зон вейпоинтов - мы точно в шахтах
        return WAYPOINTS.any { (_, data) -> 
            data.aliases.any { alias -> area.contains(alias, ignoreCase = true) } 
        }
    }
    
    private fun renderWaypoint(context: set.starlev.render.RenderContext, name: String, pos: BlockPos) {
        val playerPos = mc.player?.position() ?: return
        val waypointPos = Vec3(pos.x.toDouble() + 0.5, pos.y.toDouble() + 0.5, pos.z.toDouble() + 0.5)
        val distance = playerPos.distanceTo(waypointPos)
        val config = StarredHeltix.feature.mining.commissions
        
        if (!config.waypointsEnabled) return
        
        // Рендерим бокс вокруг точки
        val box = AABB(pos)
        context.renderBox(box, 0f, 1f, 1f, 0.4f, true)
        context.renderBox(box, 0f, 1f, 1f, 1.0f, false)
        
        // Рендерим луч маяка (высота 300 блоков)
        context.renderBeaconBeam(waypointPos.x, waypointPos.y, waypointPos.z, 300, 0x00FFFF, context.tickDelta)
        
        // Динамическое масштабирование: чем дальше, тем больше текст
        // Увеличили базовый масштаб в ~2.2 раза и ускорили динамический рост
        val dynamicScale = 1.0f + (distance.toFloat() / 80f)
        val nameScale = 5.5f * dynamicScale 
        
        // Основное название локации
        context.renderText(
            "§b§l$name", 
            waypointPos.x, waypointPos.y + 3.0, waypointPos.z, 
            color = 0xFFFFFFFF.toInt(),
            scale = nameScale, 
            seeThrough = true
        )
    }
}
