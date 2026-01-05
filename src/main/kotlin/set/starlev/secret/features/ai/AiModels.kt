package set.starlev.secret.features.ai

data class UserContext(
    var lastMessages: MutableList<String> = mutableListOf(),
    var lastPlayerMessages: MutableList<String> = mutableListOf(), // Контекст именно игрока
    var preferences: MutableMap<String, String> = mutableMapOf(),
    var memories: MutableList<String> = mutableListOf(),
    var conversationTopic: String? = null,
    var subTopic: String? = null,
    var currentSubject: String? = null,
    var mood: String = "neutral",
    var patience: Int = 100,
    var interactionCount: Int = 0,
    var lastInteractionTime: Long = System.currentTimeMillis(),
    var lastMoodChange: Long = System.currentTimeMillis(),
    var firstMeetTime: Long = System.currentTimeMillis(),
    var thoughtChain: MutableList<String> = mutableListOf(),
    var lastBotQuestion: String? = null,
    var lastBotResponse: String? = null,
    var recentBotResponses: MutableList<String> = mutableListOf(), // История последних ответов этому игроку
    var lastIntents: MutableList<String> = mutableListOf(),
    var topicHistory: MutableList<String> = mutableListOf(),
    var literacyScore: Double = 1.0, // Показатель грамотности игрока
    var respectLevel: Int = 50       // Уровень уважения бота к игроку (0-100)
)

enum class JudgmentType {
    LOGICAL,        // Logic check
    SENTIMENT,      // Tone check
    RELEVANCE,      // Relevance to server
    ABSURDITY,      // Checking for nonsense
    SUSPICIOUS      // Checking for harmful/suspicious intents
}

data class Judgment(
    val type: JudgmentType,
    val score: Float,
    val conclusion: String,
    val internalComment: String
)

data class Intent(
    val keywords: List<String>,
    val responses: List<String>,
    var trustScore: Double = 1.0,    // Доверие к этой записи (0.0 - 5.0)
    var source: String = "base",     // Кто обучил: "base", "starlev", "community"
    var category: String = "general" // Категория: "media", "top", "fact", "joke"
)

enum class EntityRole {
    MEDIA,      // Блогеры, ютуберы
    TOP,        // Топ игроки (богатые, сильные)
    ADMIN,      // Администрация
    HELPER,     // Помощники
    PLAYER,     // Обычные игроки
    UNKNOWN     // Неизвестно
}

data class WorldEntity(
    val name: String,
    var role: EntityRole = EntityRole.UNKNOWN,
    var reputation: Int = 50,        // 0-100
    var authority: Double = 1.0,    // Вес мнения (0.1 - 5.0)
    var tags: MutableSet<String> = mutableSetOf(),
    var lastSeen: Long = System.currentTimeMillis()
)
