package set.starlev.scripting

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientSuggestionProvider
import net.minecraft.commands.SharedSuggestionProvider
import org.mozilla.javascript.Callable
import org.mozilla.javascript.Context
import set.starlev.injections.accessors.ClientPacketListenerAccessor
import set.starlev.injections.accessors.CommandNodeAccessor
import java.util.concurrent.ConcurrentHashMap

/**
 * Мост между JS-командами модулей (`command.register("asprint", fn)`) и Brigadier.
 *
 * Регистрирует команду верхнего уровня `/<имя>` — команды модулей не кладутся
 * в /sh и /sh (это команды мода, а не модулей).
 *
 * Поэтому работают и Tab-подсказки, и ArgumentBuilder-синтаксис.
 * Dispatcher Fabric пересоздаётся при каждом входе на сервер
 * (ClientCommandRegistrationCallback), при этом команды перерегистрируются заново.
 */
object ScriptCommandBridge {

    /** Родительский литерал модульных команд отсутствует: команды модулей — только /<имя>. */

    data class Entry(val name: String, val moduleName: String, val callable: Callable, val schema: CommandArgsSchema? = null)

    /** Родительский узел + имя — Brigadier 1.3.10 не хранит ссылку на родителя. */
    private data class PlacedNode(val parent: CommandNode<FabricClientCommandSource>, val name: String)

    private val entries = ConcurrentHashMap<String, Entry>()

    @Volatile
    private var dispatcher: com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource>? = null

    /** Зарегистрированные узлы (для снятия при unload). commandName -> узлы */
    private val registeredNodes = ConcurrentHashMap<String, List<PlacedNode>>()

