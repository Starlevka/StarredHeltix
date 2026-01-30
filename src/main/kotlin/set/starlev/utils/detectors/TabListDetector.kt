package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.PlayerTeam
import set.starlev.mixin.accessors.PlayerTabOverlayAccessor
import set.starlev.utils.CacheManager

object TabListDetector {
    private const val COLOR_PATTERN = "(?i)§[0-9a-fk-orlnmxz]"

    /**
     * Получить табулятор лист (список игроков)
     * @return Header из таб листа
     */
    fun getTabListHeader(): String {
        val client = Minecraft.getInstance()
        val gui = client.gui ?: return ""
        val tabList = gui.tabList ?: return ""
        
        return try {
            if (tabList !is PlayerTabOverlayAccessor) return ""
            val header = tabList.getHeader()
            componentToString(header)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Получить footer из таб листа
     * @return Footer строка
     */
    fun getTabListFooter(): String {
        val client = Minecraft.getInstance()
        val gui = client.gui ?: return ""
        val tabList = gui.tabList ?: return ""
        
        return try {
            if (tabList !is PlayerTabOverlayAccessor) return ""
            val footer = tabList.getFooter()
            componentToString(footer)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Получить заголовок и футер (где обычно находятся статы и поручения)
     */
    fun getHeaderAndFooterLines(): List<String> {
        val cached = CacheManager.getCachedTabList("header_footer")
        if (cached != null) return cached

        val lines = mutableListOf<String>()
        
        val header = getTabListHeader()
        if (header.isNotEmpty()) {
            lines.addAll(header.split("\n"))
        }
        
        val footer = getTabListFooter()
        if (footer.isNotEmpty()) {
            lines.addAll(footer.split("\n"))
        }
        
        val result = lines.map { cleanLine(it) }.filter { it.isNotEmpty() }
        CacheManager.cacheTabList("header_footer", result)
        return result
    }

    /**
     * Получить все строки из таб листа (включая заголовок, футер и тело)
     */
    fun getAllTabListLines(): List<String> {
        val cached = CacheManager.getCachedTabList("all_clean")
        if (cached != null) return cached

        val result = getAllTabListLinesFormatted().map { cleanLine(it) }
        CacheManager.cacheTabList("all_clean", result)
        return result
    }

    /**
     * Получить все строки из таб листа (включая заголовок, футер и тело) с цветовыми кодами
     */
    fun getAllTabListLinesFormatted(): List<String> {
        val cached = CacheManager.getCachedTabList("all_formatted")
        if (cached != null) return cached

        val client = Minecraft.getInstance()
        val lines = mutableListOf<String>()
        
        // Добавляем header
        val header = getTabListHeaderRaw()
        if (header != null) {
            lines.addAll(componentToFormattedString(header).split("\n"))
        }
        
        // Добавляем тело (список игроков)
        val tabList = client.gui?.tabList
        if (tabList != null) {
            try {
                if (tabList is PlayerTabOverlayAccessor) {
                    val entries = tabList.invokeGetPlayerInfos()
                    entries.forEach { entry ->
                        val displayName = entry.tabListDisplayName
                        val text = if (displayName != null) {
                            componentToFormattedString(displayName)
                        } else {
                            PlayerTeam.formatNameForTeam(entry.team, Component.literal(entry.profile.name)).getString()
                        }
                        lines.add(text)
                    }
                }
            } catch (e: Exception) {
            }
        }
        
        // Добавляем footer
        val footer = getTabListFooterRaw()
        if (footer != null) {
            lines.addAll(componentToFormattedString(footer).split("\n"))
        }
        
        val result = lines.filter { it.isNotEmpty() }
        CacheManager.cacheTabList("all_formatted", result)
        return result
    }

    /**
     * Получить все строки из таб листа как объекты Component
     */
    fun getAllTabListComponents(): List<Component> {
        val client = Minecraft.getInstance()
        val components = mutableListOf<Component>()
        
        fun addComponentLines(comp: Component) {
            val formatted = componentToFormattedString(comp)
            if (formatted.contains("\n")) {
                val lines = formatted.split("\n")
                lines.forEach { line ->
                    if (line.isNotEmpty()) {
                        // Пытаемся сохранить оригинальное форматирование, если это возможно
                        // Но так как мы разделили на строки, проще всего создать новые литтералы с цветовыми кодами
                        components.add(Component.literal(line))
                    }
                }
            } else {
                components.add(comp)
            }
        }

        // Добавляем header
        getTabListHeaderRaw()?.let { addComponentLines(it) }
        
        // Добавляем тело
        val tabList = client.gui?.tabList
        if (tabList != null && tabList is PlayerTabOverlayAccessor) {
            val entries = tabList.invokeGetPlayerInfos()
            entries.forEach { entry ->
                val displayName = entry.tabListDisplayName
                if (displayName != null) {
                    addComponentLines(displayName)
                } else {
                    components.add(PlayerTeam.formatNameForTeam(entry.team, Component.literal(entry.profile.name)))
                }
            }
        }
        
        // Добавляем footer
        getTabListFooterRaw()?.let { addComponentLines(it) }
        
        return components
    }

    private fun getTabListHeaderRaw(): Component? {
        val client = Minecraft.getInstance()
        val gui = client.gui ?: return null
        val tabList = gui.tabList ?: return null
        return if (tabList is PlayerTabOverlayAccessor) tabList.getHeader() else null
    }

    private fun getTabListFooterRaw(): Component? {
        val client = Minecraft.getInstance()
        val gui = client.gui ?: return null
        val tabList = gui.tabList ?: return null
        return if (tabList is PlayerTabOverlayAccessor) tabList.getFooter() else null
    }

    @JvmStatic
    fun componentToFormattedString(component: Component): String {
        val sb = StringBuilder()
        
        fun appendComponent(comp: Component) {
            val style = comp.style
            
            // Добавляем цветовой код
            if (style.color != null) {
                val rgb = style.color!!.value
                val code = getColorCode(rgb)
                if (code != null) {
                    sb.append("§$code")
                } else {
                    // Кастомный HEX цвет в формате §x§r§g§b
                    val hex = String.format("%06x", rgb and 0xFFFFFF)
                    sb.append("§x")
                    for (c in hex) {
                        sb.append("§$c")
                    }
                }
            }
            
            // Добавляем стили
            if (style.isBold) sb.append("§l")
            if (style.isItalic) sb.append("§o")
            if (style.isUnderlined) sb.append("§n")
            if (style.isStrikethrough) sb.append("§m")
            if (style.isObfuscated) sb.append("§k")
            
            // Добавляем текст самого компонента
            val contents = comp.contents
            if (contents is net.minecraft.network.chat.contents.PlainTextContents) {
                sb.append(contents.text())
            }
            
            // Рекурсивно обрабатываем вложенные компоненты
            comp.siblings.forEach { appendComponent(it) }
        }
        
        appendComponent(component)
        return sb.toString()
    }
    
    private fun getColorCode(rgb: Int): Char? {
        // Маппинг ARGB/RGB в классические коды §
        return when (rgb and 0xFFFFFF) {
            0x000000 -> '0' // Black
            0x0000AA -> '1' // Dark Blue
            0x00AA00 -> '2' // Dark Green
            0x00AAAA -> '3' // Dark Aqua
            0xAA0000 -> '4' // Dark Red
            0xAA00AA -> '5' // Dark Purple
            0xFFAA00 -> '6' // Gold
            0xAAAAAA -> '7' // Gray
            0x555555 -> '8' // Dark Gray
            0x5555FF -> '9' // Blue
            0x55FF55 -> 'a' // Green
            0x55FFFF -> 'b' // Aqua
            0xFF5555 -> 'c' // Red
            0xFF55FF -> 'd' // Light Purple
            0xFFFF55 -> 'e' // Yellow
            0xFFFFFF -> 'f' // White
            else -> null
        }
    }

    /**
     * Очистить строку от цветовых кодов и лишних пробелов
     */
    fun cleanLine(line: String): String {
        return CacheManager.getRegex(COLOR_PATTERN).replace(line, "").trim()
    }

    /**
     * Конвертировать Component в строку
     */
    private fun componentToString(component: Component?): String {
        return component?.getString() ?: ""
    }
}
