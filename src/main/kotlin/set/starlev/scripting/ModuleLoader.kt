package set.starlev.scripting

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.mozilla.javascript.Scriptable
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

data class LoadedModule(
    val metadata: ModuleMetadata,
    val directory: File,
    val loadedAt: Long = System.currentTimeMillis()
)

/**
 * Загрузчик и менеджер модулей StarredHeltix.
 */
object ModuleLoader {
    val modulesDir = File("config/starredheltix/modules")
    private val loadedModules = ConcurrentHashMap<String, LoadedModule>()
    private const val GITHUB_API_BASE = "https://api.github.com/repos/Starlevka/StarredHeltix"
    internal const val GITHUB_RAW_BASE = "https://raw.githubusercontent.com/Starlevka/StarredHeltix/main/modules"

    /** Маркер выключенного модуля — файл-флаг рядом с папкой модуля. */
    private fun disabledFlag(folder: File) = File(modulesDir, "${folder.name}.disabled")

    fun isDisabled(folder: File): Boolean = disabledFlag(folder).exists()

    /** Включить/выключить модуль (файл-флаг .disabled рядом с папкой). */
    fun setDisabled(folder: File, disabled: Boolean) {
        val flag = disabledFlag(folder)
        if (disabled) {
            flag.writeText("")
        } else {
            flag.delete()
        }
    }

    /** Кэш каталога модулей GitHub (имена папок в modules/ репозитория) для автокомплита /sh modules install. */
    @Volatile
    private var githubCatalogCache: List<String> = emptyList()

    fun init() {
        if (!modulesDir.exists()) {
            modulesDir.mkdirs()
        }
    }

    fun getLoadedModules(): Map<String, LoadedModule> = loadedModules

    /** Живые JSConfig загруженных модулей (для ModuleConfigGui: правки конфига сразу видны модулю). */
    private val liveConfigs = ConcurrentHashMap<String, set.starlev.scripting.api.JSConfig>()

    internal fun registerLiveConfig(moduleName: String, config: set.starlev.scripting.api.JSConfig) {
        liveConfigs[moduleName.lowercase()] = config
    }

    internal fun unregisterLiveConfig(moduleName: String) {
        liveConfigs.remove(moduleName.lowercase())
    }

    /** Живой JSConfig модуля (или null, если модуль не загружен). */
    fun getLiveConfig(moduleName: String): set.starlev.scripting.api.JSConfig? = liveConfigs[moduleName.lowercase()]

    /**
     * Все модули на диске: metadata (реальная или дефолтная) + папка.
     * Для ModuleConfigGui и списка — включая выключенные.
     */
    fun getAllModuleFoldersWithMeta(): List<Pair<ModuleMetadata, File>> {
        return modulesDir.listFiles { file -> file.isDirectory }
            ?.map { folder ->
                val meta = ModuleMetadata.fromFile(File(folder, "metadata.json"))
                    ?: ModuleMetadata(name = folder.name, entry = "index.js")
                meta to folder
            }
            ?.sortedWith(compareBy({ it.first.name.lowercase() }))
            ?: emptyList()
    }

    /**
     * Локальные папки модулей (для автокомплита /sh modules remove).
     * Возвращает имена папок, которые реально содержат файлы (entry/metadata),
     * но при полном отсутствии содержимого папка тоже предлагается — её можно "доудалить".
     */
    fun getLocalModuleFolders(): List<String> {
        return modulesDir.listFiles { file -> file.isDirectory }
            ?.map { it.name }
            ?.sortedWith(String.CASE_INSENSITIVE_ORDER)
            ?: emptyList()
    }

    /** Выключенные модули (есть файл-флаг .disabled) — для автокомплита /sh modules on. */
    fun getDisabledModuleFolders(): List<String> {
        return modulesDir.listFiles { file -> file.isFile && file.name.endsWith(".disabled") }
            ?.map { it.name.removeSuffix(".disabled") }
            ?.sortedWith(String.CASE_INSENSITIVE_ORDER)
            ?: emptyList()
    }

    /**
     * Каталог доступных на GitHub модулей. Сначала отдаёт кэш (мгновенный автокомплит),
     * затем обновляет его в фоновом потоке.
     */
    fun getGithubCatalog(onFetched: (List<String>) -> Unit) {
        val cached = githubCatalogCache
        if (cached.isNotEmpty()) {
            onFetched(cached)
        }
        thread(name = "SHModuleCatalog", isDaemon = true) {
            val list = fetchGithubCatalog()
            if (list != null) {
                githubCatalogCache = list
                onFetched(list)
            }
        }
    }

