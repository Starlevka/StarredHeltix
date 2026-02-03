package set.starlev.features.misc

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import set.starlev.StarredHeltix
import set.starlev.features.chat.ChatEventsManager
import set.starlev.render.RenderEvents
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.ScoreboardDetector
import java.util.UUID
import kotlin.math.roundToInt
import java.util.regex.Pattern

object Waypoints {
    private val mc = Minecraft.getInstance()
    private const val PERSONAL_ISLAND_LOCATION = "Личный остров"

    sealed interface Visibility {
        object Any : Visibility
        data class Dimension(val id: String) : Visibility
        data class Location(val name: String) : Visibility
    }

    private data class Waypoint(
        val id: String,
        val name: String,
        val pos: BlockPos,
        val createdAt: Long,
        val expiresAt: Long?,
        var color: Int,
        var visibility: Visibility
    )

    private val waypoints = mutableListOf<Waypoint>()
    private var cachedLocation: String? = null
    private var lastLocationPollAt = 0L
    private val recentChatSuggestions = mutableMapOf<String, Long>()

    private val colorPattern = Pattern.compile("(?i)§[0-9a-fk-orlnmxz]")
    private val waypointPattern = Pattern.compile(
        "(?i)(?:\\bwaypoint\\b|\\bвейпоинт\\b|\\bметка\\b)\\s*(?::\\s*)?(?:\\[?(temp|temporary|врем(?:енная|енно)?)\\]?)?\\s*([^()\\[\\]{}]{1,40})?\\s*\\(?(?:x\\s*[:=]\\s*)?(-?\\d{1,6})\\s*[,; ]\\s*(?:y\\s*[:=]\\s*)?(-?\\d{1,6})\\s*[,; ]\\s*(?:z\\s*[:=]\\s*)?(-?\\d{1,6})\\)?"
    )
    private val shCoordsPattern = Pattern.compile(
        "(?i)starredheltix\\s*x\\s*:\\s*(-?\\d{1,7}(?:\\.\\d+)?)\\s*y\\s*:\\s*(-?\\d{1,7}(?:\\.\\d+)?)\\s*z\\s*:\\s*(-?\\d{1,7}(?:\\.\\d+)?)"
    )
    private val coordsLinePattern = Pattern.compile(
        "(?i)(?:координат[ыa]|coords)\\s*:\\s*(-?\\d{1,7}(?:\\.\\d+)?)\\s*[,;]\\s*(-?\\d{1,7}(?:\\.\\d+)?)\\s*[,;]\\s*(-?\\d{1,7}(?:\\.\\d+)?)"
    )

    fun init() {
        RenderEvents.register { context ->
            val config = StarredHeltix.feature.misc.waypoints
            if (!config.enabled) return@register
            val level = mc.level ?: return@register
            val player = mc.player ?: return@register
            val now = System.currentTimeMillis()

            waypoints.removeIf { it.expiresAt != null && now >= it.expiresAt }
            if (waypoints.isEmpty()) return@register

            val currentDimId = level.dimension().location().toString()
            val currentLocation = getCurrentLocation(now)

            for (wp in waypoints) {
                if (!matchesVisibility(wp.visibility, currentDimId, currentLocation)) continue

                val wpVec = Vec3(wp.pos.x + 0.5, wp.pos.y + 0.5, wp.pos.z + 0.5)
                val distance = player.position().distanceTo(wpVec)
                val color = wp.color
                val r = (color shr 16 and 0xFF) / 255f
                val g = (color shr 8 and 0xFF) / 255f
                val b = (color and 0xFF) / 255f

                if (config.showBox) {
                    val box = AABB(wp.pos)
                    context.renderBox(box, r, g, b, 0.25f, true)
                    context.renderBox(box, r, g, b, 0.95f, false)
                }

                if (config.showBeam) {
                    context.renderBeaconBeam(wpVec.x, wpVec.y, wpVec.z, 300, color and 0x00FFFFFF, context.tickDelta)
                }

                if (config.showText) {
                    val distText = String.format(java.util.Locale.ROOT, "%.0f", distance)
                    val text = "§b§l${wp.name} §7(${distText}m)"
                    val dynamicScale = 1.0f + (distance.toFloat() / 90f) * 1.5f
                    val scale = 1.6f * dynamicScale
                    context.renderText(
                        text,
                        wpVec.x,
                        wpVec.y + 2.8,
                        wpVec.z,
                        color = 0xFFFFFFFF.toInt(),
                        scale = scale,
                        seeThrough = true
                    )
                }
            }
        }

        ChatEventsManager.registerIncoming { message ->
            val config = StarredHeltix.feature.misc.waypoints
            if (!config.enabled || !config.parseFromChat) return@registerIncoming
            val cleaned = colorPattern.matcher(message).replaceAll("").trim()
            val wpMatcher = waypointPattern.matcher(cleaned)
            val coords = if (wpMatcher.find()) {
                val tempWord = wpMatcher.group(1)?.trim()?.lowercase()
                val nameRaw = wpMatcher.group(2)?.trim()
                val x = wpMatcher.group(3)?.toIntOrNull() ?: return@registerIncoming
                val y = wpMatcher.group(4)?.toIntOrNull() ?: return@registerIncoming
                val z = wpMatcher.group(5)?.toIntOrNull() ?: return@registerIncoming
                val isTemp = tempWord != null && tempWord.isNotBlank()
                val name = when {
                    nameRaw.isNullOrBlank() -> if (isTemp) "Temp" else "Waypoint"
                    else -> nameRaw.take(40)
                }
                ParsedChatCoords(x, y, z, name, isTemp)
            } else {
                parseChatCoords(cleaned) ?: return@registerIncoming
            }

            if (!coords.isTemp && config.chatCreatesRegular) {
                addRegular(coords.name, BlockPos(coords.x, coords.y, coords.z))
                mc.player?.displayClientMessage(
                    Component.literal("§a[Waypoints] §f${coords.name} §7-> §e${coords.x} ${coords.y} ${coords.z}"),
                    false
                )
                return@registerIncoming
            }

            if (!config.chatCreatesTemporary) return@registerIncoming
            showApplyTemporarySuggestion(cleaned, coords.name, coords.x, coords.y, coords.z)
        }

        ClientTickEvents.END_CLIENT_TICK.register {
            val now = System.currentTimeMillis()
            waypoints.removeIf { it.expiresAt != null && now >= it.expiresAt }
        }
    }

