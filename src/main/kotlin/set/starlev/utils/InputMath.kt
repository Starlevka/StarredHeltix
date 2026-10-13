package set.starlev.utils

import set.starlev.StarredHeltix
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Ввод чисел с суффиксами и арифметикой — порт из Skysoft (`utils/SkyBlockMath.kt` +
 * `features/misc/InputMath.kt`).
 *
 * Игрок пишет в поле ввода Hypixel (виртуальный «знак» с `^^^^` в строке 1) выражение:
 *   `1k` = 1000, `2.5m` = 2 500 000, `1b` = 1 000 000 000,
 *   `10к`, `10м`, `10б` — то же самое кириллицей (для русской раскладки),
 *   `1s` = 64 (стак), `1e` = 160 (зачарованный стак),
 *   `1k+2m*3`, `(1.5k+500)*2` — обычные выражения со скобками.
 * Перед отправкой Enter значение заменяется на посчитанное число.
 *
 * Почему без последующего форматирования в k/m/b: Hypixel сам не понимает суффиксы,
 * а наша задача — подставить ровно то число, которое он ожидает. Разделитель тысяч
 * вставляем только там, где сервер его терпит (обычные числовые поля), в цену — нет.
 */
object InputMath {
    private const val STACK_MULTIPLIER = 64L
    private const val ENCHANTED_MULTIPLIER = 160L
    private const val THOUSAND_MULTIPLIER = 1_000L
    private const val MILLION_MULTIPLIER = 1_000_000L
    private const val BILLION_MULTIPLIER = 1_000_000_000L

    /** Hypixel-подсказка в строке 1: 15 или 6 «крышек», либо маркер флиппинга. */
    private const val FULL_INPUT_MARKER = "^^^^^^^^^^^^^^^"
    private const val SHORT_INPUT_MARKER = "^^^^^^"
    private const val FLIPPING_INPUT_MARKER = "^^Flipping^^"
    private const val SET_NAME_PROMPT = "Enter name"

    /** Число строк в виртуальном знаке — меньше значит это не поле ввода. */
    private const val SIGN_LINE_COUNT = 4

    /** Серверные поля ввода режут длинные значения. */
    private const val MAX_INPUT_LENGTH = 15

    private val suffixPattern = Regex("""\d[sekmbккмб]""", RegexOption.IGNORE_CASE)

    /** Запятая + ровно 3 цифры на границе строки — разделитель тысяч. */
    private val thousandSeparatorPattern = Regex("""(?<=\d),(?=\d{3}(?!\d))""")

    private val enabled: Boolean
        get() = try {
            StarredHeltix.feature.skyblock.inputMath.enabled
        } catch (_: Throwable) {
            false
        }

