package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class OptimizationConfig {
    @Expose
    @Category(name = "Визуальные оптимизации", desc = "Настройки для повышения FPS или уменьшения визуального мусора")
    var visualOptimizations = VisualOptimizationsConfig()

    @Expose
    @Category(name = "Приоритет процесса", desc = "Отдаёт ОС знать, что это игра: максимальный приоритет потоков рендера и сервера. Действует на весь клиент, нужен перезапуск игры")
    var performance = PerformanceConfig()

    class PerformanceConfig {
        @Expose
        @ConfigOption(name = "Windows", desc = "MMCSS-задача «Games» + HIGH_PRIORITY_CLASS клиенту + HIGHEST потокам рендера и сервера. Взаимоисключающе с Linux. Нужен перезапуск")
        @ConfigEditorBoolean
        var windows = false

        @Expose
        @ConfigOption(name = "Linux", desc = "Feral GameMode (libgamemode → D-Bus), renice -20 процессу, nice -20/-15 потокам рендера и сервера. Взаимоисключающе с Windows. Нужен перезапуск")
        @ConfigEditorBoolean
        var linux = false
    }

    class VisualOptimizationsConfig {
        @Expose
        @ConfigOption(name = "Книга рецептов", desc = "Скрывает кнопку и интерфейс книги рецептов в инвентаре")
        @ConfigEditorBoolean
        var hideRecipeBook = false

        @Expose
        @ConfigOption(name = "Анимация после смерти", desc = "Мгновенно убирает модель моба после его смерти")
        @ConfigEditorBoolean
        var hideDeathAnimation = false

        @Expose
        @ConfigOption(name = "Отображение огня", desc = "Убирает визуальный эффект огня на экране, когда вы горите")
        @ConfigEditorBoolean
        var hideFireOverlay = false

        @Expose
        @ConfigOption(name = "Отображение эффектов", desc = "Убирает отображение иконок статус-эффектов (зелий) в углу экрана")
        @ConfigEditorBoolean
        var hideStatusEffects = false

        @Expose
        @ConfigOption(name = "Фулбрайт", desc = "Режим полной яркости")
        @ConfigEditorBoolean
        var fullbright = false

        @Expose
        @ConfigOption(name = "Частицы ломания блоков", desc = "Отключает частицы, которые появляются при добыче блоков")
        @ConfigEditorBoolean
        var disableBlockBreakingParticles = false

        @Expose
        @ConfigOption(name = "Ники всех мобов", desc = "Скрывает все таблички с именами мобов")
        @ConfigEditorBoolean
        var hideAllMobNames = false

        @Expose
        @ConfigOption(name = "Ники мобов без звёзд", desc = "Скрывает имена мобов подземелья, если у них нет звёзд в нике")
        @ConfigEditorBoolean
        var hideNonStarredDungeonNames = false
    }
}
