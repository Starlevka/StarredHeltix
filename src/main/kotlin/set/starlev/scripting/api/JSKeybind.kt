package set.starlev.scripting.api

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import org.mozilla.javascript.Callable
import org.mozilla.javascript.Context
import set.starlev.scripting.SHScriptEngine
import set.starlev.scripting.SHTrigger
import org.lwjgl.glfw.GLFW

/**
 * JS API: Объект `keybind` — бинды клавиш, привязанные к модулю.
 *
 * В отличие от events.onKey, колбэк вызывается только при НАЖАТИИ конкретной
 * клавиши (повторы/отпускание не вызываются). Бинды автоматически снимаются
 * при выгрузке модуля.
 *
 * ```js
 * keybind.register("R", function() { chat.log("Нажата R"); });
 * keybind.register("LEFT_SHIFT", function() { ... });
 * keybind.unregister("R");
 * ```
 */
class JSKeybind(private val moduleName: String) {

    private val bindings = HashMap<String, SHTrigger>() // keyName(lower) -> trigger

    /** Зарегистрировать колбэк на нажатие клавиши. Имя: "R", "F5", "LEFT_SHIFT", "KP_0" или число GLFW. */
    fun register(keyName: String, callable: Callable): Boolean {
        val keyCode = resolveKeyCode(keyName)
        if (keyCode == -1) return false
        unregister(keyName)

        val trigger = SHTrigger(moduleName, "key", Callable { cx, scope, thisObj, args ->
            // args: (key, scancode, action, modifiers) — action 1 = нажатие
            val action = args.getOrNull(2) as? Number ?: return@Callable null
            val key = args.getOrNull(0) as? Number ?: return@Callable null
            if (action.toInt() == GLFW.GLFW_PRESS && key.toInt() == keyCode) {
                callable.call(cx, scope, thisObj, emptyArray())
            }
            null
        })
        SHScriptEngine.registerTrigger(trigger)
        bindings[keyName.lowercase()] = trigger
        return true
    }

    /** Снять бинд клавиши. */
    fun unregister(keyName: String) {
        bindings.remove(keyName.lowercase())?.unregister()
    }

    /** Снять все бинды модуля (используется при ручной очистке). */
    fun unregisterAll() {
        bindings.values.toList().forEach { it.unregister() }
        bindings.clear()
    }

    /**
     * Опрос: удерживается ли клавиша ПРЯМО СЕЙЧАС (не зависит от игровых биндов —
     * опрос через GLFW, работает даже при подавленном вводе игрока во фрикафе).
     * Имя: "W", "SPACE", "LEFT_SHIFT", "KP_0" или число GLFW.
     */
    fun isDown(keyName: String): Boolean {
        val keyCode = resolveKeyCode(keyName)
        if (keyCode == -1) return false
        val mc = Minecraft.getInstance()
        return InputConstants.isKeyDown(mc.window, keyCode)
    }

    companion object {
        /** Разбор имени клавиши в GLFW-код: буквы, цифры, F1-F12, модификаторы и спецклавиши. */
        fun resolveKeyCode(name: String): Int {
            val n = name.trim().uppercase().removePrefix("GLFW_KEY_")
            n.toIntOrNull()?.let { return it }
            if (n.length == 1) {
                val c = n[0]
                if (c in 'A'..'Z') return GLFW.GLFW_KEY_A + (c - 'A')
                if (c in '0'..'9') return GLFW.GLFW_KEY_0 + (c - '0')
            }
            if (n.startsWith("F") && n.drop(1).toIntOrNull() in 1..12) {
                return GLFW.GLFW_KEY_F1 + (n.drop(1).toInt() - 1)
            }
            return when (n) {
                "SPACE" -> GLFW.GLFW_KEY_SPACE
                "ENTER", "RETURN" -> GLFW.GLFW_KEY_ENTER
                "TAB" -> GLFW.GLFW_KEY_TAB
                "ESCAPE", "ESC" -> GLFW.GLFW_KEY_ESCAPE
                "BACKSPACE" -> GLFW.GLFW_KEY_BACKSPACE
                "LEFT_SHIFT", "SHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT
                "RIGHT_SHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT
                "LEFT_CONTROL", "CTRL" -> GLFW.GLFW_KEY_LEFT_CONTROL
                "RIGHT_CONTROL" -> GLFW.GLFW_KEY_RIGHT_CONTROL
                "LEFT_ALT", "ALT" -> GLFW.GLFW_KEY_LEFT_ALT
                "RIGHT_ALT" -> GLFW.GLFW_KEY_RIGHT_ALT
                "UP" -> GLFW.GLFW_KEY_UP
                "DOWN" -> GLFW.GLFW_KEY_DOWN
                "LEFT" -> GLFW.GLFW_KEY_LEFT
                "RIGHT" -> GLFW.GLFW_KEY_RIGHT
                "DELETE" -> GLFW.GLFW_KEY_DELETE
                "INSERT" -> GLFW.GLFW_KEY_INSERT
                "HOME" -> GLFW.GLFW_KEY_HOME
                "END" -> GLFW.GLFW_KEY_END
                "PAGE_UP" -> GLFW.GLFW_KEY_PAGE_UP
                "PAGE_DOWN" -> GLFW.GLFW_KEY_PAGE_DOWN
                "KP_0" -> GLFW.GLFW_KEY_KP_0
                "KP_1" -> GLFW.GLFW_KEY_KP_1
                "KP_2" -> GLFW.GLFW_KEY_KP_2
                "KP_3" -> GLFW.GLFW_KEY_KP_3
                "KP_4" -> GLFW.GLFW_KEY_KP_4
                "KP_5" -> GLFW.GLFW_KEY_KP_5
                "KP_6" -> GLFW.GLFW_KEY_KP_6
                "KP_7" -> GLFW.GLFW_KEY_KP_7
                "KP_8" -> GLFW.GLFW_KEY_KP_8
                "KP_9" -> GLFW.GLFW_KEY_KP_9
                "MINUS" -> GLFW.GLFW_KEY_MINUS
                "EQUAL" -> GLFW.GLFW_KEY_EQUAL
                "LEFT_BRACKET" -> GLFW.GLFW_KEY_LEFT_BRACKET
                "RIGHT_BRACKET" -> GLFW.GLFW_KEY_RIGHT_BRACKET
                "SEMICOLON" -> GLFW.GLFW_KEY_SEMICOLON
                "COMMA" -> GLFW.GLFW_KEY_COMMA
                "PERIOD" -> GLFW.GLFW_KEY_PERIOD
                "SLASH" -> GLFW.GLFW_KEY_SLASH
                "BACKSLASH" -> GLFW.GLFW_KEY_BACKSLASH
                else -> -1
            }
        }
    }
}
