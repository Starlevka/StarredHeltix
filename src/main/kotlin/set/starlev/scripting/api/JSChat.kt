package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.chat.GuiMessage
import net.minecraft.network.chat.Component
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.regexp.NativeRegExp
import set.starlev.injections.accessors.ChatComponentAccessor
import set.starlev.scripting.SHScriptEngine

/**
 * JS API: Объект `chat`
 */
class JSChat {
    private val client: Minecraft get() = Minecraft.getInstance()

    fun send(message: String) {
        client.execute {
            client.player?.connection?.sendChat(message)
        }
    }

    fun command(commandText: String) {
        client.execute {
            val cmd = if (commandText.startsWith("/")) commandText.substring(1) else commandText
            client.player?.connection?.sendCommand(cmd)
        }
    }

    fun display(message: String) {
        val colored = addColor(message)
        client.execute {
            client.gui.hud.chat.addClientSystemMessage(Component.literal(colored))
        }
    }

    /** Локальное сообщение из JSTextComponent (со стилями/кликами/ховерами). */
    fun displayComponent(component: JSTextComponent) {
        client.execute {
            client.gui.hud.chat.addClientSystemMessage(component.mcComponent)
        }
    }

    /** Фейковое клиентское сообщение (не отправляется на сервер). Аналог ChatLib.simulateChat. */
    fun simulate(message: String) = display(message)

    fun actionBar(message: String) {
        val colored = addColor(message)
        client.execute {
            client.gui.hud.setOverlayMessage(Component.literal(colored), false)
        }
    }

    fun addColor(text: String): String {
        return text.replace('&', '§')
    }

    fun cleanColor(text: String): String {
        return text.replace(Regex("§[0-9a-fk-orlnmxz]"), "")
    }

    fun clear() {
        client.execute {
            client.gui.hud.chat.clearMessages(false)
        }
    }

    // ==================== Редактирование/удаление истории чата ====================

    /**
     * Все видимые строки чата (от старых к новым, как читаются на экране).
     * Аналог ChatLib.getChatLines().
     */
    fun getLines(): List<String> {
        val messages = (client.gui.hud.chat as ChatComponentAccessor).allMessages
        // allMessages хранится newest-first → разворачиваем для естественного порядка
        return messages.asReversed().map { it.content().string }
    }

    /** Текущая ширина чата в пикселях (аналог ChatLib.getChatWidth()). */
    fun getChatWidth(): Int {
        return try {
            net.minecraft.client.gui.components.ChatComponent.getWidth(client.options.chatWidth().get())
        } catch (e: Exception) {
            320
        }
    }

    /**
     * Редактировать сообщения в истории чата. Возвращает число изменённых строк.
     * @param pattern подстрока (содержится в строке) или JS-RegExp (глобальная замена)
     * @param replacements тексты-замены (склеиваются); & конвертируется в §
     *
     * ```js
     * chat.edit("TPS низкий", "§eTPS низкий");
     * chat.edit(/спам{2,}/g, "спам");
     * ```
     */
    fun edit(pattern: Any, vararg replacements: String): Int {
        val reps = replacements.map { addColor(it) }
        if (reps.isEmpty()) {
            set.starlev.scripting.ScriptConsole.warn("chat", "chat.edit: не переданы replacements")
            return 0
        }

        // Сопоставление — сразу (внутри JS-контекста), мутирование — на клиентском потоке
        val messages = (client.gui.hud.chat as ChatComponentAccessor).allMessages
        val edited = ArrayList<Pair<Int, String>>()
        for (i in messages.indices) {
            val original = messages[i].content().string
            val replaced = when (pattern) {
                is NativeRegExp -> regexReplace(original, pattern, reps) ?: continue
                else -> {
                    val needle = pattern.toString()
                    if (!original.contains(needle)) continue
                    original.replace(needle, reps.joinToString(""))
                }
            }
            if (replaced != original) edited += i to replaced
        }
        if (edited.isEmpty()) return 0

        client.execute {
            try {
                val acc = client.gui.hud.chat as ChatComponentAccessor
                val list = acc.allMessages
                for ((index, text) in edited) {
                    if (index >= list.size) continue
                    val m = list[index]
                    list[index] = GuiMessage(m.addedTime(), Component.literal(text), null, m.source(), m.tag())
                }
                acc.shRefreshTrimmedMessages()
            } catch (e: Exception) {
                set.starlev.scripting.ScriptConsole.error("chat", "chat.edit: ${e.message}", e)
            }
        }
        return edited.size
    }

