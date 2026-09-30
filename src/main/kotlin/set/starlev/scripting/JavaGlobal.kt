package set.starlev.scripting

import org.mozilla.javascript.Context

/**
 * JS API: Глобальный объект `Java` — Nashorn-стиль доступ к разрешённым Java-классам.
 *
 * ```js
 * var Vector3f = Java.type("org.joml.Vector3f");
 * var v = new Vector3f(1, 2, 3);
 *
 * var ClientTickEvents = Java.type("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents");
 * ```
 *
 * Разрешены только пакеты из белого списка SHClassShutter:
 * net.fabricmc.fabric.api.*, net.fabricmc.api.*, org.joml.*,
 * io.github.notenoughupdates.moulconfig.*, java.*, kotlin.*.
 * net.minecraft.* — заблокирован намеренно.
 */
object JavaGlobal {

    /**
     * Классы/префиксы, которые запрещены через `Java.type`, но НЕ блокируются в SHClassShutter.
     *
     * Почему не в шаттере: движок сам инжектит эти объекты в глобал-скоуп и возвращает их наружу
     * (JavaGlobal — глобал `Java`, SHTrigger — значение из events.onTick, ScriptConsole и т.д.).
     * ClassShutter вызывается и при WRAPe объекта для скрипта, поэтому блокировка этих классов в
     * шаттере роняла клиент на старте и ломала подписки. Запрет здесь закрывает именно загрузку
     * класса по имени; `load()/importClass()/Packages` уже удалены, так что Java.type — единственный
     * путь к классу, и этого достаточно.
     */
    private val deniedClasses = setOf(
        "java.lang.Class",
        "java.lang.ClassLoader",
        "java.lang.Module",
        "java.lang.ModuleLayer",
        // Внутренности движка: руление движком/чужими модулями, eval в чужом скоупе, запись чужих конфигов.
        "set.starlev.scripting.",
    )

    private fun isDenied(name: String): Boolean =
        deniedClasses.any { name == it || name.startsWith("$it.") }

    /** Загрузить класс по имени. Возвращает NativeJavaClass (можно `new Cls(...)`). */
    fun type(name: String): Any? {
        val clean = name.trim()
        if (clean.isEmpty()) return null
        if (isDenied(clean)) {
            ScriptConsole.warn("Java", "Класс '$clean' запрещён песочницей (см. docs/scripting-api.md, раздел Java)")
            return null
        }
        if (!SHClassShutter.visibleToScripts(clean)) {
            ScriptConsole.warn("Java", "Класс '$clean' запрещён песочницей (см. docs/scripting-api.md, раздел Java)")
            return null
        }
        return try {
            val cls: Class<*> = Class.forName(clean)
            // NativeJavaClass через Context.javaToJS — можно `new Cls(...)`, `Cls.staticMethod()`
            SHScriptEngine.withContext { cx ->
                Context.javaToJS(cls, SHScriptEngine.globalScope)
            }
        } catch (e: ClassNotFoundException) {
            ScriptConsole.warn("Java", "Класс '$clean' не найден (вложенные классы — через \$: 'foo.Bar\$Baz')")
            null
        } catch (e: Throwable) {
            ScriptConsole.error("Java", "Ошибка загрузки класса '$clean': ${e.message}", e)
            null
        }
    }

    /** Проверка: разрешён ли класс песочницей (без загрузки). */
    fun isAllowed(name: String): Boolean {
        val clean = name.trim()
        return !isDenied(clean) && SHClassShutter.visibleToScripts(clean)
    }
}
