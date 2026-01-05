package set.starlev.secret.features.ai

import set.starlev.secret.config.SecretMenuManager
import java.util.*
import kotlin.math.max

object AiThinking {
    private val responseCooldowns = mutableMapOf<String, Long>()
    
    // Кэш для оптимизации производительности
    private val normalizedWordCache = mutableMapOf<String, String>()
    private val stemmedBaseLexicon = mutableMapOf<String, String>()
    
    // Прекомпилированные регулярные выражения
    private val regexNonAlphaNumeric = Regex("[^a-zа-я0-9]")
    private val regexRepeatI = Regex("ии+")
    private val regexRepeatE = Regex("ее+")
    private val regexRepeatO = Regex("оо+")
    private val regexRepeatA = Regex("аа+")
    private val regexWordSplit = Regex("[\\s,!.?]+")
    private val regexMathHyphen = Regex("(?<=\\s)-(?=\\s)")
    
    // Регулярные выражения для стемминга
    private val regexPerfectiveGerund = Regex("(в|вши|вшись)$")
    private val regexAdjectival = Regex("(ий|ый|ое|ие|ые|ою|ею|ия|ыя|ою|ею|их|ых|им|ым|ом|ем|ая|яя|ое|ее)$")
    private val regexVerbal = Regex("(ла|на|ете|ите|ли|й|л|ем|н|ло|но|ет|ют|ны|ть|ешь|нно)$")
    private val regexNoun = Regex("(а|ев|ов|ие|ье|е|иями|ями|ами|еи|ии|и|ией|ей|ой|ий|й|иям|ям|ием|ем|ам|ом|о|у|ах|иях|ях|ы|ь|ию|ью|ю|ия|ья|я)$")
    private val regexReflexive = Regex("(ся|сь)$")
    private val regexDerivational = Regex("(ост|ость)$")
    private val regexEndings = Regex("(ами|ями|иями|ов|ев|ий|ей|ой|иям|ям|ам|ах|ях|иях|ие|ье|и|ы|а|о|у|е|ю|я|ь)$")

    init {
        // Инициализация обратного индекса лексикона для мгновенного поиска
        AiLeksikon.BASE_LEXICON.forEach { (key, synonyms) ->
            synonyms.forEach { synonym ->
                stemmedBaseLexicon[stem(synonym.lowercase())] = key
            }
            stemmedBaseLexicon[stem(key.lowercase())] = key
        }
    }

    fun matchIntent(message: String, sender: String, context: UserContext): Pair<String?, Double> {
        val lowerMessage = message.lowercase()
        val normalizedMessage = lowerMessage
            .replace("=", " равно ")
            .replace("+", " плюс ")
            .replace(regexMathHyphen, " это ")
        
        // Проверка на обращение
        val isDirectAddressing = AiConfig.TRIGGERS.any { trigger -> 
            normalizedMessage.startsWith(trigger) || normalizedMessage.contains(" $trigger") || normalizedMessage.endsWith(trigger)
        }
        
        val rawWords = normalizedMessage.split(regexWordSplit).filter { it.isNotBlank() && it !in AiConfig.STOP_WORDS }
        if (rawWords.isEmpty()) return null to 0.0

        // Кэшируем нормализацию для текущего сообщения
        val words = rawWords.map { normalizeWord(it) }

        var bestIntent: Intent? = null
        var maxConfidence = 0.0

        for (intent in AiLeksikon.BRAIN) {
            var confidence = calculateConfidence(words, intent, context)
            
            if (isDirectAddressing) confidence += 0.2
            
            val isQuestion = AiConfig.QUESTION_WORDS.any { lowerMessage.contains(it) } || lowerMessage.endsWith("?")
            val isQuestionIntent = intent.category in listOf("location", "time", "method", "reason", "identity")

            if (isQuestion && isQuestionIntent) {
                val questionKeywords = AiLeksikon.BASE_LEXICON[intent.category] ?: emptyList()
                if (questionKeywords.any { lowerMessage.contains(it) }) {
                    confidence += 0.5
                } else {
                    confidence += 0.3
                }
            }
            
            if (intent.category != "general" && words.any { it == intent.category }) {
                confidence += 0.2
            }

            if (context.conversationTopic != null && intent.category == context.conversationTopic) {
                confidence += 0.15
            }
            
            if (confidence > maxConfidence) {
                maxConfidence = confidence
                bestIntent = intent
                // Оптимизация: если нашли очень точное совпадение, выходим раньше
                if (maxConfidence >= 0.95) break
            }
        }

        return if (maxConfidence > 0.35 && bestIntent != null) {
            if (bestIntent.category != "general") {
                context.conversationTopic = bestIntent.category
                if (context.topicHistory.lastOrNull() != bestIntent.category) {
                    context.topicHistory.add(bestIntent.category)
                    if (context.topicHistory.size > 5) context.topicHistory.removeAt(0)
                }
            }
            
            val response = pickRandomWithCooldown(bestIntent.responses, sender, context)
            response to maxConfidence
        } else {
            null to 0.0
        }
    }