    /**
     * Удалить сообщения из истории чата по подстроке или JS-RegExp.
     * Возвращает число удалённых строк.
     *
     * ```js
     * chat.delete("+ >> ");
     * chat.delete(/^Продано:/);
     * ```
     */
    fun delete(pattern: Any): Int {
        val messages = (client.gui.hud.chat as ChatComponentAccessor).allMessages
        val toRemove = HashSet<Int>()
        for (i in messages.indices) {
            val text = messages[i].content().string
            val matches = when (pattern) {
                is NativeRegExp -> regexMatches(pattern, text)
                else -> text.contains(pattern.toString())
            }
            if (matches) toRemove += i
        }
        if (toRemove.isEmpty()) return 0

        client.execute {
            try {
                val acc = client.gui.hud.chat as ChatComponentAccessor
                val list = acc.allMessages
                val kept = ArrayList<GuiMessage>(list.size)
                for (i in list.indices) if (i !in toRemove) kept += list[i]
                list.clear()
                list.addAll(kept)
                acc.shRefreshTrimmedMessages()
            } catch (e: Exception) {
                set.starlev.scripting.ScriptConsole.error("chat", "chat.delete: ${e.message}", e)
            }
        }
        return toRemove.size
    }

    // ==================== RegExp-хелперы ====================

    private fun callRegex(pattern: NativeRegExp, text: String): Any? {
        return SHScriptEngine.withContext { cx ->
            try {
                // Сброс lastIndex - глобальный /g-regex не должен продолжать поиск с прошлой позиции
                pattern.put("lastIndex", pattern, 0)
                // В Rhino 1.9.1 NativeRegExp не реализует Callable — вызываем через
                // статический regExpExec (эквивалент JS regex.exec(text))
                org.mozilla.javascript.regexp.NativeRegExp.regExpExec(
                    pattern, text, cx, SHScriptEngine.globalScope
                )
            } catch (e: Throwable) {
                null
            }
        }
    }

    private fun regexMatches(pattern: NativeRegExp, text: String): Boolean {
        val result = callRegex(pattern, text) ?: return false
        return result !== org.mozilla.javascript.Undefined.instance
    }

    /**
     * Глобальная замена через exec-цикл. Возвращает null, если совпадений нет.
     * Замена — литеральный текст (группы $1 не поддерживаются — используйте match из JS).
     */
    private fun regexReplace(text: String, pattern: NativeRegExp, reps: List<String>): String? {
        var result = text
        var searchFrom = 0
        var guard = 0
        var matchedAny = false
        while (guard++ < 200) {
            if (searchFrom > result.length) break
            val rest = result.substring(searchFrom)
            val match = callRegex(pattern, rest) ?: break
            val sc = match as? Scriptable ?: break
            val matched = Context.toString(sc.get(0, sc))
            val index = (sc.get("index", sc) as? Number)?.toInt() ?: break
            if (matched.isEmpty()) {
                // Нулевая длина — сдвигаемся, чтобы не зациклиться
                searchFrom += index + 1
                continue
            }
            val replacement = reps.joinToString("")
            val absStart = searchFrom + index
            result = result.substring(0, absStart) + replacement + result.substring(absStart + matched.length)
            searchFrom = absStart + replacement.length
            matchedAny = true
        }
        return if (matchedAny) result else null
    }
}
