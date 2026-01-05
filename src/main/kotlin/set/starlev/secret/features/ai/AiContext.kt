package set.starlev.secret.features.ai

object AiContext {
    val contextStore = mutableMapOf<String, UserContext>()
    val worldEntities = mutableMapOf<String, WorldEntity>()

    fun getEntity(name: String): WorldEntity {
        return worldEntities.getOrPut(name.lowercase()) { WorldEntity(name) }
    }

    fun getOrUpdateContext(sender: String, message: String): UserContext {
        // Дополнительная проверка на чистоту ника
        val cleanSender = sender.replace(Regex("[^a-zA-Z0-9_]"), "")
        if (cleanSender.isEmpty()) return UserContext() // Возвращаем пустой контекст для мусора

        val context = contextStore.getOrPut(cleanSender) { UserContext() }
        val currentTime = System.currentTimeMillis()
        
        // Анализ грамотности
        val hasPunctuation = message.any { it in ".,!?" }
        val startsWithCapital = message.firstOrNull()?.isUpperCase() ?: false
        val messageLiteracy = (if (hasPunctuation) 0.5 else 0.0) + (if (startsWithCapital) 0.5 else 0.0)
        context.literacyScore = (context.literacyScore * 0.7 + messageLiteracy * 0.3)

        // Passive patience recovery over time (5 points per minute)
        val timeSinceLastInteraction = currentTime - context.lastInteractionTime
        if (timeSinceLastInteraction > 60000) {
            val recoveryAmount = (timeSinceLastInteraction / 60000).toInt() * 5
            context.patience = (context.patience + recoveryAmount).coerceAtMost(100)
        }
        val lowerMessage = message.lowercase()

        // Patience reduction for "stupid" or repetitive behavior
        if (context.lastPlayerMessages.size >= 2 && context.lastPlayerMessages.lastOrNull() == message) {
            context.patience -= 15 
        }
        
        val stupidKeywords = listOf("дай", "как", "где", "почему", "кто", "чё", "шо", "а?")
        if (lowerMessage.length < 10 && stupidKeywords.any { kw -> lowerMessage.contains(kw) }) {
            context.patience -= 5 // Немного снижаем, но не критично
        }
        
        if (lowerMessage.contains("дай денег") || lowerMessage.contains("дай гемы")) {
            context.patience -= 15
            context.respectLevel = (context.respectLevel - 2).coerceAtLeast(0)
        }

        context.patience = context.patience.coerceAtLeast(0)

        // Обновление истории сообщений (до 7 сообщений)
        context.lastPlayerMessages.add(message)
        if (context.lastPlayerMessages.size > 7) {
            context.lastPlayerMessages.removeAt(0)
        }
        
        context.interactionCount++
        context.lastInteractionTime = currentTime
        
        // Рост уважения при частом общении
        if (context.interactionCount % 10 == 0) {
            context.respectLevel = (context.respectLevel + 1).coerceAtMost(100)
        }
        
        // Topic and Mood tracking
        // Sub-topic detection
        when {
            lowerMessage.contains("как") -> context.subTopic = "method"
            lowerMessage.contains("где") -> context.subTopic = "location"
            lowerMessage.contains("кто") -> context.subTopic = "identity"
            lowerMessage.contains("почему") -> context.subTopic = "reason"
            lowerMessage.contains("когда") -> context.subTopic = "time"
            else -> context.subTopic = null
        }

        // Mood detection
        if (lowerMessage.contains("!") || listOf("круто", "класс", "ура", "топ", "лучший", "красава").any { moodWord -> lowerMessage.contains(moodWord) }) {
            context.mood = "happy"
            context.patience = (context.patience + 5).coerceAtMost(100) // Politeness increases patience
        } else if (listOf("пздц", "плохо", "бесит", "черт", "чё за", "ужас", "фигня").any { moodWord -> lowerMessage.contains(moodWord) }) {
            context.mood = "annoyed"
        } else if (lowerMessage.contains("?") && lowerMessage.length > 20) {
            context.mood = "curious"
        }

        // Simple preference extraction (AI-like)
        if (message.contains("люблю") || message.contains("нравится")) {
            val parts = message.split("люблю", "нравится")
            if (parts.size > 1) {
                val pref = parts[1].trim().take(30)
                context.preferences["likes"] = pref
                if (context.memories.size < 5) context.memories.add("Тебе нравится $pref")
            }
        }
        
        // Memory of achievements or events
        if (lowerMessage.contains("выбил") || lowerMessage.contains("купил") || lowerMessage.contains("нашел")) {
            context.memories.add(message.take(50))
            if (context.memories.size > 5) context.memories.removeAt(0)
        }

        // Кратковременная память о фактах (Я ..., У меня ...)
        val factPatterns = listOf("я ", "у меня ", "меня зовут ", "живу ")
        if (factPatterns.any { lowerMessage.startsWith(it) }) {
            val fact = message.trim().removeSuffix("!").removeSuffix(".")
            if (fact.length in 5..60) {
                context.memories.add(fact)
                if (context.memories.size > 5) context.memories.removeAt(0)
            }
        }
        
        return context
    }
}
