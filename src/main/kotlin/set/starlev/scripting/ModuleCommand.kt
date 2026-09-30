package set.starlev.scripting

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.Component
import java.io.File
import java.util.concurrent.CompletableFuture

/**
 * Команды управления модулями: `/sh modules <list|reload|install|remove|on|off|info|update|checkupdates>`
 *
 * Автокомплит:
 *  - install — каталог модулей GitHub (кэшируется, обновляется в фоне);
 *  - remove — папки в config/starredheltix/modules;
 *  - on — выключенные модули, off — включённые (загруженные);
 *  - reload/info — текущие загруженные модули.
 */
object ModuleCommand {

    private fun buildTree(root: String): LiteralArgumentBuilder<FabricClientCommandSource> {
        return literal(root)
            .executes(::listModules)
            .then(literal("list").executes(::listModules))
            .then(literal("reload")
                .executes(::reloadAll)
                .then(argument("name", StringArgumentType.string())
                    .suggests { _, builder -> suggestLoadedModules(builder) }
                    .executes(::reloadSingle)
                )
            )
            .then(literal("install")
                .then(argument("name", StringArgumentType.string())
                    .suggests { _, builder -> suggestGithubModules(builder) }
                    .executes(::installModule)
                )
            )
            .then(literal("remove")
                .then(argument("name", StringArgumentType.string())
                    .suggests { _, builder -> suggestLocalModules(builder) }
                    .executes(::removeModule)
                )
            )
            .then(literal("on")
                .then(argument("name", StringArgumentType.string())
                    .suggests { _, builder -> suggestDisabledModules(builder) }
                    .executes(::enableModule)
                )
            )
            .then(literal("off")
                .then(argument("name", StringArgumentType.string())
                    .suggests { _, builder -> suggestEnabledModules(builder) }
                    .executes(::disableModule)
                )
            )
            .then(literal("info")
                .then(argument("name", StringArgumentType.string())
                    .suggests { _, builder -> suggestAnyModules(builder) }
                    .executes(::moduleInfo)
                )
            )
            .then(literal("update")
                .executes { ctx ->
                    ctx.source.sendFeedback(Component.literal("§e[StarredHeltix] Проверка и обновление всех модулей..."))
                    updateAllModules(ctx)
                    1
                }
                .then(argument("name", StringArgumentType.string())
                    .suggests { _, builder -> suggestLoadedModules(builder) }
                    .executes { ctx ->
                        val name = StringArgumentType.getString(ctx, "name")
                        ctx.source.sendFeedback(Component.literal("§e[StarredHeltix] Обновление модуля '$name'..."))
                        ModuleUpdater.updateModule(name) { success, message ->
                            ScriptConsole.printToChat("${if (success) "§a" else "§c"}[StarredHeltix] $message")
                        }
                        1
                    }
                )
            )
            .then(literal("checkupdates")
                .executes { ctx ->
                    ctx.source.sendFeedback(Component.literal("§e[StarredHeltix] Проверка обновлений модулей..."))
                    checkUpdates(ctx)
                    1
                }
            )
    }

    /** /sh modules — единственная точка управления модулями. */
    fun registerSubcommand(): LiteralArgumentBuilder<FabricClientCommandSource> = buildTree("modules")

    private fun updateAllModules(ctx: CommandContext<FabricClientCommandSource>) {
        ModuleUpdater.checkUpdates { outdated ->
            if (outdated.isEmpty()) {
                ScriptConsole.printToChat("§a[StarredHeltix] Все модули актуальны.")
                return@checkUpdates
            }
            for (info in outdated) {
                ScriptConsole.printToChat("§e[StarredHeltix] Обновление ${info.module} (v${info.localVersion} → v${info.remoteVersion})...")
                ModuleUpdater.updateModule(info.module) { success, message ->
                    ScriptConsole.printToChat("${if (success) "§a" else "§c"}[StarredHeltix] $message")
                }
            }
        }
    }

    private fun checkUpdates(ctx: CommandContext<FabricClientCommandSource>) {
        ModuleUpdater.checkUpdates { outdated ->
            if (outdated.isEmpty()) {
                ScriptConsole.printToChat("§a[StarredHeltix] Все модули актуальны.")
            } else {
                ScriptConsole.printToChat("§6§l[StarredHeltix] Доступны обновления (${outdated.size}):")
                outdated.forEach { info ->
                    ScriptConsole.printToChat(" §e- §f${info.module} §7v${info.localVersion} → §av${info.remoteVersion} §8(/sh modules update ${info.module})")
                }
            }
        }
    }