    fun addRegular(name: String, pos: BlockPos) {
        add(name, pos, null, null, null)
    }

    fun addTemporary(name: String, pos: BlockPos, durationMs: Long) {
        val now = System.currentTimeMillis()
        add(name, pos, (now + durationMs).coerceAtLeast(now), null, null)
    }

    fun remove(id: String) {
        waypoints.removeIf { it.id == id }
    }

    data class WaypointSnapshot(
        val id: String,
        val name: String,
        val pos: BlockPos,
        val expiresAt: Long?,
        val color: Int,
        val visibility: Visibility
    )

    fun getAll(): List<WaypointSnapshot> {
        return waypoints.map { WaypointSnapshot(it.id, it.name, it.pos, it.expiresAt, it.color, it.visibility) }
    }

    fun getExpiresAt(id: String): Long? {
        return waypoints.firstOrNull { it.id == id }?.expiresAt
    }

    fun cycleColor(id: String) {
        val wp = waypoints.firstOrNull { it.id == id } ?: return
        val palette = colorPalette()
        val idx = palette.indexOfFirst { it == wp.color }
        wp.color = palette[(idx + 1).mod(palette.size)]
    }

    fun setColor(id: String, color: Int) {
        val wp = waypoints.firstOrNull { it.id == id } ?: return
        wp.color = color
    }

    fun cycleVisibility(id: String) {
        val wp = waypoints.firstOrNull { it.id == id } ?: return
        val options = buildVisibilityOptions()
        val idx = options.indexOfFirst { it == wp.visibility }
        wp.visibility = options[(idx + 1).mod(options.size)]
    }

    fun visibilityLabel(visibility: Visibility): String {
        return when (visibility) {
            Visibility.Any -> "ВЕЗДЕ"
            is Visibility.Dimension -> when (visibility.id) {
                "minecraft:overworld" -> "Hub"
                "minecraft:dungeonhub" -> "DH"
                "minecraft:dwarven" -> "DM"
                else -> visibility.id
            }

            is Visibility.Location -> visibility.name
        }
    }

    private fun add(name: String, pos: BlockPos, expiresAt: Long?, color: Int?, visibility: Visibility?) {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val config = StarredHeltix.feature.misc.waypoints
        val resolvedColor = color ?: ColorUtils.parseColor(config.colorV2, 0xFF00FFFF.toInt())
        waypoints.add(Waypoint(id, name, pos, now, expiresAt, resolvedColor, visibility ?: Visibility.Any))
    }

    private fun buildVisibilityOptions(): List<Visibility> {
        return mutableListOf<Visibility>(
            Visibility.Any,
            Visibility.Dimension("minecraft:overworld"),
            Visibility.Dimension("minecraft:dungeonhub"),
            Visibility.Dimension("minecraft:dwarven"),
            Visibility.Location(PERSONAL_ISLAND_LOCATION)
        )
    }