    /** Кэш каталога GitHub без сетевого запроса (для синхронного автокомплита). */
    fun getGithubCatalogOrNull(): List<String>? = githubCatalogCache.takeIf { it.isNotEmpty() }

    /** Фоновое обновление кэша каталога GitHub (результат будет доступен следующему вызову автокомплита). */
    fun refreshGithubCatalogAsync() {
        thread(name = "SHModuleCatalog", isDaemon = true) {
            fetchGithubCatalog()?.let { githubCatalogCache = it }
        }
    }

    private fun fetchGithubCatalog(): List<String>? {        return try {
            val repoJson = downloadJson(GITHUB_API_BASE) ?: return null
            val defaultBranch = Gson().fromJson(repoJson, RepoInfo::class.java).default_branch
            val treeJson = downloadJson("$GITHUB_API_BASE/git/trees/$defaultBranch?recursive=1") ?: return null
            val tree = Gson().fromJson(treeJson, TreeResponse::class.java)
            val prefix = "modules/"
            tree.tree
                .filter { it.type == "tree" && it.path.startsWith(prefix) }
                .map { it.path.removePrefix(prefix).substringBefore('/') }
                .filter { it.isNotBlank() }
                .distinct()
                .sortedWith(String.CASE_INSENSITIVE_ORDER)
        } catch (e: Exception) {
            ScriptConsole.warn("Loader", "Не удалось получить каталог модулей GitHub: ${e.message}")
            null
        }
    }

