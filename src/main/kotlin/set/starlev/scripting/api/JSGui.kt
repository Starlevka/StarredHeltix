package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.mozilla.javascript.Callable
import org.mozilla.javascript.Scriptable
import set.starlev.render.drawString
import set.starlev.render.drawCenteredString
import set.starlev.scripting.SHScriptEngine
import set.starlev.scripting.ScriptConsole
import java.util.concurrent.ConcurrentHashMap

/**
 * JS API: Объект `gui` — полноценные GUI-экраны для модулей.
 *
 * Экран рисуется JS-функцией каждый кадр, события мыши/клавиатуры идут в JS-колбэки.
 * JS получает тот же 2D-контекст, что и overlay (drawTexture, scissor, transforms, items).
 *
 * ```js
 * gui.openScreen({
 *     title: "Моё меню",
 *     pausesGame: false,
 *     render: function(ctx, mouseX, mouseY) {
 *         ctx.drawRect(50, 50, 100, 20, 0x80000000);
 *         ctx.drawString("Привет!", 60, 56, 0xFFFFFF);
 *     },
 *     onClick: function(x, y, button) { ... },   // вернуть true, чтобы поглотить клик
 *     onKey: function(keyCode, scanCode) { ... },// вернуть true = поглотить
 *     onChar: function(ch) { ... },              // ввод текста
 *     onScroll: function(x, y, delta) { ... },
 *     onClose: function() { ... }
 * });
 * gui.closeScreen();
 * ```
 */
class JSGui(private val moduleName: String) {

    fun openScreen(options: Scriptable?): JSGuiScreen? {
        if (options == null) return null
        return try {
            val screen = JSGuiScreen(moduleName, options)
            screen.open()
            screen
        } catch (e: Throwable) {
            ScriptConsole.error(moduleName, "Ошибка открытия GUI: ${e.message}", e)
            null
        }
    }

    fun closeScreen() {
        JSGuiManager.closeForModule(moduleName)
    }

    /** Открыт ли сейчас экран этого модуля. */
    fun isScreenOpen(): Boolean = JSGuiManager.currentOwner == moduleName
}

/**
 * Реестр открытых JS-экранов (один экран на всё приложение).
 */
object JSGuiManager {
    @Volatile
    var current: JSGuiScreen? = null
        private set

    @Volatile
    var currentOwner: String? = null
        private set

    fun show(screen: JSGuiScreen) {
        current = screen
        currentOwner = screen.moduleName
        val mc = Minecraft.getInstance()
        mc.execute {
            mc.gui.setScreen(screen)
        }
    }

    fun closeForModule(moduleName: String) {
        if (currentOwner != null && currentOwner.equals(moduleName, ignoreCase = true)) {
            val screen = current
            current = null
            currentOwner = null
            Minecraft.getInstance().execute {
                // Закрываем только если этот экран ещё открыт
                if (screen != null && Minecraft.getInstance().gui.screen() === screen) {
                    screen.silentClose()
                }
            }
        }
    }

    fun closeAll() {
        val screen = current
        current = null
        currentOwner = null
        Minecraft.getInstance().execute {
            if (screen != null && Minecraft.getInstance().gui.screen() === screen) {
                screen.silentClose()
            }
        }
    }

    internal fun onClose(screen: JSGuiScreen) {
        if (current === screen) {
            current = null
            currentOwner = null
        }
        screen.fireOnClose()
    }
}

/**
 * Экран, управляемый JS-колбэками.
 */
