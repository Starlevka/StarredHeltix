package set.starlev.features.mining

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import set.starlev.StarredHeltix
import set.starlev.render.RenderEvents
import set.starlev.utils.detectors.ScoreboardDetector
import java.util.Locale

object DwarvenWaypoints {
    private val mc = Minecraft.getInstance()

    private var cacheUpdatedAt = 0L
    private var cachedAreaLower = ""
    private var cachedActiveCommissionsLower: List<String> = emptyList()
    private var cachedIsInDwarvenMines = false
    private val dwarvenKeywordsLower = listOf("гномьи", "dwarven", "mines", "шахты")

    private fun waypoint(pos: BlockPos, aliases: List<String>): WaypointData {
        return WaypointData(pos, aliases, aliases.map { it.lowercase(Locale.getDefault()) })
    }
    
    private val WAYPOINTS = mapOf(
        "Верхние шахты" to waypoint(BlockPos(-122, 172, -71), listOf("Верхние шахты", "Upper Mines", "Верхние пещеры", "Upper Caves", "Верхние")),
        "Лавовый источник" to waypoint(BlockPos(61, 199, -17), listOf("Лавовый источник", "Lava Springs", "Лава", "Lava")),
        "Горные рудники" to waypoint(BlockPos(6, 129, 47), listOf("Горные рудники", "Cliffside Veins", "Cliffside", "Клифсайд")),
        "Карьер" to waypoint(BlockPos(-88, 147, 1), listOf("Карьер", "Quarry", "Карьер")),
        "Королевские шахты" to waypoint(BlockPos(153, 152, 35), listOf("Королевские шахты", "Royal mines", "Королевские", "Royal")),
    )

    private data class WaypointData(val pos: BlockPos, val aliases: List<String>, val aliasesLower: List<String>)

    fun init() {
        RenderEvents.register { context ->
            val config = StarredHeltix.feature.mining.commissions
            if (!config.waypointsEnabled) return@register
            
            updateCache()
            if (!cachedIsInDwarvenMines) return@register

            WAYPOINTS.forEach { (name, data) ->
                val hasCommission = cachedActiveCommissionsLower.any { commissionLower ->
                    data.aliasesLower.any { aliasLower ->
                        commissionLower.contains(aliasLower)
                    }
                }

                if (!hasCommission) return@forEach

                val isInside = data.aliasesLower.any { aliasLower ->
                    cachedAreaLower.contains(aliasLower)
                }

                if (!isInside) {
                    renderWaypoint(context, name, data.pos)
                }
            }
        }
    }

    private fun updateCache() {
        val now = System.currentTimeMillis()
        if (now - cacheUpdatedAt < 250) return
        cacheUpdatedAt = now

        val level = mc.level
        if (level == null) {
            cachedAreaLower = ""
            cachedActiveCommissionsLower = emptyList()
            cachedIsInDwarvenMines = false
            return
        }

        val title = ScoreboardDetector.getScoreboardTitle()
        val lines = ScoreboardDetector.getScoreboardText()

        val area = getCurrentArea(title, lines)
        cachedAreaLower = area.lowercase(Locale.getDefault())
        cachedActiveCommissionsLower = CommissionsHud.getActiveCommissions().map { it.string.lowercase(Locale.getDefault()) }

        val dimensionLower = level.dimension().location().toString().lowercase(Locale.getDefault())
        if (dimensionLower.contains("dwarven")) {
            cachedIsInDwarvenMines = true
            return
        }

        val titleLower = title.lowercase(Locale.getDefault())
        val matchesKeyword = dwarvenKeywordsLower.any { keywordLower ->
            cachedAreaLower.contains(keywordLower) || titleLower.contains(keywordLower)
        }

        cachedIsInDwarvenMines = matchesKeyword || WAYPOINTS.values.any { data ->
            data.aliasesLower.any { aliasLower -> cachedAreaLower.contains(aliasLower) }
        }
    }
    
    private fun getCurrentArea(title: String, lines: List<String>): String {
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
