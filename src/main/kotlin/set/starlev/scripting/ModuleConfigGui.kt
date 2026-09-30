package set.starlev.scripting

import com.google.gson.Gson
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.gui.GuiComponent
import io.github.notenoughupdates.moulconfig.gui.editors.ComponentEditor
import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorBoolean
import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorButton
import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorInfoText
import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorText
import io.github.notenoughupdates.moulconfig.processor.ProcessedCategory
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import java.io.File

/**
 * MoulConfig-интерфейс для модулей: левая панель — модули, правая — их config.json.
 *
 * Категории и опции строятся ВРУЧНУЮ (без рефлексии): ProcessedOption реализуется
 * напрямую, редакторы — стандартные (Boolean/Text/Button/InfoText).
 *
 * Boolean-ключи -> свитч; строки/числа -> текстовое поле (числа парсятся обратно);
 * списки/объекты/null -> информационная строка (редактируются в файле).
 *
 * Если модуль загружен — изменения идут в живой JSConfig (модуль видит их сразу),
 * иначе — прямое чтение/запись config.json папки модуля.
 */
object ModuleConfigGui {

    private val gson = Gson()
    private val gsonPretty = com.google.gson.GsonBuilder().setPrettyPrinting().create()

    // ==================== Фасад Config ====================

    private class ModuleConfigFacade : Config() {
        override fun getTitle(): StructuredText = StructuredText.of("§a§lStarredHeltix §fМодули")
    }

    // ==================== Реализации MoulConfig ====================

    /** Опция с внешним геттером/сеттером и заранее созданным редактором. */
    private class SHOption(
        private val optionName: String,
        private val optionDesc: String,
        private val optionPath: String,
        private val category: SHCategory,
        private val facade: Config,
        private val type: Class<*>,
        private val getter: () -> Any?,
        private val setter: (Any?) -> Unit
    ) : ProcessedOption {

        var editorField: ComponentEditor? = null
            private set

        fun setEditor(e: ComponentEditor) {
            editorField = e
        }

        override fun getSearchTags(): Array<SearchTag> = arrayOf()
        override fun getAccordionId(): Int = -1
        override fun getCategory(): ProcessedCategory = category
        override fun getConfig(): Config = facade
        override fun getName(): StructuredText = StructuredText.of(optionName)
        override fun getDescription(): StructuredText = StructuredText.of(optionDesc)
        override fun getPath(): String = optionPath
        override fun getType(): Class<*> = type
        override fun getEditor(): io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor =
            editorField ?: error("Редактор не создан для опции '$optionName'")
        override fun get(): Any? = getter()
        override fun set(value: Any?): Boolean {
            setter(value)
            return true
        }
        override fun explicitNotifyChange() {}
        override fun getDebugDeclarationLocation(): String = "module-config:$optionPath"
    }

    private class SHCategory(
        val id: String,
        val title: String,
        val desc: String
    ) : ProcessedCategory {
        val options = ArrayList<ProcessedOption>()
        override fun getDisplayName(): StructuredText = StructuredText.of(title)
        override fun getDescription(): StructuredText = StructuredText.of(desc)
        override fun getIdentifier(): String = id
        override fun getParentCategoryId(): String? = null
        override fun getOptions(): List<ProcessedOption> = options
        override fun getAccordionAnchors(): Map<Int, ProcessedOption> = emptyMap()
        override fun getDebugDeclarationLocation(): String = "module-category:$id"
    }

    // ==================== Чтение/запись config.json ====================

    private fun readConfigFile(folder: File): LinkedHashMap<String, Any?> {
        val f = File(folder, "config.json")
        if (!f.exists()) return LinkedHashMap()
        return try {
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(f.readText(Charsets.UTF_8), LinkedHashMap::class.java) as? LinkedHashMap<String, Any?>
                ?: LinkedHashMap()
        } catch (e: Exception) {
            ScriptConsole.error("ModuleConfigGui", "Ошибка чтения config.json в ${folder.name}: ${e.message}")
            LinkedHashMap()
        }
    }

