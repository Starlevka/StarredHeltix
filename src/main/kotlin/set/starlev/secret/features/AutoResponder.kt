package set.starlev.secret.features

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.secret.config.SecretMenuManager
import set.starlev.secret.features.ai.*
import java.io.File
import java.util.regex.Pattern
import kotlin.random.Random

object AutoResponder {
    // Pattern to match chat messages: "[!] [RANK] Name: Message"
    // Capture group 1: Global prefix (! or [!]), Group 2: Full Sender (Rank + Name), Group 3: Content
    private val MESSAGE_PATTERN = Pattern.compile("^\\s*(\\[?!\\]?)?\\s*(.+?)\\s*:\\s*(.*)$", Pattern.CASE_INSENSITIVE)
    
    // Remote Control System
    private var puppetMode = false
    private var nextResponseOverride: String? = null
    
    private var lastEnabledState = false
    private var lastFullModeState = false
    private var isInitialized = false
    private var lastMessageHash: Int = 0
    private var lastMessageTime: Long = 0
    private var lastSentResponseHash: Int = 0
    private var lastSentResponseTime: Long = 0
    private val lastResponseTimes = mutableMapOf<String, Long>() // Player Name -> Last Response Time
    private val blacklistedPlayers = mutableSetOf<String>() // Names of players to ignore
    
    // Chat Analysis System
    private val chatHistory = mutableListOf<ChatMessage>()
    private var lastChatActivityTime = System.currentTimeMillis()
    private val MAX_HISTORY = 50

    data class ChatMessage(val sender: String, val content: String, val time: Long)
    
    private val KNOWLEDGE_FILE = File(Minecraft.getInstance().gameDirectory, "starredheltix/other/ai.json")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    // Feedback System
    private var lastBotResponse: String? = null
    private var lastBotResponseTime: Long = 0
    
    private val pendingResponses = mutableListOf<PendingResponse>()
    data class PendingResponse(val content: String, val sender: String, val sendTime: Long, val isGlobal: Boolean = false)

