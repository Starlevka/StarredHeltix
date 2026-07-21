package set.starlev.config.highlight

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

/**
 * Базовый конфиг подсветки моба/сущности.
 *
 * Использовать в новых конфиг-классах вместо ручного объявления полей `enabled/box/outline/...`:
 *
 * ```kotlin
 * class HighlightCategoryConfig {
 *     @Expose @ConfigOption(name = "Эндермены", desc = "...")
 *     @Accordion
 *     var enderman = HighlightConfig(HighlightTarget.ENDERMAN)
 *
 *     @Expose @ConfigOption(name = "Криперы", desc = "...")
 *     @Accordion
 *     var creeper = HighlightConfig(HighlightTarget.CREEPER)
 * }
 * ```
 *
 * Преимущества:
 * — Один источник истины для набора полей подсветки (меньше копипасты).
 * — Цвет по умолчанию берётся из [HighlightTarget.defaultColor].
 * — Если завтра добавится, скажем, `thickness` для обводки — правим одно место.
 *
 * Ограничения:
 * — MoulConfig показывает поля через `@ConfigEditor*` по имени, поэтому каждое поле
 *   должно быть `var` (не `val`) и иметь `@Expose`.
 * — Цель (target) хранится только в коде, не в конфиге — пользователь не выбирает
 *   цель через UI, это просто метаданные для дефолтов.
 */
class HighlightConfig(val target: HighlightTarget) {

    @Expose
    @ConfigOption(name = "Подсветка", desc = "Активирует обводку для цели «${'$'}targetDisplayName».")
    @ConfigEditorBoolean
    var enabled = false

    @Expose
    @ConfigOption(name = "=> Бокс", desc = "Рисует бокс вокруг сущности (заливка или контур).")
    @ConfigEditorBoolean
    var box = true

    @Expose
    @ConfigOption(name = "=> Обводка", desc = "Будет обводить в виде хитбокса сущность.")
    @ConfigEditorBoolean
    var outline = false

    @Expose
    @ConfigOption(name = "=> Glow", desc = "Включает ванильное свечение (outline shader) для сущности.")
    @ConfigEditorBoolean
    var glow = false

    @Expose
    @ConfigOption(name = "Цвет", desc = "Выберите цвет обводки.")
    @ConfigEditorColour
    var colorV2: String = target.defaultColor

    @Expose
    @ConfigOption(name = "Прозрачность", desc = "Установите уровень прозрачности подсветки.")
    @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.05f)
    var transparency = 0.5f

    /** Локализованное имя цели — используется только в UI конфига. */
    private val targetDisplayName: String get() = target.displayName
}