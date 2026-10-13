package set.starlev.features.skyblock.scoreboard.elements

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.features.skyblock.HudScoreboard.ScoreboardLine
import set.starlev.features.skyblock.scoreboard.ScoreboardElement
import set.starlev.secret.features.SecretFunFeatures
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.ItemLoreDetector

/**
 * Строка «Сила» для кастомного табло — выбранная сила и магическая сила из «Мешка для аксессуаров».
 *
 * Три источника по убыванию дешевизны: сайдбар, открытый контейнер, инвентарь игрока.
 * Каждый в своём try/catch — раньше одно исключение глушило весь скан целиком.
 * Значение кэшируется в конфиг, строка живёт после закрытия меню и рестарта.
 *
 * Скан идёт по `ContainerDetector.registerOpen` и по тику, а не из рендера табло:
 * иначе значение зависело бы от того, включена ли строка и рисуется ли HUD.
 */
object ScoreboardElementPower : ScoreboardElement() {

    /** §/& коды + неразрывные пробелы: без нормализации `\s` не берёт U+00A0. */
    private val stripRegex = Regex("(?i)[§&][0-9a-fk-orlnmxz]")
    private val spacesRegex = Regex("[\\u00A0\\u2000-\\u200A\\u202F\\u205F\\u3000\\t]")

    // «Магическая сила: 556». Между словами любой разделитель, а между двоеточием и числом —
    // любой не-цифровой мусор до 16 символов: «: 556», «(556)», «§6 556».
    private val mpRegexRu = Regex("(?iu)магическ[а-яё]*[\\s\\-–—]*сил[а-яё]*[^\\d]{0,16}?(\\d[\\d\\s,.'’]*)")
    private val mpRegexEn = Regex("(?iu)(?:magical|accessory)[\\s\\-–—]*power[^\\d]{0,24}?(\\d[\\d\\s,.'’]*)")
    private val powerRegex =
        Regex("(?iu)(?:выбранн?[а-яё]*|выбрана|избранн?[а-яё]*|активн?[а-яё]*|текущ[а-яё]*|selected|active|current)\\s*сил[а-яё]*\\s*[:：]?\\s*(.+)")

    /** Пока контейнер открыт — раз в секунду; иначе раз в 5, если значение уже есть. */
    private const val SCAN_THROTTLE_MS = 1000L
    private const val SCAN_THROTTLE_IDLE_MS = 5000L

    private var lastScanMs = 0L
    private var initialised = false

    @Volatile
    private var cachedName: String? = null

    @Volatile
    private var cachedMp: Int? = null

    /** Где нашли в последний раз — только для состояния элемента. */
    @Volatile
    private var lastSource = "—"

    /**
     * Мешок для аксессуаров — по СОЧЕТАНИЮ слов в названии, а не по точной строке: на Heltix
     * (и тем более на RP-серверах) его зовут и «Сумка украшений», и «Кошель фанатика».
     * Это только быстрый путь — если имя не подошло, регекс «Магическая сила» всё равно
     * проверяется по лоре ВСЕХ предметов.
     */
    private val BAG_WORDS = listOf("мешок", "мешоч", "сумк", "кошел", "рюкзак", "клатч", "bag", "pouch", "satchel")
    private val ACCESSORY_WORDS = listOf(
        "аксессуар", "аксес", "украшен", "украшени", "самоцвет", "accessor", "ornament", "trinket"
    )

    init {
        loadCacheFromConfig()
    }

    /** Вызывается из [set.starlev.StarredHeltix] после `ContainerDetector.init()`. */
    fun init() {
        if (initialised) return
        initialised = true
        // Скан сразу при открытии контейнера — не ждём, пока сервер досинхронизирует слоты.
        set.starlev.utils.detectors.ContainerDetector.registerOpen {
            lastScanMs = 0L
            maybeRescan()
        }
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register {
            maybeRescan()
        }
    }

    private fun loadCacheFromConfig() {
        try {
            val cfg = StarredHeltix.feature.skyblock.scoreboard.infoElements.power
            if (cfg.cachedPowerName.isNotBlank()) cachedName = cfg.cachedPowerName
            if (cfg.cachedPowerMp >= 0) cachedMp = cfg.cachedPowerMp
        } catch (_: Exception) {
        }
    }