    // ==================== Автокомплит ====================

    /** Автокомплит по загруженным модулям (reload/info). */
    private fun suggestLoadedModules(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        ModuleLoader.getLoadedModules().values.forEach { mod ->
            val meta = mod.metadata
            val tooltip = "§7v${meta.version} §8- §7${meta.description}".take(60)
            builder.suggest(meta.name, Component.literal(tooltip))
        }
        return builder.buildFuture()
    }

    /** Автокомплит по папкам в config/starredheltix/modules (remove). */
    private fun suggestLocalModules(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val loaded = ModuleLoader.getLoadedModules()
        ModuleLoader.getLocalModuleFolders().forEach { folderName ->
            val meta = loaded.values.firstOrNull { it.metadata.name.equals(folderName, ignoreCase = true) }?.metadata
            val tooltip = if (meta != null) {
                "§7v${meta.version} §8- §7${meta.description}".take(60)
            } else {
                "§8(папка на диске, не загружен)"
            }
            builder.suggest(folderName, Component.literal(tooltip))
        }
        return builder.buildFuture()
    }

    /**
     * Автокомплит по каталогу GitHub (install).
     * Кэш показывается мгновенно; если кэша нет — запускается фоновое обновление
     * (подсказки появятся при следующем обращении), а сейчас предлагаются локальные модули.
     */
    private fun suggestGithubModules(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val cached = ModuleLoader.getGithubCatalogOrNull()
        if (cached != null) {
            cached.forEach { name ->
                if (name.startsWith(builder.remaining, ignoreCase = true)) {
                    builder.suggest(name, Component.literal("§7модуль из GitHub"))
                }
            }
        } else {
            ModuleLoader.refreshGithubCatalogAsync()
            ModuleLoader.getLoadedModules().values.forEach { mod ->
                if (mod.metadata.name.startsWith(builder.remaining, ignoreCase = true)) {
                    builder.suggest(mod.metadata.name, Component.literal("§8(установлен локально)"))
                }
            }
        }
        return builder.buildFuture()
    }

    /** Автокомплит по выключенным модулям (on). */
    private fun suggestDisabledModules(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        ModuleLoader.getDisabledModuleFolders().forEach { folderName ->
            if (folderName.startsWith(builder.remaining, ignoreCase = true)) {
                builder.suggest(folderName, Component.literal("§8выключен — включить"))
            }
        }
        return builder.buildFuture()
    }

    /** Автокомплит по включённым (загруженным) модулям (off). */
    private fun suggestEnabledModules(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        ModuleLoader.getLoadedModules().values.forEach { mod ->
            if (mod.metadata.name.startsWith(builder.remaining, ignoreCase = true)) {
                val tooltip = "§7v${mod.metadata.version} §8- §7${mod.metadata.description}".take(60)
                builder.suggest(mod.metadata.name, Component.literal(tooltip))
            }
        }
        return builder.buildFuture()
    }

    /** Автокомплит по всем модулям на диске (info). */
    private fun suggestAnyModules(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val seen = mutableSetOf<String>()
        ModuleLoader.getLoadedModules().values.forEach { mod ->
            if (mod.metadata.name.startsWith(builder.remaining, ignoreCase = true)) {
                val tooltip = "§7v${mod.metadata.version} §8- §7${mod.metadata.description}".take(60)
                builder.suggest(mod.metadata.name, Component.literal(tooltip))
            }
            seen.add(mod.metadata.name.lowercase())
        }
        ModuleLoader.getLocalModuleFolders().forEach { folderName ->
            if (folderName.lowercase() !in seen && folderName.startsWith(builder.remaining, ignoreCase = true)) {
                builder.suggest(folderName, Component.literal("§8(на диске)"))
            }
        }
        return builder.buildFuture()
    }

    // ==================== Действия ====================

    private fun listModules(ctx: CommandContext<FabricClientCommandSource>): Int {
        val loaded = ModuleLoader.getLoadedModules()
        val disabled = ModuleLoader.getDisabledModuleFolders()

        if (loaded.isEmpty() && disabled.isEmpty()) {
            ctx.source.sendFeedback(Component.literal("§e[StarredHeltix] Модулей нет. Установка: §f/sh modules install <имя>"))
            return 1
        }

        ctx.source.sendFeedback(Component.literal("§6§l[StarredHeltix Модули] (§e${loaded.size}§6 вкл, §7${disabled.size}§6 выкл):"))
        loaded.values.forEach { mod ->
            val meta = mod.metadata
            val tag = when {
                meta.library -> "§d[библиотека] "
                meta.requires.isNotEmpty() -> "§7[зав: ${meta.requires.joinToString(", ")}] "
                else -> ""
            }
            ctx.source.sendFeedback(Component.literal(" §a● §f${meta.name} §7v${meta.version} ${tag}§8- §7${meta.description}"))
        }
        disabled.forEach { name ->
            ctx.source.sendFeedback(Component.literal(" §c○ §7$name §8(выключен — /sh modules on $name)"))
        }
        return 1
    }