    private fun writeConfigFile(folder: File, data: Map<String, Any?>) {
        try {
            folder.mkdirs()
            File(folder, "config.json").writeText(gsonPretty.toJson(data), Charsets.UTF_8)
        } catch (e: Exception) {
            ScriptConsole.error("ModuleConfigGui", "Ошибка записи config.json в ${folder.name}: ${e.message}")
        }
    }

    private fun parseNumber(text: String?, original: Number): Number? {
        if (text.isNullOrBlank()) return null
        return when (original) {
            is Int -> text.trim().toDoubleOrNull()?.toInt()
            is Long -> text.trim().toDoubleOrNull()?.toLong()
            is Float -> text.trim().toFloatOrNull()
            else -> text.trim().toDoubleOrNull()
        }
    }

    // ==================== Построение категорий ====================

    /** Максимальная длина описания категории/опции: длинные тексты ломают высоту панели MoulConfig. */
    private const val MAX_DESC_LENGTH = 160

    private fun clampDesc(text: String): String {
        val single = text.replace("\n", " ").trim()
        return if (single.length <= MAX_DESC_LENGTH) single
        else single.take(MAX_DESC_LENGTH - 1) + "…"
    }

    private fun addModuleCategory(
        categories: LinkedHashMap<String, ProcessedCategory>,
        facade: Config,
        meta: ModuleMetadata,
        folder: File
    ) {
        val categoryName = buildString {
            append(meta.name)
            if (meta.library) append(" §d[библиотека]")
            if (ModuleLoader.isDisabled(folder)) append(" §c[выкл]")
        }
        val category = SHCategory(
            "module:${folder.name.lowercase()}",
            categoryName,
            clampDesc(meta.description.ifBlank { "Модуль ${meta.name}" })
        )

        // Инфо: версия/автор/зависимости
        val metaInfo = buildString {
            append("v").append(meta.version)
            if (meta.author.isNotBlank()) append(" §7— §f").append(meta.author)
            if (meta.requires.isNotEmpty()) append(" §7| зав: ").append(meta.requires.joinToString(", "))
        }
        val infoOpt = SHOption("Инфо", metaInfo, "meta:${folder.name}", category, facade, String::class.java, { "" }, {})
        infoOpt.setEditor(GuiOptionEditorInfoText(infoOpt, StructuredText.of(metaInfo)))
        category.options.add(infoOpt)

        // Тумблер включения
        val enabledOpt = SHOption(
            "Включён?",
            "Выключенный модуль не загружается при старте и выгружается прямо сейчас",
            "enabled:${folder.name}",
            category, facade, Boolean::class.java,
            { !ModuleLoader.isDisabled(folder) },
            { value ->
                val wantEnabled = value == true
                if (wantEnabled && ModuleLoader.isDisabled(folder)) {
                    ModuleLoader.setDisabled(folder, false)
                    val m = ModuleMetadata.fromFile(File(folder, "metadata.json")) ?: ModuleMetadata(name = folder.name)
                    ModuleLoader.loadModule(folder, m)
                } else if (!wantEnabled && !ModuleLoader.isDisabled(folder)) {
                    ModuleLoader.setDisabled(folder, true)
                    val key = ModuleLoader.getLoadedModules().entries
                        .firstOrNull { it.value.directory == folder }?.key ?: folder.name
                    ModuleLoader.unloadModule(key)
                }
            }
        )
        enabledOpt.setEditor(GuiOptionEditorBoolean(enabledOpt, -1, facade))
        category.options.add(enabledOpt)

        // Перезагрузить
        val reloadOpt = SHOption("Перезагрузить?", "Заново выполнить entry-файл модуля", "reload:${folder.name}", category, facade, String::class.java, { "" }, {})
        reloadOpt.setEditor(object : GuiOptionEditorButton(reloadOpt, -1, StructuredText.of("Да"), facade) {
            override fun onClick() {
                ModuleLoader.reloadModule(folder.name)
            }
        })
        category.options.add(reloadOpt)

        // Открыть папку
        val openOpt = SHOption("Папка модуля?", "Открыть папку модуля в проводнике", "folder:${folder.name}", category, facade, String::class.java, { "" }, {})
        openOpt.setEditor(object : GuiOptionEditorButton(openOpt, -1, StructuredText.of("Открыть"), facade) {
            override fun onClick() {
                try {
                    net.minecraft.util.Util.getPlatform().openFile(folder)
                } catch (e: Exception) {
                    ScriptConsole.error("ModuleConfigGui", "Не удалось открыть папку: ${e.message}")
                }
            }
        })
        category.options.add(openOpt)

        // Ключи config.json.
        // Геттеры опций читают ЖИВОЕ значение на каждый кадр (иначе свитчи/поля
        // не перерисовываются после клика). Живой модуль -> память JSConfig;
        // выключенный -> кэш файла, чтобы не читать диск каждый кадр.
        val liveConfig = ModuleLoader.getLiveConfig(meta.name)
        val fileData: LinkedHashMap<String, Any?> = if (liveConfig == null) readConfigFile(folder) else LinkedHashMap()
        val data: Map<String, Any?> = liveConfig?.getAll() ?: fileData
        val schema: Map<String, Any> = liveConfig?.getSchema() ?: readSchemaFile(folder)

        fun configValue(key: String, fallback: Any?): Any? =
            liveConfig?.get(key, fallback) ?: fileData[key] ?: fallback

        fun configWrite(key: String, v: Any?) {
            if (liveConfig != null) {
                liveConfig.set(key, v)
            } else {
                fileData[key] = v
                writeConfigFile(folder, fileData)
            }
        }

        for ((key, value) in data) {
            if (key == "__schema") continue
            val schemaEntry = schema[key] as? Map<*, *>
            val schemaType = (schemaEntry?.get("type") ?: schemaEntry?.get("editor"))?.toString()?.lowercase()
            val desc = clampDesc(buildString {
                append(key)
                schemaEntry?.get("description")?.let { append(" — ").append(it) }
            })
            when (value) {
                is Boolean -> {
                    val opt = SHOption(key, desc, "config:${folder.name}:$key", category, facade, Boolean::class.java, { configValue(key, value) }, {
                        if (it is Boolean) configWrite(key, it)
                    })
                    opt.setEditor(GuiOptionEditorBoolean(opt, -1, facade))
                    category.options.add(opt)
                }
                is String -> {
                    // Цвет через палитру MoulConfig (формат "0:R:G:B:A")
                    if (schemaType == "colour" || schemaType == "color") {
                        val opt = SHOption(key, desc, "config:${folder.name}:$key", category, facade, String::class.java, { configValue(key, value) as? String ?: value }, {
                            configWrite(key, it?.toString() ?: "0:255:255:255:255")
                        })
                        opt.setEditor(io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorColour(opt))
                        category.options.add(opt)
                        continue
                    }
                    // Дропдаун: схема с options
                    if (schemaType == "dropdown") {
                        val options = (schemaEntry?.get("options") as? List<*>)?.map { it.toString() }
                        if (options != null && options.isNotEmpty()) {
                            val opt = SHOption(key, desc, "config:${folder.name}:$key", category, facade, String::class.java, { configValue(key, value) as? String ?: value }, {
                                configWrite(key, it?.toString() ?: value)
                            })
                            opt.setEditor(io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorDropdown(opt, options.toTypedArray(), true))
                            category.options.add(opt)
                            continue
                        }
                    }
                    val opt = SHOption(key, desc, "config:${folder.name}:$key", category, facade, String::class.java, { configValue(key, value) as? String ?: value }, {
                        configWrite(key, it?.toString() ?: "")
                    })
                    opt.setEditor(GuiOptionEditorText(opt, ""))
                    category.options.add(opt)
                }
                is Number -> {
                    // Слайдер: схема с min/max/step
                    if (schemaType == "slider") {
                        val min = (schemaEntry?.get("min") as? Number)?.toFloat() ?: 0f
                        val max = (schemaEntry?.get("max") as? Number)?.toFloat() ?: 100f
                        val step = (schemaEntry?.get("step") as? Number)?.toFloat() ?: 1f
                        val opt = SHOption(
                            key, desc, "config:${folder.name}:$key", category, facade, Float::class.java,
                            { (configValue(key, value) as? Number ?: value).toFloat() },
                            {
                                if (it is Number) configWrite(key, convertNumberType(it, value))
                            }
                        )
                        opt.setEditor(io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorSlider(opt, min, max, step))
                        category.options.add(opt)
                        continue
                    }
                    val opt = SHOption(
                        key, desc, "config:${folder.name}:$key", category, facade, String::class.java,
                        { (configValue(key, value) ?: value).toString() },
                        {
                            val parsed = parseNumber(it?.toString(), value)
                            if (parsed != null) configWrite(key, parsed)
                        }
                    )
                    opt.setEditor(GuiOptionEditorText(opt, ""))
                    category.options.add(opt)
                }
                else -> {
                    val shown = if (value == null) "null" else value.toString().take(120)
                    val opt = SHOption(key, "Сложное значение — редактируйте в файле", "config:${folder.name}:$key", category, facade, String::class.java, { "" }, {})
                    opt.setEditor(GuiOptionEditorInfoText(opt, StructuredText.of("§7$shown")))
                    category.options.add(opt)
                }
            }
        }

        categories[category.id] = category
    }