    private fun colorPalette(): List<Int> {
        val config = StarredHeltix.feature.misc.waypoints
        val base = ColorUtils.parseColor(config.colorV2, 0xFF00FFFF.toInt())
        return listOf(
            base,
            0xFFFF5555.toInt(),
            0xFF55FF55.toInt(),
            0xFFFFFF55.toInt(),
            0xFF55FFFF.toInt(),
            0xFFFF55FF.toInt(),
            0xFFFFFFFF.toInt()
        ).distinct()
    }

    private fun matchesVisibility(visibility: Visibility, dimId: String, location: String?): Boolean {
        return when (visibility) {
            Visibility.Any -> true
            is Visibility.Dimension -> dimId == visibility.id
            is Visibility.Location -> location != null && location.equals(visibility.name, ignoreCase = true)
        }
    }

    private fun getCurrentLocation(nowMs: Long): String? {
        if (nowMs - lastLocationPollAt < 1000) return cachedLocation
        lastLocationPollAt = nowMs

        val lines = ScoreboardDetector.getScoreboardText()
        val rawLocation = extractLocation(lines)
        val resolved = if (rawLocation != null && rawLocation.equals(PERSONAL_ISLAND_LOCATION, ignoreCase = true)) {
            PERSONAL_ISLAND_LOCATION
        } else {
            null
        }
        cachedLocation = resolved
        return resolved
    }

    private fun extractLocation(lines: List<String>): String? {
        for (line in lines) {
            val idx = line.indexOf('⏣')
            if (idx == -1) continue
            val after = line.substring(idx + 1).trim()
            if (after.isNotBlank()) return after.take(60)
        }
        return null
    }

    private fun extractSender(cleanedMessage: String): String? {
        Regex("^<([^>]{1,32})>").find(cleanedMessage)?.let { return it.groupValues[1].trim() }
        Regex("^([A-Za-z0-9_]{2,16})\\s*:").find(cleanedMessage)?.let { return it.groupValues[1].trim() }
        Regex("\\b([A-Za-z0-9_]{2,16})\\b\\s*:\\s*(?:Координат|Coords|Waypoint|Вейпоинт|Метка)", RegexOption.IGNORE_CASE)
            .find(cleanedMessage)
            ?.let { return it.groupValues[1].trim() }
        return null
    }

    private fun sanitizeNameForCommand(name: String): String {
        val cleaned = name
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\"", "")
            .trim()
        return if (cleaned.isBlank()) "Waypoint" else cleaned.take(40)
    }

    private data class ParsedChatCoords(
        val x: Int,
        val y: Int,
        val z: Int,
        val name: String,
        val isTemp: Boolean
    )

    private fun parseChatCoords(cleaned: String): ParsedChatCoords? {
        val sh = shCoordsPattern.matcher(cleaned)
        if (sh.find()) {
            val x = sh.group(1)?.toDoubleOrNull()?.roundToInt() ?: return null
            val y = sh.group(2)?.toDoubleOrNull()?.roundToInt() ?: return null
            val z = sh.group(3)?.toDoubleOrNull()?.roundToInt() ?: return null
            return ParsedChatCoords(x, y, z, "Coords", isTemp = true)
        }

        val coords = coordsLinePattern.matcher(cleaned)
        if (coords.find()) {
            val x = coords.group(1)?.toDoubleOrNull()?.roundToInt() ?: return null
            val y = coords.group(2)?.toDoubleOrNull()?.roundToInt() ?: return null
            val z = coords.group(3)?.toDoubleOrNull()?.roundToInt() ?: return null
            return ParsedChatCoords(x, y, z, "Coords", isTemp = true)
        }

        return null
    }

    private fun showApplyTemporarySuggestion(cleanedMessage: String, name: String, x: Int, y: Int, z: Int) {
        val now = System.currentTimeMillis()
        val key = "$x:$y:$z:$name"
        val last = recentChatSuggestions[key]
        if (last != null && now - last < 5000) return
        recentChatSuggestions[key] = now
        recentChatSuggestions.entries.removeIf { now - it.value > 60000 }

        val sender = extractSender(cleanedMessage)
        val safeName = sanitizeNameForCommand(name)
        val command = "/sh waypoint apply $x $y $z $safeName"
        val applyButton = Component.literal("§a[Применить]")
            .withStyle { style ->
                style
                    .withUnderlined(true)
                    .withClickEvent(ClickEvent.RunCommand(command))
                    .withHoverEvent(HoverEvent.ShowText(Component.literal("§7Создать временную метку (16с)")))
            }

        val prefix = if (sender != null) {
            "§a[Waypoints] §fНайдены координаты §7от §e$sender§7."
        } else {
            "§a[Waypoints] §fНайдены координаты в чате§7."
        }
        val msg = Component.literal("$prefix §7Координаты: §e$x $y $z §7— ")
            .append(applyButton)

        mc.player?.displayClientMessage(msg, false)
    }
}