    private fun calculateConfidence(words: List<String>, intent: Intent, context: UserContext): Double {
        var matches = 0.0
        val intentKeywords = intent.keywords
        
        for (word in words) {
            // IQ: Вес слова зависит от его длины и редкости
            val weight = when {
                word.length > 7 -> 1.5
                word.length > 5 -> 1.2
                else -> 1.0
            }
            
            if (intentKeywords.any { kw ->
                kw == word || 
                (word.length > 3 && kw.contains(word)) || 
                (kw.length > 3 && word.contains(kw)) ||
                levenshteinDistance(kw, word) <= (word.length / 4)
            }) {
                matches += weight
            }
        }

        if (matches == 0.0) return 0.0

        var confidence = matches / max(words.size, intentKeywords.size / 2)
        
        // Context boosts
        if (context.conversationTopic != null && intentKeywords.any { it.contains(context.conversationTopic!!.lowercase()) }) {
            confidence *= 1.3
        }
        
        if (context.literacyScore > 0.8) confidence *= 1.1

        return confidence.coerceAtMost(1.0)
    }

    fun generateAdvancedAnswer(sender: String, message: String, context: UserContext): String {
        val lowerMessage = message.lowercase()
        
        // 0. ПРИОРИТЕТ ЛИЧНОСТИ (AiPersona)
        val persona = SecretMenuManager.secretConfig.funCategory.aiPersona
        
        // 0.1 ПРИОРИТЕТ КОНТЕКСТА (Повторение, просьбы повторить)
        
        // Анализ настроения из общего чата
        val chatMood = set.starlev.secret.features.AutoResponder.getChatMood()
        if (chatMood != "neutral") context.mood = chatMood

        // Просьба повторить/написать что-то
        if (lowerMessage.contains("скажи") || lowerMessage.contains("напиши") || lowerMessage.contains("повтори")) {
            val parts = lowerMessage.split(Regex("(скажи|напиши|повтори)"), limit = 2)
            if (parts.size > 1) {
                val toRepeat = parts[1].trim().removePrefix("что").removePrefix("это").trim()
                if (toRepeat.isNotEmpty()) {
                    return "!$toRepeat"
                }
            }
        }

        // 1. Проверка на спам/повторение
        if (context.lastPlayerMessages.size >= 2 && context.lastPlayerMessages.takeLast(2).all { it.equals(message, ignoreCase = true) }) {
            return pickRandomWithCooldown(AiLeksikon.REPETITION_RESPONSES, sender, context)
        }

        // 2. Попытка решить математику
        val mathResult = AiMath.trySolve(message)
        if (mathResult != null) {
            val mathResponse = when(persona) {
                AiPersona.SARCASTIC -> "!$sender, даже калькулятор знает, что это $mathResult. Пользуйся."
                AiPersona.PHILOSOPHICAL -> "!Цифры говорят нам, что итог — $mathResult. В математике всегда есть порядок, $sender."
                AiPersona.AGGRESSIVE -> "!$sender, это же элементарно! $mathResult! Хватит тупить."
                AiPersona.CUTE -> "!$sender, я посчитала для тебя! Получилось $mathResult ^-^"
                else -> "!$sender, будет $mathResult!"
            }
            return mathResponse
        }

        // 2.5 Специальные вопросы (Кто я, время и т.д.)
        if (lowerMessage.contains("кто я") || lowerMessage.contains("как меня зовут")) {
            val myFacts = context.memories.filter { it.lowercase().startsWith("я ") || it.lowercase().startsWith("меня зовут ") }
            if (myFacts.isNotEmpty() && Random().nextDouble() < 0.8) {
                val fact = myFacts.random()
                return pickRandomWithCooldown(listOf(
                    "!Ты говорил, что $fact. Я все помню!",
                    "!Если не ошибаюсь, ты упоминал: $fact.",
                    "!По моим данным, ты — {player}, и ты говорил: $fact."
                ), sender, context)
            }
            return pickRandomWithCooldown(listOf(
                "!Ты — {player}, отважный исследователь Хелтикса! Или ты забыл свой ник?",
                "!Мои сенсоры говорят, что ты — {player}. Приятно познакомиться снова!",
                "!Для меня ты — {player}, один из тех, кто делает этот мир интереснее."
            ), sender, context)
        }

        if (lowerMessage.contains("сколько время") || lowerMessage.contains("какой час") || lowerMessage.contains("который час")) {
            return "!Сейчас ровно {time}. Время летит, когда ты в игре!"
        }

        // 2.6 Бинарные вопросы (Да/Нет, Можно/Нельзя)
        if (lowerMessage.contains("да или нет") || lowerMessage.contains("правда или ложь") || (lowerMessage.contains("можно") && lowerMessage.contains("нельзя"))) {
            return pickRandomWithCooldown(listOf(
                "!Я думаю, что скорее да, чем нет. Но это не точно!",
                "!Мои алгоритмы склоняются к варианту 'Да'.",
                "!Скорее всего, нет. Хотя в этом мире всё возможно.",
                "!Тут 50 на 50. Но я выберу 'Да', просто потому что я оптимист!",
                "!Нет. Однозначно нет. Мой процессор протестует.",
                "!Правда! Я чувствую это своими сенсорами.",
                "!Ложь. Не верь этому, это всё баги восприятия.",
                "!Можно! Если осторожно и не палиться перед админами.",
                "!Нельзя. Правила есть правила, даже для ИИ."
            ), sender, context)
        }

        // Поиск по сущностям мира для вопросов "кто/что такое X"
        if (lowerMessage.contains("кто такой") || lowerMessage.contains("что такое") || lowerMessage.contains("кто такая") || lowerMessage.contains("кто это")) {
            val entityName = lowerMessage
                .replace("кто такой", "")
                .replace("что такое", "")
                .replace("кто такая", "")
                .replace("кто это", "")
                .replace("?", "")
                .trim()
            
            if (entityName.isNotEmpty()) {
                val entity = AiContext.worldEntities.values.find { 
                    it.name.equals(entityName, ignoreCase = true) || 
                    it.tags.any { tag -> tag.equals(entityName, ignoreCase = true) } 
                }
                
                if (entity != null) {
                    val info = when(entity.role) {
                        EntityRole.MEDIA -> "!{player}, ${entity.name} — это известный медиа-персонаж! Его авторитет в наших кругах: ${"%.1f".format(entity.authority)}."
                        EntityRole.TOP -> "!О, ${entity.name} — один из топовых игроков сервера. С ним лучше дружить!"
                        EntityRole.ADMIN -> "!${entity.name} — представитель администрации. Серьезный человек, следит за порядком."
                        EntityRole.HELPER -> "!Это наш помощник ${entity.name}. Всегда готов подсказать по игре."
                        else -> "!Я знаю о ${entity.name}, что это важная часть нашего мира. Репутация: ${entity.reputation}."
                    }
                    return info
                }
            }
        }

        // 3. Поиск интента (База знаний)
        val (intentResponse, confidence) = matchIntent(message, sender, context)

        // Дополнительная логика для приветствий с учетом времени
        if (intentResponse != null && (intentResponse.contains("Привет") || intentResponse.contains("привет"))) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val timeGreeting = when (hour) {
                in 5..11 -> "!Доброе утро, {player}!"
                in 12..17 -> "!Добрый день, {player}!"
                in 18..22 -> "!Добрый вечер, {player}!"
                else -> "!Доброй ночи, {player}! Почему не спишь?"
            }
            if (Random().nextDouble() < 0.4) return timeGreeting
        }
        
