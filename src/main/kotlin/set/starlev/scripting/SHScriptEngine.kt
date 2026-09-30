package set.starlev.scripting

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import org.mozilla.javascript.Callable
import org.mozilla.javascript.Context
import org.mozilla.javascript.TopLevel
import org.mozilla.javascript.Scriptable
import set.starlev.events.EventBus
import set.starlev.events.SlotUpdateEvent
import set.starlev.events.StarredEvents
import set.starlev.scripting.api.*
import set.starlev.utils.detectors.EntityDeathDetector
import set.starlev.utils.detectors.SkillXpDetector
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Главный исполнительный движок JavaScript для StarredHeltix.
 */
object SHScriptEngine {
    val contextFactory = SHContextFactory()
    lateinit var globalScope: Scriptable
        private set

    // CopyOnWriteArrayList: итерация в fireTrigger (горячий путь tick/worldRender 20-240 р/с)
    // не аллоцирует итератор и не блокирует редкие register/unregister.
    private val triggers = ConcurrentHashMap<String, CopyOnWriteArrayList<SHTrigger>>()
    val dynamicCommands = ConcurrentHashMap<String, Pair<String, Callable>>() // name -> (moduleName, callable)

    /** Имя модуля, чей код сейчас исполняется (для атрибуции Display/Image/ресурсов,
     *  создаваемых не только на этапе загрузки, но и в триггерах). Все JS-колбэки идут
     *  в клиентском потоке, поэтому достаточно одного поля (не ConcurrentHashMap). */
    @Volatile var currentModule: String? = null
        private set

    var isLoaded = false
        private set

    /** Биндинги на Fabric/Starred-события ставятся РОВНО ОДИН РАЗ за жизнь процесса.
     *  reload() -> shutdown() -> init() не должен переподписываться, иначе каждый
     *  reload удваивает fireTrigger("tick"/"chat"/...) (утечка слушателей). */
    private var bindingsInstalled = false

