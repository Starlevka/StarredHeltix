package set.starlev.scripting

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.UniqueTag

/**
 * Типизированная схема аргументов JS-команды.
 *
 * ```js
 * command.register("give", function(args) { ... }, { args: [
 *     { name: "count", type: "int", min: 1, max: 64 },
 *     { name: "mode",  type: "choice", options: ["on", "off"] },
 *     { name: "rest",  type: "greedy" }
 * ]});
 * ```
 *
 * Типы: int (min/max), float (min/max), bool, word, string (в кавычках),
 * greedy (только последним аргументом), choice (options — таб-подсказки).
 *
 * Парсинг ошибок схемы — безопасный: при проблемах команда регистрируется
 * в простом режиме (массив сырых строк) с warn в лог.
 */
data class CommandArg(
    val name: String,
    val type: String,
    val min: Double? = null,
    val max: Double? = null,
    val choices: List<String>? = null
) {
    /** Brigadier-тип аргумента. */
    fun argumentType(): ArgumentType<*> = when (type) {
        "int" -> {
            val lo = min?.toInt() ?: Int.MIN_VALUE
            val hi = max?.toInt() ?: Int.MAX_VALUE
            IntegerArgumentType.integer(lo, hi)
        }
        "float" -> {
            val lo = min ?: -Double.MAX_VALUE
            val hi = max ?: Double.MAX_VALUE
            DoubleArgumentType.doubleArg(lo, hi)
        }
        "bool" -> BoolArgumentType.bool()
        "word" -> StringArgumentType.word()
        "greedy" -> StringArgumentType.greedyString()
        else -> StringArgumentType.string() // "string" и "choice" (выбор валидируем сами)
    }

    /** Извлечь значение из контекста в JS-совместимом виде. */
    fun getValue(ctx: CommandContext<FabricClientCommandSource>): Any? = when (type) {
        "int" -> IntegerArgumentType.getInteger(ctx, name)
        "float" -> DoubleArgumentType.getDouble(ctx, name)
        "bool" -> BoolArgumentType.getBool(ctx, name)
        else -> StringArgumentType.getString(ctx, name)
    }
}

data class CommandArgsSchema(val args: List<CommandArg>) {

    companion object {
        /** Разбор схемы из JS-объекта { args: [...] }. null — простая строковая команда. */
        fun parse(options: Scriptable?, moduleName: String, commandName: String): CommandArgsSchema? {
            if (options == null) return null
            return try {
                val argsObj = options.get("args", options)
                if (argsObj !is Scriptable || argsObj == UniqueTag.NOT_FOUND) return null
                val args = mutableListOf<CommandArg>()
                val count = (argsObj.get("length", argsObj) as? Number)?.toInt() ?: 0
                for (i in 0 until count) {
                    val item = argsObj.get(i, argsObj)
                    if (item !is Scriptable || item == UniqueTag.NOT_FOUND) continue
                    val name = (item.get("name", item) as? String)?.takeIf { it.isNotBlank() } ?: "arg$i"
                    val type = (item.get("type", item) as? String)?.lowercase() ?: "string"
                    val min = (item.get("min", item) as? Number)?.toDouble()
                    val max = (item.get("max", item) as? Number)?.toDouble()
                    val choicesObj = item.get("options", item)
                    val choices = if (choicesObj is Scriptable && choicesObj != UniqueTag.NOT_FOUND) {
                        val n = (choicesObj.get("length", choicesObj) as? Number)?.toInt() ?: 0
                        (0 until n).mapNotNull { choicesObj.get(it, choicesObj)?.toString() }
                    } else null
                    var resolvedType = type
                    // greedy — только последним аргументом
                    if (resolvedType == "greedy" && i != count - 1) {
                        ScriptConsole.warn(moduleName, "command.register('/$commandName'): 'greedy' ('$name') допустим только последним — используется string")
                        resolvedType = "string"
                    }
                    // choice без options → word
                    if (resolvedType == "choice" && choices.isNullOrEmpty()) {
                        ScriptConsole.warn(moduleName, "command.register('/$commandName'): 'choice' ('$name') без options — используется word")
                        resolvedType = "word"
                    }
                    args.add(CommandArg(name, resolvedType, min, max, choices))
                }
                if (args.isEmpty()) null else CommandArgsSchema(args)
            } catch (e: Throwable) {
                ScriptConsole.warn(
                    moduleName,
                    "Схема аргументов команды '/$commandName' не разобрана (${e.message}) — используется простой режим"
                )
                null
            }
        }
    }
}
