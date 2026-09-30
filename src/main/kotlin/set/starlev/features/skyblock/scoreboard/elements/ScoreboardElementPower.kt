package set.starlev.features.skyblock.scoreboard.elements

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Items
import set.starlev.StarredHeltix
import set.starlev.features.skyblock.HudScoreboard.ScoreboardLine
import set.starlev.features.skyblock.scoreboard.ScoreboardElement
import set.starlev.secret.features.SecretFunFeatures
import set.starlev.utils.ColorUtils
import set.starlev.utils.detectors.ItemLoreDetector

/**
 * Строка «Текущая сила» для кастомного табло.
 *
 * Целевой предмет — голова «Мешок для аксессуаров» (player_head): мод ищет её
 * в ЛЮБОМ открытом контейнере (не только в меню Мешков) и читает лору:
 * - «Магическая сила: N» (число)
 * - «Выбранная сила: <название>»
 * По ним строится строка вида «Текущая сила: Зрячая (544 МС)».
 * Значения кэшируются в конфиг — строка живёт после закрытия меню и рестарта.
 */
object ScoreboardElementPower : ScoreboardElement() {

    private val stripRegex = Regex("(?i)§[0-9a-fk-orlnmxz]")
    // Разделитель — обычное или полноширинное двоеточие, либо его отсутствие («Магическая сила 544»)
    //
    // ВАЖНО: инлайн-флаг (?iu), а НЕ RegexOption.IGNORE_CASE. Kotlin-овский IGNORE_CASE —
    // это Pattern.CASE_INSENSITIVE без UNICODE_CASE, а тот по умолчанию case-insensitive
    // только для ASCII. С «Магическая сила: 556» такие регексы не совпадут никогда.
    private val mpRegex = Regex("(?iu)магическ[а-я]*\\s*сил[а-я]*\\s*[:：]?\\s*([\\d\\s,.]+)")
    private val mpRegexEn = Regex("(?iu)magical\\s*power\\s*[:：]?\\s*([\\d\\s,.]+)")
    private val powerRegex =
        Regex("(?iu)(?:выбранная|выбраная|активная|текущая|selected|active|current)\\s*сил[а-я]*\\s*[:：]?\\s*(.+)")

    private const val SCAN_THROTTLE_MS = 1000L
    private var lastScanMs = 0L

    @Volatile
    private var cachedName: String? = null

    @Volatile
    private var cachedMp: Int? = null

    init {
        // Подхватываем персистентный кэш из конфига (SkyHanni/Skyblocker-стиль)
        try {
            val cfg = StarredHeltix.feature.skyblock.scoreboard.infoElements.power
            val name = cfg.cachedPowerName
            if (name.isNotBlank()) cachedName = name
            val mp = cfg.cachedPowerMp
            if (mp >= 0) cachedMp = mp
        } catch (_: Exception) {
        }
    }

    override fun getDisplay(): List<ScoreboardLine> {
        maybeRescan()
        val name = cachedName
        val mp = cachedMp
        if (name.isNullOrBlank() && mp == null) return emptyList()
        val color = ColorUtils.parseColor(
            StarredHeltix.feature.skyblock.scoreboard.infoElements.power.valueColorV2,
            0xFF55FFFF.toInt()
        )
        val valueText = when {
            !name.isNullOrBlank() && mp != null -> "$name ($mp МС)"
            !name.isNullOrBlank() -> name
            else -> "$mp МС"
        }
        val processed = SecretFunFeatures.processComponent(
            Component.literal("§7Текущая сила: ").append(Component.literal(valueText).withColor(color)),
            true
        )
        return listOf(ScoreboardLine("power", processed, centered = false))
    }

    override fun showWhen(): Boolean =
        StarredHeltix.feature.skyblock.scoreboard.infoElements.power.enabled

    override val configLine = "§bТекущая сила"