    fun init() {
        if (isLoaded) return

        // Мост команд модулей (реальные Brigadier-команды /<имя>)
        ScriptCommandBridge.init()

        // Редкие игровые события: connect/disconnect (pickup/damage — через сетевой миксин)
        SHGameEvents.init()

        withContext { cx ->
            // Plain стандартный top-level scope вместо ImporterTopLevel:
            // без Rhino-импортёра нет load()/loadFile()/setPaths()/importClass()/importPackage(),
            // которыми модуль мог выполнить JS за пределами своей папки или притащить классы в обход
            // Java.type. Java/Packages доступ остаётся через наш JavaGlobal (см. put("Java", ...) ниже).
            globalScope = cx.initStandardObjects()

            // Регистрация глобальных синглтонов API
            val jsPlayer = JSPlayer()
            val jsChat = JSChat()
            val jsDetectors = JSDetectors()
            val jsClient = JSClient()

            globalScope.put("player", globalScope, Context.javaToJS(jsPlayer, globalScope))
            globalScope.put("chat", globalScope, Context.javaToJS(jsChat, globalScope))
            globalScope.put("detector", globalScope, Context.javaToJS(jsDetectors, globalScope))
            globalScope.put("detectors", globalScope, Context.javaToJS(jsDetectors, globalScope)) // алиас
            globalScope.put("client", globalScope, Context.javaToJS(jsClient, globalScope))
            // Камера — глобальный ресурс (одна на клиент), фрикаф
            globalScope.put("camera", globalScope, Context.javaToJS(JSCamera(), globalScope))
            globalScope.put("settings", globalScope, Context.javaToJS(JSSettings(), globalScope))
            globalScope.put("math", globalScope, Context.javaToJS(JSMathLib(), globalScope))
            globalScope.put("cps", globalScope, Context.javaToJS(JSCps(), globalScope))
            globalScope.put("textComponent", globalScope, Context.javaToJS(JSTextComponentFactory, globalScope))
            // CTJS-паритет: Display / Text / Image / Scoreboard / Team / Renderer как глобальные классы
            globalScope.put("Display", globalScope, Context.javaToJS(set.starlev.scripting.api.JSDisplay::class.java, globalScope))
            globalScope.put("Text", globalScope, Context.javaToJS(set.starlev.scripting.api.JSText::class.java, globalScope))
            globalScope.put("Image", globalScope, Context.javaToJS(set.starlev.scripting.api.JSImage::class.java, globalScope))
            globalScope.put("Scoreboard", globalScope, Context.javaToJS(set.starlev.scripting.api.JSScoreboard, globalScope))
            globalScope.put("Team", globalScope, Context.javaToJS(set.starlev.scripting.api.JSTeam::class.java, globalScope))
            globalScope.put("Renderer", globalScope, Context.javaToJS(set.starlev.scripting.api.JSRendererCTJS, globalScope))
            globalScope.put("Maps", globalScope, Context.javaToJS(set.starlev.scripting.api.JSMaps, globalScope))
            globalScope.put("DungeonMap", globalScope, Context.javaToJS(set.starlev.scripting.api.JSDungeonMap, globalScope))
            // alias maps/dungeonMap lower-case для удобства
            globalScope.put("maps", globalScope, Context.javaToJS(set.starlev.scripting.api.JSMaps, globalScope))
            globalScope.put("dungeonMap", globalScope, Context.javaToJS(set.starlev.scripting.api.JSDungeonMap, globalScope))
            // Блок-хелперы ctjs (JSBlock/JSBlockFace/JSBlockType/JSBlockPos) задокументированы
            // как доступные напрямую, но раньше НЕ были в globalScope — код из доков падал
            // с ReferenceError. Регистрируем через ::class.java, чтобы работал companion
            // (JSBlockFace.UP/NORTH, JSBlock.of(...), JSBlockType.fromID(...), ...).
            globalScope.put("JSBlock", globalScope, Context.javaToJS(set.starlev.scripting.api.JSBlock::class.java, globalScope))
            globalScope.put("JSBlockFace", globalScope, Context.javaToJS(set.starlev.scripting.api.JSBlockFace::class.java, globalScope))
            globalScope.put("JSBlockType", globalScope, Context.javaToJS(set.starlev.scripting.api.JSBlockType::class.java, globalScope))
            globalScope.put("JSBlockPos", globalScope, Context.javaToJS(set.starlev.scripting.api.JSBlockPos::class.java, globalScope))
            // Kotlin-хелперы, доступные из JS (console.log и таймеры ссылаются на них).
            // set.starlev.scripting.* разрешён в SHClassShutter как доверенный.
            globalScope.put("ScriptConsole", globalScope, Context.javaToJS(ScriptConsole, globalScope))
            globalScope.put("JSUtils", globalScope, Context.javaToJS(JSUtils("<globals>"), globalScope))
            globalScope.put("Java", globalScope, Context.javaToJS(JavaGlobal, globalScope))

            // Регистрация глобальных функций
            injectGlobals(cx, globalScope)
        }

        setupEventBindings()
        isLoaded = true
        ScriptConsole.info("Engine", "JS Script Engine инициализирован.")
    }

    private fun injectGlobals(cx: Context, scope: Scriptable) {
        val script = """
            var console = {
                log: function(msg) { ScriptConsole.info("JS", String(msg)); },
                info: function(msg) { ScriptConsole.info("JS", String(msg)); },
                warn: function(msg) { ScriptConsole.warn("JS", String(msg)); },
                error: function(msg) { ScriptConsole.error("JS", String(msg)); }
            };
        """.trimIndent()

        cx.evaluateString(scope, script, "<globals>", 1, null)
    }

