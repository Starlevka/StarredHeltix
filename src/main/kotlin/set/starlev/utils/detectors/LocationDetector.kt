package set.starlev.utils.detectors

/**
 * Известная локация Heltix: каноническое русское название + алиасы
 * (англоязычные варианты с Hypixel и сокращения).
 *
 * @param name каноническое русское название из интерфейса игры
 * @param aliases альтернативные названия для сопоставления
 */
data class KnownLocation(
    val name: String,
    val aliases: List<String> = emptyList()
) {
    /** Все варианты названия для сопоставления. */
    val allNames: List<String> = listOf(name) + aliases
}

/**
 * Детектор текущей локации на основе данных scoreboard.
 * Определяет локацию по символу ⏣/ф и заголовку скорборда.
 * Содержит реестр всех известных локаций Heltix (48 основных + 12 Гномьих шахт)
 * и умеет разрешать текущую локацию в каноническую с учётом алиасов.
 */
object LocationDetector {

    private var cachedLocation: String? = null
    private var lastPollTime: Long = 0
    private const val POLL_INTERVAL_MS = 2000L

    /** Кэш разрешения текущей локации в известную (ключ — чистая строка локации). */
    private var cachedKnownLocation: KnownLocation? = null
    private var cachedKnownLocationSource: String? = null

    /** Основные локации Heltix — все 48 из интерфейса путешествий. */
    val KNOWN_LOCATIONS: List<KnownLocation> = listOf(
        KnownLocation("Адская крепость", listOf("Hell Fortress", "Nether Fortress")),
        KnownLocation("Алмазный резерв", listOf("Diamond Reserve")),
        KnownLocation("Амбар", listOf("Barn")),
        KnownLocation("Аукцион", listOf("Auction House", "Auctions")),
        KnownLocation("Банк", listOf("Bank")),
        KnownLocation("Бездна", listOf("Void", "The Void")),
        KnownLocation("Берёзовый парк", listOf("Birch Park")),
        KnownLocation("Библиотека", listOf("Library")),
        KnownLocation("Воющая пещера", listOf("Howling Cave")),
        KnownLocation("Глубокие пещеры", listOf("Deep Caverns")),
        KnownLocation("Гора", listOf("Mountain")),
        KnownLocation("Гравиевые шахты", listOf("Gravel Mines", "Gravel Mine")),
        KnownLocation("Грибная пустыня", listOf("Mushroom Desert")),
        KnownLocation("Джунгли", listOf("Jungle")),
        KnownLocation("Дикая природа", listOf("Wilderness")),
        KnownLocation("Дом викингов", listOf("Viking House", "Vikings House")),
        KnownLocation("Дом строителя", listOf("Builder's House", "Builders House")),
        KnownLocation("Драконье гнездо", listOf("Dragon's Nest", "Dragons Nest")),
        KnownLocation("Еловый лес", listOf("Spruce Forest")),
        KnownLocation("Золотая шахта", listOf("Gold Mine")),
        KnownLocation("Кладбище", listOf("Graveyard")),
        KnownLocation("Кузница", listOf("Forge", "The Forge")),
        KnownLocation("Лагерь археолога", listOf("Archaeologist Camp")),
        KnownLocation("Лазуритовый карьер", listOf("Lapis Quarry")),
        KnownLocation("Лес", listOf("Forest")),
        KnownLocation("Логово Арахны", listOf("Arachne's Lair", "Arachne Lair")),
        KnownLocation("Логово свинозомби", listOf("Zombie Pigman Lair", "Pigman Lair")),
        KnownLocation("Магазин модной одежды", listOf("Fashion Shop")),
        KnownLocation("Музей", listOf("Museum")),
        KnownLocation("Оазис", listOf("Oasis")),
        KnownLocation("Обсидиановое святилище", listOf("Obsidian Sanctuary")),
        KnownLocation("Общественный центр", listOf("Community Center")),
        KnownLocation("Оружейня", listOf("Armory", "Armoury")),
        KnownLocation("Паучье логово", listOf("Spider's Den", "Spiders Den")),
        KnownLocation("Пещера светящихся грибов", listOf("Glowing Mushroom Cave")),
        KnownLocation("Пороховые пещеры", listOf("Gunpowder Mines")),
        KnownLocation("Пустотный склеп", listOf("Void Crypt")),
        KnownLocation("Руины", listOf("Ruins")),
        KnownLocation("Рыночный переулок", listOf("Market Alley")),
        KnownLocation("Саванна", listOf("Savanna")),
        KnownLocation("Слизневая гора", listOf("Slimehill", "Slime Hill")),
        KnownLocation("Таверна", listOf("Tavern")),
        KnownLocation("Тёмная чаща", listOf("Dark Thicket")),
        KnownLocation("Убежище Арахны", listOf("Arachne's Sanctuary", "Arachne Sanctuary")),
        KnownLocation("Убежище Зеалотов", listOf("Zealot's Refuge", "Zealots Refuge")),
        KnownLocation("Угольная шахта", listOf("Coal Mine")),
        KnownLocation("Ферма", listOf("Farm", "The Farm")),
        KnownLocation("Энд", listOf("The End", "End"))
    )

