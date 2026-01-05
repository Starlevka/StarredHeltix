package set.starlev.secret.features.ai

import java.util.Stack
import kotlin.math.pow

object AiMath {
    // Улучшенное регулярное выражение для поиска математических выражений
    // Поддерживает: числа с . и ,, пробелы, операторы +, -, *, /, ^, x, скобки
    private val mathRegex = Regex("""(?<=\s|^|\b)(?:\(?\s*\d+[\.,]?\d*\s*\)?[\s\+\-\*\/\^x]*)+(?=\s|$|\?|=|\b)""")

    fun trySolve(message: String): String? {
        val lowerMessage = message.lowercase()
        
        // Очищаем сообщение от триггеров обращения, чтобы не мешали регексу
        var cleanMessage = lowerMessage
        AiConfig.TRIGGERS.forEach { cleanMessage = cleanMessage.replace(it, "").trim() }
        
        val isMathQuestion = lowerMessage.contains("сколько") || 
                            lowerMessage.contains("посчитай") || 
                            lowerMessage.contains("реши") || 
                            lowerMessage.contains("вычисли") || 
                            lowerMessage.contains("=") ||
                            lowerMessage.contains("?") ||
                            lowerMessage.contains("+") ||
                            lowerMessage.contains("-") ||
                            lowerMessage.contains("*") ||
                            lowerMessage.contains("/") ||
                            lowerMessage.contains("^")
        
        if (!isMathQuestion) {
            // Если нет ключевых слов, проверяем просто наличие выражения
            if (!mathRegex.containsMatchIn(cleanMessage)) return null
        }

        var expression = extractExpression(cleanMessage) ?: return null
        
        // Очистка от '=' в конце если есть
        if (expression.endsWith("=")) expression = expression.dropLast(1).trim()
        if (expression.endsWith("?")) expression = expression.dropLast(1).trim()
        
        // Поддержка записи 139(59*65) -> 139*(59*65)
        // И замена ** на ^ для возведения в степень
        var cleanExpression = expression.replace("x", "*").replace(" ", "")
        cleanExpression = cleanExpression.replace("**", "^")
        cleanExpression = addMissingMultiplication(cleanExpression)
        
        return try {
            val result = evaluate(cleanExpression)
            // Форматирование результата: убираем лишние нули если целое, иначе 2 знака после запятой
            val formattedResult = if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                "%.2f".format(java.util.Locale.US, result)
            }
            formattedResult
        } catch (e: Exception) {
            null
        }
    }

    private fun addMissingMultiplication(expression: String): String {
        val sb = StringBuilder()
        for (i in expression.indices) {
            sb.append(expression[i])
            if (i < expression.length - 1) {
                val current = expression[i]
                val next = expression[i + 1]
                // Число перед скобкой: 139( -> 139*(
                if (current.isDigit() && next == '(') {
                    sb.append('*')
                }
                // Скобка перед числом: )2 -> )*2
                if (current == ')' && next.isDigit()) {
                    sb.append('*')
                }
                // Скобка перед скобкой: )( -> )*(
                if (current == ')' && next == '(') {
                    sb.append('*')
                }
            }
        }
        return sb.toString()
    }

    private fun extractExpression(message: String): String? {
        val match = mathRegex.find(message)
        return match?.value?.trim()
    }

    private fun evaluate(expression: String): Double {
        val tokens = tokenize(expression)
        return parseExpression(tokens)
    }

    private fun tokenize(expression: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < expression.length) {
            val c = expression[i]
            when {
                c.isDigit() || c == '.' || c == ',' -> {
                    val sb = StringBuilder()
                    while (i < expression.length && (expression[i].isDigit() || expression[i] == '.' || expression[i] == ',')) {
                        sb.append(expression[i])
                        i++
                    }
                    val numStr = sb.toString()
                    result.add(normalizeNumber(numStr))
                    i--
                }
                c.isLetter() -> {
                    val sb = StringBuilder()
                    while (i < expression.length && expression[i].isLetter()) {
                        sb.append(expression[i])
                        i++
                    }
                    val func = sb.toString().lowercase()
                    when (func) {
                        "pi" -> result.add(Math.PI.toString())
                        "e" -> result.add(Math.E.toString())
                        "sqrt", "sin", "cos", "tan", "log", "abs" -> result.add(func)
                        else -> {} // Ignore unknown letters
                    }
                    i--
                }
                c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == ')' || c == '^' -> {
                    result.add(c.toString())
                }
            }
            i++
        }
        return result
    }

    private fun normalizeNumber(s: String): String {
        if (s.isEmpty()) return "0"
        
        val dots = s.count { it == '.' }
        val commas = s.count { it == ',' }
        
        return when {
            // Если есть и точка и запятая: 2.500,50
            dots > 0 && commas > 0 -> {
                val lastDot = s.lastIndexOf('.')
                val lastComma = s.lastIndexOf(',')
                if (lastDot > lastComma) {
                    s.replace(",", "").replace(".", ".")
                } else {
                    s.replace(".", "").replace(",", ".")
                }
            }
            // Только запятые (например 2,500,000 или просто 2,5)
            commas > 1 -> s.replace(",", "")
            commas == 1 -> s.replace(",", ".")
            // Только точки (например 2.500.000 или просто 2.5)
            dots > 1 -> s.replace(".", "")
            dots == 1 -> {
                // Для математики считаем точку десятичным разделителем
                s
            }
            else -> s
        }
    }

    private fun parseExpression(tokens: List<String>): Double {
        val values = Stack<Double>()
        val ops = Stack<String>()
        
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when {
                // Обработка отрицательных чисел
                token == "-" && (i == 0 || tokens[i-1] == "(" || "+-*/^".contains(tokens[i-1])) -> {
                    values.push(0.0)
                    ops.push("-")
                }
                token[0].isDigit() || (token.length > 1 && token[1].isDigit()) -> {
                    values.push(token.toDouble())
                }
                token == "(" -> ops.push("(")
                token == ")" -> {
                    while (ops.isNotEmpty() && ops.peek() != "(") {
                        val op = ops.pop()
                        if (isFunction(op)) {
                            values.push(applyFunc(op, values.pop()))
                        } else {
                            values.push(applyOp(op[0], values.pop(), values.pop()))
                        }
                    }
                    if (ops.isNotEmpty()) ops.pop()
                    
                    // Проверка на функцию перед скобкой
                    if (ops.isNotEmpty() && isFunction(ops.peek())) {
                        values.push(applyFunc(ops.pop(), values.pop()))
                    }
                }
                isFunction(token) -> ops.push(token)
                "+-*/^".contains(token) -> {
                    while (ops.isNotEmpty() && !isFunction(ops.peek()) && hasPrecedence(token[0], ops.peek()[0])) {
                        values.push(applyOp(ops.pop()[0], values.pop(), values.pop()))
                    }
                    ops.push(token)
                }
            }
            i++
        }
        
        while (ops.isNotEmpty()) {
            val op = ops.pop()
            if (isFunction(op)) {
                values.push(applyFunc(op, values.pop()))
            } else {
                values.push(applyOp(op[0], values.pop(), values.pop()))
            }
        }
        
        return if (values.isEmpty()) 0.0 else values.pop()
    }

    private fun isFunction(s: String): Boolean = s in listOf("sqrt", "sin", "cos", "tan", "log", "abs")

    private fun applyFunc(func: String, a: Double): Double {
        return when (func) {
            "sqrt" -> kotlin.math.sqrt(a)
            "sin" -> kotlin.math.sin(Math.toRadians(a))
            "cos" -> kotlin.math.cos(Math.toRadians(a))
            "tan" -> kotlin.math.tan(Math.toRadians(a))
            "log" -> kotlin.math.log10(a)
            "abs" -> kotlin.math.abs(a)
            else -> a
        }
    }

    private fun hasPrecedence(op1: Char, op2: Char): Boolean {
        if (op2 == '(' || op2 == ')') return false
        if (op1 == '^' && op2 != '^') return false
        if ((op1 == '*' || op1 == '/') && (op2 == '+' || op2 == '-')) return false
        return true
    }

    private fun applyOp(op: Char, b: Double, a: Double): Double {
        return when (op) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> {
                if (b == 0.0) throw UnsupportedOperationException("Division by zero")
                a / b
            }
            '^' -> a.pow(b)
            else -> 0.0
        }
    }
}
