package set.starlev.scripting

import kotlin.concurrent.thread

/**
 * Проверка и обновление модулей из GitHub-репозитория StarredHeltix.
 * Версия берётся из metadata.json модуля (semver-подобное сравнение).
 */
object ModuleUpdater {
    private const val GITHUB_RAW_BASE = ModuleLoader.GITHUB_RAW_BASE

    data class UpdateInfo(val module: String, val localVersion: String, val remoteVersion: String)

    /**
     * Проверить обновления всех загруженных модулей асинхронно.
     * @param onResult вызывается в фоновом потоке со списком (имя, локальная версия, удалённая версия)
     */
    fun checkUpdates(onResult: (List<UpdateInfo>) -> Unit) {
        thread(name = "SHModuleUpdateCheck", isDaemon = true) {
            val outdated = mutableListOf<UpdateInfo>()
            for ((_, mod) in ModuleLoader.getLoadedModules()) {
                val remoteVersion = fetchRemoteVersion(mod.metadata.name) ?: continue
                if (isNewerVersion(remoteVersion, mod.metadata.version)) {
                    outdated.add(UpdateInfo(mod.metadata.name, mod.metadata.version, remoteVersion))
                }
            }
            onResult(outdated)
        }
    }

    /**
     * Обновить один модуль: скачать свежие файлы с GitHub поверх существующих и перезагрузить.
     * Работает даже если папка модуля занята (файлы перезаписываются, а не удаляются).
     */
    fun updateModule(moduleName: String, onComplete: (Boolean, String) -> Unit) {
        val loaded = ModuleLoader.getLoadedModules()[moduleName.lowercase()]
        if (loaded == null) {
            onComplete(false, "Модуль '$moduleName' не загружен")
            return
        }

        thread(name = "SHModuleUpdater") {
            try {
                val remoteVersion = fetchRemoteVersion(loaded.metadata.name)
                if (remoteVersion != null && !isNewerVersion(remoteVersion, loaded.metadata.version)) {
                    onComplete(true, "Модуль '${loaded.metadata.name}' уже актуален (v${loaded.metadata.version})")
                    return@thread
                }

                ScriptConsole.printToChat("§e[StarredHeltix] Обновление модуля '${loaded.metadata.name}'...")

                // Выгружаем модуль (но не удаляем папку!) — останавливаем таймеры и триггеры
                ModuleLoader.unloadModule(loaded.metadata.name)

                val metaBytes = ModuleLoader.downloadRaw("$GITHUB_RAW_BASE/${loaded.metadata.name}/metadata.json")
                val meta = if (metaBytes != null) {
                    ModuleMetadata.fromJson(String(metaBytes, Charsets.UTF_8)) ?: loaded.metadata
                } else {
                    loaded.metadata
                }

                val filePaths = ModuleLoader.fetchModuleFileListPublic(loaded.metadata.name)
                    ?: listOf("metadata.json", meta.entry)

                var updated = 0
                for (relPath in filePaths) {
                    val bytes = ModuleLoader.downloadRaw("$GITHUB_RAW_BASE/${loaded.metadata.name}/$relPath") ?: continue
                    val outFile = java.io.File(loaded.directory, relPath)
                    outFile.parentFile?.mkdirs()
                    outFile.writeBytes(bytes)
                    updated++
                }

                val success = ModuleLoader.loadModule(loaded.directory, meta)
                if (success) {
                    onComplete(true, "Модуль '${meta.name}' обновлён до v${meta.version} (файлов: $updated) и перезагружен!")
                } else {
                    onComplete(false, "Файлы обновлены, но при запуске модуля '${meta.name}' произошла ошибка")
                }
            } catch (e: Exception) {
                onComplete(false, "Ошибка обновления '${moduleName}': ${e.message}")
            }
        }
    }

    private fun fetchRemoteVersion(moduleName: String): String? {
        return try {
            val bytes = ModuleLoader.downloadRaw("$GITHUB_RAW_BASE/$moduleName/metadata.json") ?: return null
            ModuleMetadata.fromJson(String(bytes, Charsets.UTF_8))?.version
        } catch (e: Exception) {
            null
        }
    }

    /** Сравнение версий — делегирует в VersionUtils (единый). */
    fun isNewerVersion(candidate: String, current: String): Boolean = set.starlev.utils.VersionUtils.isNewer(candidate, current)
}
