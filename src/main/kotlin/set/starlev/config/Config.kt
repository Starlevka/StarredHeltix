package set.starlev.config

import com.google.gson.GsonBuilder
import io.github.notenoughupdates.moulconfig.observer.PropertyTypeAdapterFactory
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import io.github.notenoughupdates.moulconfig.processor.ProcessedCategory
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import set.starlev.StarredHeltix
import set.starlev.config.categories.OptimizationConfig
import set.starlev.features.skyblock.scoreboard.ScoreboardConfigElement
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64
import kotlin.concurrent.fixedRateTimer

object ConfigManager {
    val gson = GsonBuilder().setPrettyPrinting()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeSpecialFloatingPointValues()
        .registerTypeAdapterFactory(PropertyTypeAdapterFactory())
        .enableComplexMapKeySerialization()
        .create()

    private const val ENC_PREFIX = "ENC:"
    private const val ENC_KEY = "StarredHeltixSalt2026_Starlevka"

    fun encodePassword(raw: String): String {
        if (raw.isEmpty()) return ""
        val bytes = raw.toByteArray(Charsets.UTF_8)
        val key = ENC_KEY.toByteArray(Charsets.UTF_8)
        for (i in bytes.indices) bytes[i] = (bytes[i].toInt() xor key[i % key.size].toInt()).toByte()
        return ENC_PREFIX + Base64.getEncoder().encodeToString(bytes)
    }

    fun decodePassword(enc: String): String {
        if (!enc.startsWith(ENC_PREFIX)) return enc
        return try {
            val b64 = enc.removePrefix(ENC_PREFIX)
            val bytes = Base64.getDecoder().decode(b64)
            val key = ENC_KEY.toByteArray(Charsets.UTF_8)
            for (i in bytes.indices) bytes[i] = (bytes[i].toInt() xor key[i % key.size].toInt()).toByte()
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) { "" }
    }

    lateinit var features: Features

    fun getFeaturesSafe(): Features {
        return if (this::features.isInitialized) {
            features
        } else {
            Features()
        }
    }

    private var configDirectory = File("config/starredheltix")
    private var configFile: File? = null
    lateinit var processor: MoulConfigProcessor<Features>
    private var autoSaveTimer: java.util.Timer? = null

    fun cancelAutoSave() {
        try { autoSaveTimer?.cancel() } catch (_: Exception) {}
        autoSaveTimer = null
    }