    override fun getDisplay(): List<ScoreboardLine> {
        maybeRescan()
        val name = cachedName
        val mp = cachedMp
        val color = ColorUtils.parseColor(
            StarredHeltix.feature.skyblock.scoreboard.infoElements.power.valueColorV2,
            0xFF55FFFF.toInt()
        )
        // Строка показывается всегда: без данных — заготовка «—», чтобы место не «прыгало».
        val value = when {
            !name.isNullOrBlank() && mp != null -> Component.literal("$name ($mp МС)").withColor(color)
            !name.isNullOrBlank() -> Component.literal(name).withColor(color)
            mp != null -> Component.literal("$mp МС").withColor(color)
            else -> Component.literal("—")
        }
        val processed = SecretFunFeatures.processComponent(
            Component.literal("§fСила: ").append(value),
            true
        )
        return listOf(ScoreboardLine("power", processed, centered = false))
    }

    override fun showWhen(): Boolean =
        StarredHeltix.feature.skyblock.scoreboard.infoElements.power.enabled

    override val configLine = "§fСила"

    private fun maybeRescan() {
        val now = System.currentTimeMillis()
        val screen = currentScreen()
        val throttle = if (cachedMp == null || screen != null) SCAN_THROTTLE_MS else SCAN_THROTTLE_IDLE_MS
        if (now - lastScanMs < throttle) return
        lastScanMs = now
        var items = 0
        lastSource = "—"

        if (runStage { scanSidebar() }) {
            lastSource = "сайдбар"
        } else {
            items += sidebarLineCount()
        }
        if (lastSource == "—" && screen != null) {
            val menuItems = screen.menu.slots.count { !it.item.isEmpty }
            if (runStage { scanMenu(screen) }) lastSource = "контейнер" else items += menuItems
        }
        val inventory = try {
            Minecraft.getInstance().player?.inventory?.getNonEquipmentItems()
        } catch (_: Throwable) {
            null
        }
        if (lastSource == "—" && inventory != null) {
            val invItems = inventory.count { !it.isEmpty }
            if (runStage { scanInventory(inventory) }) lastSource = "инвентарь" else items += invItems
        }

        // Ничего не нашли — раньше здесь раз в 30 с писался дамп «имя + лор» всех предметов
        // в лог. Он забивал latest.log на сотни строк, поэтому убран: состояние видно по
        // самому элементу скорборда (прочерк «—», если источник не найден).
    }

    private fun currentScreen(): AbstractContainerScreen<*>? = try {
        Minecraft.getInstance().gui.screen() as? AbstractContainerScreen<*>
    } catch (_: Throwable) {
        null
    }

    /** Источник не бросает наружу: ошибка уходит в лог и не съедает остальные стадии. */
    private inline fun runStage(block: () -> Boolean): Boolean {
        return try {
            block()
        } catch (t: Throwable) {
            StarredHeltix.LOGGER.warn("[POWER] Скан упал: ${t.javaClass.simpleName}: ${t.message}")
            false
        }
    }

    private fun sidebarLineCount(): Int {
        return try {
            val mc = Minecraft.getInstance()
            val scoreboard = mc.level?.scoreboard ?: return 0
            val objective = scoreboard.getDisplayObjective(net.minecraft.world.scores.DisplaySlot.SIDEBAR)
            if (objective == null) 0 else scoreboard.listPlayerScores(objective).size
        } catch (_: Throwable) {
            0
        }
    }

    /** Сайдбар: там же сервер иногда печатает реальное число МС. */
    private fun scanSidebar(): Boolean {
        val mc = Minecraft.getInstance()
        val scoreboard = mc.level?.scoreboard ?: return false
        val objective = scoreboard.getDisplayObjective(net.minecraft.world.scores.DisplaySlot.SIDEBAR) ?: return false
        var found = false
        for (score in scoreboard.listPlayerScores(objective)) {
            val owner = score.owner
            val team = scoreboard.getPlayersTeam(owner)
            val text = score.display?.string
                ?: team?.let { net.minecraft.world.scores.PlayerTeam.formatNameForTeam(it, Component.literal(owner)).string }
                ?: owner
            val clean = cleanText(text)
            if (clean.isEmpty()) continue
            if (parseLines(listOf(clean), loose = true)) found = true
        }
        return found
    }