    /** Сохранение числа с исходным типом (Int остаётся Int и т.д.). */
    private fun convertNumberType(input: Number, original: Number): Number {
        return when (original) {
            is Int -> input.toDouble().toInt()
            is Long -> input.toDouble().toLong()
            is Float -> input.toFloat()
            is Double -> input.toDouble()
            else -> input.toDouble()
        }
    }

    /** Схема из config.json выключенного модуля (ключ "__schema"). */
    private fun readSchemaFile(folder: File): Map<String, Any> {
        @Suppress("UNCHECKED_CAST")
        val data = readConfigFile(folder) as? Map<String, Any?> ?: return emptyMap()
        @Suppress("UNCHECKED_CAST")
        return (data["__schema"] as? Map<String, Any>) ?: emptyMap()
    }

    /** Открыть менеджер модулей (категория = модуль, опции = config.json). */
    fun open() {
        val facade = ModuleConfigFacade()
        val categories = LinkedHashMap<String, ProcessedCategory>()

        // Справочная категория
        val help = SHCategory("help", "О менеджере", "Управление JS-модулями StarredHeltix")
        val helpOpt = SHOption(
            "Как это работает",
            "Слева — модули из config/starredheltix/modules, справа — их настройки. Изменения сохраняются в config.json сразу",
            "help.about", help, facade, String::class.java, { "" }, {}
        )
        helpOpt.setEditor(GuiOptionEditorInfoText(helpOpt, StructuredText.of("Выберите модуль слева")))
        help.options.add(helpOpt)
        categories["help"] = help

        for ((meta, folder) in ModuleLoader.getAllModuleFoldersWithMeta()) {
            addModuleCategory(categories, facade, meta, folder)
        }

        val editor = MoulConfigEditor(categories, facade).apply { wide = true }
        set.starlev.utils.ConfigUtils.openEditor(editor)
    }

    /** Открыть настройки конкретного модуля (config.openMenu() из JS). */
    fun openModule(moduleName: String) {
        val facade = ModuleConfigFacade()
        val categories = LinkedHashMap<String, ProcessedCategory>()

        for ((meta, folder) in ModuleLoader.getAllModuleFoldersWithMeta()) {
            addModuleCategory(categories, facade, meta, folder)
        }

        val editor = MoulConfigEditor(categories, facade).apply { wide = true }
        // Выделяем категорию модуля через публичный API
        editor.allCategories["module:${moduleName.lowercase()}"]?.let {
            if (!editor.setSelectedCategory(it)) editor.search(moduleName)
        }
        set.starlev.utils.ConfigUtils.openEditor(editor)
    }
}
