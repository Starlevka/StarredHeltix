package set.starlev.scripting.api

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import java.net.URI

/**
 * JS API: Текстовый компонент — цвет, стили, клик- и ховер-действия.
 *
 * ```js
 * var t = textComponent("Привет!").color(0x55FF55).bold();
 * t.append(" мир").italic().color(0x5555FF);
 * t.onClick("run_command", "/spawn");
 * t.onHover("Показать подсказку");
 * chat.displayComponent(t);   // или в тултипах/lore
 * ```
 *
 * Цвета: `color(0xRRGGBB)` — точный RGB; `formatting("&a")` / `formatting("gold")` — коды.
 * Клик: "open_url" (http://...), "run_command", "suggest_command", "copy".
 */
class JSTextComponent internal constructor(private val root: MutableComponent) {

    /** Указатель на текущий конец цепочки (для append-стилей). */
    private var tail: MutableComponent = root

    internal val mcComponent: Component get() = root

    internal companion object {
        /** Обёртка существующего Component (для getLoreComponents и внутренних нужд). */
        fun wrap(component: Component): JSTextComponent {
            val mutable = component as? MutableComponent ?: Component.literal("").append(component)
            return JSTextComponent(mutable)
        }
    }

    /** Сборка из строки с §-кодами. */
    internal constructor(text: String) : this(Component.literal("")) {
        appendColored(text)
    }    // ==================== Модификаторы текущего текста ====================

    /** Цвет RGB (0xRRGGBB). */
    fun color(rgb: Int): JSTextComponent {
        tail = tail.withStyle { it.withColor(rgb and 0xFFFFFF) }
        return this
    }

    /** Цвет по коду: "&a"/"§a" или имя ("gold", "dark_purple", "bold"...). */
    fun formatting(code: String): JSTextComponent {
        val clean = code.replace("&", "§").trim()
        if (clean.isEmpty()) return this
        val chatFormatting = ChatFormatting.getByCode(clean[0])
            ?: ChatFormatting.entries.firstOrNull { it.name.equals(clean.removePrefix("§"), ignoreCase = true) }
            ?: return this
        tail = tail.withStyle { it.applyFormat(chatFormatting) }
        return this
    }

    fun bold(): JSTextComponent = applyStyle { it.withBold(true) }
    fun italic(): JSTextComponent = applyStyle { it.withItalic(true) }
    fun underlined(): JSTextComponent = applyStyle { it.withUnderlined(true) }
    fun strikethrough(): JSTextComponent = applyStyle { it.withStrikethrough(true) }
    fun obfuscated(): JSTextComponent = applyStyle { it.withObfuscated(true) }
    fun resetStyle(): JSTextComponent = applyStyle { Style.EMPTY }

    /**
     * Действие по клику:
     *  - "open_url"  + url ("https://...")
     *  - "run_command" + команда ("/spawn")
     *  - "suggest_command" + команда
     *  - "copy" + текст
     */
    fun onClick(action: String, value: String): JSTextComponent {
        val event: ClickEvent? = when (action.lowercase()) {
            "open_url" -> runCatching { ClickEvent.OpenUrl(URI.create(value)) }.getOrNull()
            "run_command" -> ClickEvent.RunCommand(value)
            "suggest_command" -> ClickEvent.SuggestCommand(value)
            "copy", "copy_to_clipboard" -> ClickEvent.CopyToClipboard(value)
            else -> null
        }
        if (event == null) {
            set.starlev.scripting.ScriptConsole.warn(
                "textComponent",
                "Неизвестное клик-действие '$action' (open_url | run_command | suggest_command | copy)"
            )
            return this
        }
        return applyStyle { it.withClickEvent(event) }
    }

    /** Подсказка при наведении (текст с §-кодами или другой компонент). */
    fun onHover(text: String): JSTextComponent = onHover(JSTextComponent(text))
    fun onHover(component: JSTextComponent): JSTextComponent =
        applyStyle { it.withHoverEvent(HoverEvent.ShowText(component.root)) }

    // ==================== Содержимое ====================

    /** Добавить текст (строка с §-кодами) и продолжить модификацию этой части. */
    fun append(text: String): JSTextComponent {
        appendColored(text)
        return this
    }

    /** Добавить другой компонент и продолжить модификацию этой части. */
    fun append(component: JSTextComponent): JSTextComponent {
        root.append(component.root)
        tail = component.root
        return this
    }

    /** Итоговый текст без стилей. */
    fun getString(): String = root.string

    override fun toString(): String = root.string

    private fun appendColored(text: String) {
        if (text.isEmpty()) return
        // Разбивка по §-кодам: каждый сегмент — отдельный literal со своим стилем
        var buffer = StringBuilder()
        var style = Style.EMPTY
        var i = 0
        fun flush() {
            if (buffer.isNotEmpty()) {
                val literal = Component.literal(buffer.toString()).withStyle(style)
                root.append(literal)
                tail = literal
                buffer = StringBuilder()
            }
        }
        while (i < text.length) {
            val c = text[i]
            if ((c == '§' || c == '&') && i + 1 < text.length) {
                val chatFormatting = ChatFormatting.getByCode(text[i + 1])
                if (chatFormatting != null) {
                    flush()
                    // RESET сбрасывает и цвет, и стили (как в игре)
                    style = if (chatFormatting == ChatFormatting.RESET) {
                        Style.EMPTY
                    } else {
                        style.applyFormat(chatFormatting)
                    }
                    i += 2
                    continue
                }
            }
            if (c == '\n') {
                flush()
                root.append(Component.literal("\n"))
                i++
                continue
            }
            buffer.append(c)
            i++
        }
        flush()
    }

    private fun applyStyle(op: (Style) -> Style): JSTextComponent {
        tail = tail.withStyle { op(it) }
        return this
    }
}

/**
 * Фабрика глобального `textComponent`:
 * ```js
 * var t = textComponent("Привет").color(0x55FF55);
 * var l = textComponent.literal("ключ");
 * var tr = textComponent.translatable("item.minecraft.diamond");
 * ```
 */
object JSTextComponentFactory {
    private val shim = JSTextComponent(Component.literal(""))

    /** Из строки с §-кодами. */
    fun of(text: String): JSTextComponent = JSTextComponent(text)

    /** Пустой компонент (собирайте через append). */
    fun empty(): JSTextComponent = JSTextComponent(Component.literal(""))

    /** Локальный literal-текст (без разбора §-кодов; стили — методами). */
    fun literal(text: String): JSTextComponent = JSTextComponent(Component.literal(text))

    /** Переводимый ключ (локализуется клиентом). */
    fun translatable(key: String): JSTextComponent = JSTextComponent(Component.translatable(key))

    /** Переводимый ключ с fallback-текстом, если ключ не найден. */
    fun translatable(key: String, fallback: String): JSTextComponent =
        JSTextComponent(Component.translatableWithFallback(key, fallback))
}