    private fun maybeRescan() {
        val now = System.currentTimeMillis()
        if (now - lastScanMs < SCAN_THROTTLE_MS) return
        lastScanMs = now
        try {
            val mc = Minecraft.getInstance()
            val screen = mc.gui.screen() as? AbstractContainerScreen<*> ?: return
            scanMenu(screen)
        } catch (_: Exception) {
        }
    }

    /** Hfont-декодер заголовков (на Heltix имена/лор могут быть PUA-глифами) — для обычного текста no-op. */
    private fun decode(text: String): String {
        return try {
            set.starlev.utils.detectors.ContainerDetector.decodeTitle(text)
        } catch (_: Exception) {
            text
        }
    }

    private fun scanMenu(screen: AbstractContainerScreen<*>) {
        // 1. Целенаправленно: предмет с названием «Мешок для аксессуаров» в любом открытом меню.
        // Требование Items.PLAYER_HEAD убрано намеренно: на Heltix мешок не обязательно голова,
        // и раньше предмет просто пропускался. Ориентируемся на имя — оно в лоре/имя всегда.
        // Свободный поиск (loose=true): внутри своего предмета ложных срабатываний нет.
        for (slot in screen.menu.slots) {
            val stack = slot.item
            if (stack.isEmpty) continue
            val itemName = stripRegex.replace(decode(stack.hoverName.string), "").trim().lowercase()
            if (!isAccessoryBagName(itemName)) continue
            val lore = loreLines(stack)
            if (parseLines(lore, loose = true)) return
        }
        // 2. Фолбэк: вся лора всех слотов строгими паттернами (без loose — чужой лор вокруг)
        for (slot in screen.menu.slots) {
            val stack = slot.item
            if (stack.isEmpty) continue
            if (parseLines(loreLines(stack), loose = false)) return
        }
    }

    /**
     * Диагностика: печатает в чат всё, что мод реально видит в открытом контейнере —
     * предмет, имя, количество строк лоры и что нашли регексы. Нужна, когда сила
     * «не находится»: сразу видно, на каком шаге цепочка рвётся (предмет / имя / лора / регекс).
     */
    fun debugDump() {
        val mc = Minecraft.getInstance()
        val screen = mc.gui.screen()
        val msg = { s: String -> mc.player?.sendSystemMessage(Component.literal(s)) }
        msg("§8§m                                        ")
        msg("§d§l[POWER-DEBUG] §fэкран: §7${screen?.javaClass?.simpleName ?: "нет"}")
        if (screen !is AbstractContainerScreen<*>) {
            msg("§c  Это не контейнер — открыть меню с мешком и повторить.")
            return
        }
        val slots = screen.menu.slots
        msg("§7  слотов в menu: §f${slots.size}")
        var foundHead = false
        for ((i, slot) in slots.withIndex()) {
            val stack = slot.item
            if (stack.isEmpty) continue
            val isHead = stack.item == Items.PLAYER_HEAD
            val name = stripRegex.replace(decode(stack.hoverName.string), "").trim()
            val lines = loreLines(stack)
            val bag = isAccessoryBagName(name.lowercase())
            if (isHead || bag) {
                foundHead = true
                msg("§7  слот §f$i §7предмет §f${BuiltInRegistries.ITEM.getKey(stack.item)}")
                msg("§7    имя: §f$name §8(мешок? $bag, голова? $isHead)")
                msg("§7    строк лоры: §f${lines.size}")
                lines.forEach { l -> msg("§8      · ${l.replace('§', '&')}") }
                val mpHit = lines.map { stripRegex.replace(it, "").trim() }
                    .firstOrNull { mpRegex.containsMatchIn(it) || mpRegexEn.containsMatchIn(it) }
                msg("§7    регекп МС: §f${mpHit ?: "НЕ НАЙДЕН"}")
            }
        }
        if (!foundHead) {
            msg("§c  Ни головы, ни «мешка» в слотах не найдено.")
            msg("§7  Выведи список всех непустых слотов: §f/sh powerdump2")
        }
        msg("§d§l[POWER-DEBUG] §7кэш: имя=§f${cachedName ?: "—"}§7 МС=§f${cachedMp ?: "—"}")
        msg("§8§m                                        ")
    }

