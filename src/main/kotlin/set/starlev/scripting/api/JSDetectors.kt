package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import set.starlev.utils.detectors.*

/**
 * JS API: Фасад для всех 19 детекторов StarredHeltix
 */
class JSDetectors {
    val actionBar = JSActionBarDetector()
    val biome = JSBiomeDetector()
    val chat = JSChatDetector()
    val container = JSContainerDetector()
    val dungeon = JSDungeonDetector()
    val enchantProgress = JSEnchantProgressDetector()
    val entityDeath = JSEntityDeathDetector()
    val heldItem = JSHeldItemDetector()
    val itemLore = JSItemLoreDetector()
    val location = JSLocationDetector()
    val mobHead = JSMobHeadDisplayDetector()
    val museum = JSMuseumDetector()
    val playerItem = JSPlayerHeldItemDetector()
    val scoreboard = JSScoreboardDetector()
    val skillXp = JSSkillXpDetector()
    val slotPhase = JSSlotPhaseDetector()
    val tabList = JSTabListDetector()
    val time = JSTimeDetector()
    val title = JSTitleDetector()
}

class JSActionBarDetector {
    fun getText(): String = ActionBarDetector.getActionBarText()
}

class JSBiomeDetector {
    fun get(): String = BiomeDetector.getPlayerBiome()
}

class JSChatDetector {
    fun getLastMessage(): String = ChatDetector.getLastChatMessage()
    fun getHistory(limit: Int): List<String> = ChatDetector.getChatHistory(limit)
    fun cleanColor(text: String): String = ChatDetector.cleanColor(text)
}

class JSContainerDetector {
    fun getInfo(): Map<String, Any?>? {
        val info = ContainerDetector.getCurrentContainerInfo() ?: return null
        return mapOf(
            "rawTitle" to info.rawTitle,
            "decodedTitle" to info.decodedTitle,
            "normalizedTitle" to info.normalizedTitle,
            "screenClass" to info.screenClassName
        )
    }

    fun decodeTitle(text: String): String = ContainerDetector.decodeTitle(text)
}

class JSDungeonDetector {
    fun isInDungeon(): Boolean = DungeonDetector.isInDungeon()
}

class JSEnchantProgressDetector {
    fun getHeldData(): List<Map<String, Any?>> {
        return EnchantmentProgressDetector.getHeldEnchantmentData().map {
            mapOf(
                "type" to it.type.name,
                "displayName" to it.type.displayName,
                "skillName" to it.type.skillName,
                "level" to it.level,
                "current" to (it.progress?.current ?: 0.0),
                "target" to (it.progress?.target ?: 0.0)
            )
        }
    }

    fun getActiveData(): List<Map<String, Any?>> {
        return EnchantmentProgressDetector.getActiveEnchantmentData().map {
            mapOf(
                "type" to it.type.name,
                "displayName" to it.type.displayName,
                "skillName" to it.type.skillName,
                "level" to it.level,
                "current" to (it.progress?.current ?: 0.0),
                "target" to (it.progress?.target ?: 0.0)
            )
        }
    }

    fun parseNumber(str: String): Double? = EnchantmentProgressDetector.parseNumber(str)
    fun formatNumber(value: Double): String = EnchantmentProgressDetector.formatNumber(value)
}

class JSEntityDeathDetector {
    // Обработка слушателей идёт через JSEvents
}

class JSHeldItemDetector {
    fun getTooltipText(): String = HeldItemDetector.getTooltipText()
    fun isTooltipVisible(): Boolean = HeldItemDetector.isTooltipVisible()
}

class JSItemLoreDetector {
    fun getHeldLore(): List<String> {
        val stack = Minecraft.getInstance().player?.mainHandItem ?: return emptyList()
        return ItemLoreDetector.getLore(stack)
    }

    fun getHeldFullTooltip(): List<String> {
        val stack = Minecraft.getInstance().player?.mainHandItem ?: return emptyList()
        return ItemLoreDetector.getFullTooltip(stack)
    }
}

class JSLocationDetector {
    fun get(): String? = LocationDetector.getCurrentLocation()
    fun getClean(): String? = LocationDetector.getCurrentLocationClean()
    fun isIn(name: String): Boolean = LocationDetector.isInLocation(name)
    fun isInAny(vararg names: String): Boolean = LocationDetector.isInAnyLocation(*names)
    fun resetCache() = LocationDetector.resetCache()
}

class JSMobHeadDisplayDetector {
    // Доступен на экземплярах сущностей при необходимости
}

class JSMuseumDetector {
    fun isInZone(): Boolean = MuseumDetector.isInMuseumZone()
    fun isOpen(): Boolean = MuseumDetector.isOpen()
}

class JSPlayerHeldItemDetector {
    fun getMainHandItemName(): String = PlayerHeldItemDetector.getMainHandItemName()
    fun getOffHandItemName(): String = PlayerHeldItemDetector.getOffHandItemName()
}

class JSScoreboardDetector {
    fun getLines(): List<String> = ScoreboardDetector.getScoreboardText()
    fun getTitle(): String = ScoreboardDetector.getScoreboardTitle()
    fun getCookieStatus(): String = ScoreboardDetector.getCookieStatus()
}

class JSSkillXpDetector {
    fun getLastSkill(): String? = SkillXpDetector.getLastSkill()
    fun getLastInfo(): Map<String, Any?>? {
        val info = SkillXpDetector.getLastInfo() ?: return null
        return mapOf(
            "gained" to info.gained,
            "skill" to info.skill,
            "current" to info.current,
            "target" to info.target,
            "isMax" to info.isMax,
            "percent" to info.percent,
            "lastUpdate" to info.lastUpdate
        )
    }
}

class JSSlotPhaseDetector {
    fun detectHeld(): String {
        val stack = Minecraft.getInstance().player?.mainHandItem ?: return "NONE"
        return SlotPhaseDetector.detect(stack).name
    }
}

class JSTabListDetector {
    fun getHeader(): String = TabListDetector.getTabListHeader()
    fun getFooter(): String = TabListDetector.getTabListFooter()
    fun getAllLines(): List<String> = TabListDetector.getAllTabListLines()
    fun getAllLinesFormatted(): List<String> = TabListDetector.getAllTabListLinesFormatted()
    fun getGems(): String? = TabListDetector.getGems()
    fun getGemsLine(): String? = TabListDetector.getGemsLine()
    fun getBank(): String? = TabListDetector.getBank()
    fun getBankLine(): String? = TabListDetector.getBankLine()
    fun cleanLine(line: String): String = TabListDetector.cleanLine(line)
}

class JSTimeDetector {
    fun getTime(): String = TimeDetector.getTime()
    fun getDate(): String = TimeDetector.getDate()
    fun getYear(): String = TimeDetector.getYear()
    fun getFullDateTime(): String = TimeDetector.getFullDateTime()
}

class JSTitleDetector {
    fun getText(): String = TitleDetector.getTitleText()
    fun getSubtitle(): String = TitleDetector.getSubtitleText()
    fun isVisible(): Boolean = TitleDetector.isTitleVisible()
}