    /**
     * Поиск папки модуля без учёта регистра (пользователи часто пишут имя в другом регистре).
     */
    fun findModuleFolder(name: String): File? {
        val direct = File(modulesDir, name)
        if (direct.isDirectory) return direct
        return modulesDir.listFiles { file -> file.isDirectory }
            ?.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Рекурсивное удаление папки модуля с повторными попытками (файловые блокировки
     * на Windows могут держать дескрипторы пару мгновений после выгрузки).
     * @return true, если папка удалена полностью (пустых остатков не остаётся)
     */
    fun deleteModuleFolder(folder: File): Boolean {
        if (!folder.exists()) return true
        repeat(5) { attempt ->
            folder.deleteRecursively()
            if (!folder.exists()) return true
            System.gc()
            Thread.sleep(50L shl attempt)
        }
        // Последний шанс: снять read-only атрибуты и повторить
        folder.walkBottomUp().forEach { if (it.isFile) it.setWritable(true) }
        folder.deleteRecursively()
        return !folder.exists()
    }

    fun loadAll() {
        init()
        unloadAll()

        if (!set.starlev.StarredHeltix.feature.scripting.enabled) {
            ScriptConsole.info("Loader", "Система JavaScript модулей отключена в конфиге.")
            return
        }

        val folders = modulesDir.listFiles { file -> file.isDirectory } ?: emptyArray()
        if (folders.isEmpty()) {
            ScriptConsole.info("Loader", "Папка модулей пуста ($modulesDir).")
            return
        }

        val parsedList = mutableListOf<Pair<File, ModuleMetadata>>()
        for (folder in folders) {
            if (isDisabled(folder)) {
                ScriptConsole.info("Loader", "Модуль '${folder.name}' выключен (.disabled), пропускаю.")
                continue
            }
            val metaFile = File(folder, "metadata.json")
            val meta = ModuleMetadata.fromFile(metaFile) ?: ModuleMetadata(
                name = folder.name,
                entry = "index.js",
                description = "Без описания"
            )
            if (!meta.ignored) {
                parsedList.add(folder to meta)
            }
        }

        val sorted = sortModules(parsedList)
        for ((folder, meta) in sorted) {
            loadModule(folder, meta)
        }

        ScriptConsole.info("Loader", "Успешно загружено ${loadedModules.size} модулей.")
        ScriptConsole.printToChat("§a[StarredHeltix] Загружено модулей: §e${loadedModules.size}")

        // Прогрев кэша каталога GitHub для автокомплита /sh modules install
        if (githubCatalogCache.isEmpty()) {
            refreshGithubCatalogAsync()
        }
    }

    /**
     * Загрузка модуля. Если объявлены requires, которых нет на диске — они автоматически
     * скачиваются с GitHub (библиотеки подтягиваются как зависимости).
     */
    fun loadModule(folder: File, meta: ModuleMetadata, onLoadError: ((Boolean, String) -> Unit)? = null): Boolean {
        // Автодокачка недостающих зависимостей с GitHub (один раз на папку, защита от циклов)
        if (meta.requires.isNotEmpty() && folder !in resolvingRequires) {
            val missing = meta.requires.filter { findModuleFolder(it) == null }
            if (missing.isNotEmpty()) {
                resolvingRequires.add(folder)
                try {
                    for (dep in missing) {
                        ScriptConsole.printToChat("§e[StarredHeltix] Модуль '${meta.name}' требует '$dep' — установка с GitHub...")
                        installFromGitHubBlocking(dep)
                    }
                } finally {
                    resolvingRequires.remove(folder)
                }
            }
        }

        val entryFile = File(folder, meta.entry)
        if (!entryFile.exists()) {
            ScriptConsole.error(meta.name, "Файл точки входа не найден: ${entryFile.name}")
            return false
        }

        try {
            val code = entryFile.readText(Charsets.UTF_8)
            // currentModule=meta.name на время top-level eval: Display/Image, созданные на этапе
            // загрузки, корректно атрибутируются модулю и будут удалены при его выгрузке.
            SHScriptEngine.withModule(meta.name) {
                val scope = SHScriptEngine.createModuleScope(folder, meta.name)
                SHScriptEngine.eval(code, meta.entry, scope)
                loadedModules[meta.name.lowercase()] = LoadedModule(meta, folder)
                // Живой конфиг для ModuleConfigGui: правки в GUI сразу видны модулю
                SHScriptEngine.lastCreatedConfigs[meta.name.lowercase()]?.let { registerLiveConfig(meta.name, it) }
                // Библиотеки публикуют API через module.exports — кэшируем для require("имя")
                if (meta.library) {
                    val exports = SHScriptEngine.getModuleExportsObject(scope)
                    if (exports != null) {
                        moduleExports[meta.name.lowercase()] = exports
                    }
                }
            }
            ScriptConsole.info("Loader", "Модуль '${meta.name}' v${meta.version} загружен${if (meta.library) " (библиотека)" else ""}.")
            return true
        } catch (e: Throwable) {
            ScriptConsole.error(meta.name, "Ошибка при загрузке: ${e.message}", e)
            // Модуль мог создать триггеры/таймеры/HUD/Display до того, как упал, но не попасть
            // в loadedModules -> unloadModule его бы не почистил. Снимаем всё его сразу.
            try { SHScriptEngine.removeTriggersForModule(meta.name) } catch (_: Exception) {}
            onLoadError?.invoke(false, "Ошибка при загрузке модуля '${meta.name}': ${e.message}")
            return false
        }
    }

    /** Папки, для которых уже идёт автодокачка зависимостей (защита от циклов). */
    private val resolvingRequires = ConcurrentHashMap.newKeySet<File>()

    /** Кэш exports модулей-библиотек: ключ — имя модуля в нижнем регистре. */
    private val moduleExports = ConcurrentHashMap<String, Scriptable>()

    /** Exports загруженного модуля-библиотеки (или null). */
    fun getModuleExports(name: String): Scriptable? = moduleExports[name.lowercase()]

    /**
     * Состояние модуля по имени: загружен / выключен / отсутствует.
     * Используется для понятных сообщений об ошибках в require().
     */
    fun getModuleState(name: String): String {
        if (getModuleExports(name) != null) return "загружен"
        val folder = findModuleFolder(name)
        if (folder == null) return "не установлен"
        return if (isDisabled(folder)) "выключен" else "не загружен"
    }

    /**
     * Фоновая установка недостающей библиотеки (вызывается из require() при загрузке модуля).
     * После установки модуль будет доступен при следующем /sh modules reload.
     */
    fun installMissingLibraryAsync(name: String) {
        thread(name = "SHModuleLibInstaller", isDaemon = true) {
            installFromGitHubBlocking(name)
        }
    }

    /** Блокирующая установка модуля с GitHub (для каскадной загрузки зависимостей). */
    private fun installFromGitHubBlocking(moduleName: String): Boolean {
        return try {
            val targetDir = File(modulesDir, moduleName)
            if (targetDir.exists()) return true

            val metaBytes = downloadBytes("$GITHUB_RAW_BASE/$moduleName/metadata.json")
            val meta = if (metaBytes != null) {
                ModuleMetadata.fromJson(String(metaBytes, Charsets.UTF_8))
                    ?: ModuleMetadata(name = moduleName, entry = "index.js")
            } else {
                ModuleMetadata(name = moduleName, entry = "index.js")
            }

            val filePaths = fetchModuleFileList(moduleName) ?: listOf("metadata.json", meta.entry)

            targetDir.mkdirs()
            var downloaded = 0
            for (relPath in filePaths) {
                val bytes = downloadBytes("$GITHUB_RAW_BASE/$moduleName/$relPath") ?: continue
                val outFile = File(targetDir, relPath)
                outFile.parentFile?.mkdirs()
                outFile.writeBytes(bytes)
                downloaded++
            }
            if (!File(targetDir, meta.entry).exists()) {
                targetDir.deleteRecursively()
                ScriptConsole.error("Loader", "Не удалось скачать зависимость '$moduleName' (нет ${meta.entry})")
                return false
            }
            ScriptConsole.printToChat("§a[StarredHeltix] Зависимость '$moduleName' установлена (файлов: $downloaded). Перезагрузите модули: /sh modules reload")
            true
        } catch (e: Exception) {
            ScriptConsole.error("Loader", "Ошибка установки зависимости '$moduleName': ${e.message}")
            false
        }
    }

    fun unloadModule(name: String) {
        val key = name.lowercase()
        val loaded = loadedModules.remove(key)
        if (loaded != null) {
            moduleExports.remove(key)
            unregisterLiveConfig(key)
            SHScriptEngine.removeTriggersForModule(loaded.metadata.name)
            // Кастомная камера: если отцеплена этим модулем — вернуть на место
            CameraController.onModuleUnloaded(loaded.metadata.name)
            ScriptConsole.info("Loader", "Модуль '${loaded.metadata.name}' выгружен.")
        }
    }

    /**
     * Модули, зависящие от данного (requires). Используется, чтобы
     * предупреждать о выключении библиотеки и перезагружать зависимых.
     */
    fun getDependents(name: String): List<String> {
        val key = name.lowercase()
        return loadedModules.values
            .filter { it.metadata.requires.any { r -> r.equals(name, ignoreCase = true) } }
            .map { it.metadata.name }
    }

    fun reloadModule(name: String): Boolean {
        val key = name.lowercase()
        val loaded = loadedModules[key]
        val folder = loaded?.directory ?: File(modulesDir, name)
        if (!folder.exists() || !folder.isDirectory) {
            ScriptConsole.error("Loader", "Папка модуля не найдена: $name")
            return false
        }
        unloadModule(name)
        val meta = ModuleMetadata.fromFile(File(folder, "metadata.json")) ?: ModuleMetadata(name = folder.name)
        return loadModule(folder, meta)
    }

    fun unloadAll() {
        loadedModules.clear()
        SHScriptEngine.clearAllTriggers()
    }

    private fun sortModules(modules: List<Pair<File, ModuleMetadata>>): List<Pair<File, ModuleMetadata>> {
        val map = modules.associateBy { it.second.name.lowercase() }
        val result = mutableListOf<Pair<File, ModuleMetadata>>()
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()

        fun dfs(nodeName: String) {
            val node = map[nodeName] ?: return
            visited.add(nodeName)
            recursionStack.add(nodeName)

            for (dep in node.second.requires) {
                val depKey = dep.lowercase()
                if (depKey in recursionStack) {
                    ScriptConsole.warn("Loader", "Обнаружена циклическая зависимость: $nodeName -> $dep")
                    continue
                }
                if (depKey !in visited) {
                    dfs(depKey)
                }
            }

            recursionStack.remove(nodeName)
            if (node !in result) {
                result.add(node)
            }
        }

        for ((_, meta) in modules) {
            val key = meta.name.lowercase()
            if (key !in visited) {
                dfs(key)
            }
        }

        return result
    }

    /**
     * Скачивание модуля из репозитория GitHub.
     * Формат — папочный: через GitHub Trees API рекурсивно получаем ВСЕ файлы
     * модуля (включая вложенные папки, assets, require-файлы) и скачиваем каждый
     * через raw.githubusercontent.com. Архивы не нужны.
     */
    fun installFromGitHub(moduleName: String, onComplete: (Boolean, String) -> Unit) {
        thread(name = "SHModuleInstaller", isDaemon = true) {
            try {
                val targetDir = File(modulesDir, moduleName)
                if (targetDir.exists()) {
                    onComplete(false, "Модуль '$moduleName' уже существует. Используйте /sh modules reload $moduleName")
                    return@thread
                }

                ScriptConsole.printToChat("§e[StarredHeltix] Скачивание модуля '$moduleName'...")

                // 1. metadata.json
                val metaBytes = downloadBytes("$GITHUB_RAW_BASE/$moduleName/metadata.json")
                val meta = if (metaBytes != null) {
                    ModuleMetadata.fromJson(String(metaBytes, Charsets.UTF_8))
                        ?: ModuleMetadata(name = moduleName, entry = "index.js")
                } else {
                    ModuleMetadata(name = moduleName, entry = "index.js")
                }

                // 2. Полный список файлов модуля через GitHub Trees API (recursive).
                //    Ищем subtree "modules/<moduleName>" в дереве дефолтной ветки.
                val filePaths = fetchModuleFileList(moduleName)
                if (filePaths == null) {
                    // Фоллбэк: качаем минимум (metadata + entry), как раньше
                    ScriptConsole.warn("Loader", "Trees API недоступен, скачиваю только entry-файл.")
                }

                val filesToDownload = if (filePaths != null) filePaths else listOf("metadata.json", meta.entry)

                // 3. Скачиваем все файлы, сохраняя структуру папок
                targetDir.mkdirs()
                var downloaded = 0
                for (relPath in filesToDownload) {
                    val bytes = downloadBytes("$GITHUB_RAW_BASE/$moduleName/$relPath") ?: continue
                    val outFile = File(targetDir, relPath)
                    outFile.parentFile?.mkdirs()
                    outFile.writeBytes(bytes)
                    downloaded++
                }

                if (!File(targetDir, meta.entry).exists()) {
                    targetDir.deleteRecursively()
                    onComplete(false, "Не удалось найти ${meta.entry} для модуля '$moduleName' на GitHub")
                    return@thread
                }

                ScriptConsole.printToChat("§7[StarredHeltix] Скачано файлов: §e$downloaded")

                val success = loadModule(targetDir, meta)
                if (success) {
                    onComplete(true, "Модуль '${meta.name}' v${meta.version} успешно установлен и запущен!")
                } else {
                    onComplete(false, "Файлы скачаны, но при запуске модуля '${meta.name}' произошла ошибка.")
                }
            } catch (e: Exception) {
                onComplete(false, "Ошибка при скачивании модуля: ${e.message}")
            }
        }
    }

    /**
     * Получить список всех файлов модуля через GitHub Trees API (recursive=1).
     * @return список относительных путей внутри modules/<moduleName>/ или null при ошибке
     */
    private fun fetchModuleFileList(moduleName: String): List<String>? {        return try {
            // Сначала получаем дефолтную ветку репозитория
            val repoJson = downloadJson(GITHUB_API_BASE) ?: return null
            val defaultBranch = Gson().fromJson(repoJson, RepoInfo::class.java).default_branch

            // Затем рекурсивное дерево ветки
            val treeJson = downloadJson("$GITHUB_API_BASE/git/trees/$defaultBranch?recursive=1")
                ?: return null
            val tree = Gson().fromJson(treeJson, TreeResponse::class.java)

            // Заодно кэшируем каталог модулей (имена папок под modules/) для автокомплита
            val catalogPrefix = "modules/"
            val catalog = tree.tree
                .filter { it.type == "tree" && it.path.startsWith(catalogPrefix) }
                .map { it.path.removePrefix(catalogPrefix).substringBefore('/') }
                .filter { it.isNotBlank() }
                .distinct()
            if (catalog.isNotEmpty()) {
                githubCatalogCache = catalog.sortedWith(String.CASE_INSENSITIVE_ORDER)
            }

            val prefix = "modules/$moduleName/"
            tree.tree
                .filter { it.type == "blob" && it.path.startsWith(prefix) }
                .map { it.path.removePrefix(prefix) }
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            ScriptConsole.warn("Loader", "Не удалось получить список файлов с GitHub: ${e.message}")
            null
        }
    }

    /** Публичная обёртка над списком файлов модуля (для ModuleUpdater). */
    fun fetchModuleFileListPublic(moduleName: String): List<String>? = fetchModuleFileList(moduleName)

    /** Публичное скачивание сырого файла с GitHub (для ModuleUpdater). */
    fun downloadRaw(urlString: String): ByteArray? = downloadBytes(urlString)

    private fun downloadJson(urlString: String): String? = set.starlev.utils.HttpUtils.downloadString(urlString)
    private fun downloadBytes(urlString: String): ByteArray? = set.starlev.utils.HttpUtils.downloadBytes(urlString)
}

/** Ответ GitHub API: информация о репозитории. */
private data class RepoInfo(val default_branch: String = "main")

/** Ответ GitHub Trees API. */
private data class TreeResponse(val tree: List<TreeEntry> = emptyList())

private data class TreeEntry(val path: String = "", val type: String = "")
