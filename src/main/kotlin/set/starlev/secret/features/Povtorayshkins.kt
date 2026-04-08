package set.starlev.secret.features

import net.minecraft.client.Minecraft
import set.starlev.secret.config.SecretMenuManager
import kotlin.random.Random

/**
 * Повторяшкинс - фановая функция из секретного меню.
 * С шансом 10% повторяет сообщения игрока в чате с забавными подписями.
 * 
 * Варианты:
 * - Лёва AI: повторяет сообщение как есть с подписью "(Лёва AI повторил сообщение.)"
 * - Амёба AI: повторяет сообщение исковерканным с подписью "(Амёба AI на связи!)"
 * 
 * Сохраняет префикс "!" если он был в оригинальном сообщении.
 */
object Povtorayshkins {
    private val mc = Minecraft.getInstance()
    private const val TRIGGER_CHANCE = 0.10 // 10% шанс
    private val recentMessages = mutableSetOf<String>() // Для предотвращения бесконечного цикла

    // Время жизни записи в кэше (2 секунды)
    private const val CACHE_TIMEOUT = 2000L
    private val messageTimestamps = mutableMapOf<String, Long>()

    fun init() {
        // Инициализация через миксин PovtorayshkinsMixin
    }

    /**
     * Вызывается из PovtorayshkinsMixin при отправке сообщения игроком
     */
    fun onPlayerSendMessage(message: String) {
        if (!isEnabled()) return
        
        // Не реагируем на свои повторения
        if (isOwnRepeat(message)) return
        
        // Проверяем шанс 10%
        if (Random.nextFloat() >= TRIGGER_CHANCE) return
        
        // Выбираем тип повторения (50/50)
        if (Random.nextBoolean()) {
            scheduleLevaRepeat(message)
        } else {
            scheduleAmoebaRepeat(message)
        }
    }

    private fun isEnabled(): Boolean {
        if (!SecretMenuManager.isConfigInitialized) return false
        return SecretMenuManager.secretConfig.funCategory.povtorayshkins
    }

    private fun isOwnRepeat(message: String): Boolean {
        // Проверяем, не является ли это сообщение нашим повторением
        return message.contains("(Лёва AI повторил сообщение.)") || 
               message.contains("(Амёба AI на связи!)")
    }

    private fun scheduleLevaRepeat(originalMessage: String) {
        // Задержка от 100ms до 1000ms
        val delay = 100 + Random.nextLong(900)
        
        Thread {
            Thread.sleep(delay)
            
            val processedMessage = processLevaMessage(originalMessage)
            
            mc.execute {
                sendMessage(processedMessage)
                addToCache(processedMessage)
            }
        }.start()
    }

    private fun scheduleAmoebaRepeat(originalMessage: String) {
        // Задержка от 200ms до 1500ms (Амёба думает дольше)
        val delay = 200 + Random.nextLong(1300)
        
        Thread {
            Thread.sleep(delay)
            
            val processedMessage = processAmoebaMessage(originalMessage)
            
            mc.execute {
                sendMessage(processedMessage)
                addToCache(processedMessage)
            }
        }.start()
    }

    private fun processLevaMessage(message: String): String {
        val hasExclamation = message.startsWith("!")
        val cleanMessage = if (hasExclamation) message.substring(1) else message
        
        return if (hasExclamation) {
            "!$cleanMessage (Лёва AI повторил сообщение.)"
        } else {
            "$cleanMessage (Лёва AI повторил сообщение.)"
        }
    }

    private fun processAmoebaMessage(message: String): String {
        val hasExclamation = message.startsWith("!")
        val cleanMessage = if (hasExclamation) message.substring(1) else message
        
        // Искажаем сообщение: перемешиваем буквы, заменяем на похожие
        val distorted = distortMessage(cleanMessage)
        
        return if (hasExclamation) {
            "!$distorted (Амёба AI на связи!)"
        } else {
            "$distorted (Амёба AI на связи!)"
        }
    }

    private fun distortMessage(message: String): String {
        if (message.isEmpty()) return message
        
        val result = StringBuilder()
        
        for (char in message) {
            when {
                char.isLetter() -> {
                    // С шансом 30% заменяем букву на похожую или меняем регистр
                    when {
                        Random.nextFloat() < 0.15 -> result.append(if (char.isUpperCase()) char.lowercase() else char.uppercase())
                        Random.nextFloat() < 0.15 -> result.append(getSimilarChar(char))
                        Random.nextFloat() < 0.1 -> result.append(" ") // Добавляем лишний пробел
                        else -> result.append(char)
                    }
                }
                char.isDigit() -> {
                    // С шансом 20% заменяем цифру на слово
                    if (Random.nextFloat() < 0.2) {
                        result.append(digitToWord(char))
                    } else {
                        result.append(char)
                    }
                }
                else -> result.append(char)
            }
        }
        
        return result.toString()
    }

    private fun getSimilarChar(char: Char): Char {
        return when (char) {
            'a', 'A' -> arrayOf('\u0430', 'A', '@')[Random.nextInt(3)]
            'e', 'E' -> arrayOf('\u0435', '3', 'E')[Random.nextInt(3)]
            'o', 'O' -> arrayOf('\u043E', '0', 'O')[Random.nextInt(3)]
            'c', 'C' -> arrayOf('\u0441', 'C', '(')[Random.nextInt(3)]
            'x', 'X' -> arrayOf('\u0445', 'X', '*')[Random.nextInt(3)]
            'y', 'Y' -> arrayOf('\u0443', 'Y', 'v')[Random.nextInt(3)]
            'b', 'B' -> arrayOf('\u044C', 'B', '6')[Random.nextInt(3)]
            else -> char
        }
    }

    private fun digitToWord(digit: Char): String {
        return when (digit) {
            '0' -> "ноль"
            '1' -> "один"
            '2' -> "два"
            '3' -> "три"
            '4' -> "четыре"
            '5' -> "пять"
            '6' -> "шесть"
            '7' -> "семь"
            '8' -> "восемь"
            '9' -> "девять"
            else -> digit.toString()
        }
    }

    private fun sendMessage(message: String) {
        val player = mc.player ?: return
        val connection = player.connection ?: return
        
        // Отправляем сообщение
        connection.sendChat(message)
    }

    private fun addToCache(message: String) {
        recentMessages.add(message)
        messageTimestamps[message] = System.currentTimeMillis()
        
        // Очищаем старые записи
        val now = System.currentTimeMillis()
        messageTimestamps.entries.removeAll { (msg, timestamp) ->
            now - timestamp > CACHE_TIMEOUT
        }
        recentMessages.removeAll { msg ->
            messageTimestamps[msg] == null
        }
    }
}