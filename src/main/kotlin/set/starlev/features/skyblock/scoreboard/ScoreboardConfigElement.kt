package set.starlev.features.skyblock.scoreboard

import set.starlev.features.skyblock.scoreboard.elements.*

enum class ScoreboardConfigElement(val element: ScoreboardElement) {
    PURSE(ScoreboardElementPurse),
    BANK(ScoreboardElementBank),
    GEMS(ScoreboardElementGems),
    COOKIE(ScoreboardElementCookie),
    FPS(ScoreboardElementFPS),
    PING(ScoreboardElementPing),
    CPS(ScoreboardElementCPS),
    BPS(ScoreboardElementBPS),
    LOCATION(ScoreboardElementLocation),
    EMPTY_LINE(ScoreboardElementEmptyLine),
    EMPTY_LINE2(ScoreboardElementEmptyLine),
    EMPTY_LINE3(ScoreboardElementEmptyLine),
    EMPTY_LINE4(ScoreboardElementEmptyLine),
    EMPTY_LINE5(ScoreboardElementEmptyLine),
    ;

    override fun toString() = element.configLine

    companion object {
        fun getElements() = entries.map { it.element }

        @JvmField
        val defaultOptions = listOf(
            EMPTY_LINE,
            LOCATION,
            EMPTY_LINE2,
            PURSE,
            BANK,
            GEMS,
            EMPTY_LINE3,
            FPS,
            PING,
            CPS,
            BPS,
            EMPTY_LINE4,
            COOKIE,
            EMPTY_LINE5
        )
    }
}