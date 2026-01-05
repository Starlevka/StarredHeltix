package set.starlev.utils.detectors

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.PlayerTeam
import set.starlev.mixin.accessors.PlayerTabOverlayAccessor

object TabListDetector {
    private val COLOR_PATTERN = Regex("(?i)§[0-9a-fk-orlnmxz]")

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
     * Получить только заголовок и футер (где обычно находятся статы и поручения)
     */
    fun getHeaderAndFooterLines(): List<String> {
        val lines = mutableListOf<String>()
        
        val header = getTabListHeader()
        if (header.isNotEmpty()) {
            lines.addAll(header.split("\n"))
        }
        
        val footer = getTabListFooter()
        if (footer.isNotEmpty()) {
            lines.addAll(footer.split("\n"))
        }
        
        return lines.map { cleanLine(it) }.filter { it.isNotEmpty() }
    }

    /**
     * Получить все строки из таб листа (включая заголовок, футер и тело)
     */
    fun getAllTabListLines(): List<String> {
        val client = Minecraft.getInstance()
        val lines = mutableListOf<String>()
        
        // Добавляем header
        val header = getTabListHeader()
        if (header.isNotEmpty()) {
            lines.addAll(header.split("\n"))
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
                            displayName.getString()
                        } else {
                            PlayerTeam.formatNameForTeam(entry.team, Component.literal(entry.profile.name)).getString()
                        }
                        lines.add(text)
                    }
                } else if (client.connection != null) {
                    // Fallback если Mixin не сработал
                    val entries = client.connection?.onlinePlayers
                    entries?.forEach { entry ->
                        val displayName = entry.tabListDisplayName
                        val text = if (displayName != null) {
                            displayName.getString()
                        } else {
                            PlayerTeam.formatNameForTeam(entry.team, Component.literal(entry.profile.name)).getString()
                        }
                        lines.add(text)
                    }
                }
            } catch (e: Exception) {
                // Игнорируем ошибки при получении списка игроков
            }
        }
        
        // Добавляем footer
        val footer = getTabListFooter()
        if (footer.isNotEmpty()) {
            lines.addAll(footer.split("\n"))
        }
        
        return lines.map { cleanLine(it) }.filter { it.isNotEmpty() }
    }

    /**
     * Очистить строку от цветовых кодов и лишних пробелов
     */
    fun cleanLine(line: String): String {
        return COLOR_PATTERN.replace(line, "").trim()
    }

    /**
     * Конвертировать Component в строку
     */
    private fun componentToString(component: Component?): String {
        return component?.getString() ?: ""
    }
}