        // Если это вопрос и мы нашли подходящий интент с хорошей уверенностью
        if (intentResponse != null) {
            val isQuestion = AiConfig.QUESTION_WORDS.any { lowerMessage.contains(it) } || message.endsWith("?")
            val threshold = if (isQuestion) 0.35 else 0.45 // Для вопросов порог ниже
            
            if (confidence > threshold) {
                 // Если это вопрос, пробуем найти более специфичный ответ в темах
                 if (isQuestion && context.conversationTopic != null) {
                     val themeKey = when(context.conversationTopic) {
                         "identity" -> "кто"
                         "location" -> "где"
                         "time" -> "когда"
                         "method" -> "как"
                         "reason" -> "почему"
                         else -> null
                     }
                     val themeResponses = if (themeKey != null) AiLeksikon.QUESTION_THEMES[themeKey] else null
                     if (themeResponses != null && Random().nextDouble() < 0.7) {
                         return pickRandomWithCooldown(themeResponses, sender, context)
                     }
                 }
                 return intentResponse
             }
        }

        // 4. Предложение добавить в память при непонимании
        if (confidence < 0.2 && (lowerMessage.contains("что такое") || lowerMessage.contains("кто такой") || lowerMessage.contains("кто эта"))) {
            val subject = lowerMessage.replace("что такое", "").replace("кто такой", "").replace("кто эта", "").trim().removeSuffix("?").trim()
            if (subject.length > 2) {
                return "!Я пока не знаю, кто или что такое \"$subject\". Но ты можешь меня научить! Напиши: !учи $subject -> [твой ответ]"
            }
        }

