package set.starlev.config.highlight

/**
 * Цель подсветки (моб или сущность).
 *
 * Используется в [HighlightConfig] как метаданные: отображаемое имя в конфиге
 * и цвет по умолчанию. Новые цели добавляются сюда, а в существующих конфиг-классах
 * остаётся ссылка на элемент enum-а вместо дублирования полей.
 *
 * Существующие конфиги в `config/categories/CombatConfig.kt` остаются без изменений —
 * этот enum нужен как шаблон для **новых** подсветок, чтобы не плодить копипасту.
 */
enum class HighlightTarget(
    /** Имя для UI конфига (на русском). */
    val displayName: String,
    /** Цвет по умолчанию в формате MoulConfig: "0:R:G:B:A" или "0:R:G:B". */
    val defaultColor: String
) {
    ENDERMAN("Эндермены", "255:0:255:255:0"),
    CREEPER("Криперы", "255:0:255:0:0"),
    WOLF("Волки", "255:200:200:200:0"),
    SPIDER("Пауки", "255:30:30:80:0"),
    CAVE_SPIDER("Пещерные пауки", "255:120:0:120:0"),
    ZOMBIE("Зомби", "255:0:180:0:0"),
    SLAYER_BOSS_ZOMBIE("Слеер Зомби", "255:0:255:0:0"),
    SLAYER_BOSS_SPIDER("Слеер Паук", "255:0:165:255:0"),
    SLAYER_BOSS_WOLF("Слеер Волк", "255:255:255:255:0");

    companion object {
        /** Поиск по имени (для импорта/экспорта). */
        fun byName(name: String): HighlightTarget? = entries.firstOrNull { it.name == name }
    }
}