    /** Локации Гномьих шахт (Dwarven Mines) — все 12. */
    val DWARVEN_MINES_LOCATIONS: List<KnownLocation> = listOf(
        KnownLocation("Великая ледяная стена", listOf("Great Ice Wall")),
        KnownLocation("Верхние шахты", listOf("Upper Mines")),
        KnownLocation("Ворота Дивана", listOf("Divan's Gate", "Gates to the Mines")),
        KnownLocation("Гномья кузница", listOf("Dwarven Forge")),
        KnownLocation("Горные рудники", listOf("Cliffside Veins")),
        KnownLocation("Дальний резерв", listOf("Far Reserve")),
        KnownLocation("Карьер", listOf("Quarry")),
        KnownLocation("Королевские шахты", listOf("Royal Mines")),
        KnownLocation("Королевский дворец", listOf("Royal Palace", "Palace")),
        KnownLocation("Лавовый источник", listOf("Lava Springs")),
        KnownLocation("Норы гоблинов", listOf("Goblin Burrows")),
        KnownLocation("Туман", listOf("Mist"))
    )

    /** Все известные локации (основные + Гномьи шахты). */
    val ALL_KNOWN_LOCATIONS: List<KnownLocation> = KNOWN_LOCATIONS + DWARVEN_MINES_LOCATIONS

    /**
     * Порядок для частичного матчинга: длинные названия проверяются первыми,
     * чтобы «Гномья кузница» не распознавалась как «Кузница».
     */
    private val matchOrdered = ALL_KNOWN_LOCATIONS.sortedByDescending {
        it.allNames.maxOf { name -> name.length }
    }

    /**
        * Получить текущую локацию (кэшируется на 2 секунды).
        * Возвращает полную строку из scoreboard, содержащую локацию.
        * @return Строка scoreboard с локацией или null если не определена
        */
    fun getCurrentLocation(): String? {
        val now = System.currentTimeMillis()
        if (now - lastPollTime < POLL_INTERVAL_MS) return cachedLocation

        lastPollTime = now
        val location = detectLocation()
        cachedLocation = location
        return location
    }

    /**
        * Получить чистое название локации без цветовых кодов.
        */
    fun getCurrentLocationClean(): String? {
        return getCurrentLocation()?.let { cleanLocationName(it) }
    }

    /**
     * Разрешить текущую локацию в известную из реестра (основные + Гномьи шахты).
     * Учитывает алиасы: «Slimehill» и «Слизневая гора» — одна локация.
     * @return каноническая локация или null, если текущая не входит в реестр
     */
    fun getKnownLocation(): KnownLocation? {
        val current = getCurrentLocationClean() ?: return null
        if (cachedKnownLocationSource == current) return cachedKnownLocation

        val resolved = resolveKnownLocation(current)
        cachedKnownLocationSource = current
        cachedKnownLocation = resolved
        return resolved
    }

    /**
     * Разрешить произвольную строку в известную локацию.
     * Сначала точное совпадение по всем названиям, затем частичное
     * (длинные названия первыми, ё↔е и регистр не важны).
     */
    fun resolveKnownLocation(location: String): KnownLocation? {
        val normalized = normalizeForMatch(normalizeLocation(location))
        if (normalized.isBlank()) return null

        ALL_KNOWN_LOCATIONS.firstOrNull { known ->
            known.allNames.any { normalizeForMatch(it) == normalized }
        }?.let { return it }

        if (normalized.length < 3) return null
        matchOrdered.firstOrNull { known ->
            known.allNames.any { name ->
                val n = normalizeForMatch(name)
                n.length >= 3 && (normalized.contains(n) || n.contains(normalized))
            }
        }?.let { return it }

        return null
    }

