package set.starlev.utils

import set.starlev.StarredHeltix
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Ввод чисел с суффиксами и арифметикой.
 *
 * Игрок пишет в поле ввода виртуального «знака» (строка 1 — маркер из `^`) выражение:
 *   `1k` = 1000, `2.5m` = 2 500 000, `1b` = 1 000 000 000,
 *   `10к`, `10м`, `10б` — то же самое кириллицей (для русской раскладки),
 *   `1s` = 64 (стак), `1e` = 160 (зачарованный стак),
 *   `1k+2m*3`, `(1.5k+500)*2` — обычные выражения со скобками.
 * Перед отправкой Enter значение заменяется на посчитанное число.
 *
 * Маркер распознаётся как ЛЮБАЯ строка из крышек (а не только «15 крышек» или «6 крышек»),
 * строки знака чистятся от §/& кодов и Hfont-глифов, подсказки понимаются и по-русски —
 * иначе на Heltix (копия Hypixel со своими строками) фича молча не срабатывала.
 *
 * Почему без последующего форматирования в k/m/b: сервер сам не понимает суффиксы,
 * а наша задача — подставить ровно то число, которое он ожидает. Разделитель тысяч
 * вставляем только там, где сервер его терпит (обычные числовые поля), в цену — нет.
 */
object InputMath {
    private const val STACK_MULTIPLIER = 64L
    private const val ENCHANTED_MULTIPLIER = 160L
    private const val THOUSAND_MULTIPLIER = 1_000L
    private const val MILLION_MULTIPLIER = 1_000_000L
    private const val BILLION_MULTIPLIER = 1_000_000_000L

    /** Число строк в виртуальном знаке — меньше значит это не поле ввода. */
    private const val SIGN_LINE_COUNT = 4

    /** Серверные поля ввода режут длинные значения. */
    private const val MAX_INPUT_LENGTH = 15

    private val suffixPattern = Regex("""\d[sekmbккмб]""", RegexOption.IGNORE_CASE)

    /** Запятая + ровно 3 цифры на границе строки — разделитель тысяч. */
    private val thousandSeparatorPattern = Regex("""(?<=\d),(?=\d{3}(?!\d))""")

    /**
     * Маркер поля ввода — строка 1 виртуального знака. Раньше сравнение шло с тремя
     * хардкод-строками (15 крышек / 6 крышек / `^^Flipping^^`), и на Heltix — это копия
     * Hypixel — число крышек/регистр/цветовые коды отличались, поэтому фича молча
     * выключалась. Теперь берём ЛЮБУЮ строку из одних крышек.
     */
    private val caretMarkerPattern = Regex("""^\^(\^+)$""")
    private val flippingMarkerPattern = Regex("""^\^+(?i:flipping)\^+$""")

    private val formattingRegex = Regex("""(?i)[§&][0-9a-fk-orlnmxz]""")

    /** Подсказки, где вводят ТЕКСТ, а не число — их трогать нельзя. */
    private val textPromptSuffixes = listOf(
        "your", "query", "inventory", "name", "search",
        "введите", "введи", "ввод", "назван", "имя", "поиск", "запрос", "инвентар", "свои"
    )

    /** Подсказки с ценой — там нужны копейки, поэтому scale = 2. */
    private val pricePromptWords = listOf("price", "cost", "цена", "стоимость", "руб")

    /** Знаки конца предложения — «Search your inventory:» заканчивается двоеточием. */
    private const val PROMPT_TRAILING = " \t.:!?;,"

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
        if (!enabled) return null
        return evaluate(messages).first
    }

    /** Разбор поля ввода: значение для подстановки + причина решения. */
    private fun evaluate(messages: Array<String>): Pair<String?, String> {
        if (messages.size < SIGN_LINE_COUNT) return null to "знак из ${messages.size} строк"

        // Маркер ищем по ВСЕМ строкам, а не только по messages[1]
        val markerIndex = messages.indices.firstOrNull { isMarker(messages[it]) }
        if (markerIndex == null) {
            return null to "нет строки-маркера из крышек"
        }

        // Значение и подсказка НЕ обязаны стоять вплотную к маркеру: на Heltix раскладка
        // `[поле | (пусто) | ^^^^^^^^ | подсказка]` — между полем и маркером пустая строка,
        // поэтому берём ближайшую НЕПУСТУЮ строку с каждой стороны, а не «на индекс ±1».
        val valueIndex = (markerIndex - 1 downTo 0).firstOrNull { messages[it].isNotBlank() }
        if (valueIndex == null) return null to "перед маркером нет непустой строки"

        val promptIndex = (markerIndex + 1 until messages.size).firstOrNull { messages[it].isNotBlank() }
            ?: (valueIndex - 1 downTo 0).firstOrNull { messages[it].isNotBlank() }
        if (promptIndex == null) return null to "рядом с маркером нет подсказки"

        val prompt = normalizePrompt(messages[promptIndex])
        val textPrompt = textPromptSuffixes.firstOrNull { prompt.endsWith(it) }
        if (textPrompt != null) return null to "текстовое поле («$prompt»)"

        val raw = messages[valueIndex].trim()
        val number = calculate(raw) ?: return null to "в строке $valueIndex не число/выражение («$raw»)"
        val scale = if (isPricePrompt(messages[promptIndex])) 2 else 0
        val result = number.setScale(scale, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
        if (result.length > MAX_INPUT_LENGTH) return null to "результат длиннее $MAX_INPUT_LENGTH символов"
        return result to "строка $valueIndex («$raw»), подсказка «$prompt»"
    }

    /** Снимает §/& коды и Hfont-глифы — на Heltix строки знака могут быть закодированы. */
    private fun clean(text: String): String {
        val decoded = try {
            set.starlev.utils.detectors.ContainerDetector.decodeTitle(text)
        } catch (_: Throwable) {
            text
        }
        return formattingRegex.replace(decoded, "").trim()
    }

    /**
     * Маркер поля ввода — строка, где после снятия кодов и ПРОБЕЛОВ остались одни крышки.
     * Пробелы убираем потому, что сервер иногда разделяет крышки (`^ ^ ^ ^ ^`).
     */
    private fun isMarker(line: String): Boolean {
        val bare = clean(line).filterNot { it.isWhitespace() }
        if (bare.isEmpty()) return false
        return caretMarkerPattern.matches(bare) || flippingMarkerPattern.matches(bare)
    }

    /** Подсказка в нижнем регистре без хвостовой пунктуации — «Введите имя:» → «введите имя». */
    private fun normalizePrompt(prompt: String): String =
        clean(prompt).lowercase().trimEnd(*PROMPT_TRAILING.toCharArray())

    private fun isPricePrompt(prompt: String): Boolean {
        val lower = normalizePrompt(prompt)
        return pricePromptWords.any { lower.contains(it) }
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