        // 5. Анализ сущностей из мира
        val words = message.lowercase().split(Regex("[\\s,!.?]+"))
        for (word in words) {
            val normalized = normalizeWord(word)
            val entity = AiContext.worldEntities.values.find { it.name.equals(normalized, ignoreCase = true) || it.tags.contains(normalized) }
            if (entity != null) {
                val entityInfo = when(entity.role) {
                    EntityRole.MEDIA -> "О, ${entity.name} - это же известный медиа-человек! У него авторитет ${"%.1f".format(entity.authority)}."
                    EntityRole.TOP -> "${entity.name} - один из сильнейших игроков нашего сервера."
                    else -> null
                }
                if (entityInfo != null && java.util.Random().nextDouble() < 0.5) {
                    return pickRandomWithCooldown(listOf("!$entityInfo"), sender, context)
                }
            }
        }

        // 6. Анализ контекста для выбора "умного" ответа
        if (context.respectLevel > 70 && context.literacyScore > 0.8) {
            val wiseFallbacks = listOf(
                "!Я обдумываю твои слова. В них определенно есть зерно мудрости.",
                "!Твой стиль изложения наводит на интересные размышления.",
                "!Интересный оборот. Позволь мне проанализировать это глубже."
            )
            if (Random().nextDouble() < 0.3) return pickRandomWithCooldown(wiseFallbacks, sender, context)
        }

        // 7. Если похоже на вопрос, но интент не найден
        if (AiConfig.QUESTION_WORDS.any { message.lowercase().contains(it) } || message.endsWith("?")) {
            return pickRandomWithCooldown(AiLeksikon.GENERAL_QUESTION_RESPONSES, sender, context)
        }

        // 8. Финальный ответ-заглушка (только если боту было адресовано или включен полный режим)
        if (AiConfig.TRIGGERS.any { message.lowercase().contains(it) } || SecretMenuManager.secretConfig.funCategory.fullModeEnabled) {
            return pickRandomWithCooldown(AiLeksikon.UNKNOWN_RESPONSES, sender, context)
        }
        
