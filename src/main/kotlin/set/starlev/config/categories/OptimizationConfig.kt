package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class OptimizationConfig {
    @Expose
    @ConfigOption(name = "Визуальные оптимизации", desc = "Настройки для повышения FPS и уменьшения визуального мусора.")
    @Accordion
    var visualOptimizations = VisualOptimizationsConfig()

    @Expose
    @ConfigOption(name = "Рендер оптимизации", desc = "Оптимизации рендера: куллинг, чанки, погода.")
    @Accordion
    var renderOptimizations = RenderOptimizationsConfig()

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
        @ConfigOption(name = "Fullbright", desc = "Режим полной яркости.")
        @ConfigEditorBoolean
        var fullbright = false

        @Expose
        @ConfigOption(name = "Убрать ночное зрение", desc = "Отключает эффект ночного зрения при включенном Fullbright.")
        @ConfigEditorBoolean
        var fullbrightRemoveNightVision = false

        @Expose
        @ConfigOption(name = "Скрыть частицы блоков", desc = "Убирает частицы, которые появляются при ломании блоков.")
        @ConfigEditorBoolean
        var disableBlockBreakingParticles = false
    }

    class RenderOptimizationsConfig {
        @Expose
        @ConfigOption(name = "Куллинг блок-энтити", desc = "Не рендерит block entity (сундуки, печи и т.д.) за пределами видимости камеры.")
        @ConfigEditorBoolean
        var blockEntityCulling = true

        @Expose
        @ConfigOption(name = "Куллинг энтити", desc = "Не рендерит сущности (мобы, игроки, предметы) за пределами видимости камеры.")
        @ConfigEditorBoolean
        var entityCulling = true

        @Expose
        @ConfigOption(name = "Куллинг партиклов", desc = "Не рендерит частицы за пределами видимости камеры.")
        @ConfigEditorBoolean
        var particleCulling = true

        @Expose
        @ConfigOption(name = "Приоритеты чанков", desc = "Понижает приоритет потоков загрузки чанков для снижения нагрузки на CPU.")
        @ConfigEditorBoolean
        var chunkOptimization = true

        @Expose
        @ConfigOption(name = "Оптимизация погоды", desc = "Отключает рендер дождя и снега для повышения FPS.")
        @ConfigEditorBoolean
        var weatherOptimization = false
    }
}
