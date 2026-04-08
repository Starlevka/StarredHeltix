package set.starlev.utils.detectors

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.Resource
import net.minecraft.server.packs.resources.ResourceManager
import set.starlev.events.GuiEvents
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Детектор для обнаружения и идентификации контейнеров (меню).
 */
object ContainerDetector {
    private val openCallbacks = mutableListOf<(AbstractContainerScreen<*>) -> Unit>()
    private val closeCallbacks = mutableListOf<(AbstractContainerScreen<*>) -> Unit>()
    @Volatile
    private var currentContainerInfo: ContainerInfo? = null

    private val stripFormattingRegex = Regex("§.")
    private val whitespaceRegex = Regex("\\s+")

    data class ContainerInfo(
        val rawTitle: String,
        val decodedTitle: String,
        val normalizedTitle: String,
        val screenClassName: String,
    )

    fun init() {
        GuiEvents.registerOpen { screen ->
            val rawTitle = screen.title.string
            val decodedTitle = decodeTitle(rawTitle)
            currentContainerInfo = ContainerInfo(
                rawTitle = rawTitle,
                decodedTitle = decodedTitle,
                normalizedTitle = normalize(decodedTitle),
                screenClassName = screen::class.java.name,
            )
            openCallbacks.forEach { it(screen) }
        }

        GuiEvents.registerClose { screen ->
            currentContainerInfo = null
            closeCallbacks.forEach { it(screen) }
        }
    }

    /**
     * Регистрирует колбэк, который вызывается при открытии любого контейнера.
     */
    fun registerOpen(callback: (AbstractContainerScreen<*>) -> Unit) {
        openCallbacks.add(callback)
    }

    /**
     * Регистрирует колбэк, который вызывается при закрытии любого контейнера.
     */
    fun registerClose(callback: (AbstractContainerScreen<*>) -> Unit) {
        closeCallbacks.add(callback)
    }

    fun getCurrentContainerInfo(): ContainerInfo? = currentContainerInfo

    fun decodeTitle(text: String): String {
        return FontTitleDecoder.decode(text)
    }

    private fun normalize(text: String): String {
        return text
            .replace(stripFormattingRegex, "")
            .replace('\u00A0', ' ')
            .lowercase()
            .replace(whitespaceRegex, " ")
            .trim()
    }

    private object FontTitleDecoder {
        @Volatile
        private var codepointMap: Map<Int, String>? = null
        @Volatile
        private var lastResourceManager: ResourceManager? = null

        fun invalidate() {
            codepointMap = null
        }

        fun decode(text: String): String {
            val resourceManager = Minecraft.getInstance().resourceManager
            if (lastResourceManager !== resourceManager) {
                lastResourceManager = resourceManager
                codepointMap = null
            }

            val map = codepointMap ?: run {
                val built = buildCodepointMap(resourceManager)
                codepointMap = built
                built
            }

            if (map.isEmpty()) return stripFormatting(text)

            val out = StringBuilder(text.length)
            val cps = stripFormatting(text).codePoints().toArray()
            for (cp in cps) {
                val replacement = map[cp]
                if (replacement != null) {
                    out.append(replacement)
                } else {
                    out.appendCodePoint(cp)
                }
            }
            return out.toString()
        }

        private fun stripFormatting(text: String): String {
            return text.replace(stripFormattingRegex, "")
        }

        private fun buildCodepointMap(resourceManager: ResourceManager): Map<Int, String> {
            val map = HashMap<Int, String>(256)
            val fontJsons = listOf(
                ResourceLocation.fromNamespaceAndPath("minecraft", "font/default.json"),
                ResourceLocation.fromNamespaceAndPath("minecraft", "font/skyletters.json"),
                ResourceLocation.fromNamespaceAndPath("minecraft", "font/hfont.json"),
            )

            for (loc in fontJsons) {
                val root = readJsonObject(resourceManager, loc) ?: continue
                val providers = root.getAsJsonArray("providers") ?: continue
                for (providerEl in providers) {
                    val providerObj = providerEl.asJsonObject
                    val type = providerObj.get("type")?.asString ?: continue
                    if (type != "bitmap") continue

                    val file = providerObj.get("file")?.asString ?: continue
                    val chars = providerObj.getAsJsonArray("chars") ?: continue
                    val codepoints = extractSingleCodepoint(chars) ?: continue

                    val replacement = inferReplacementFromFile(file) ?: continue
                    map.putIfAbsent(codepoints, replacement)
                }
            }

            return map
        }

        private fun extractSingleCodepoint(chars: JsonArray): Int? {
            var found: Int? = null
            for (rowEl in chars) {
                val row = rowEl.asString
                val cps = row.codePoints().toArray()
                for (cp in cps) {
                    if (found != null) return null
                    found = cp
                }
            }
            return found
        }

        private fun inferReplacementFromFile(file: String): String? {
            val normalized = file.lowercase()

            Regex("custom/ui/ranks/skyletters/letter_([a-z])\\.png").find(normalized)?.let { match ->
                return match.groupValues[1].uppercase()
            }

            Regex("custom/hfont/([a-z0-9_]+)\\.png").find(normalized)?.let { match ->
                val key = match.groupValues[1]
                return hfontKeyToCyrillic(key)
            }

            return null
        }

        private fun hfontKeyToCyrillic(key: String): String? {
            return when (key) {
                "a" -> "А"
                "b" -> "Б"
                "v" -> "В"
                "g" -> "Г"
                "d" -> "Д"
                "e" -> "Е"
                "e2" -> "Ё"
                "j" -> "Ж"
                "z" -> "З"
                "i" -> "И"
                "i2" -> "Й"
                "k" -> "К"
                "l" -> "Л"
                "m" -> "М"
                "n" -> "Н"
                "o" -> "О"
                "p" -> "П"
                "r" -> "Р"
                "s" -> "С"
                "t" -> "Т"
                "y" -> "У"
                "f" -> "Ф"
                "h" -> "Х"
                "c" -> "Ц"
                "ch" -> "Ч"
                "sh" -> "Ш"
                "sch" -> "Щ"
                "tverd" -> "Ъ"
                "ib" -> "Ы"
                "mgk" -> "Ь"
                "etverd" -> "Э"
                "yu" -> "Ю"
                "ya" -> "Я"
                else -> null
            }
        }

        private fun readJsonObject(resourceManager: ResourceManager, loc: ResourceLocation): JsonObject? {
            val resource = getResource(resourceManager, loc) ?: return null
            try {
                InputStreamReader(resource.open(), StandardCharsets.UTF_8).use { reader ->
                    val el = JsonParser.parseReader(reader)
                    if (!el.isJsonObject) return null
                    return el.asJsonObject
                }
            } catch (_: Throwable) {
                return null
            }
        }

        private fun getResource(resourceManager: ResourceManager, loc: ResourceLocation): Resource? {
            return try {
                val opt = resourceManager.getResource(loc)
                if (opt.isPresent) opt.get() else null
            } catch (_: Throwable) {
                null
            }
        }
    }
}
