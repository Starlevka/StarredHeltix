package set.starlev.features

import net.minecraft.client.Minecraft

/**
 * Базовый класс для фич с категорией — только метаданные + жизненный цикл.
 * Включение/выключение — через MoulConfig ([set.starlev.StarredHeltix.feature]),
 * а не через поле [enabled] (оставлено для совместимости и будущего единого
 * SH-экрана «Фичи по категориям» через [FeatureManager.getByCategory]).
 *
 * Сейчас расширяют лишь 4 объекта (GardenPlotsOverlay, ExperimentSolverManager,
 * EndHighlight, MiniBossHighlight); остальные фичи — plain `object` с `fun init()`,
 * как описано в `docs/architecture.md`. Это нормально: Feature — опциональный.
 */
abstract class Feature(
    val name: String,
    val category: Category,
    val description: String = "",
) {
    protected val mc: Minecraft get() = Minecraft.getInstance()

    /**
     * Уникальный идентификатор (snake_case).
     */
    open val id: String = name.lowercase().replace(" ", "_")

    /**
     * Вызывается один раз при старте мода.
     */
    open fun init() {}

    override fun toString(): String = "Feature($name, $category)"
}
