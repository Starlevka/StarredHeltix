package set.starlev.features.chat

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import set.starlev.StarredHeltix
import set.starlev.utils.CacheManager
import set.starlev.utils.TextUtils

/**
 * Правило пользовательского уведомления. Хранится в конфиге (`chat.notifiers.rules`),
 * поэтому переживает перезапуск без миграций.
 *
 * @param pattern что ищем в чате: обычная подстрока или `/регекс/` в косых чертах
 * @param title   крупный заголовок по центру экрана (поддерживает §-коды)
 * @param subtitle мелкая строка под заголовком
 * @param sound   id звука из [NotifierSounds]
 * @param stay    сколько тиков висит заголовок
 */
data class NotifierRule(
    var name: String = "",
    var pattern: String = "",
    var title: String = "",
    var subtitle: String = "",
    var sound: String = NotifierSounds.DEFAULT_ID,
    var stay: Int = 60,
    var enabled: Boolean = true,
)

/** Пресеты звука для уведомлений — короткий список вместо поля ввода айдишника. */
data class NotifierSoundOption(val id: String, val label: String, val event: SoundEvent?)

object NotifierSounds {
    const val DEFAULT_ID = "pling"

    val ALL: List<NotifierSoundOption> by lazy {
        listOf(
            NotifierSoundOption("none", "Без звука", null),
            NotifierSoundOption(DEFAULT_ID, "Пинг", SoundEvents.NOTE_BLOCK_PLING.value()),
            NotifierSoundOption("bell", "Колокольчик", SoundEvents.NOTE_BLOCK_BELL.value()),
            NotifierSoundOption("bass", "Бас", SoundEvents.NOTE_BLOCK_BASS.value()),
            NotifierSoundOption("chime", "Христалл", SoundEvents.NOTE_BLOCK_CHIME.value()),
            NotifierSoundOption("xp", "Опыт", SoundEvents.EXPERIENCE_ORB_PICKUP),
            NotifierSoundOption("levelup", "Новый уровень", SoundEvents.PLAYER_LEVELUP),
            NotifierSoundOption("anvil", "Наковальня", SoundEvents.ANVIL_LAND),
            NotifierSoundOption("wither", "Вихрь", SoundEvents.WITHER_SPAWN),
            NotifierSoundOption("dragon", "Дракон", SoundEvents.ENDER_DRAGON_GROWL),
            NotifierSoundOption("portal", "Портал", SoundEvents.PORTAL_TRIGGER),
            NotifierSoundOption("villager", "Крестьянин", SoundEvents.VILLAGER_YES),
            NotifierSoundOption("blaze", "Блейз", SoundEvents.BLAZE_BURN),
            NotifierSoundOption("beacon", "Маяк", SoundEvents.BEACON_ACTIVATE),
        )
    }

    private val byId: Map<String, NotifierSoundOption> by lazy { ALL.associateBy { it.id } }

    fun byId(id: String): NotifierSoundOption = byId[id] ?: ALL[1]

    /** Проиграть пресет (null/пусто = без звука). Ошибки глушим — это только UI. */
    fun play(id: String) {
        val option = byId[id] ?: return
        val event = option.event ?: return
        try {
            Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(event, 1.0f))
        } catch (t: Throwable) {
            StarredHeltix.LOGGER.warn("[Notifiers] Звук '${option.id}' не сыграл: ${t.message}")
        }
    }
}

/**
 * Пользовательские уведомления: игрок сам пишет правила «если в чате встретилось X —
 * показать заголовок и проиграть звук».
 *
 * Заменяет разрозненные оповещения фич: вместо отдельного HUD-элемента на каждый случай
 * (как удалённый «Легендарный улов») есть один список правил и одно меню `/sh notifiers`.
 *
 * Показ — ВАНИЛЬНЫЕ титры по центру экрана (`Hud.setTitle/setSubtitle/setTimes`), то есть
 * видно всегда, даже когда HUD скрыт. Правило можно перебить другим: титры просто заменятся.
 *
 * Совпадение: подстрока без учёта регистра, а если шаблон в косых чертах (`/…/`) — регекс
 * (компилируется через `CacheManager`, битый регекс просто не срабатывает). Сравниваем по
 * тексту БЕЗ §-кодов — иначе правило не найдёт серверную строку.
 */
object NotifierCenter {
    private val mc = Minecraft.getInstance()

