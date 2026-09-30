package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.item.MapItem
import com.mojang.blaze3d.platform.NativeImage
import set.starlev.features.combat.dungeons.DungeonMapHud
import set.starlev.scripting.SHScriptEngine
import set.starlev.scripting.ScriptConsole
import java.util.concurrent.ConcurrentHashMap

/**
 * API карт: подземелья + общие.
 * Подземелья: читает ванильную карту из offhand (как DungeonMapHud) + crypts.
 * Общие: кастомные 128x128 карты как DynamicTexture для HUD/overlay.
 */
object JSDungeonMap {
    private val mc get() = Minecraft.getInstance()

    fun isInDungeon(): Boolean = try { set.starlev.utils.detectors.DungeonDetector.isInDungeon() } catch (_: Exception) { false }

    fun getCrypts(): Int? = try { DungeonMapHud.getCryptsCount() } catch (_: Exception) { null }

    fun hasMap(): Boolean {
        return try {
            val p = mc.player ?: return false
            val lvl = mc.level ?: return false
            val mapId = p.offhandItem.get(DataComponents.MAP_ID) ?: return false
            MapItem.getSavedData(mapId, lvl) != null
        } catch (_: Exception) { false }
    }

    fun getMapId(): Int? {
        return try {
            val p = mc.player ?: return null
            p.offhandItem.get(DataComponents.MAP_ID)?.id
        } catch (_: Exception) { null }
    }