    /** @return true если в открытом меню нашли хоть что-то (имя или МС). */
    private fun scanMenu(screen: AbstractContainerScreen<*>): Boolean {
        val slots = screen.menu.slots
        // 1. Целенаправленно: предмет-мешок по сочетанию слов в названии.
        // Свободный поиск (loose=true): внутри своего предмета ложных срабатываний нет.
        for (slot in slots) {
            val stack = slot.item
            if (stack.isEmpty) continue
            if (!isAccessoryBagName(cleanText(stack.hoverName.string).lowercase())) continue
            if (parseLines(loreLines(stack), loose = true)) return true
        }
        // 2. Фолбэк: вся лора всех слотов. Регекс «Магическая сила» не зависит от названия
        // предмета, поэтому сработает даже если сервер назвал мешок как-то непривычно.
        for (slot in slots) {
            val stack = slot.item
            if (stack.isEmpty) continue
            if (parseLines(loreLines(stack), loose = false)) return true
        }
        return false
    }

    /** Инвентарь игрока: сначала предмет-мешок по имени, потом общий проход по лору. */
    private fun scanInventory(items: List<net.minecraft.world.item.ItemStack>): Boolean {
        for (stack in items) {
            if (stack.isEmpty) continue
            if (!isAccessoryBagName(cleanText(stack.hoverName.string).lowercase())) continue
            if (parseLines(loreLines(stack), loose = true)) return true
        }
        for (stack in items) {
            if (stack.isEmpty) continue
            if (parseLines(loreLines(stack), loose = false)) return true
        }
        return false
    }

    /** Hfont-декодер заголовков (на Heltix имена/лор могут быть PUA-глифами) — для обычного текста no-op. */
    private fun decode(text: String): String {
        return try {
            set.starlev.utils.detectors.ContainerDetector.decodeTitle(text)
        } catch (_: Exception) {
            text
        }
    }

    /** Снимает §/& коды, Hfont-глифы и неразрывные пробелы. */
    private fun cleanText(raw: String): String {
        val decoded = try { decode(raw) } catch (_: Throwable) { raw }
        return stripRegex.replace(spacesRegex.replace(decoded, " "), "").trim()
    }

    private fun isAccessoryBagName(cleanLowerName: String): Boolean {
        if (cleanLowerName.isEmpty()) return false
        return BAG_WORDS.any { cleanLowerName.contains(it) } &&
            ACCESSORY_WORDS.any { cleanLowerName.contains(it) }
    }

    /**
     * Лор предмета. Сначала сырой `LORE`-компонент — он не зависит от контекста тултипа
     * (не нужен игрок/мир) и не кэшируется по хешу стека; `getTooltipLines` оставлен как
     * дополнение, если компонента нет.
     */
    private fun loreLines(stack: net.minecraft.world.item.ItemStack): List<String> {
        val texts = ArrayList<String>(10)
        try {
            texts.add(cleanText(stack.hoverName.string))
        } catch (_: Exception) {
        }
        val raw = rawNbtLore(stack)
        for (line in raw) texts.add(cleanText(line))
        if (raw.isEmpty()) {
            try {
                for (line in ItemLoreDetector.getLore(stack)) texts.add(cleanText(line))
            } catch (_: Exception) {
            }
        }
        return texts.filter { it.isNotEmpty() }
    }