class JSGuiScreen internal constructor(
    val moduleName: String,
    private val options: Scriptable
) : Screen(Component.literal(jsTitle(options))) {

    companion object {
        private fun jsTitle(options: Scriptable): String {
            return try {
                val v = options.get("title", options)
                if (v == null || v == Scriptable.NOT_FOUND) "StarredHeltix GUI" else v.toString()
            } catch (e: Exception) {
                "StarredHeltix GUI"
            }
        }
    }

    private val renderFn: Callable? = options.optCallable("render")
    private val onClickFn: Callable? = options.optCallable("onClick")
    private val onKeyFn: Callable? = options.optCallable("onKey")
    private val onCharFn: Callable? = options.optCallable("onChar")
    private val onScrollFn: Callable? = options.optCallable("onScroll")
    private val onCloseFn: Callable? = options.optCallable("onClose")
    private val pauses: Boolean = options.optBoolean("pausesGame", false)

    @Volatile
    private var closed = false

    init {
        (this as net.minecraft.client.gui.components.events.GuiEventListener)
        // pausesGame — в 26.2 экран сам решает; заменяем поле через публичный API нельзя,
        // поэтому просто игнорируем паузу: игра не ставится на паузу, если экран не PauseScreen.
    }

    fun open() {
        JSGuiManager.show(this)
    }

    /** Закрыть без вызова onClose (выгрузка модуля). */
    fun silentClose() {
        closed = true
        val mc = Minecraft.getInstance()
        if (mc.gui.screen() === this) {
            mc.gui.setScreen(null)
        }
    }

    override fun isPauseScreen(): Boolean = pauses

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (renderFn == null || closed) return
        SHScriptEngine.withContext { cx ->
            try {
                val ctx = JSOverlayContext(graphics)
                renderFn.call(
                    cx, SHScriptEngine.globalScope, SHScriptEngine.globalScope,
                    arrayOf(ctx, mouseX, mouseY)
                )
            } catch (e: Throwable) {
                ScriptConsole.error(moduleName, "Ошибка в gui.render: ${e.message}", e)
            }
        }
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        // Полупрозрачный фон как у стандартных экранов
        graphics.fillGradient(0, 0, width, height, 0xC0101010.toInt(), 0xD0101010.toInt())
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        if (onClickFn != null && !closed) {
            val result = callBool(onClickFn, event.x().toInt(), event.y().toInt(), event.button())
            if (result) return true
        }
        return super.mouseClicked(event, isDoubleClick)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.input() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && !closed) {
            minecraft.gui.setScreen(null)
            return true
        }
        if (onKeyFn != null && !closed) {
            val result = callBool(onKeyFn, event.input(), event.scancode())
            if (result) return true
        }
        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (onCharFn != null && !closed) {
            val result = callBool(onCharFn, event.codepoint())
            if (result) return true
        }
        return super.charTyped(event)
    }

    override fun mouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean {
        if (onScrollFn != null && !closed) {
            val result = callBool(onScrollFn, x.toInt(), y.toInt(), scrollY.toInt())
            if (result) return true
        }
        return super.mouseScrolled(x, y, scrollX, scrollY)
    }

    override fun onClose() {
        if (!closed) {
            closed = true
            JSGuiManager.onClose(this)
        }
    }

    internal fun fireOnClose() {
        val fn = onCloseFn ?: return
        SHScriptEngine.withContext { cx ->
            try {
                fn.call(cx, SHScriptEngine.globalScope, SHScriptEngine.globalScope, emptyArray())
            } catch (e: Throwable) {
                ScriptConsole.error(moduleName, "Ошибка в gui.onClose: ${e.message}", e)
            }
        }
    }

    private fun callBool(fn: Callable, vararg args: Any?): Boolean {
        return SHScriptEngine.withContext { cx ->
            try {
                val jsArgs = args.map { org.mozilla.javascript.Context.javaToJS(it, SHScriptEngine.globalScope) }
                val result = fn.call(cx, SHScriptEngine.globalScope, SHScriptEngine.globalScope, jsArgs.toTypedArray())
                org.mozilla.javascript.ScriptRuntime.toBoolean(result)
            } catch (e: Throwable) {
                ScriptConsole.error(moduleName, "Ошибка в gui-колбэке: ${e.message}", e)
                false
            }
        }
    }
}
