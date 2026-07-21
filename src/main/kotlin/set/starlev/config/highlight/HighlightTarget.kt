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
    ENDERMAN("Эндермены", "0:255:255:0:255"),
    CREEPER("Криперы", "0:0:255:0:255"),
    WOLF("Волки", "0:200:200:200:255"),
    SPIDER("Пауки", "0:80:30:30:255"),
    CAVE_SPIDER("Пещерные пауки", "0:120:0:120:255"),
    ZOMBIE("Зомби", "0:0:180:0:255"),
    SLAYER_BOSS_ZOMBIE("Слеер Зомби", "0:0:255:0:255"),
    SLAYER_BOSS_SPIDER("Слеер Паук", "0:255:165:0:255"),
    SLAYER_BOSS_WOLF("Слеер Волк", "0:255:255:255:255");

    companion object {
        /** Поиск по имени (для импорта/экспорта). */
        fun byName(name: String): HighlightTarget? = entries.firstOrNull { it.name == name }
    }
}