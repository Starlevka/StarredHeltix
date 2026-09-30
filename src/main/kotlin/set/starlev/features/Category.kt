package set.starlev.features

/**
 * Категории фич мода — метаданные для [Feature].
 * Для HUD-фильтра используется отдельный [set.starlev.render.HudManager.HudCategory]
 * (упрощённый, 9 значений: Все/Скайблок/Данж/Ферма/Шахта/Рыбалка/Лес/Бой/Другое),
 * который мапится из этого enum через [toHudCategory]. MoulConfig-категории
 * — ещё отдельный слой ([set.starlev.config.Features] аннотации @Category).
 */
enum class Category(val displayName: String) {
    DETECTOR("Детекторы"),
    DUNGEON("Подземелья"),
    CHAT("Чат"),
    INVENTORY("Инвентарь"),
    FARMING("Фермерство"),
    MINING("Шахтёрство"),
    FISHING("Рыболовство"),
    FORAGING("Лесничество"),
    ENCHANTING("Чародейство"),
    SKYBLOCK("Skyblock"),
    VISUAL("Визуальное"),
    MISC("Разное"),
    SECRET("Секретное");

    /** Маппинг в HUD-категорию для единого фильтра в HudEditor (SH-чипы). */
    fun toHudCategory(): set.starlev.render.HudManager.HudCategory = when (this) {
        SKYBLOCK, INVENTORY -> set.starlev.render.HudManager.HudCategory.SKYBLOCK
        DUNGEON -> set.starlev.render.HudManager.HudCategory.DUNGEON
        FARMING -> set.starlev.render.HudManager.HudCategory.FARMING
        MINING -> set.starlev.render.HudManager.HudCategory.MINING
        FISHING -> set.starlev.render.HudManager.HudCategory.FISHING
        FORAGING -> set.starlev.render.HudManager.HudCategory.FORAGING
        ENCHANTING, CHAT, VISUAL, MISC, SECRET, DETECTOR -> set.starlev.render.HudManager.HudCategory.MISC
    }
}