    private fun reloadAll(ctx: CommandContext<FabricClientCommandSource>): Int {
        ctx.source.sendFeedback(Component.literal("§e[StarredHeltix] Перезагрузка всех модулей..."))
        ModuleLoader.loadAll()
        return 1
    }

    private fun reloadSingle(ctx: CommandContext<FabricClientCommandSource>): Int {
        val name = StringArgumentType.getString(ctx, "name")
        val success = ModuleLoader.reloadModule(name)
        if (success) {
            ctx.source.sendFeedback(Component.literal("§a[StarredHeltix] Модуль §e$name §aуспешно перезагружен!"))
        } else {
            ctx.source.sendError(Component.literal("§c[StarredHeltix] Не удалось перезагрузить модуль '$name'"))
        }
        return 1
    }

    private fun installModule(ctx: CommandContext<FabricClientCommandSource>): Int {
        val name = StringArgumentType.getString(ctx, "name")
        ctx.source.sendFeedback(Component.literal("§e[StarredHeltix] Запрос на установку модуля §f$name §eс GitHub..."))
        ModuleLoader.installFromGitHub(name) { success, message ->
            if (success) {
                ScriptConsole.printToChat("§a[StarredHeltix] $message")
            } else {
                ScriptConsole.printToChat("§c[StarredHeltix] $message")
            }
        }
        return 1
    }

    private fun removeModule(ctx: CommandContext<FabricClientCommandSource>): Int {
        val name = StringArgumentType.getString(ctx, "name")

        // Папку ищем без учёта регистра и с приоритетом реальной директории загруженного модуля
        // (имя в metadata.json может отличаться от имени папки)
        val folder = ModuleLoader.getLoadedModules()[name.lowercase()]?.directory
            ?: ModuleLoader.findModuleFolder(name)
        if (folder == null || !folder.isDirectory) {
            ctx.source.sendError(Component.literal("§c[StarredHeltix] Папка модуля '$name' не найдена"))
            return 0
        }

        // Защита: нельзя удалять модуль, если от него зависят другие включённые модули
        val dependents = ModuleLoader.getDependents(folder.name)
            .filter { it.equals(folder.name, ignoreCase = true).not() }
        if (dependents.isNotEmpty()) {
            ctx.source.sendError(Component.literal(
                "§c[StarredHeltix] '$name' — библиотека для: §e${dependents.joinToString(", ")}§c. " +
                "Сначала выключите/удалите их: /sh modules off <имя>"
            ))
            return 0
        }

        val loadedKey = ModuleLoader.getLoadedModules().entries
            .firstOrNull { it.value.directory == folder }?.key
        ModuleLoader.unloadModule(loadedKey ?: name)

        // Удаляем и файл-флаг выключенного модуля
        ModuleLoader.setDisabled(folder, false)

        val deleted = ModuleLoader.deleteModuleFolder(folder)
        if (deleted) {
            ctx.source.sendFeedback(Component.literal("§a[StarredHeltix] Модуль §e${folder.name} §aудалён (файлы и папка)."))
        } else {
            val leftover = folder.listFiles()?.joinToString("§7, §c") { it.name }
            ctx.source.sendError(Component.literal(
                "§c[StarredHeltix] Файлы модуля удалены, но папка §f${folder.name} §cзанята системой. " +
                "Остались: §e${leftover ?: "(пусто)"}§c. Пустую папку можно удалить вручную."
            ))
        }
        return 1
    }

    // ==================== Вкл/Выкл ====================

