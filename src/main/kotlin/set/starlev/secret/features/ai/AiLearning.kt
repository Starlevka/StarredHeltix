package set.starlev.secret.features.ai

object AiLearning {
    fun processSelfLearning(sender: String, message: String, context: UserContext) {
        val lowerMessage = message.lowercase()
        val entity = AiContext.getEntity(sender)
        
        // 1. Update authority based on role and interaction
        updateAuthority(entity, context)

        // 2. Logic filter for learning (бред-детектор)
        if (isAbsurd(message, entity)) return

        // 3. Memory storage (if user says something new/interesting)
        if (lowerMessage.contains("это ") || lowerMessage.contains(" называется") || lowerMessage.contains(" зовут")) {
            learnFromMessage(sender, message, entity)
        }

        // 4. Feedback processing
        if (AiConfig.CORRECTION_MARKERS.any { lowerMessage.contains(it) }) {
            context.patience -= 10
            context.mood = "annoyed"
            entity.reputation -= 1
        } else if (AiConfig.FEEDBACK_POSITIVE.any { lowerMessage.contains(it) }) {
            context.patience += 5
            context.mood = "happy"
            entity.reputation += 1
        } else if (AiConfig.FEEDBACK_NEGATIVE.any { lowerMessage.contains(it) }) {
            context.patience -= 15
            context.mood = "annoyed"
            entity.reputation -= 2
        }

        // 5. Topic tracking
        updateTopic(lowerMessage, context)
        
        // 6. Interaction tracking
        context.interactionCount++
        context.lastInteractionTime = System.currentTimeMillis()
        entity.lastSeen = System.currentTimeMillis()
    }

    private fun updateAuthority(entity: WorldEntity, context: UserContext) {
        var baseAuth = when (entity.role) {
            EntityRole.MEDIA -> 3.0
            EntityRole.TOP -> 2.5
            EntityRole.PLAYER -> 1.0
            else -> 0.5
        }
        
        // Reputation modifier
        baseAuth *= (entity.reputation / 50.0)
        
        // Literacy modifier
        baseAuth *= (0.5 + context.literacyScore * 0.5)
        
        entity.authority = baseAuth.coerceIn(0.1, 5.0)
    }

    private fun isAbsurd(message: String, entity: WorldEntity): Boolean {
        val lower = message.lowercase()
        
        // Если говорит Starlev или кто-то очень авторитетный (authority > 4.5), то это не бред
        if (entity.authority > 4.5) return false

        // Проверка на бессмысленные сочетания (пример: тазик дракон)
        val absurdCombos = listOf(
            "тазик" to "дракон",
            "шкаф" to "летает",
            "админ" to "нуб",
            "сервер" to "говно"
        )
        
        if (absurdCombos.any { (w1, w2) -> lower.contains(w1) && lower.contains(w2) }) {
            return true
        }

        // Если авторитет низкий и сообщение слишком короткое/странное
        if (entity.authority < 0.8 && lower.length < 5) return true

        return false
    }

    fun learnFromMessage(sender: String, message: String, entity: WorldEntity) {
        val lower = message.lowercase()
        // Извлекаем "A это B"
        if (lower.contains(" это ")) {
            val parts = lower.split(" это ", limit = 2)
            val key = parts[0].trim()
            val value = parts[1].trim()
            
            if (key.length > 2 && value.length > 2) {
                val intent = Intent(
                    keywords = listOf(key),
                    responses = listOf("!$value"),
                    trustScore = entity.authority,
                    source = "community",
                    category = if (entity.role == EntityRole.MEDIA) "media" else "fact"
                )
                
                AiLeksikon.addIntent(intent)
            }
        }
    }

    fun learnServerTip(tip: String) {
        // Извлекаем суть совета (обычно после "Совет:" или "Подсказка:")
        val cleanTip = tip.replace(Regex("(?i)^(совет|подсказка)[:\\s]+"), "").trim()
        if (cleanTip.length < 10) return

        val intent = Intent(
            keywords = listOf("совет", "подсказка", "помощь", "как играть"),
            responses = listOf("!Сервер советует: $cleanTip", "!Я слышал подсказку: $cleanTip"),
            trustScore = 5.0, // Максимальное доверие серверу
            source = "server",
            category = "tips"
        )
        AiLeksikon.addIntent(intent)
    }

    private fun updateTopic(message: String, context: UserContext) {
        // Simple keyword-based topic tracking
        val words = message.split(" ").filter { it.length > 3 }
        for (word in words) {
            val normalized = AiThinking.normalizeWord(word)
            if (normalized.length > 4) {
                context.conversationTopic = normalized
                if (!context.topicHistory.contains(normalized)) {
                    context.topicHistory.add(normalized)
                    if (context.topicHistory.size > 5) context.topicHistory.removeAt(0)
                }
                break
            }
        }
    }
}