    private fun setupEventBindings() {
        if (bindingsInstalled) return
        bindingsInstalled = true

        ClientTickEvents.END_CLIENT_TICK.register {
            JSCps.tick()
            fireTrigger("tick")
        }

        StarredEvents.chatIncoming.subscribe { event ->
            fireTrigger("chat", event.message)
        }

        StarredEvents.chatOutgoing.subscribe { event ->
            fireTrigger("chatOutgoing", event.message)
        }

        StarredEvents.worldRender.subscribe { ctx ->
            // FPS — каждый кадр (world)
            fireFps()
            // Сначала нативно рисуем персистентные батчи модулей (без JS в hot-path),
            // затем запускаем JS-триггеры worldRender
            SHWorldRenderBatches.renderAll(ctx)
            val jsCtx = JSRenderContext(ctx)
            set.starlev.scripting.api.JSRendererCTJS.setWorldContext(ctx)
            try { fireTrigger("worldRender", jsCtx) } finally { set.starlev.scripting.api.JSRendererCTJS.setWorldContext(null) }
        }

        StarredEvents.guiRender.subscribe { event ->
            // FPS — каждый кадр (HUD, в т.ч. в контейнерах). Дебаунс внутри fireFps() — не дублируем worldRender
            fireFps()
            // CTJS Display/Text — рендерим все активные Display поверх HUD (как в CTJS)
            try { set.starlev.scripting.api.DisplayManager.renderAll(event.graphics) } catch (_: Exception) {}
            try { set.starlev.scripting.api.ImageDrawQueue.renderAll(event.graphics) } catch (_: Exception) {}
            set.starlev.scripting.api.JSRendererCTJS.setGuiContext(event.graphics)
            try {
                fireTrigger("overlay", JSOverlayContext(event.graphics))
            } finally { set.starlev.scripting.api.JSRendererCTJS.setGuiContext(null) }
        }

        StarredEvents.guiOpen.subscribe { event ->
            fireTrigger("guiOpen", event.screen.title.string)
        }

        StarredEvents.guiClose.subscribe { event ->
            fireTrigger("guiClose", event.screen.title.string)
        }

        StarredEvents.guiClick.subscribe { event ->
            fireTrigger("guiClick", event.mouseX, event.mouseY, event.button)
        }

        SkillXpDetector.registerListener { info ->
            // HashMap (а не Kotlin mapOf=LinkedHashMap): класс должен быть в whitelist песочницы
            val map = java.util.HashMap<String, Any?>()
            map["gained"] = info.gained
            map["skill"] = info.skill
            map["current"] = info.current
            map["target"] = info.target
            map["isMax"] = info.isMax
            map["percent"] = info.percent
            fireTrigger("skillXp", map)
        }

        EntityDeathDetector.registerListener { entity ->
            // JS-контракт events.onEntityDeath(name, typeId, uuid):
            // раньше передавалось только имя — uuid/typeId были undefined,
            // и модули не могли связать смерть с позицией из кэша сущностей.
            val typeId = try {
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.type).toString()
            } catch (_: Exception) { "minecraft:unknown" }
            fireTrigger("entityDeath", entity.name.string, typeId, entity.uuid.toString())
        }