        return "" // В обычном чате просто молчим, если не поняли
    }

    fun buildDynamicResponse(baseResponse: String, sender: String, context: UserContext): String {
        val persona = SecretMenuManager.secretConfig.funCategory.aiPersona
        val hasPrefix = baseResponse.startsWith("!")
        var finalResponse = if (hasPrefix) baseResponse.substring(1) else baseResponse
        
        // Dynamic variables replacement
        finalResponse = finalResponse.replace("{player}", sender)
            .replace("{time}", java.text.SimpleDateFormat("HH:mm").format(java.util.Date()))

        // Адаптация под личность
        finalResponse = when(persona) {
            AiPersona.SARCASTIC -> {
                if (Random().nextDouble() < 0.3) finalResponse + " (но это не точно)"
                else finalResponse
            }
            AiPersona.AGGRESSIVE -> finalResponse.replace(".", "!")
            AiPersona.CUTE -> finalResponse.replace("!", " ^-^")
            else -> finalResponse
        }

        val sb = StringBuilder()
        if (hasPrefix) sb.append("!") // Preserve global prefix
        
        // Маппинг категорий для "мыслей" бота
        val categoryNames = mapOf(
            "location" to "местоположении",
            "time" to "времени",
            "method" to "способах",
            "reason" to "причинах",
            "identity" to "личностях",
            "safety" to "безопасности",
            "psychologist" to "эмоциях",
            "rules" to "правилах",
            "philosophy" to "философии",
            "lore" to "истории сервера",
            "respect" to "уважении",
            "chat" to "беседе",
            "tips" to "советах",
            "admin" to "администрации",
            "social" to "общении",
            "weather" to "погоде",
            "tech" to "технических деталях",
            "greeting" to "приветствиях",
            "bye" to "прощаниях",
            "mood" to "настроении",
            "positive" to "позитиве",
            "negative" to "негативе",
            "activity" to "занятиях",
            "starlev" to "создателе",
            "updates" to "обновлениях",
            "contact" to "контактах",
            "server_rules" to "правилах сервера",
            "logic" to "логике",
            "filler" to "реакциях",
            "secret" to "секретах",
            "joke" to "юморе",
            "server" to "сервере",
            "help" to "помощи"
        )

        // Интеллектуальный выбор обращения и префикса
        val isOldFriend = context.interactionCount > 50
        val isLiterate = context.literacyScore > 0.7
        val random = java.util.Random()
        
        // Шанс 15% добавить "мысль" о категории, если она есть
        if (context.conversationTopic != null && context.conversationTopic != "general" && random.nextDouble() < 0.15) {
            val catName = categoryNames[context.conversationTopic] ?: context.conversationTopic
            val thinkingPrefixes = listOf(
                "(Анализирую тему о $catName) ",
                "(Размышляю о $catName...) ",
                "(Мои мысли сейчас о $catName) ",
                "[Тема: $catName] "
            )
            sb.append(thinkingPrefixes.random())
        }

        // Шанс 40% добавить обращение, если это не короткий ответ
        if (finalResponse.length > 10 || random.nextDouble() < 0.4) {
            when {
                context.respectLevel > 80 -> {
                    if (isLiterate) sb.append("Глубокоуважаемый $sender, ") 
                    else sb.append("Дорогой мой $sender! ")
                }
                isOldFriend -> {
                    val friendPrefixes = listOf("О, старый добрый друг $sender! ", "Приветствую снова, $sender. ", "Рад нашей встрече, $sender! ")
                    sb.append(friendPrefixes.random())
                }
                context.patience < 30 -> {
                    sb.append("Послушай, $sender... ")
                }
                context.mood == "happy" -> {
                    val happyPrefixes = listOf("Рад слышать, $sender. ")
                    sb.append(happyPrefixes.random())
                }
                else -> {
                    if (isLiterate) sb.append("$sender, позволь ответить: ")
                    else if (random.nextBoolean()) sb.append("$sender, ")
                }
            }
        }

        // Адаптация стиля ответа под грамотность
        if (isLiterate && finalResponse.endsWith("!")) {
            finalResponse = finalResponse.dropLast(1) + "."
        }

        sb.append(finalResponse)

        // Добавление умных суффиксов (адаптация)
        if (random.nextDouble() < 0.3) {
            val intelligentSuffixes = listOf(
                "Как считаешь?",
                "Надеюсь, это прояснило ситуацию.",
                "Всегда рад нашей беседе.",
                "Твое присутствие украшает этот чат.",
                "Ты очень интересный собеседник.",
                "Кстати, отличный ник!",
                "Мир Minecraft полон сюрпризов, не так ли?",
                "Что думаешь об этом?",
                "Удачного тебе дня!",
                "Если будут еще вопросы — я здесь."
            )
            sb.append(" ").append(intelligentSuffixes.random())
        }
        
        return sb.toString()
    }

    private fun extractValue(info: String, key: String): String {
        val pattern = "$key: (.*?)\\. ".toRegex()
        return pattern.find(info)?.groupValues?.get(1) ?: "неизвестно"
    }

   private fun applyPersona(response: String, persona: AiPersona): String {
        val random = Random()
        return when (persona) {
            AiPersona.SARCASTIC -> {
                val sarcasticSuffixes = listOf("...наверное.", " (нет)", ", если ты понимаешь о чем я.", ". Ну ты понял.", " *закатил глаза*")
                if (random.nextDouble() < 0.4) response.removeSuffix(".") + sarcasticSuffixes.random() else response
            }
            AiPersona.PHILOSOPHICAL -> {
                val philosophicalPrefixes = listOf("В глубине души я чувствую, что ", "Если задуматься, то ", "Звезды говорят, что ", "В этом бренном мире ")
                if (random.nextDouble() < 0.4) philosophicalPrefixes.random() + response.replaceFirstChar { it.lowercase() } else response
            }
            AiPersona.AGGRESSIVE -> {
                val aggressivePrefixes = listOf("СЛУШАЙ СЮДА! ", "ЭЙ! ", "ТАК ВОТ: ")
                val res = if (random.nextDouble() < 0.3) aggressivePrefixes.random() + response else response
                res.replace(".", "!").replace("?", "??!").uppercase()
            }
            AiPersona.CUTE -> {
                val cuteSuffixes = listOf(" ^-^", " <3", " owo", " :3", " ня!")
                response.replace(".", "").replace("!", "") + cuteSuffixes.random()
            }
            AiPersona.HELPFUL -> response
        }
    }

    fun normalizeWord(word: String): String {
        val lower = word.lowercase()
        // Проверка кэша для мгновенного результата
        normalizedWordCache[lower]?.let { return it }

        // Базовая нормализация символов
        val base = stem(lower
            .replace("ё", "е")
            .replace(regexNonAlphaNumeric, "")
            .replace(regexRepeatI, "и")
            .replace(regexRepeatE, "е")
            .replace(regexRepeatO, "о")
            .replace(regexRepeatA, "а"))
            
        // Мгновенный поиск в прекомпилированном лексиконе O(1)
        val result = stemmedBaseLexicon[base] ?: base
        
        // Сохраняем в кэш (ограничиваем размер кэша)
        if (normalizedWordCache.size < 1000) {
            normalizedWordCache[lower] = result
        }
        
        return result
    }

    private fun stem(word: String): String {
        if (word.length <= 3) return word
        
        var result = word
        result = result.replace(regexReflexive, "")
        
        val temp = result.replace(regexPerfectiveGerund, "")
        if (temp == result) {
            result = result.replace(regexAdjectival, "")
            val temp2 = result.replace(regexVerbal, "")
            if (temp2 == result) {
                result = result.replace(regexNoun, "")
            } else {
                result = temp2
            }
        } else {
            result = temp
        }

        result = result.replace(regexDerivational, "")
        return result.replace(regexEndings, "") // Дополнительная очистка окончаний для русского языка
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) dp[i - 1][j - 1]
                else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
            }
        }
        return dp[s1.length][s2.length]
    }

    fun pickRandomWithCooldown(responses: List<String>, sender: String, context: UserContext? = null): String {
        val currentTime = System.currentTimeMillis()
        
        // 1. Фильтр по глобальному кулдауну (чтобы не спамить одной фразой в чат всем)
        var available = responses.filter { response ->
            val lastTime = responseCooldowns[response] ?: 0L
            currentTime - lastTime > 60000 // 1 минута глобальный кулдаун
        }

        // 2. Дополнительный фильтр по истории конкретного игрока (чтобы не отвечать ему одно и то же подряд)
        if (context != null && available.size > 1) {
            val userHistory = context.recentBotResponses
            val diverse = available.filter { it !in userHistory }
            if (diverse.isNotEmpty()) {
                available = diverse
            }
        }

        val selected = if (available.isNotEmpty()) {
            available.random()
        } else {
            // Если всё на кулдауне, берем то, что использовалось дольше всего назад
            responses.minByOrNull { resp -> responseCooldowns[resp] ?: 0L } ?: responses.random()
        }

        // Обновляем глобальный кулдаун
        responseCooldowns[selected] = currentTime
        
        // Обновляем историю игрока
        if (context != null) {
            context.lastBotResponse = selected
            context.recentBotResponses.add(selected)
            if (context.recentBotResponses.size > 5) {
                context.recentBotResponses.removeAt(0)
            }
            return buildDynamicResponse(selected, sender, context)
        } else {
            val cleanResponse = if (selected.startsWith("!")) selected.substring(1) else selected
            return "!$sender, $cleanResponse"
        }
    }
}