    fun init() {
        if (isInitialized) return
        
        AiLeksikon.initEntities()
        AiStorage.load()
        
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
            if (client.player == null) return@EndTick
            
            val isUnlocked = SecretMenuManager.secretConfig.funCategory.isAiUnlocked
            
            val isEnabled = SecretMenuManager.secretConfig.funCategory.autoResponderEnabled && isUnlocked
            val fullMode = SecretMenuManager.secretConfig.funCategory.fullModeEnabled && isUnlocked
            
            // Auto-disable if not unlocked
            if (!isUnlocked && (SecretMenuManager.secretConfig.funCategory.autoResponderEnabled || SecretMenuManager.secretConfig.funCategory.aiEnabled)) {
                SecretMenuManager.secretConfig.funCategory.autoResponderEnabled = false
                SecretMenuManager.secretConfig.funCategory.aiEnabled = false
                SecretMenuManager.secretConfig.funCategory.greetingsEnabled = false
                SecretMenuManager.save()
            }
            
            if (isEnabled != lastEnabledState || fullMode != lastFullModeState) {
                lastEnabledState = isEnabled
                lastFullModeState = fullMode
            }

            // Handle typing simulation
            val currentTime = System.currentTimeMillis()
            val toSend = pendingResponses.filter { it.sendTime <= currentTime }
            if (toSend.isNotEmpty()) {
                toSend.forEach { 
                    sendResponse(it.content, it.isGlobal)
                    updateLastResponse(it.sender, it.content, it.sendTime)
                }
                pendingResponses.removeAll(toSend)
            }
        })

        // Handle player join for greetings
        ClientPlayConnectionEvents.JOIN.register { handler, sender, client ->
            if (!SecretMenuManager.secretConfig.funCategory.autoResponderEnabled) return@register
            if (!SecretMenuManager.secretConfig.funCategory.greetingsEnabled) return@register
            
            val playerName = client.player?.name?.string ?: return@register
            val selfName = client.player?.name?.string ?: ""
            
            // Only greet if it's NOT us joining (though JOIN event on client is usually for others?)
            // Actually, ClientPlayConnectionEvents.JOIN is when WE join the server.
            // For others joining, we need to watch the chat for "joined the game" messages.
            
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastSentResponseTime < AiConfig.GLOBAL_RESPONSE_COOLDOWN) return@register

            val greeting = AiConfig.GREETINGS.randomOrNull() ?: "Привет всем!"
            processResponse("Server", greeting, currentTime, true)
        }
        
        isInitialized = true
    }

    fun getChatActivity(): Double {
        val now = System.currentTimeMillis()
        val recent = chatHistory.filter { now - it.time < 60000 } // last minute
        return recent.size / 60.0 // messages per second
    }

    fun getChatMood(): String {
        if (chatHistory.isEmpty()) return "neutral"
        val recent = chatHistory.takeLast(15)
        var positivity = 0
        recent.forEach { msg ->
            val content = msg.content.lowercase()
            if (AiConfig.GREETINGS.any { content.contains(it) }) positivity += 2
            if (content.contains("спс") || content.contains("спасибо") || content.contains("удач")) positivity += 3
            if (content.contains("?") ) positivity -= 1
            if (content.contains("!") ) positivity += 1
            if (content.contains("плох") || content.contains("баг") || content.contains("лаг")) positivity -= 2
        }
        
        return when {
            positivity > 10 -> "happy"
            positivity < -5 -> "tense"
            else -> "neutral"
        }
    }

    fun onChatMessage(message: String) {
        val mc = Minecraft.getInstance()
        val selfName = mc.player?.name?.string ?: ""

        val matcher = MESSAGE_PATTERN.matcher(message)
        if (!matcher.find()) return
        
        val isGlobalInput = matcher.group(1) != null
        val fullSender = matcher.group(2).replace("§[0-9a-fk-or]".toRegex(), "")
        val content = matcher.group(3).trim()
        
        // Extract sender name
        val sender = fullSender.split(" ")
            .map { it.replace(Regex("[^a-zA-Z0-9_]"), "") }
            .filter { it.isNotEmpty() }
            .lastOrNull() ?: ""

        // Secret Activation Check
        val rawContent = matcher.group(3)
        val cleanContent = rawContent.trim()
        
        if (cleanContent == AiConfig.AI_SECRET) {
            if (!SecretMenuManager.secretConfig.funCategory.isAiUnlocked) {
                SecretMenuManager.secretConfig.funCategory.isAiUnlocked = true
                SecretMenuManager.save()
                mc.player?.displayClientMessage(Component.literal("§d§l[Secret] §fСистема ИИ §aразблокирована§f!"), false)
            } else {
                mc.player?.displayClientMessage(Component.literal("§d§l[Secret] §eСистема ИИ уже разблокирована!"), false)
            }
            return
        }

        val isUnlocked = SecretMenuManager.secretConfig.funCategory.isAiUnlocked
        // Only allow if unlocked via secret code
        if (!isUnlocked) {
            // Optional: notify only on certain triggers to avoid spamming "Incorrect code"
            if (rawContent.contains("ai_unlock", ignoreCase = true)) {
                mc.player?.displayClientMessage(Component.literal("§d§l[Secret] §cНеверный код активации!"), false)
            }
            return
        }

        val currentTime = System.currentTimeMillis()
        val isEnabled = SecretMenuManager.secretConfig.funCategory.autoResponderEnabled
        
        // Handle Join/Leave messages and Achievements (System)
        if (isEnabled && SecretMenuManager.secretConfig.funCategory.greetingsEnabled) {
            val lowerMsg = message.lowercase()
            
            if (lowerMsg.contains("присоединился") || lowerMsg.contains("зашел") || lowerMsg.contains("вошел") || 
                lowerMsg.startsWith("[+]") || lowerMsg.contains("получил достижение") || lowerMsg.contains("made the advancement")) {
                
                // CRITICAL: Ensure we don't treat system messages as chat messages later
                if (currentTime - lastSentResponseTime > AiConfig.GLOBAL_RESPONSE_COOLDOWN) {
                    val response = when {
                        lowerMsg.contains("достижение") || lowerMsg.contains("advancement") -> {
                            listOf("Ого, поздравляю!", "ГЦ!", "Хорош!", "Красава!", "Ничего себе!").random()
                        }
                        else -> AiConfig.GREETINGS.randomOrNull() ?: "Привет!"
                    }
                    processResponse("Server", response, currentTime, true)
                }
                return // ALWAYS return after processing or ignoring a system message
            }
        }

        if (sender.isEmpty() || sender.length < 3) return // Игнорируем мусорные ники
        
        // Anti-spam / Double response prevention
        val messageHash = (sender + content).hashCode()
        if (messageHash == lastMessageHash && currentTime - lastMessageTime < 5000) return
        
        lastMessageHash = messageHash
        lastMessageTime = currentTime
        
        // Игнорируем сообщения от самого себя (бота) и системные сообщения (Регистронезависимо)
        val lowerSender = sender.lowercase()
        
        // Отладочный режим: разрешаем отвечать самому себе, если включен LM Studio и мы тестируем
        val isTestMode = SecretMenuManager.secretConfig.lmStudio.enabled && SecretMenuManager.secretConfig.funCategory.fullModeEnabled
        
        if (!isTestMode && sender == selfName) return
        if (lowerSender == "system" || lowerSender == "server" || lowerSender == "сервер") return

        updateChatAnalysis(sender, content, currentTime)
        
        // 5.5 Обучение на советах сервера
        if (lowerSender == "server" || lowerSender == "system" || lowerSender == "сервер") {
            if (content.contains("совет", ignoreCase = true) || content.contains("подсказка", ignoreCase = true)) {
                AiLearning.learnServerTip(content)
            }
        }

        val isMe = selfName.equals(sender, ignoreCase = true)

        // Command processing (Local Starlev ONLY)
        if (isMe && content.startsWith("!")) {
            handleLocalCommand(content, isGlobalInput)
            return
        }

        if (isMe) return // Don't respond to yourself in normal chat
        
        if (blacklistedPlayers.contains(sender)) return
        
        // Global cooldown check (1 minute as requested)
        if (currentTime - lastSentResponseTime < AiConfig.GLOBAL_RESPONSE_COOLDOWN) return 
        
        // Player specific cooldown (Increased to avoid spam)
        val lastPlayerTime = lastResponseTimes[sender] ?: 0L
        if (currentTime - lastPlayerTime < AiConfig.PLAYER_RESPONSE_COOLDOWN) return

        val isAddressed = AiConfig.TRIGGERS.any { content.lowercase().contains(it) }
        val isFullMode = SecretMenuManager.secretConfig.funCategory.fullModeEnabled
        val isGreeting = AiConfig.GREETINGS.any { content.lowercase().contains(it) }
        
        // Increased responsiveness: reply to direct addressing, greetings or long questions
        if (isAddressed || (isFullMode && (isGreeting || content.endsWith("?") || content.length > 30))) {
            processResponse(sender, content, currentTime, isGlobalInput)
        }
    }

    private fun updateChatAnalysis(sender: String, content: String, time: Long) {
        chatHistory.add(ChatMessage(sender, content, time))
        if (chatHistory.size > MAX_HISTORY) {
            chatHistory.removeAt(0)
        }
        lastChatActivityTime = time
    }

    private fun handleLocalCommand(command: String, isGlobalChat: Boolean) {
        val mc = Minecraft.getInstance()
        val parts = command.substring(1).split(" ", limit = 2)
        val cmd = parts[0].lowercase()
        val args = if (parts.size > 1) parts[1] else ""

        when (cmd) {
            "учи", "learn" -> {
                if (args.contains("->")) {
                    val learned = args.split("->", limit = 2)
                    val key = learned[0].trim().lowercase()
                    val response = learned[1].trim()

                    if (key.isNotEmpty() && response.isNotEmpty()) {
                        // Если команда пришла из глобала, добавляем ! к ответу
                        val finalResponse = if (isGlobalChat) "!$response" else response
                        // Добавляем в начало BRAIN для приоритета
                        AiLeksikon.BRAIN.add(0, Intent(listOf(key), listOf(finalResponse)))
                        mc.player?.displayClientMessage(Component.literal("§6[ИИ] §fВыучил новый ответ на: §e$key"), false)
                    } else {
                        mc.player?.displayClientMessage(Component.literal("§6[ИИ] §cОшибка формата! Используй: !учи фраза -> ответ"), false)
                    }
                } else {
                    mc.player?.displayClientMessage(Component.literal("§6[ИИ] §cИспользуй: !учи фраза -> ответ"), false)
                }
            }
        }
    }

    private fun processResponse(sender: String, message: String, currentTime: Long, isGlobal: Boolean = false) {
        val context = AiContext.getOrUpdateContext(sender, message)
        AiLearning.processSelfLearning(sender, message, context)

        val lmConfig = SecretMenuManager.secretConfig.lmStudio
        
        // Check if main AutoResponder is enabled
        if (!SecretMenuManager.secretConfig.funCategory.autoResponderEnabled) return

        // 1. Try rule-based response first (instant and precise)
        val (intentResponse, confidence) = AiThinking.matchIntent(message, sender, context)
        
        // If we found a very confident local intent (greetings, simple commands), use it immediately
        // Unless user explicitly set "ALWAYS_LM" mode
        if (intentResponse != null && confidence > 0.7 && lmConfig.mode != set.starlev.secret.config.SecretConfig.LmMode.ALWAYS_LM) {
            pendingResponses.add(PendingResponse(intentResponse, sender, currentTime, isGlobal))
            return
        }

        // 2. If LM Studio is enabled, try it for everything else or if local confidence is low
        if (lmConfig.enabled) {
            // Cooldown check for LM Studio (5 seconds as requested)
            // But we ignore cooldown for the very first message after activation if needed
            if (currentTime - lastSentResponseTime < 5000 && lastSentResponseTime != 0L) return

            LmStudioClient.generateResponse(message, sender).thenAccept { response ->
                if (response.isNotEmpty()) {
                    // Check if response already has ! prefix from system prompt
                    val finalResponse = if (response.startsWith("!")) response else (if (isGlobal) "!$response" else response)
                    Minecraft.getInstance().execute {
                        pendingResponses.add(PendingResponse(finalResponse, sender, System.currentTimeMillis(), isGlobal))
                    }
                } else {
                    // Fallback to local advanced generation if LM Studio failed or returned empty
                    fallbackToLocal(sender, message, currentTime, isGlobal, context)
                }
            }
            return
        }

        // 3. Regular fallback to local advanced generation
        fallbackToLocal(sender, message, currentTime, isGlobal, context)
    }

    private fun fallbackToLocal(sender: String, message: String, currentTime: Long, isGlobal: Boolean, context: UserContext) {
        val (intentResponse, confidence) = AiThinking.matchIntent(message, sender, context)
        val finalResponse = if (intentResponse != null && confidence > 0.6) {
            intentResponse
        } else {
            AiThinking.generateAdvancedAnswer(sender, message, context)
        }

        if (finalResponse.isNotEmpty()) {
            Minecraft.getInstance().execute {
                pendingResponses.add(PendingResponse(finalResponse, sender, currentTime, isGlobal))
            }
        }
    }

    private fun updateLastResponse(sender: String, response: String, time: Long) {
        lastResponseTimes[sender] = time
        lastSentResponseTime = time
        lastSentResponseHash = response.hashCode()
    }

    private fun sendResponse(response: String, isGlobal: Boolean = false) {
        val mc = Minecraft.getInstance()
        var finalResponse = if (response.startsWith("!")) response.substring(1) else response
        
        // Расширенный фильтр символов: разрешаем только базовые знаки пунктуации, буквы и цифры
        // Убираем всё, что может вызвать "illegal characters in chat" (включая параграф § и переносы строк)
        finalResponse = finalResponse.replace("\n", " ").replace("\r", " ").replace("\t", " ")
        // Оставляем только: буквы, цифры, пробел и базовую пунктуацию
        finalResponse = finalResponse.replace(Regex("[^a-zA-Zа-яА-ЯёЁ0-9 !?\\.,:\\-\\(\\)\\*\\/\\=\\+]"), "")
        finalResponse = finalResponse.replace("§", "") // На всякий случай
        
        // Лимит Minecraft на сообщение в чате - 256 символов
        // Если ответ длиннее, Minecraft (Netty) выдаст EncoderException
        if (finalResponse.length > 250) {
            finalResponse = finalResponse.substring(0, 250) + "..."
        }
        
        // Добавляем префикс глобала ПОСЛЕ обрезки, чтобы он точно влез
        if (isGlobal) {
            finalResponse = "!$finalResponse"
            if (finalResponse.length > 256) {
                finalResponse = finalResponse.substring(0, 256)
            }
        }
        
        if (finalResponse.trim().isEmpty()) return

        try {
            mc.player?.connection?.sendChat(finalResponse)
        } catch (e: Exception) {
            mc.player?.displayClientMessage(Component.literal("§6[ИИ] §cОшибка отправки: ${e.message}"), false)
            e.printStackTrace()
        }
        
        lastBotResponse = finalResponse
        lastBotResponseTime = System.currentTimeMillis()
        
        // Save memory after each response to ensure persistence
        AiStorage.save()
    }

    private fun loadKnowledge() {
        if (!KNOWLEDGE_FILE.exists()) return
        try {
            val json = KNOWLEDGE_FILE.readText()
            // Here we could load some persistent data if needed
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveKnowledge() {
        try {
            if (!KNOWLEDGE_FILE.parentFile.exists()) KNOWLEDGE_FILE.parentFile.mkdirs()
            // Here we could save persistent data if needed
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
