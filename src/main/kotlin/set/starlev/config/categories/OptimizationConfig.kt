package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class OptimizationConfig {
    @Expose
    @ConfigOption(name = "Визуальные оптимизации", desc = "Настройки для повышения FPS и уменьшения визуального мусора.")
    @Accordion
    var visualOptimizations = VisualOptimizationsConfig()

    @Expose
    @ConfigOption(name = "§bКэширование", desc = "§b(БЕТА) Настройки внутреннего кэширования для ускорения работы мода.")
    @Accordion
    var performance = PerformanceConfig()

    @Expose
    @ConfigOption(name = "Оптимизация сущностей", desc = "Настройки оптимизации ArmorStands и Display Entities.")
    @Accordion
    var entityOptimization = EntityOptimizationConfig()

    class VisualOptimizationsConfig {
        @Expose
        @ConfigOption(name = "Убрать анимацию после смерти", desc = "Мгновенно убирает модель моба после его смерти.")
        @ConfigEditorBoolean
        var hideDeathAnimation = false

        @Expose
        @ConfigOption(name = "Скрыть огонь", desc = "Убирает визуальный эффект огня на экране, когда вы горите.")
        @ConfigEditorBoolean
        var hideFireOverlay = false

        @Expose
        @ConfigOption(name = "Скрыть эффекты", desc = "Убирает отображение иконок статус-эффектов (зелий) в углу экрана.")
        @ConfigEditorBoolean
        var hideStatusEffects = false

        @Expose
        @ConfigOption(name = "Скрыть свечение", desc = "Отключает эффект свечения у всех сущностей для повышения FPS.")
        @ConfigEditorBoolean
        var disableGlowing = false

        @Expose
        @ConfigOption(name = "Fullbright", desc = "Режим полной яркости.")
        @ConfigEditorBoolean
        var fullbright = false

        @Expose
        @ConfigOption(name = "Скрыть частицы ломания блоков", desc = "Убирает частицы, которые появляются при ломании блоков.")
        @ConfigEditorBoolean
        var disableBlockBreakingParticles = false
    }

    class PerformanceConfig {
        @Expose
        @ConfigOption(name = "Scoreboard", desc = "Кэширует текст скорборда для уменьшения нагрузки на CPU.")
        @ConfigEditorBoolean
        var cacheScoreboard = true

        @Expose
        @ConfigOption(name = "Лог предметов", desc = "Кэширует описание предметов. Помогает при большом количестве предметов в инвентаре.")
        @ConfigEditorBoolean
        var cacheItemLore = true

        @Expose
        @ConfigOption(name = "Кэщ", desc = "Ускоряет работу детекторов чата и других функций. §cДай бог.")
        @ConfigEditorBoolean
        var cacheRegex = true
    }

    class EntityOptimizationConfig {
        @Expose
        @ConfigOption(name = "Culling стоек брони", desc = "Не рендерит стойки для брони, которые находятся слишком далеко.")
        @ConfigEditorBoolean
        var cullArmorStands = false

        @Expose
        @ConfigOption(name = "Дистанция стоек брони", desc = "Максимальное расстояние для рендеринга ArmorStands.")
        @ConfigEditorSlider(minValue = 8f, maxValue = 64f, minStep = 1f)
        var armorStandDistance = 32f

        @Expose
        @ConfigOption(name = "Оптимизация голограмм", desc = "Отключает лишние вычисления для статических стоек для брони. Не очень помогает.")
        @ConfigEditorBoolean
        var optimizeHolograms = false

        @Expose
        @ConfigOption(name = "Culling дисплей-мобов", desc = "Оптимизирует рендеринг Block Display и Text Display.")
        @ConfigEditorBoolean
        var cullDisplayEntities = false

        @Expose
        @ConfigOption(name = "Дистанция дисплей-мобов", desc = "Максимальное расстояние для рендеринга Display сущностей.")
        @ConfigEditorSlider(minValue = 16f, maxValue = 128f, minStep = 1f)
        var displayEntityDistance = 64f

        @Expose
        @ConfigOption(name = "Culling частиц", desc = "Не рендерит частицы, которые находится слишком далеко.")
        @ConfigEditorBoolean
        var optimizeParticles = false

        @Expose
        @ConfigOption(name = "Дистанция частиц", desc = "Максимальное расстояние для отрисовки частиц.")
        @ConfigEditorSlider(minValue = 4f, maxValue = 64f, minStep = 1f)
        var particleDistance = 24f
    }
}