    /** Прямое чтение lore из NBT (без тултипа): переживает отсутствие игрока/мира. */
    private fun rawNbtLore(stack: net.minecraft.world.item.ItemStack): List<String> {
        val out = ArrayList<String>()
        try {
            val lore = stack.get(net.minecraft.core.component.DataComponents.LORE) ?: return out
            for (line in lore.lines) {
                try {
                    out.add(line.string)
                } catch (_: Exception) {
                }
            }
            // styledLines — на случай если lines пустые
            if (out.isEmpty()) {
                try {
                    for (line in lore.styledLines) {
                        try {
                            out.add(line.string)
                        } catch (_: Exception) {
                        }
                    }
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
        return out
    }

    /**
     * Разбирает строки лоры одного предмета.
     * @param loose свободный поиск для целевой головы (там нет чужого лора):
     * включает запасные эвристики, когда строгие паттерны не зацепились.
     * @return true если нашли хоть что-то новое (имя или МС).
     */
    private fun parseLines(texts: List<String>, loose: Boolean = false): Boolean {
        var found = false
        val cleaned = texts.map { cleanText(it) }.filter { it.isNotEmpty() }
        for ((idx, clean) in cleaned.withIndex()) {
            val isMpLine = clean.contains("магическ", ignoreCase = true) ||
                clean.contains("magical", ignoreCase = true) ||
                clean.contains("accessory", ignoreCase = true)
            val mpMatch = mpRegexRu.find(clean) ?: mpRegexEn.find(clean)
            if (mpMatch != null) {
                (extractNumber(mpMatch.groupValues[1]) ?: extractNumber(cleaned.getOrNull(idx + 1)))?.let {
                    if (cachedMp != it) {
                        cachedMp = it
                        found = true
                    } else {
                        found = found || cachedMp != null
                    }
                }
            } else if (loose && isMpLine) {
                // Запасной вариант: «Магическая сила» + первое число в строке/следующей
                (firstNumber(clean) ?: firstNumber(cleaned.getOrNull(idx + 1)))?.let {
                    if (cachedMp != it) {
                        cachedMp = it
                        found = true
                    } else {
                        found = found || cachedMp != null
                    }
                }
            }
            // Строка МС («магическая сила») не должна разбираться как выбранная сила
            if (isMpLine) continue
            val powerMatch = powerRegex.find(clean)
            if (powerMatch != null) {
                var name = powerMatch.groupValues[1].trim().trimEnd('.', '!', ' ', ':', '：')
                // Значение на следующей строке («Выбранная сила:» + «Зрячая»)
                if (name.isEmpty() || name.matches(Regex("[\\d\\s,.]+"))) {
                    val next = cleaned.getOrNull(idx + 1)?.trim()?.trimEnd('.', '!', ' ')
                    if (!next.isNullOrBlank() && !next.matches(Regex("[\\d\\s,.]+"))) name = next
                }
                if (name.isNotEmpty() && !name.matches(Regex("[\\d\\s,.]+"))) {
                    if (cachedName != name) {
                        cachedName = name
                        found = true
                    } else {
                        found = true
                    }
                }
            } else if (loose && (clean.contains("выбран", ignoreCase = true) ||
                        clean.contains("выбрана", ignoreCase = true) ||
                        clean.contains("selected", ignoreCase = true) ||
                        clean.contains("активна", ignoreCase = true))
            ) {
                // Запасной вариант: хвост после разделителя
                val tail = clean.split(Regex("[:：]"), limit = 2).getOrNull(1)
                    ?.trim()?.trimEnd('.', '!', ' ')
                if (!tail.isNullOrBlank() && !tail.matches(Regex("[\\d\\s,.]+"))) {
                    if (cachedName != tail) {
                        cachedName = tail
                        found = true
                    } else {
                        found = true
                    }
                }
            }
        }
        if (found) saveCache()
        return found
    }

    /** Первое число в строке (для loose-режима). */
    private fun firstNumber(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val m = Regex("(\\d[\\d\\s,.]*)").find(raw) ?: return null
        return extractNumber(m.groupValues[1])
    }

    private fun extractNumber(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        // Тысячные разделители (пробел/запятая/точка) выкидываем — МС всегда целое
        val digits = raw.replace(Regex("[\\s,.]"), "")
        if (digits.isEmpty() || !digits.all { it.isDigit() }) return null
        return digits.toIntOrNull()
    }

    private fun saveCache() {
        try {
            val cfg = StarredHeltix.feature.skyblock.scoreboard.infoElements.power
            val name = cachedName ?: ""
            val mp = cachedMp ?: -1
            if (cfg.cachedPowerName == name && cfg.cachedPowerMp == mp) return
            cfg.cachedPowerName = name
            cfg.cachedPowerMp = mp
            StarredHeltix.configManager.saveConfig("power-cache")
        } catch (_: Exception) {
        }
    }
}