    fun init() {
        if (eventRegistered) return
        eventRegistered = true
        // Вызывается при каждом входе на сервер — dispatcher каждый раз новый
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            this.dispatcher = dispatcher
            registeredNodes.clear()
            // Перерегистрируем все живые команды модулей в новый dispatcher
            entries.values.forEach { entry -> registerNode(dispatcher, entry) }
        }
    }

    private var eventRegistered = false

    /**
     * Регистрация команды модуля. Потокобезопасно: сама карта обновляется сразу,
     * узлы Brigadier добавляются на главном потоке. Повторная регистрация (reload
     * модуля) заменяет узел вместе с колбэком.
     */
    fun registerCommand(name: String, moduleName: String, callable: Callable, schema: CommandArgsSchema? = null) {
        val key = name.lowercase()
        val entry = Entry(key, moduleName, callable, schema)
        val previous = entries.put(key, entry)
        if (previous != null && !previous.moduleName.equals(moduleName, ignoreCase = true)) {
            ScriptConsole.warn(moduleName, "Команда '/$key' перехвачена у модуля '${previous.moduleName}'")
        }

        val dispatcher = this.dispatcher ?: return
        Minecraft.getInstance().execute {
            val current = entries[key] ?: return@execute
            registerNode(dispatcher, current)
        }
    }

    fun unregisterCommand(name: String) {
        val key = name.lowercase()
        entries.remove(key)
        removeNodes(key)
    }

    fun unregisterForModule(moduleName: String) {
        // Удаляем только свои записи (два модуля могут делить одно имя команды)
        entries.forEach { (key, entry) ->
            if (entry.moduleName.equals(moduleName, ignoreCase = true)) {
                entries.remove(key, entry)
            }
        }
        registeredNodes.keys.toList().forEach { key ->
            if (entries[key] == null) removeNodes(key)
        }
    }

    fun clearAll() {
        entries.clear()
        val snapshot = registeredNodes.toMap()
        registeredNodes.clear()
        val dispatcher = this.dispatcher ?: return
        Minecraft.getInstance().execute {
            for ((name, placedList) in snapshot) {
                for (placed in placedList) {
                    removeChildOf(placed.parent, placed.name)
                }
                syncVanillaRemove(name)
            }
            // На случай если registeredNodes был уже очищен ранее — чистим остатки по имени из vanilla
            for (name in snapshot.keys) {
                // уже удалено выше, но для надёжности
                syncVanillaRemove(name)
            }
        }
    }

    // ==================== Brigadier ====================

    private fun registerNode(
        dispatcher: com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource>,
        entry: Entry
    ) {
        // Заменяем нашу старую регистрацию (reload модуля) — снимаем и ставим заново
        removeNodesSync(entry.name, dispatcher)

        // Не перекрываем чужие команды (встроенные /sh, серверные и т.д.)
        if (dispatcher.root.getChild(entry.name) != null) {
            ScriptConsole.warn(entry.moduleName, "Команда '/${entry.name}' уже занята, регистрация отменена")
            return
        }

        try {
            dispatcher.register(if (entry.schema != null) buildSchemaBuilder(entry) else buildBuilder(entry))
            val placed = listOf(PlacedNode(dispatcher.root, entry.name))
            registeredNodes[entry.name] = placed
            ScriptConsole.info(entry.moduleName, "Зарегистрирована команда '/${entry.name}'")
            // Сразу синхронизируем с ванильным dispatcher для автокомплита без перезахода
            syncVanillaAdd(entry.name)
        } catch (e: Throwable) {
            ScriptConsole.error(entry.moduleName, "Не удалось зарегистрировать команду '/${entry.name}': ${e.message}", e)
        }
    }

    private fun buildBuilder(entry: Entry) = ClientCommands.literal(entry.name)
        .executes { ctx -> execute(entry, ctx, ""); 1 }
        .then(ClientCommands.argument("args", StringArgumentType.greedyString())
            .executes { ctx -> execute(entry, ctx, StringArgumentType.getString(ctx, "args")); 1 }
        )

    /**
     * Строитель по схеме аргументов: цепочка типизированных Brigadier-узлов.
     * На каждом уровне висит executes — аргументы факультативны (не передал → null).
     * Последний уровень: greedy-аргумент захватывает остаток строки.
     */
    private fun buildSchemaBuilder(entry: Entry): com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> {
        val args = entry.schema!!.args
        val root = ClientCommands.literal(entry.name)
        root.executes { ctx -> executeTyped(entry, ctx, 0); 1 }

        var current: com.mojang.brigadier.builder.ArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource, *>? = null
        for (i in args.indices.reversed()) {
            val arg = args[i]
            val node = ClientCommands.argument(arg.name, arg.argumentType())
            arg.choices?.let { options ->
                node.suggests { _, builder ->
                    options.forEach { builder.suggest(it) }
                    builder.buildFuture()
                }
            }
            node.executes { ctx -> executeTyped(entry, ctx, i + 1); 1 }
            current?.let { node.then(it) }
            current = node
        }
        current?.let { root.then(it) }
        return root
    }

    /** Вызов колбэка с типизированными значениями: парсинг аргументов 0..depth-1, остальные null. */
    private fun executeTyped(
        entry: Entry,
        ctx: CommandContext<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>,
        depth: Int
    ) {
        val args = entry.schema?.args ?: return
        SHScriptEngine.withContext { cx ->
            try {
                val values = arrayOfNulls<Any?>(args.size)
                for (i in 0 until depth.coerceAtMost(args.size)) {
                    values[i] = Context.javaToJS(args[i].getValue(ctx), SHScriptEngine.globalScope)
                }
                val jsArray = cx.newArray(SHScriptEngine.globalScope, values)
                entry.callable.call(cx, SHScriptEngine.globalScope, SHScriptEngine.globalScope, arrayOf(jsArray))
            } catch (e: Throwable) {
                ScriptConsole.error(entry.moduleName, "Ошибка выполнения команды '/${entry.name}': ${e.message}", e)
            }
        }
    }

    private fun execute(entry: Entry, ctx: CommandContext<FabricClientCommandSource>, rawArgs: String) {
        SHScriptEngine.withContext { cx ->
            try {
                val args: Array<Any> = rawArgs.trim().split(Regex("\\s+"))
                    .filter { it.isNotEmpty() }
                    .toTypedArray()
                // Настоящий JS-массив: заворачивать Java-массив нельзя —
                // NativeJavaArray требует java.lang.Object, заблокированный песочницей.
                val jsArray = cx.newArray(
                    SHScriptEngine.globalScope,
                    args.map { Context.javaToJS(it, SHScriptEngine.globalScope) }.toTypedArray()
                )
                entry.callable.call(cx, SHScriptEngine.globalScope, SHScriptEngine.globalScope, arrayOf(jsArray))
            } catch (e: Throwable) {
                ScriptConsole.error(entry.moduleName, "Ошибка выполнения команды '/${entry.name}': ${e.message}", e)
            }
        }
    }

    private fun removeNodes(name: String) {
        val dispatcher = this.dispatcher ?: return
        Minecraft.getInstance().execute { removeNodesSync(name, dispatcher) }
    }

    /** Снятие узлов команды с dispatcher. Вызывать только на главном потоке. */
    private fun removeNodesSync(name: String, dispatcher: com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource>) {
        val placedList = registeredNodes.remove(name) ?: return
        for (placed in placedList) {
            removeChildOf(placed.parent, placed.name)
        }
        // Снимаем и с ванильного dispatcher для мгновенного обновления автокомплита
        syncVanillaRemove(name)
    }

    private fun removeChildOf(parent: CommandNode<FabricClientCommandSource>, name: String) {
        @Suppress("UNCHECKED_CAST")
        val acc = parent as CommandNodeAccessor<FabricClientCommandSource>
        acc.shGetChildren().remove(name)
        acc.shGetLiterals().remove(name)
    }

    // ==================== Vanilla autocomplete sync ====================
    // Fabric хранит client-команды в activeDispatcher, а ванильный dispatcher `commands`
    // наполняется только при handleCommands (пакет от сервера). Поэтому Tab-подсказки
    // для динамически добавленных команд появлялись только после перезахода.
    // Синхронизируем сразу: копируем новые команды в ванильный dispatcher и чистим удалённые.

    @Suppress("UNCHECKED_CAST")
    private fun syncVanillaAdd(name: String) {
        try {
            val conn = Minecraft.getInstance().connection ?: return
            val access = conn as? ClientPacketListenerAccessor ?: return
            val vanillaDispatcher = access.getCommands() ?: return
            val provider = access.getSuggestionsProvider() ?: return
            val fabricProvider = provider as? FabricClientCommandSource ?: return
            val active = dispatcher ?: return
            // Проверяем что команда действительно есть в active
            if (active.root.getChild(name) == null) return
            // Копируем все активные команды в ванильный dispatcher (как делает Fabric в onOnCommandTree)
            // Используем рефлексию к impl-классу чтобы не зависеть от compile-time
            try {
                val clazz = Class.forName("net.fabricmc.fabric.impl.command.client.ClientCommandInternals")
                val method = clazz.getMethod("addCommands", com.mojang.brigadier.CommandDispatcher::class.java, FabricClientCommandSource::class.java)
                @Suppress("UNCHECKED_CAST")
                method.invoke(null, vanillaDispatcher as com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource>, fabricProvider)
                return
            } catch (_: Exception) {
                // Fallback: вручную добавляем только эту команду
            }
            // Fallback — прямое добавление копии узла
            @Suppress("UNCHECKED_CAST")
            val vanillaAsFabric = vanillaDispatcher as com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource>
            val activeNode = active.root.getChild(name) ?: return
            // Убираем старую если была
            (vanillaAsFabric.root as CommandNodeAccessor<FabricClientCommandSource>).shGetChildren().remove(name)
            (vanillaAsFabric.root as CommandNodeAccessor<FabricClientCommandSource>).shGetLiterals().remove(name)
            // Копируем через builder (упрощённо — добавляем тот же узел, т.к. Brigadier использует identity для children/literals,
            // но для автокомплита достаточно共享 узла — Fabric именно копирует через builder, но для 1 команды достаточно addChild)
            // Делаем безопасную копию через createBuilder чтобы не шарить mutable state
            try {
                val copy = activeNode.createBuilder().build()
                // Копируем children рекурсивно как в Fabric's copyChildren — для наших простых команд (literal + greedy arg) достаточно одного уровня
                // Но вызываем addCommands fallback выше уже скопировал бы всё, так что здесь просто addChild копии
                vanillaAsFabric.root.addChild(copy)
            } catch (_: Exception) {
                vanillaAsFabric.root.addChild(activeNode)
            }
        } catch (_: Exception) {
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun syncVanillaRemove(name: String) {
        try {
            val conn = Minecraft.getInstance().connection ?: return
            val access = conn as? ClientPacketListenerAccessor ?: return
            val vanillaDispatcher = access.getCommands() ?: return
            val vanillaAsFabric = vanillaDispatcher as com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource>
            (vanillaAsFabric.root as CommandNodeAccessor<FabricClientCommandSource>).shGetChildren().remove(name)
            (vanillaAsFabric.root as CommandNodeAccessor<FabricClientCommandSource>).shGetLiterals().remove(name)
            // Также чистим из children map через SharedSuggestionProvider тип (на случай если Fabric хранит как SharedSuggestionProvider)
            val vanillaAsShared = vanillaDispatcher as com.mojang.brigadier.CommandDispatcher<SharedSuggestionProvider>
            (vanillaAsShared.root as CommandNodeAccessor<SharedSuggestionProvider>).shGetChildren().remove(name)
            (vanillaAsShared.root as CommandNodeAccessor<SharedSuggestionProvider>).shGetLiterals().remove(name)
        } catch (_: Exception) {
        }
    }
    }