    /** Декор карты (иконки игроков/маркера): [{x, y, rot, name, hasIcon}] */
    fun getDecorations(): List<Map<String, Any>> {
        return try {
            val p = mc.player ?: return emptyList()
            val lvl = mc.level ?: return emptyList()
            val mapId = p.offhandItem.get(DataComponents.MAP_ID) ?: return emptyList()
            val state = MapItem.getSavedData(mapId, lvl) ?: return emptyList()
            val renderState = net.minecraft.client.renderer.state.MapRenderState()
            mc.mapRenderer.extractRenderState(mapId, state, renderState)
            renderState.decorations.map { d ->
                mapOf(
                    "x" to d.x.toInt(),
                    "y" to d.y.toInt(),
                    "rot" to d.rot.toInt(),
                    "name" to (d.name?.string ?: ""),
                    "hasIcon" to (d.atlasSprite != null)
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    /** Сырые цвета карты 128x128 (если мапа есть) как List<Int> ARGB, или null. */
    fun getMapColors(): List<Int>? {
        return try {
            val p = mc.player ?: return null
            val lvl = mc.level ?: return null
            val mapId = p.offhandItem.get(DataComponents.MAP_ID) ?: return null
            val state = MapItem.getSavedData(mapId, lvl) ?: return null
            // state.colors is byte[16384] (MapItemSavedData.colors)
            val colorsField = state.javaClass.getDeclaredField("colors")
            colorsField.isAccessible = true
            val bytes = colorsField.get(state) as? ByteArray ?: return null
            // Map color palette translation: use MapColor handling? Simplified: return byte values as Ints
            bytes.map { it.toInt() and 0xFF }
        } catch (_: Exception) { null }
    }

    /** Центр карты (как в MapItemSavedData). */
    fun getMapCenter(): Map<String, Int>? {
        return try {
            val p = mc.player ?: return null
            val lvl = mc.level ?: return null
            val mapId = p.offhandItem.get(DataComponents.MAP_ID) ?: return null
            val state = MapItem.getSavedData(mapId, lvl) ?: return null
            val xField = state.javaClass.getDeclaredField("centerX")
            val zField = state.javaClass.getDeclaredField("centerZ")
            xField.isAccessible = true; zField.isAccessible = true
            mapOf("x" to (xField.get(state) as Int), "z" to (zField.get(state) as Int))
        } catch (_: Exception) { null }
    }

    fun getMapScale(): Int? {
        return try {
            val p = mc.player ?: return null
            val lvl = mc.level ?: return null
            val mapId = p.offhandItem.get(DataComponents.MAP_ID) ?: return null
            val state = MapItem.getSavedData(mapId, lvl) ?: return null
            val f = state.javaClass.getDeclaredField("scale")
            f.isAccessible = true
            f.get(state) as Int
        } catch (_: Exception) { null }
    }
}

/**
 * Кастомные карты (DynamicTexture 128x128) для модулей.
 * Каждая — HUD-элемент или текстура "starredheltix:module/<mod>/<id>" для ctx.drawTexture.
 */
class JSMapHandle internal constructor(
    val module: String,
    val id: String,
    val width: Int,
    val height: Int,
    private val identifier: Identifier,
    private val native: NativeImage,
    private val texture: DynamicTexture
) {
    @Volatile private var dirty = true

    fun setPixel(x: Int, y: Int, color: Double) {
        if (x !in 0 until width || y !in 0 until height) return
        val c = color.toInt()
        // NativeImage uses ABGR? In 26.2 NativeImage.setPixel ABGR, but we pass ARGB and trust
        try { native.setPixel(x, y, c) } catch (_: Exception) {}
        dirty = true
    }

    fun getPixel(x: Int, y: Int): Int {
        if (x !in 0 until width || y !in 0 until height) return 0
        return try { native.getPixel(x, y) } catch (_: Exception) { 0 }
    }

    fun fill(color: Double) {
        val c = color.toInt()
        for (y in 0 until height) for (x in 0 until width) {
            try { native.setPixel(x, y, c) } catch (_: Exception) {}
        }
        dirty = true
    }

    fun clear() = fill(0x00000000.toDouble())

    fun update() {
        if (!dirty) return
        try {
            texture.upload()
            dirty = false
        } catch (e: Exception) {
            ScriptConsole.error(module, "maps[$id].update(): ${e.message}")
        }
    }

    /** Текстура ID для drawTexture: "starredheltix:module/<mod>/<id>" */
    fun getTextureId(): String = identifier.toString()

    fun destroy() {
        try { texture.close() } catch (_: Exception) {}
        try { Minecraft.getInstance().textureManager.release(identifier) } catch (_: Exception) {}
        try { native.close() } catch (_: Exception) {}
    }
}

object JSMaps {
    private val maps = ConcurrentHashMap<String, JSMapHandle>() // key = module.lower + ":" + id

    private fun key(module: String, id: String) = "${module.lowercase()}:$id"

    @JvmOverloads
    fun create(id: String, width: Int = 128, height: Int = 128): JSMapHandle? {
        val module = SHScriptEngine.currentModule ?: "unknown"
        val k = key(module, id)
        // удалить старый с тем же id
        maps[k]?.destroy()
        if (width <= 0 || height <= 0 || width > 512 || height > 512) {
            ScriptConsole.error(module, "maps.create('$id'): размер 1..512, получено ${width}x${height}")
            return null
        }
        return try {
            val identifier = Identifier.fromNamespaceAndPath("starredheltix", "module/${module.lowercase()}/map_${id.lowercase()}")
            val native = NativeImage(width, height, false)
            // fill transparent
            for (y in 0 until height) for (x in 0 until width) native.setPixel(x, y, 0)
            val tex = DynamicTexture({ "SHMap-${module}-${id}" }, native)
            Minecraft.getInstance().textureManager.register(identifier, tex)
            val handle = JSMapHandle(module, id, width, height, identifier, native, tex)
            maps[k] = handle
            handle
        } catch (e: Exception) {
            ScriptConsole.error(module, "maps.create('$id'): ${e.message}", e)
            null
        }
    }

    fun get(id: String): JSMapHandle? {
        val module = SHScriptEngine.currentModule ?: "unknown"
        return maps[key(module, id)]
    }

    fun remove(id: String): Boolean {
        val module = SHScriptEngine.currentModule ?: "unknown"
        val k = key(module, id)
        val h = maps.remove(k) ?: return false
        h.destroy()
        return true
    }

    fun list(): List<String> {
        val module = SHScriptEngine.currentModule ?: "unknown"
        val prefix = "${module.lowercase()}:"
        return maps.keys.filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }
    }

    // Внутренние — для SHScriptEngine cleanup
    fun removeForModule(module: String) {
        val lower = module.lowercase()
        val toRemove = maps.keys.filter { it.startsWith("$lower:") }
        for (k in toRemove) {
            maps.remove(k)?.destroy()
        }
    }
    fun clearAll() {
        maps.values.forEach { it.destroy() }
        maps.clear()
    }

    // Dungeon delegation (удобный алиас)
    fun dungeon(): JSDungeonMap = JSDungeonMap
}