    private fun enableModule(ctx: CommandContext<FabricClientCommandSource>): Int {
        val name = StringArgumentType.getString(ctx, "name")
        val folder = ModuleLoader.findModuleFolder(name)
        if (folder == null || !folder.isDirectory) {
            // Модуля нет локально — предлагаем установить с GitHub
            ctx.source.sendError(Component.literal("§c[StarredHeltix] Модуль '$name' не найден локально. Установить: §f/sh modules install $name"))
            return 0
        }
        if (!ModuleLoader.isDisabled(folder)) {
            ctx.source.sendFeedback(Component.literal("§e[StarredHeltix] Модуль '$name' уже включён."))
            return 1
        }
        ModuleLoader.setDisabled(folder, false)
        ctx.source.sendFeedback(Component.literal("§e[StarredHeltix] Включение модуля '$name'..."))
        val meta = ModuleMetadata.fromFile(File(folder, "metadata.json")) ?: ModuleMetadata(name = folder.name)
        val success = ModuleLoader.loadModule(folder, meta)
        if (success) {
            ctx.source.sendFeedback(Component.literal("§a[StarredHeltix] Модуль §e$name §aвключён и загружен."))
        } else {
            ctx.source.sendError(Component.literal("§c[StarredHeltix] Модуль '$name' включён, но при загрузке произошла ошибка (см. лог)."))
        }
        return 1
    }

    private fun disableModule(ctx: CommandContext<FabricClientCommandSource>): Int {
        val name = StringArgumentType.getString(ctx, "name")
        val folder = ModuleLoader.findModuleFolder(name)
        if (folder == null || !folder.isDirectory) {
            ctx.source.sendError(Component.literal("§c[StarredHeltix] Модуль '$name' не найден"))
            return 0
        }
        if (ModuleLoader.isDisabled(folder)) {
            ctx.source.sendFeedback(Component.literal("§e[StarredHeltix] Модуль '$name' уже выключен."))
            return 1
        }
        // Предупреждаем, если от модуля зависят другие
        val dependents = ModuleLoader.getDependents(name)
        if (dependents.isNotEmpty()) {
            ctx.source.sendFeedback(Component.literal(
                "§e[StarredHeltix] Внимание: '$name' используется модулями: §f${dependents.joinToString(", ")}§e — они могут перестать работать."
            ))
        }
        ModuleLoader.setDisabled(folder, true)
        val loadedKey = ModuleLoader.getLoadedModules().entries
            .firstOrNull { it.value.directory == folder }?.key ?: name
        ModuleLoader.unloadModule(loadedKey)
        ctx.source.sendFeedback(Component.literal("§a[StarredHeltix] Модуль §e$name §aвыключен (не будет загружаться при старте)."))
        return 1
    }

    private fun moduleInfo(ctx: CommandContext<FabricClientCommandSource>): Int {
        val name = StringArgumentType.getString(ctx, "name").lowercase()
        val loaded = ModuleLoader.getLoadedModules()[name]
        val folder = loaded?.directory ?: ModuleLoader.findModuleFolder(name)
        if (loaded == null) {
            // Модуль есть на диске, но не загружен — покажем это явно
            if (folder != null) {
                val disabled = ModuleLoader.isDisabled(folder)
                val hint = if (disabled) "/sh modules on ${folder.name}" else "/sh modules reload ${folder.name}"
                ctx.source.sendError(Component.literal("§e[StarredHeltix] Модуль '$name' найден на диске (${if (disabled) "выключен" else "не загружен"}). $hint"))
            } else {
                ctx.source.sendError(Component.literal("§c[StarredHeltix] Модуль '$name' не установлен"))
            }
            return 0
        }

        val meta = loaded.metadata
        ctx.source.sendFeedback(Component.literal("§6§lИнформация о модуле §e${meta.name}:"))
        ctx.source.sendFeedback(Component.literal(" §7Версия: §f${meta.version}"))
        ctx.source.sendFeedback(Component.literal(" §7Автор: §f${meta.author}"))
        ctx.source.sendFeedback(Component.literal(" §7Описание: §f${meta.description}"))
        ctx.source.sendFeedback(Component.literal(" §7Точка входа: §f${meta.entry}"))
        if (meta.library) {
            ctx.source.sendFeedback(Component.literal(" §7Тип: §dбиблиотека§7 (exports доступны другим модулям через require(\"${meta.name}\"))"))
        }
        if (meta.requires.isNotEmpty()) {
            ctx.source.sendFeedback(Component.literal(" §7Зависимости: §f${meta.requires.joinToString(", ")}"))
        }
        val dependents = ModuleLoader.getDependents(meta.name)
        if (dependents.isNotEmpty()) {
            ctx.source.sendFeedback(Component.literal(" §7Используется модулями: §f${dependents.joinToString(", ")}"))
        }
        ctx.source.sendFeedback(Component.literal(" §7Путь: §8${loaded.directory.absolutePath}"))
        return 1
    }
}
