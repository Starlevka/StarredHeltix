package set.starlev.scripting.api

import org.mozilla.javascript.Callable
import org.mozilla.javascript.Scriptable
import set.starlev.scripting.CommandArgsSchema
import set.starlev.scripting.SHScriptEngine

/**
 * JS API: Динамическая регистрация команд с реальными Brigadier-узлами,
 * таб-подсказками и опциональной схемой типизированных аргументов.
 *
 * ```js
 * // Простая форма (как раньше): args — массив сырых строк
 * command.register("mycmd", function(args) { ... });
 *
 * // Схема аргументов: парсинг типов + таб-подсказки
 * command.register("give", function(args) {
 *     // args[0] — число, args[1] — строка из подсказок
 * }, { args: [
 *     { name: "count", type: "int", min: 1, max: 64 },
 *     { name: "mode",  type: "choice", options: ["on", "off"] },
 *     { name: "rest",  type: "greedy" }
 * ]});
 * ```
 *
 * Типы: int (min/max), float (min/max), bool, word, string (в кавычках),
 * greedy (только последним), choice (options).
 */
class JSCommand(private val moduleName: String) {

    fun register(name: String, callable: Callable) {
        SHScriptEngine.registerCommand(name, moduleName, callable, null)
    }

    fun register(name: String, callable: Callable, options: Scriptable?) {
        val schema = CommandArgsSchema.parse(options, moduleName, name)
        SHScriptEngine.registerCommand(name, moduleName, callable, schema)
    }

    fun unregister(name: String) {
        SHScriptEngine.unregisterCommand(name)
    }
}