    /**
     * Есть ли в поле ввода число, которое можно посчитать.
     * @return новая строка для `messages[0]` или null, если считать нечего.
     */
    fun compile(messages: Array<String>): String? {
        if (!enabled || !isNumberInput(messages)) return null
        val result = calculate(messages[0]) ?: return null
        // Цена принимается с копейками, обычные количества — целые.
        val scale = if (messages[2].contains("price", ignoreCase = true)) 2 else 0
        return result.setScale(scale, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
            .takeIf { it.length <= MAX_INPUT_LENGTH }
    }

    /** Проверяет структуру поля ввода: только поля с «крышками» Hypixel. */
    private fun isNumberInput(messages: Array<String>): Boolean {
        if (messages.size < SIGN_LINE_COUNT) return false
        val prompt = messages[2]
        return when (messages[1]) {
            FULL_INPUT_MARKER -> !prompt.endsWith("your") && !prompt.endsWith("query")
            SHORT_INPUT_MARKER -> !prompt.endsWith("your") && prompt != SET_NAME_PROMPT
            FLIPPING_INPUT_MARKER -> true
            else -> false
        }
    }

    /**
     * Считает выражение. Возвращает null, если это не число и нет операторов/суффиксов
     * (например, обычное слово или «просто число» — их не трогаем).
     */
    fun calculate(expression: String): BigDecimal? {
        val normalized = normalizeSeparators(expression)
        if (normalized.isEmpty()) return null
        if (normalized.none { it in "+-*/xX" } && !suffixPattern.containsMatchIn(normalized)) return null
        return try {
            ExpressionParser(normalized).parse()
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Разделители тысяч и десятичные запятые. `1,500` → `1500` (US-разделитель), но
     * `1,5` → `1.5` (русская десятичная запятая). Запятая считается разделителем тысяч
     * только когда за ней ровно 3 цифры.
     */
    private fun normalizeSeparators(expression: String): String {
        val trimmed = expression.trim()
        if (',' !in trimmed) return trimmed
        // Запятая перед ровно 3 цифрами — разделитель тысяч (убираем), остальные — десятичные.
        return trimmed.replace(thousandSeparatorPattern, "").replace(',', '.')
    }

    /** Форматирует результат: с разделителем тысяч (`12,345`) или без. */
    fun format(value: BigDecimal, grouped: Boolean = true): String = DecimalFormat(
        if (grouped) "#,##0.##########" else "0.##########",
        DecimalFormatSymbols(Locale.US)
    ).format(value)

    /** Рекурсивный спуск: expr := term (('+'|'-') term)*, term := factor (('*'|'/'|'x') factor)*. */
    private class ExpressionParser(private val expression: String) {
        private var index = 0

        fun parse(): BigDecimal {
            val result = parseExpression()
            skipSpaces()
            check(index == expression.length) { "лишние символы в выражении" }
            return result
        }

        private fun parseExpression(): BigDecimal {
            var result = parseTerm()
            while (true) {
                result = when (nextOperator('+', '-')) {
                    '+' -> result + parseTerm()
                    '-' -> result - parseTerm()
                    else -> return result
                }
            }
        }

        private fun parseTerm(): BigDecimal {
            var result = parseFactor()
            while (true) {
                result = when (nextOperator('*', 'x', 'X', '/')) {
                    '*', 'x', 'X' -> result * parseFactor()
                    '/' -> result.divide(parseFactor(), MathContext.DECIMAL64)
                    else -> return result
                }
            }
        }

        private fun parseFactor(): BigDecimal {
            skipSpaces()
            return when (expression.getOrNull(index)) {
                '+' -> { index++; parseFactor() }
                '-' -> { index++; -parseFactor() }
                '(' -> {
                    index++
                    val result = parseExpression()
                    skipSpaces()
                    check(expression.getOrNull(index++) == ')') { "нет закрывающей скобки" }
                    result
                }
                else -> parseNumber()
            }
        }

        /** Число с необязательным суффиксом: 1.5k, 64s, 10м, 2b. */
        private fun parseNumber(): BigDecimal {
            skipSpaces()
            val start = index
            while (expression.getOrNull(index)?.let { it.isDigit() || it == '.' } == true) index++
            check(index > start) { "ожидалось число" }
            val value = expression.substring(start, index).toBigDecimal()
            val multiplier = multiplierFor(expression.getOrNull(index))
            if (multiplier != null) index++
            return value * BigDecimal.valueOf(multiplier ?: 1L)
        }

        private fun multiplierFor(char: Char?): Long? = when (char?.lowercaseChar()) {
            's' -> STACK_MULTIPLIER
            'e' -> ENCHANTED_MULTIPLIER
            'k', 'к' -> THOUSAND_MULTIPLIER
            'm', 'м' -> MILLION_MULTIPLIER
            'b', 'б' -> BILLION_MULTIPLIER
            else -> null
        }

        private fun nextOperator(vararg operators: Char): Char? {
            skipSpaces()
            val operator = expression.getOrNull(index)?.takeIf { it in operators } ?: return null
            index++
            return operator
        }

        private fun skipSpaces() {
            while (expression.getOrNull(index)?.isWhitespace() == true) index++
        }
    }
}
