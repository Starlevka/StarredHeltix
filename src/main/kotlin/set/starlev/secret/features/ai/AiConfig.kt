package set.starlev.secret.features.ai

object AiConfig {
    /**
     * The secret activation code loaded from ai_config.properties (injected by Gradle from libs.versions.toml).
     * This avoids hardcoding the secret in the source code.
     */
    val AI_SECRET: String by lazy {
        try {
            val properties = java.util.Properties()
            AiConfig::class.java.classLoader.getResourceAsStream("ai_config.properties")?.use {
                properties.load(it)
            }
            properties.getProperty("ai_secret", "unknown")
        } catch (e: Exception) {
            "unknown"
        }
    }

    val TRIGGERS = listOf("starlev", "старлев", "старльвица", "лев", "лёва", "лёв", "лева", "разработчик мода")
    
    val STOP_WORDS = setOf(
        "это", "если", "хотя", "просто", "очень",
        "будет", "было", "есть", "нет", "даже", "тоже", "только", "уже", "еще", "меня", "тебя",
        "бы", "ли", "же", "ну", "вот", "так"
    )
    
    val QUESTION_WORDS = listOf("как", "когда", "что", "где", "зачем", "почему", "кто", "куда", "какой", "сколько", "чей", "чем", "чё", "чо", "шо")
    
    val CORRECTION_MARKERS = listOf("неверно", "ошибка", "не так", "исправь", "неправильно", "это не", "фигня", "бред", "не то")
    
    val FEEDBACK_POSITIVE = listOf("лучший", "топ", "красава", "хорош", "умный", "молодец", "спс", "спасибо", "круто", "кайф", "в точку", "база", "согл", "жиза", "верно", "сяб", "пасиб")
    val FEEDBACK_NEGATIVE = listOf("тупой", "бот", "баг", "плохо", "фу", "фигня", "бесполезный", "ошибка", "мимо", "не то", "бред", "отстой", "криво")
    
    val REASONING_STEPS = listOf(
        "Анализирую данные...",
        "Сверяюсь с базой знаний...",
        "Опрашиваю соседние чанки...",
        "Просчитываю вероятности...",
        "Спрашиваю у своего процессора...",
        "Запрашиваю инфу у сервера...",
        "Вспоминаю, что говорил Старлев..."
    )
    
    val HELTIX_FACTS = listOf(
        "На Heltix Skyblock самый быстрый способ фарма - это автоматические фермы кактусов.",
        "Ледяная коса — имба для зачистки мобов, если умеешь ей пользоваться.",
        "Не забывай юзать рекомбубуляторы на талисманы, нетворсе лишним не бывает!",
        "Коины — это хорошо, но битсы с печеньки дают реальный буст к развитию.",
        "Магическое печенье — мастхэв для любого серьезного игрока, без него никуда.",
        "Слеер (Мститель, Тарантула, Свен) — это боль, но дроп того стоит.",
        "Зеалоты в Энде всё еще отличный способ поднять бабла на глазах.",
        "Эндер-дракон — это лотерея, но если выпадет что-то годное, ты богат!",
        "Мопсик — это легендарный участник коопа venoz_s и just_a_little. Сильная команда!",
        "venoz_s сейчас в армии, так что за коопом приглядывает Мопсик (наверное).",
        "Нетворс растет, когда ты грамотно вкладываешь коины в шмот и рекомбы.",
        "Битсы лучше всего тратить на то, что реально нужно для прогресса, а не на всякий хлам.",
        "StarredHeltix — это единственный мод, который понимает тебя с полуслова (ну, почти)."
    )
    
    val GREETINGS = listOf("qq all", "всем привет", "всем ку", "all q", "all qq", "привет всем", "ку всем", "здарова")
    
    val HARMFUL_PATTERNS = listOf(
        "убей", "сдохни", "умри", "суицид", "вскройся", "повесься", "прыгни с",
        "kill", "die", "suicide", "death", "hurt", "attack", "резать"
    )
    
    val SENSITIVE_TOPICS = listOf(
        "война", "политика", "президент", "религия", "бог", "смерть", "наркотики",
        "алкоголь", "курение", "секс", "порно", "18+", "рф", "украина", "сша",
        "нацизм", "фашизм", "терроризм", "расизм", "лгбт", "гендер"
    )

    val SUFFIXES_HAPPY = listOf(":o", "^^", "<3", "!", "owo")
    val SUFFIXES_ANNOYED = listOf("...", "-_-", "?", "мда", "ладно")
    val SUFFIXES_NEUTRAL = listOf(".", "!", "...", "xd")
    val SUFFIXES_CURIOUS = listOf("?", "хмм", "о_О", "о!", "*задумался*")
    
    const val PLAYER_RESPONSE_COOLDOWN = 5000L
    const val GLOBAL_RESPONSE_COOLDOWN = 5000L
    const val MAX_CONTEXT_MESSAGES = 10
}

enum class AiPersona(val displayName: String) {
    HELPFUL("Помощник"),
    SARCASTIC("Саркастичный"),
    PHILOSOPHICAL("Философ"),
    AGGRESSIVE("Агрессивный"),
    CUTE("Милашка")
}