        // events.onSlotUpdate: мост из внутреннего EventBus (ContainerEventWire публикует
        // SlotUpdateEvent при каждом обновлении слота открытого контейнера).
        // net.minecraft-объекты наружу не отдаём — только slotId и строковый id предмета.
        EventBus.subscribe<SlotUpdateEvent> { e ->
            if (!isLoaded) return@subscribe
            if (!hasListeners("slotUpdate")) return@subscribe
            val itemId = try {
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.stack.item).toString()
            } catch (_: Exception) { "minecraft:air" }
            fireTrigger("slotUpdate", e.slotId, itemId, e.stack.count)
        }
    }

    fun registerTrigger(trigger: SHTrigger) {
        val list = triggers.computeIfAbsent(trigger.type) { CopyOnWriteArrayList() }
        // Вставка с сохранением сортировки по приоритету (раньше был ConcurrentSkipListSet)
        var insertAt = list.size
        for (i in list.indices) {
            if (trigger.compareTo(list[i]) < 0) {
                insertAt = i
                break
            }
        }
        list.add(insertAt, trigger)
    }

    fun removeTrigger(trigger: SHTrigger) {
        triggers[trigger.type]?.remove(trigger)
    }

    fun removeTriggersForModule(moduleName: String) {
        triggers.values.forEach { set ->
            set.removeIf { it.moduleName.equals(moduleName, ignoreCase = true) }
        }
        dynamicCommands.entries.removeIf { it.value.first.equals(moduleName, ignoreCase = true) }
        // Полная очистка ресурсов модуля: таймеры (иначе setInterval мог пересоздавать
        // папку модуля после удаления через storage.save() -> mkdirs()) и команды
        JSUtils.clearTimersForModule(moduleName)
        ScriptCommandBridge.unregisterForModule(moduleName)
        ScriptChatHandler.removeForModule(moduleName)
        set.starlev.render.ScriptNPCManager.removeForModule(moduleName)
        SHWorldRenderBatches.removeForModule(moduleName)
        JSHud.removeForModule(moduleName)
        JSGuiManager.closeForModule(moduleName)
        SHEventBus.removeForModule(moduleName)
        SHModuleTextures.removeForModule(moduleName)
        SHModuleFonts.removeForModule(moduleName)
        SHModuleSounds.removeForModule(moduleName)
        JSNet.stopForModule(moduleName)
        // CTJS-паритет ресурсы + cancellable sound/particle — тоже чисто per-module:
        DisplayManager.removeForModule(moduleName)
        JSImage.releaseForModule(moduleName)
        JSBossBars.clearOwnedFor(moduleName)
        ScriptSoundHandler.removeForModule(moduleName)
        ScriptParticleHandler.removeForModule(moduleName)
        set.starlev.scripting.ModuleGlowRegistry.clearForModule(moduleName)
        set.starlev.scripting.ModuleCustomScoreboard.removeForModule(moduleName)
        set.starlev.scripting.api.JSMaps.removeForModule(moduleName)
    }

    fun clearAllTriggers() {
        triggers.clear()
        dynamicCommands.clear()
        JSUtils.clearAllTimers()
        ScriptCommandBridge.clearAll()
        ScriptChatHandler.clearAll()
        set.starlev.render.ScriptNPCManager.clearAll()
        SHWorldRenderBatches.clearAll()
        JSHud.removeAll()
        JSGuiManager.closeAll()
        SHEventBus.clearAll()
        SHModuleTextures.clearAll()
        SHModuleFonts.clearAll()
        SHModuleSounds.clearAll()
        JSNet.stopAll()
        DisplayManager.clear()
        ScriptSoundHandler.clearAll()
        ScriptParticleHandler.clearAll()
        set.starlev.scripting.ModuleGlowRegistry.clearAll()
        set.starlev.scripting.ModuleCustomScoreboard.clearAll()
        set.starlev.scripting.api.JSMaps.clearAll()
    }

    fun registerCommand(name: String, moduleName: String, callable: Callable, schema: CommandArgsSchema? = null) {
        dynamicCommands[name.lowercase()] = Pair(moduleName, callable)
        ScriptCommandBridge.registerCommand(name, moduleName, callable, schema)
    }

    fun unregisterCommand(name: String) {
        dynamicCommands.remove(name.lowercase())
        ScriptCommandBridge.unregisterCommand(name)
    }

    @Volatile private var lastFpsNs = 0L
    private fun fireFps() {
        val now = System.nanoTime()
        if (now - lastFpsNs < 8_000_000) return
        lastFpsNs = now
        fireTrigger("fps")
        fireTrigger("frame")
        fireTrigger("render")
    }

    /** Есть ли хоть один активный слушатель на тип (для раннего выхода в горячих путях — пакеты/слоты). */
    fun hasListeners(type: String): Boolean {
        val list = triggers[type] ?: return false
        for (t in list) if (t.isActive) return true
        return false
    }

    fun fireTrigger(type: String, vararg args: Any?) {
        val list = triggers[type] ?: return
        if (list.isEmpty()) return

        withContext { cx ->
            for (trigger in list) {
                if (!trigger.isActive) continue
                SHContextFactory.beginTracking()
                val prevModule = currentModule
                currentModule = trigger.moduleName
                try {
                    trigger.invoke(cx, globalScope, *args)
                } finally {
                    currentModule = prevModule
                    SHContextFactory.endTracking()
                }
            }
        }
    }

    /** Пакет-инфо для JS: песочница не даёт читать поля net.minecraft пакетов. */
    fun describePacket(packet: Any): Map<String, Any?> {
        val map = java.util.HashMap<String, Any?>()
        map["type"] = packet.javaClass.simpleName
        map["fullType"] = packet.javaClass.name
        return map
    }

    /** Ссылки на JSConfig последнего созданного scope модуля (имя -> конфиг). */
    internal val lastCreatedConfigs = ConcurrentHashMap<String, JSConfig>()

    fun createModuleScope(moduleDir: File, moduleName: String): Scriptable {
        return withContext { cx ->
            val scope = cx.newObject(globalScope)
            scope.prototype = globalScope
            scope.parentScope = null

            // module.exports — библиотеки отдают свой API через module.exports,
            // другие модули получают его через require("имя-модуля")
            val exportsObj = cx.newObject(scope)
            val moduleObj = cx.newObject(scope)
            moduleObj.put("name", moduleObj, moduleName)
            moduleObj.put("exports", moduleObj, exportsObj)
            scope.put("module", scope, moduleObj)
            scope.put("exports", scope, exportsObj)

            val jsEvents = JSEvents(moduleName)
            val jsHud = JSHud(moduleName)
            val jsStorage = JSStorage(moduleDir, moduleName)
            val jsCommand = JSCommand(moduleName)
            val jsUtils = JSUtils(moduleName)
            val jsInventory = JSInventory()
            val jsWorld = JSWorld()
            val jsBossBars = JSBossBars(moduleName)
            val jsToast = JSToast()
            val jsFileLib = JSFileLib(moduleDir, moduleName)
            val jsConfig = JSConfig(moduleDir, moduleName)
            val jsNpc = JSNpc(moduleName)
            val jsRenderApi = JSRenderApi(moduleName)
            val jsGui = JSGui(moduleName)
            val jsKeybind = JSKeybind(moduleName)
            val jsNet = JSNet(moduleName)
            val jsSound = JSSound(moduleName)
            val jsParticles = JSParticles(moduleName)
            val jsItem = JSItemApi()

            // Ссылка на живой конфиг для ModuleLoader/ModuleConfigGui
            lastCreatedConfigs[moduleName.lowercase()] = jsConfig

            scope.put("events", scope, Context.javaToJS(jsEvents, scope))
            scope.put("hud", scope, Context.javaToJS(jsHud, scope))
            scope.put("storage", scope, Context.javaToJS(jsStorage, scope))
            scope.put("command", scope, Context.javaToJS(jsCommand, scope))
            scope.put("utils", scope, Context.javaToJS(jsUtils, scope))
            // World / Inventory API (ctjs-паритет)
            scope.put("inventory", scope, Context.javaToJS(jsInventory, scope))
            scope.put("world", scope, Context.javaToJS(jsWorld, scope))
            scope.put("bossbars", scope, Context.javaToJS(jsBossBars, scope))
            scope.put("toast", scope, Context.javaToJS(jsToast, scope))
            scope.put("file", scope, Context.javaToJS(jsFileLib, scope))
            scope.put("config", scope, Context.javaToJS(jsConfig, scope))
            scope.put("npc", scope, Context.javaToJS(jsNpc, scope))
            scope.put("render", scope, Context.javaToJS(jsRenderApi, scope))
            scope.put("gui", scope, Context.javaToJS(jsGui, scope))
            scope.put("keybind", scope, Context.javaToJS(jsKeybind, scope))
            scope.put("net", scope, Context.javaToJS(jsNet, scope))
            scope.put("sound", scope, Context.javaToJS(jsSound, scope))
            scope.put("particles", scope, Context.javaToJS(jsParticles, scope))
            scope.put("item", scope, Context.javaToJS(jsItem, scope))

            // Таймеры как глобальные функции: привязаны к модулю и автоматически
            // очищаются при его выгрузке (JSUtils хранит владельца каждого таймера)
            val timerScript = """
                function setTimeout(fn, delay) { return utils.setTimeout(fn, delay || 0); }
                function setInterval(fn, period) { return utils.setInterval(fn, period || 0); }
                function clearTimeout(id) { utils.clearTimeout(id); }
                function clearInterval(id) { utils.clearInterval(id); }
            """.trimIndent()
            cx.evaluateString(scope, timerScript, "<module-timers:$moduleName>", 1, null)

            // Добавляем функцию require для загрузки локальных js-файлов модуля.
            // Бонус: require("имя-другого-модуля") отдаёт exports модуля-библиотеки.
            scope.put("require", scope, object : Callable {
                override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any>?): Any? {
                    if (args == null || args.isEmpty()) return null
                    val relPath = args[0].toString()

                    // Библиотеки-модули: require("проверка-чс") -> exports загруженного модуля
                    if (!relPath.startsWith("./") && !relPath.startsWith("../") && !relPath.startsWith("/")) {
                        ModuleLoader.getModuleExports(relPath)?.let { return it }
                    }

                    val targetFile = if (relPath.endsWith(".js")) File(moduleDir, relPath) else File(moduleDir, "$relPath.js")
                    // Канонический «джейл»: require не может выйти за пределы папки модуля
                    // через ../ или абсолютный путь (иначе — исполнение JS из чужих каталогов).
                    val jail = moduleDir.canonicalFile
                    val realTarget = try { targetFile.canonicalFile } catch (e: Exception) { null }
                    if (realTarget == null ||
                        !(realTarget.path == jail.path ||
                            realTarget.path.startsWith(jail.path + File.separator))
                    ) {
                        ScriptConsole.error(moduleName, "require('$relPath') запрещён: путь вне папки модуля")
                        return null
                    }
                    if (!realTarget.exists()) {
                        // Уточнённая ошибка: возможно, это модуль-библиотека, но он не загружен/выключен
                        if (!relPath.startsWith("./") && !relPath.startsWith("../") && !relPath.startsWith("/")) {
                            val state = ModuleLoader.getModuleState(relPath)
                            ScriptConsole.error(moduleName, "Модуль-библиотека '$relPath' недоступен (${state}). Требуется: /sh modules on $relPath")
                            ModuleLoader.installMissingLibraryAsync(relPath)
                            return null
                        }
                        ScriptConsole.error(moduleName, "Файл не найден при require('$relPath'): ${realTarget.absolutePath}")
                        return null
                    }
                    val exports = cx.newObject(scope)
                    val moduleObj = cx.newObject(scope)
                    moduleObj.put("exports", moduleObj, exports)

                    val subScope = cx.newObject(scope)
                    subScope.prototype = scope
                    subScope.parentScope = null
                    subScope.put("exports", subScope, exports)
                    subScope.put("module", subScope, moduleObj)

                    try {
                        cx.evaluateString(subScope, realTarget.readText(Charsets.UTF_8), realTarget.name, 1, null)
                        return moduleObj.get("exports", moduleObj)
                    } catch (e: Throwable) {
                        ScriptConsole.error(moduleName, "Ошибка выполнения require('$relPath'): ${e.message}", e)
                        return null
                    }
                }
            })

            scope
        }
    }

    /** Exports-объект scope модуля (для кэша библиотек). */
    fun getModuleExportsObject(scope: Scriptable): Scriptable? {
        val moduleObj = scope.get("module", scope) as? Scriptable ?: return null
        return moduleObj.get("exports", moduleObj) as? Scriptable
    }

    fun eval(code: String, sourceName: String, scope: Scriptable = globalScope): Any? {
        return withContext { cx ->
            SHContextFactory.beginTracking()
            try {
                cx.evaluateString(scope, code, sourceName, 1, null)
            } finally {
                SHContextFactory.endTracking()
            }
        }
    }

    /** Выполнить [block], приписав создаваемые ресурсы модулю [moduleName] (для top-level eval). */
    fun <T> withModule(moduleName: String, block: () -> T): T {
        val prev = currentModule
        currentModule = moduleName
        try {
            return block()
        } finally {
            currentModule = prev
        }
    }

    fun shutdown() {
        clearAllTriggers()
        JSUtils.shutdownScheduler()
        isLoaded = false
        ScriptConsole.info("Engine", "JS Script Engine выключен.")
    }

    fun reload() {
        shutdown()
        init()
        ModuleLoader.loadAll()
    }

    inline fun <T> withContext(block: (Context) -> T): T {
        val cx = contextFactory.enterContext()
        return try {
            block(cx)
        } finally {
            Context.exit()
        }
    }
}