    fun firstLoad() {
        configDirectory.mkdirs()
        configFile = File(configDirectory, "config.json")

        if (configFile!!.exists()) {
            try {
                val inputStreamReader = InputStreamReader(FileInputStream(configFile!!), StandardCharsets.UTF_8)
                val bufferedReader = BufferedReader(inputStreamReader)
                features = gson.fromJson(bufferedReader.readText(), Features::class.java)
                // Миграция пароля: ENC:... -> plain в памяти, plain -> останется plain до первого save (зашифруется)
                val pwd = features.skyblock.autoLogin.password
                if (pwd.startsWith(ENC_PREFIX)) {
                    features.skyblock.autoLogin.password = decodePassword(pwd)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!this::features.isInitialized) {
            features = Features()
            saveConfig("blank config")
        }

        if (!features.skyblock.migratedInventoryHistoryToMisc) {
            val old = features.visuals.inventoryHistory
            val target = features.skyblock.inventoryHistory
            target.enabled = old.enabled
            target.duration = old.duration
            target.maxEntries = old.maxEntries
            target.showBackground = old.showBackground
            target.ignoreEquipped = old.ignoreEquipped
            features.skyblock.migratedInventoryHistoryToMisc = true
            saveConfig("migrate-inventory-history-to-skyblock")
        }

        // Миграция раскрытия «Общее» в корень Скайблока: переносим значения
        // из legacy-контейнера general.* в плоские поля (только если плоские ещё дефолтные,
        // чтобы не затереть уже настроенное; массивы/объекты копируем целиком).
        if (!features.skyblock.migratedGeneralFlatten) {
            val sb = features.skyblock
            val legacy = sb.general
            // autoSprint: переносим только включение (false->false ничего не меняет)
            if (legacy.autoSprint) sb.autoSprint = true
            // inventoryHistory / inventoryButtons: переносим если legacy отличен от дефолта.
            // Эвристика: legacy считаем «настроенным», если включён или плоский ещё дефолтный.
            if (legacy.inventoryHistory.enabled || !sb.inventoryHistory.enabled) {
                sb.inventoryHistory.enabled = legacy.inventoryHistory.enabled
                sb.inventoryHistory.duration = legacy.inventoryHistory.duration
                sb.inventoryHistory.maxEntries = legacy.inventoryHistory.maxEntries
                sb.inventoryHistory.showBackground = legacy.inventoryHistory.showBackground
                sb.inventoryHistory.ignoreEquipped = legacy.inventoryHistory.ignoreEquipped
            }
            if (legacy.inventoryButtons.enabled || !sb.inventoryButtons.enabled) {
                sb.inventoryButtons.enabled = legacy.inventoryButtons.enabled
                sb.inventoryButtons.onlyInventory = legacy.inventoryButtons.onlyInventory
                sb.inventoryButtons.tabBackgroundColorV2 = legacy.inventoryButtons.tabBackgroundColorV2
                sb.inventoryButtons.tabHoverBackgroundColorV2 = legacy.inventoryButtons.tabHoverBackgroundColorV2
                sb.inventoryButtons.tabBorderColorV2 = legacy.inventoryButtons.tabBorderColorV2
                sb.inventoryButtons.tabHoverBorderColorV2 = legacy.inventoryButtons.tabHoverBorderColorV2
            }
            sb.migratedGeneralFlatten = true
            saveConfig("migrate-general-flatten")
        }

        // Пост-чистка скорборда: после удаления элементов enum (например PURSE) Gson
        // превращает неизвестные имена в null — null-элементы крашат рендер скорборда
        // и редактор перетаскивания в конфиг-гуе.
        run {
            val entries = features.skyblock.scoreboard.scoreboardEntries
            val cleaned = entries.filterNotNull()
            if (cleaned.size != entries.size) {
                entries.clear()
                entries.addAll(cleaned)
                saveConfig("sanitize-scoreboard-entries")
            }
        }

        // Миграция: строка «Текущая сила» (POWER) добавлена позже остальных —
        // у существующих конфигов её нет в сохранённом порядке, и элемент
        // вообще не появляется в списке. Вставляем после BPS один раз;
        // если пользователь потом удалит строку сам — повторно не добавляем.
        if (!features.skyblock.scoreboard.migratedPowerLine) {
            val entries = features.skyblock.scoreboard.scoreboardEntries
            if (entries.none { it == ScoreboardConfigElement.POWER }) {
                val bpsIdx = entries.indexOfFirst { it == ScoreboardConfigElement.BPS }
                if (bpsIdx >= 0) entries.add(bpsIdx + 1, ScoreboardConfigElement.POWER)
                else entries.add(ScoreboardConfigElement.POWER)
            }
            features.skyblock.scoreboard.migratedPowerLine = true
            saveConfig("migrate-power-scoreboard-line")
        }

        // Буст приоритета раньше был двумя взаимоисключающими тумблерами (Windows/Linux),
        // теперь это один флаг, а платформа определяется по системе. Переносим значение.
        val perf = features.optimization.performance
        if (!perf.migratedSinglePriorityToggle) {
            if (perf.windows || perf.linux) {
                perf.threadPriority = true
            }
            perf.migratedSinglePriorityToggle = true
            saveConfig("migrate-single-priority-toggle")
        }

        // Порядок каналов в цветах: старые значения были в порядке `Chroma:A:R:G:B`,
        // а пикер MoulConfig читает ChromaColour как B:G:R:A:Chroma. С 4-частной строкой
        // (например дефолт цвета неба) он падает с ArrayIndexOutOfBounds, с 5-частной —
        // показывает переставленные R и B. Переводим все @ConfigEditorColour-поля разом.
        if (!features.migratedColourOrder) {
            val converted = convertColourFieldsToMoulConfigOrder(features)
            features.migratedColourOrder = true
            saveConfig("migrate-colour-order")
        }

        // Миграция старой системы анимации удара в редактор
        if (!features.visuals.animations.swingAnimation.migratedToEditor) {
            features.visuals.animations.swingAnimation.applyLegacyMigration()
            saveConfig("migrate-swing-to-editor")
        }
        features.visuals.animations.swingAnimation.repairLoadedValues()

        processor = MoulConfigProcessor(StarredHeltix.feature)
        BuiltinMoulConfigGuis.addProcessors(processor)
        val driver = ConfigProcessorDriver(processor)
        driver.warnForPrivateFields = false
        driver.processConfig(StarredHeltix.feature)
        hideCategories("inputMath")
        addSubcategoryHints()
        addColourResetButtons()

        autoSaveTimer = fixedRateTimer(name = "starredheltix-config-auto-save", daemon = true, period = 600_000L, initialDelay = 600_000L) {
            try {
                saveConfig("auto-save-600s")
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

/**
 * Рекурсивно переводит строковые поля с @ConfigEditorColour в порядок `B:G:R:A:Chroma`.
 * Возвращает количество переведённых полей.
 */
private fun convertColourFieldsToMoulConfigOrder(root: Any): Int {
    var converted = 0
    val visited = HashSet<Any>()
    val queue = ArrayDeque<Any>()
    queue.add(root)
    while (queue.isNotEmpty()) {
        val target = queue.removeFirst()
        if (!visited.add(target)) continue
        for (field in target.javaClass.declaredFields) {
            if (field.isSynthetic || java.lang.reflect.Modifier.isStatic(field.modifiers)) continue
            val value = try {
                field.isAccessible = true
                field.get(target)
            } catch (_: Throwable) {
                continue
            }
            when {
                value is String && field.getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour::class.java) != null -> {
                    val fixed = set.starlev.utils.ColorUtils.toMoulConfigOrder(value)
                    if (fixed != null && fixed != value) {
                        try {
                            field.set(target, fixed)
                            converted++
                        } catch (_: Throwable) {
                        }
                    }
                }
                value != null && !value.javaClass.isPrimitive && !value.javaClass.isEnum &&
                    !value.javaClass.name.startsWith("java.") && !value.javaClass.name.startsWith("kotlin.") -> queue.add(value)
            }
        }
    }
    return converted
}

/**
 * ВРЕМЕННО: спрятать категорию из меню настроек.
 *
 * Само поле конфига остаётся на месте — Gson его читает и пишет как обычно, код фичи
 * продолжает работать, сохранённые настройки не теряются. Прячется только запись в
 * `MoulConfigEditor`. Когда фича починится — просто удали вызов `hideCategories`.
 */
private fun hideCategories(vararg fieldNames: String) {
    try {
        val map = processor.allCategories as MutableMap<String, io.github.notenoughupdates.moulconfig.processor.ProcessedCategory>
        val hidden = map.keys.filter { id -> fieldNames.any { id.contains(it, ignoreCase = true) } }
        hidden.forEach { map.remove(it) }
    } catch (t: Throwable) {
        StarredHeltix.LOGGER.warn("[Config] Не удалось скрыть категорию: ${t.message}")
    }
}

    /**
     * Подсказка в правой панели для категорий, у которых есть только подкатегории.
     *
     * Без неё MoulConfig рисует свой английский текст («Seems like your search is found
     * in a subcategory…»), т.к. список опций пустой. Своя опция-инфотекст выглядит
     * аккуратнее и говорит по-русски, что делать дальше.
     */
    private fun addSubcategoryHints() {
        try {
            val config = StarredHeltix.feature
            val all = processor.allCategories
            val parents = HashSet<String>()
            all.values.forEach { category ->
                val parentId = category.parentCategoryId ?: return@forEach
                parents.add(parentId)
            }
            all.values.forEach { category ->
                if (category.identifier !in parents) return@forEach
                // ProcessedCategory.options — ArrayList, но Kotlin видит List<out ProcessedOption>
                @Suppress("UNCHECKED_CAST")
                val options = category.options as MutableList<ProcessedOption>
                if (options.isNotEmpty()) return@forEach
                val parent = category.parentCategoryId?.let { all[it] }
                options.add(SubcategoryHintOption(category, parent, config))
            }
        } catch (t: Throwable) {
            StarredHeltix.LOGGER.warn("[Config] Не удалось добавить подсказки подкатегорий: ${t.message}")
        }
    }

    /** Неизменяемая опция-заголовок «вы находитесь здесь» (вставка в ProcessedCategory.options). */
    private class SubcategoryHintOption(
        private val owner: ProcessedCategory,
        private val parent: ProcessedCategory?,
        private val config: Features
    ) : ProcessedOption {
        private val title = owner.displayName

        private val subtitle = io.github.notenoughupdates.moulconfig.common.text.StructuredText.of(
            buildString {
                parent?.displayName?.text?.let { append(it).append("  →  ") }
                append(owner.displayName.text)
            }
        ).grey()

        private val footnote = io.github.notenoughupdates.moulconfig.common.text.StructuredText
            .of("Все настройки этой категории — в подкатегориях в списке слева.")
            .grey()

        private val editor by lazy {
            set.starlev.config.gui.SubcategoryHintEditor(this, title, subtitle, footnote)
        }

        override fun getSearchTags(): Array<io.github.notenoughupdates.moulconfig.annotations.SearchTag> = emptyArray()
        override fun getAccordionId(): Int = -1
        override fun getCategory(): ProcessedCategory = owner
        override fun getConfig(): io.github.notenoughupdates.moulconfig.Config = config
        override fun getName(): io.github.notenoughupdates.moulconfig.common.text.StructuredText =
            io.github.notenoughupdates.moulconfig.common.text.StructuredText.of("")

        override fun getDescription(): io.github.notenoughupdates.moulconfig.common.text.StructuredText =
            io.github.notenoughupdates.moulconfig.common.text.StructuredText.of("")

        override fun getPath(): String = "${owner.identifier}.subcategoryHint"
        override fun getType(): Class<*> = String::class.java
        override fun getEditor(): io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor = editor
        override fun get(): Any? = null
        override fun set(value: Any?): Boolean = false
        override fun explicitNotifyChange() {}
        override fun getDebugDeclarationLocation(): String = "subcategory-hint:${owner.identifier}"
    }

    /**
     * Кнопка «Сброс» **справа от цвета**, в одной строке — вернуть дефолт.
     *
     * Раньше это была отдельная синтетическая опция ПОД цветом (целая лишняя строка).
     * Теперь сама опция заменяется на обёртку с кастомным редактором
     * (`config/gui/ColourWithResetEditor.kt`), который рисует образец цвета и кнопку рядом.
     * Заменить редактор у `ProcessedOptionImpl` нельзя — сеттера нет, поэтому оборачиваем.
     *
     * Дефолт берём из ЧИСТОГО `Features()` (у него в полях записаны инициализаторы),
     * а не из загруженного конфига — иначе «сброс» просто записал бы текущее значение.
     * Ключ дефолта — «класс#поле»: значение по умолчанию принадлежит полю класса,
     * а не экземпляру, поэтому одинаковые вложенные классы не путаются.
     */
    private fun addColourResetButtons() {
        try {
            val defaults = collectColourDefaults(Features())
            if (defaults.isEmpty()) return
            val seen = HashSet<ProcessedOption>()
            var added = 0
            processor.allCategories.values.forEach { category ->
                @Suppress("UNCHECKED_CAST")
                val options = category.options as MutableList<ProcessedOption>
                // Идём по копии: список меняется прямо здесь
                for (option in options.toList()) {
                    if (!seen.add(option)) continue
                    val field = (option as? ProcessedOption.HasField)?.field ?: continue
                    if (field.getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour::class.java) == null) {
                        continue
                    }
                    val default = defaults["${field.declaringClass.name}#${field.name}"] ?: continue
                    val index = options.indexOf(option)
                    if (index < 0) continue
                    options[index] = ColourResetOption(option, default, category, this)
                    added++
                }
            }
        } catch (t: Throwable) {
            StarredHeltix.LOGGER.warn("[Config] Не удалось добавить сброс цвета: ${t.message}")
        }
    }

    /** Рекурсивный обход: все строковые `@ConfigEditorColour` поля → «класс#поле» → дефолт. */
    private fun collectColourDefaults(root: Any): Map<String, String> {
        val out = HashMap<String, String>()
        val visited = HashSet<Any>()
        val queue = ArrayDeque<Any>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val target = queue.removeFirst()
            if (!visited.add(target)) continue
            for (field in target.javaClass.declaredFields) {
                if (field.isSynthetic || java.lang.reflect.Modifier.isStatic(field.modifiers)) continue
                val value = try {
                    field.isAccessible = true
                    field.get(target)
                } catch (_: Throwable) {
                    continue
                }
                when {
                    value is String &&
                        field.getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour::class.java) != null -> {
                        out.putIfAbsent("${field.declaringClass.name}#${field.name}", value)
                    }
                    value != null && !value.javaClass.isPrimitive && !value.javaClass.isEnum &&
                        !value.javaClass.name.startsWith("java.") && !value.javaClass.name.startsWith("kotlin.") -> queue.add(value)
                }
            }
        }
        return out
    }

    /**
     * Обёртка цветовой опции: всё поведение — исходной опции, но редактор свой
     * (образец цвета + кнопка «Сброс» справа). Сам список опций категории не меняется —
     * цвет занимает ровно одну строку, как ванильный.
     */
    private class ColourResetOption(
        private val target: ProcessedOption,
        private val defaultValue: String,
        private val owner: ProcessedCategory,
        private val manager: ConfigManager
    ) : ProcessedOption, ProcessedOption.HasField {

        private val editor by lazy {
            set.starlev.config.gui.ColourWithResetEditor(target, defaultValue) {
                target.set(defaultValue)
                target.explicitNotifyChange()
                manager.saveConfig("colour-reset")
            }
        }

        override fun getSearchTags(): Array<io.github.notenoughupdates.moulconfig.annotations.SearchTag> = target.searchTags
        override fun getAccordionId(): Int = target.accordionId
        override fun getCategory(): ProcessedCategory = owner
        override fun getConfig(): io.github.notenoughupdates.moulconfig.Config = target.config
        override fun getName(): io.github.notenoughupdates.moulconfig.common.text.StructuredText = target.name
        override fun getDescription(): io.github.notenoughupdates.moulconfig.common.text.StructuredText =
            target.description
        override fun getPath(): String = target.path
        override fun getType(): java.lang.reflect.Type = target.type
        override fun getEditor(): io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor = editor
        override fun get(): Any? = target.get()
        override fun set(value: Any?): Boolean = target.set(value)
        override fun explicitNotifyChange() = target.explicitNotifyChange()
        override fun getDebugDeclarationLocation(): String? = target.getDebugDeclarationLocation()

        // HasField — по нему GUI/MoulConfig узнаёт исходное поле (например, при поиске)
        override fun getField() = (target as ProcessedOption.HasField).field
    }

    fun saveConfig(reason: String) {
        val file = configFile ?: throw Error("Не удалось сохранить конфиг, configFile = null")
        try {
            file.parentFile.mkdirs()
            // Шифруем пароль только на диск, в памяти остаётся plain для UI и /вход
            val feature = StarredHeltix.feature
            val plain = feature.skyblock.autoLogin.password
            val wasEncoded = plain.startsWith(ENC_PREFIX)
            if (plain.isNotEmpty() && !wasEncoded) {
                feature.skyblock.autoLogin.password = encodePassword(plain)
            }
            try {
                BufferedWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)).use { writer ->
                    writer.write(gson.toJson(feature))
                }
            } finally {
                // Возвращаем plain в память
                if (plain.isNotEmpty() && !wasEncoded) {
                    feature.skyblock.autoLogin.password = plain
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}