    /** Полный дамп слотов — когда целевой предмет не опознан вообще. */
    fun debugDumpAll() {
        val mc = Minecraft.getInstance()
        val screen = mc.gui.screen() as? AbstractContainerScreen<*> ?: run {
            mc.player?.sendSystemMessage(Component.literal("§cОткрой контейнер."))
            return
        }
        val msg = { s: String -> mc.player?.sendSystemMessage(Component.literal(s)) }
        msg("§8§m                                        ")
        msg("§d§l[POWER-DEBUG] §fвсе непустые слоты:")
        for ((i, slot) in screen.menu.slots.withIndex()) {
            val stack = slot.item
            if (stack.isEmpty) continue
            val id = BuiltInRegistries.ITEM.getKey(stack.item).toString()
            val name = stripRegex.replace(decode(stack.hoverName.string), "").trim()
            val lore = loreLines(stack)
            msg("§7  #$i §f$id §8— §7$name §8(лор: ${lore.size})")
        }
        msg("§8§m                                        ")
    }

    /** Сброс кэша силы (в т.ч. в конфиге) — для диагностики и ручной переинициализации. */
    fun resetCache() {
        cachedName = null
        cachedMp = null
        lastScanMs = 0L
        try {
            val cfg = StarredHeltix.feature.skyblock.scoreboard.infoElements.power
            if (cfg.cachedPowerName.isNotEmpty() || cfg.cachedPowerMp >= 0) {
                cfg.cachedPowerName = ""
                cfg.cachedPowerMp = -1
                StarredHeltix.configManager.saveConfig("power-reset")
            }
        } catch (_: Exception) {
        }
        Minecraft.getInstance().player?.sendSystemMessage(
            Component.literal("§7[POWER] §fкэш силы сброшен.")
        )
    }

    private fun isAccessoryBagName(cleanLowerName: String): Boolean {
        if (cleanLowerName.isEmpty()) return false
        if (cleanLowerName.contains("accessory bag")) return true
        if (cleanLowerName.contains("accessory") && cleanLowerName.contains("bag")) return true
        if (cleanLowerName.contains("мешок") && cleanLowerName.contains("аксессуар")) return true
        // Hfont/сокращения: «мешочек», «аксес»
        if (cleanLowerName.contains("мешоч") && cleanLowerName.contains("аксес")) return true
        return false
    }

    private fun loreLines(stack: net.minecraft.world.item.ItemStack): List<String> {
        val texts = ArrayList<String>(8)
        try {
            texts.add(decode(stack.hoverName.string))
        } catch (_: Exception) {
        }
        try {
            for (line in ItemLoreDetector.getLore(stack)) texts.add(decode(line))
        } catch (_: Exception) {
        }
        // ItemLoreDetector иногда отдаёт пусто для голов без игрока рядом — добираем NBT-лор напрямую
        if (texts.size <= 1) {
            try {
                for (line in rawNbtLore(stack)) texts.add(decode(line))
            } catch (_: Exception) {
            }
        }
        return texts
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
        val cleaned = texts.map { stripRegex.replace(it, "").trim() }.filter { it.isNotEmpty() }
        for ((idx, clean) in cleaned.withIndex()) {
            val isMpLine = clean.contains("магическ", ignoreCase = true) || clean.contains("magical", ignoreCase = true)
            val mpMatch = mpRegex.find(clean) ?: mpRegexEn.find(clean)
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

    @Suppress("unused")
    private fun pathOf(stack: net.minecraft.world.item.ItemStack): String {
        return try {
            BuiltInRegistries.ITEM.getKey(stack.item)?.path?.lowercase() ?: ""
        } catch (_: Exception) {
            ""
        }
    }
}