    /**
     * Проверить, находится ли игрок в одной из локаций Гномьих шахт.
     * Ловит и общий регион «Гномьи шахты» по ключевым словам.
     */
    fun isInDwarvenMines(): Boolean {
        val known = getKnownLocation()
        if (known != null && known in DWARVEN_MINES_LOCATIONS) return true
        return isInAnyLocation("Гномьи", "Dwarven", "Гномьих шахт")
    }

    /**
        * Проверить, находится ли игрок в определённой локации.
        * Кроме прямого вхождения учитываются алиасы известной локации
        * (например, «Slimehill» найдёт «Слизневую гору» на скорборде).
        * @param locationName название локации или её алиас (регистронезависимо)
        */
    fun isInLocation(locationName: String): Boolean {
        val current = getCurrentLocationClean() ?: return false
        if (current.contains(locationName, ignoreCase = true)) return true

        val known = getKnownLocation() ?: return false
        val query = normalizeForMatch(locationName)
        if (query.isBlank()) return false
        return known.allNames.any { normalizeForMatch(it).contains(query) }
    }

    /**
        * Проверить, находится ли игрок в одной из перечисленных локаций.
        */
    fun isInAnyLocation(vararg locationNames: String): Boolean {
        return locationNames.any { isInLocation(it) }
    }

    private fun detectLocation(): String? {
        val lines = ScoreboardDetector.getScoreboardText()

        // Строка локации — та, где глиф локации стоит В НАЧАЛЕ строки: ⏣ или буква,
        // в которую этот глиф декодируется на некоторых серверах.
        for (line in lines) {
            val stripped = line.replace(COLOR_CODE_REGEX, "").replace("~", "").trim()
            if (!LOCATION_GLYPH_REGEX.containsMatchIn(stripped)) continue
            // Убран спам в логах: ранее было LOGGER.info каждый раз при смене кэша
            return normalizeLocation(stripped)
        }

        // Фоллбэк: заголовок scoreboard
        val title = ScoreboardDetector.getScoreboardTitle()
        if (title.isNotBlank() && !title.equals("www.hypixel.net", ignoreCase = true)) {
            return cleanLocationName(title)
        }

        return null
    }

    // Предкомпилированные регулярки: normalizeLocation/cleanLocationName вызываются из
    // рендер-хуков (EndHighlight/GlowingMushroom) КАЖДЫЙ кадр — компилить Regex на каждый
    // вызов нельзя (RULES: regex не в цикле/горячем пути).
    private val COLOR_CODE_REGEX = Regex("(?i)§[0-9a-fk-orlnmxz]")
    private val LOCATION_PREFIX_REGEX = Regex("(?i)^(локация|area|зона|location)\\s*[:：]?\\s*")
    private val LOCATION_GLYPH_REGEX = Regex("^[⏣ф]+\\s*")

    /**
        * Очистить строку локации ДЛЯ ОТОБРАЖЕНИЯ (скорборд, Discord RPC).
        *
        * Снимаем цветовые коды и глиф локации `⏣`. Букву «ф» здесь НЕ трогаем: на части
        * серверов глиф локации декодируется именно в неё, и глобальный `replace("ф", "")`
        * съедал буквы в обычных словах — «Февраль 8» превращалось в «евраль 8» в Discord.
        * Снятие глифа «ф» осталось только в normalizeLocation (сопоставление с реестром).
        */
    private fun cleanLocationName(raw: String): String {
        return raw
            .replace(COLOR_CODE_REGEX, "")
            .replace("~", "")
            .replace("⏣", "")
            .trim()
    }

    /**
     * Нормализация для сопоставления: трим, нижний регистр, ё→е.
     */
    private fun normalizeForMatch(text: String): String {
        return text.trim().lowercase().replace("ё", "е")
    }

    /**
     * Убрать префиксы «Локация:»/«Area:»/«Зона:» и символы локации из строки.
     */
    private fun normalizeLocation(raw: String): String {
        return cleanLocationName(raw)
            .replace(LOCATION_PREFIX_REGEX, "")
            .replace(LOCATION_GLYPH_REGEX, "")
            .trim()
    }

    /**
        * Сбросить кэш (вызывать при смене мира/сервера).
        */
    fun resetCache() {
        cachedLocation = null
        lastPollTime = 0
        cachedKnownLocation = null
        cachedKnownLocationSource = null
    }
}
