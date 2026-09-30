package set.starlev.features.skyblock

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.JsonOps
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.nbt.TagParser
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.ItemStack
import set.starlev.StarredHeltix
import java.util.concurrent.ConcurrentHashMap

/**
 * Предпросмотр содержимого эндер-сундуков и рюкзаков Heltix — порт ItemStackMixin +
 * BundleTooltipComponent из heltix-mod.
 *
 * Сервер кладёт в предмет JSON-карту {индекс слота -> SNBT предмета} в
 * CustomData → PublicBukkitValues → "space:preview_items". Мы подменяем
 * ItemStack.getTooltipImage() (см. ItemStackMixin) на StoragePreviewTooltip,
 * а ClientTooltipComponentMixin маппит её в клиентский рендер-грид.
 */
object StoragePreview {
    private class Entry(val at: Long, val tooltip: StoragePreviewTooltip)
    private val cache = ConcurrentHashMap<String, Entry>()
    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, JsonElement>>() {}.type

    fun isEnabled(): Boolean = StarredHeltix.feature.skyblock.storagePreview.enabled

    /** Сброс при смене мира (WorldChangeHandler). */
    fun clear() = cache.clear()

    /** @return тултип превью или null, если маркера в NBT нет. */
    fun getOrBuild(stack: ItemStack): StoragePreviewTooltip? {
        val customData: net.minecraft.world.item.component.CustomData = stack.get(DataComponents.CUSTOM_DATA) ?: return null
        val bukkit = customData.copyTag().getCompound("PublicBukkitValues").orElse(null) ?: return null
        val json = bukkit.getStringOr("space:preview_items", "")
        if (json.isEmpty()) return null
        // Кэш: имя + хэш содержимого — одинаковые по названию сундуки не конфликтуют
        val key = (stack.get(DataComponents.CUSTOM_NAME)?.string ?: "") + '#' + json.hashCode()
        val now = System.currentTimeMillis()
        cache[key]?.let { if (now - it.at < 1000L) return it.tooltip }
        val built = parse(json, key) ?: return null
        cache[key] = Entry(now, built)
        return built
    }

    private fun parse(json: String, diagKey: String): StoragePreviewTooltip? {
        val map: Map<String, JsonElement>? = try {
            gson.fromJson(json, mapType)
        } catch (e: Exception) {
            diag(diagKey, "JSON карты не разобран (${json.length} символов, возможно обрезан сервером): ${e.message}")
            null
        }
        if (map.isNullOrEmpty()) return null
        val level = Minecraft.getInstance().level ?: return null
        val registry = level.registryAccess()
        val nbtOps: DynamicOps<Tag> = registry.createSerializationContext(NbtOps.INSTANCE)
        val jsonOps: DynamicOps<JsonElement> = registry.createSerializationContext(JsonOps.INSTANCE)
        var maxIndex = -1
        var outOfRange = false
        for (k in map.keys) {
            val i = k.toIntOrNull() ?: continue
            if (i >= 54) { outOfRange = true; continue }
            if (i >= 0) maxIndex = maxOf(maxIndex, i)
        }
        if (outOfRange) diag(diagKey, "сервер прислал слоты с индексами >= 54 — они не влезут в сетку эндер-сундука")
        if (maxIndex < 0) {
            diag(diagKey, "в карте нет числовых ключей 0..53, примеры: ${map.keys.take(5)}")
            return null
        }
        // Выравнивание по индексам: дыры (отсутствующие ключи) → EMPTY,
        // иначе грид съезжает (в оригинале этого не было).
        val items = ArrayList<ItemStack>(maxIndex + 1)
        for (i in 0..maxIndex) items.add(decode(map[i.toString()], i, nbtOps, jsonOps, diagKey))
        if (items.none { !it.isEmpty }) return null
        return StoragePreviewTooltip(items)
    }

    private fun decode(
        el: JsonElement?,
        idx: Int,
        nbtOps: DynamicOps<Tag>,
        jsonOps: DynamicOps<JsonElement>,
        diagKey: String
    ): ItemStack {
        if (el == null || el.isJsonNull) return ItemStack.EMPTY
        try {
            val result = if (el.isJsonObject) {
                // Сервер мог положить объект вместо строки — декодим прямо из JSON
                ItemStack.CODEC.parse(jsonOps, el.asJsonObject)
            } else {
                val s = try { el.asString } catch (_: Exception) { return ItemStack.EMPTY }
                if (s.isEmpty() || s == "null") return ItemStack.EMPTY
                val tag = TagParser.parseCompoundFully(s)
                ItemStack.CODEC.parse(nbtOps, tag)
            }
            return result.resultOrPartial { msg ->
                diag(diagKey, "слот $idx: предмет не декодирован: $msg")
            }.orElse(ItemStack.EMPTY)
        } catch (e: Exception) {
            diag(diagKey, "слот $idx: ошибка разбора SNBT: ${e.message}")
            return ItemStack.EMPTY
        }
    }

    // Диагностика без спама: каждое новое содержимое превью — максимум несколько строк в лог
    private var lastDiagKey: String? = null
    private var diagCount = 0
    private fun diag(diagKey: String, msg: String) {
        if (diagKey != lastDiagKey) {
            lastDiagKey = diagKey
            diagCount = 0
        }
        if (diagCount < 6) {
            diagCount++
            StarredHeltix.LOGGER.warn("[StoragePreview] $msg")
        }
    }
}

/** Общий маркер — возвращается из ItemStack.getTooltipImage(). */
class StoragePreviewTooltip(val items: List<ItemStack>) : TooltipComponent

/**
 * Клиентский рендер: сетка 9 слотов в ряд (как инвентарь/сундук), максимум 54,
 * с подложкой по редкости предмета — стиль подложки наследуется из «Слот-подсветки»
 * (круг/квадрат с обводкой/простой квадрат), при выключенной подсветке — старый прямоугольник.
 */
class StoragePreviewClientTooltip(private val preview: StoragePreviewTooltip) : ClientTooltipComponent {
    private val cols = 9
    private val rows = (preview.items.size + cols - 1) / cols

    override fun getHeight(font: Font): Int = rows * 19 + 2
    override fun getWidth(font: Font): Int = cols * 19 + 2

    // Как у оригинала из heltix-mod: тултип превью живёт и когда предмет в руке
    override fun showTooltipWithItemInHand(): Boolean = true

    override fun extractImage(font: Font, x: Int, y: Int, w: Int, h: Int, graphics: GuiGraphicsExtractor) {
        val offX = x + (w - cols * 19) / 2
        for ((idx, stack) in preview.items.withIndex()) {
            if (stack.isEmpty) continue
            val sx = offX + (idx % cols) * 19
            val sy = y + 1 + (idx / cols) * 19
            val rgb = SlotOverlayUtil.getRgbColor(stack)
            if (rgb != null) {
                SlotOverlayUtil.drawPreviewBackground(graphics, sx + 1, sy + 1, 16, rgb)
            }
            graphics.item(stack, sx + 2, sy + 2, idx + 1)
            graphics.itemDecorations(font, stack, sx + 2, sy + 2)
        }
    }
}