    /** Не чаще раза в секунду на правило — иначе серверный спам превратит титры в кашу. */
    private const val COOLDOWN_MS = 1000L

    /** Кэш «последний сработавший индекс правила» — переживает удаление правила. */
    private val lastFired = HashMap<Int, Long>()

    private val rules = mutableListOf<NotifierRule>()

    private val enabled: Boolean
        get() = try {
            StarredHeltix.feature.chat.notifiers.enabled
        } catch (_: Throwable) {
            false
        }

    fun init() {
        reload()
        ChatEventsManager.registerIncoming { message -> onChat(message) }
    }

    /** Перечитывает правила из конфига (после сброса конфига или ручной правки). */
    fun reload() {
        rules.clear()
        lastFired.clear()
        try {
            StarredHeltix.feature.chat.notifiers.rules.forEach { rule ->
                if (rule.pattern.isBlank()) return@forEach
                rules.add(rule)
            }
        } catch (t: Throwable) {
            StarredHeltix.LOGGER.warn("[Notifiers] Не удалось прочитать правила: ${t.message}")
        }
    }

    fun snapshot(): List<NotifierRule> = rules

    // ─────────────────────────── правка правил (из меню) ───────────────────────────

    fun add(): Int {
        rules.add(NotifierRule(name = "Новое уведомление"))
        save("notifier-add")
        return rules.size - 1
    }

    fun remove(index: Int) {
        if (index !in rules.indices) return
        rules.removeAt(index)
        lastFired.keys.toList().filter { it >= index }.forEach { lastFired.remove(it) }
        save("notifier-remove")
    }

    fun toggle(index: Int): Boolean {
        val rule = rules.getOrNull(index) ?: return false
        rule.enabled = !rule.enabled
        save("notifier-toggle")
        return rule.enabled
    }

    /** Сохранить правку поля. Вызывается из меню при каждом изменении. */
    fun commit(reason: String = "notifier-edit") = save(reason)

    fun save(reason: String) {
        try {
            val cfg = StarredHeltix.feature.chat.notifiers
            cfg.rules.clear()
            cfg.rules.addAll(rules)
            StarredHeltix.configManager.saveConfig(reason)
        } catch (t: Throwable) {
            StarredHeltix.LOGGER.warn("[Notifiers] Не удалось сохранить: ${t.message}")
        }
    }

    /** Показать уведомление (используется меню для предпросмотра и теста). */
    fun fire(rule: NotifierRule) {
        val title = rule.title.ifBlank { rule.name }.trim()
        if (title.isBlank()) return
        try {
            mc.gui.hud.setTitle(Component.literal(title))
            val subtitle = rule.subtitle.trim()
            mc.gui.hud.setSubtitle(
                if (subtitle.isEmpty()) Component.empty() else Component.literal(subtitle)
            )
            mc.gui.hud.setTimes(FADE_IN, rule.stay.coerceIn(MIN_STAY, MAX_STAY), FADE_OUT)
            NotifierSounds.play(rule.sound)
        } catch (t: Throwable) {
            StarredHeltix.LOGGER.warn("[Notifiers] Не удалось показать титры: ${t.message}")
        }
    }

    // ─────────────────────────── внутреннее ───────────────────────────

    private fun onChat(message: String) {
        if (!enabled || rules.isEmpty()) return
        val plain = TextUtils.stripAll(message)
        if (plain.isEmpty()) return
        val now = System.currentTimeMillis()
        rules.forEachIndexed { index, rule ->
            if (!rule.enabled) return@forEachIndexed
            if (!matches(rule.pattern, plain)) return@forEachIndexed
            val last = lastFired[index] ?: 0L
            if (now - last < COOLDOWN_MS) return@forEachIndexed
            lastFired[index] = now
            fire(rule)
        }
    }

    private fun matches(pattern: String, plain: String): Boolean {
        val p = pattern.trim()
        if (p.isEmpty()) return false
        if (p.length > 2 && p.startsWith("/") && p.endsWith("/")) {
            val regex = try {
                CacheManager.getRegex(p.substring(1, p.length - 1))
            } catch (_: Throwable) {
                null
            } ?: return false
            return regex.containsMatchIn(plain)
        }
        return plain.contains(p, ignoreCase = true)
    }

    private const val FADE_IN = 5
    private const val FADE_OUT = 15
    private const val MIN_STAY = 10
    private const val MAX_STAY = 400
}