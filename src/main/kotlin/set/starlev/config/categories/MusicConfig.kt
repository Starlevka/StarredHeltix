package set.starlev.config.categories
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
class MusicConfig {
    @Expose
    @ConfigOption(name = "Отключить звуки взрывов", desc = "Блокирует звуки взрывов (TNT, криперы, фейерверки и др.).")
    @ConfigEditorBoolean
    var disableExplosionSounds = false

    @Expose
    @ConfigOption(name = "Музыка по локациям", desc = "Атмосферная музыка из нотных блоков для каждой локации.")
    @Accordion
    var locationMusic = LocationMusicConfig()

    class LocationMusicConfig {
        @Expose
        @ConfigOption(name = "Включить музыку", desc = "Включает систему музыки по локациям.")
        @ConfigEditorBoolean
        var enabled = false

        @Expose
        @ConfigOption(name = "Громкость музыки", desc = "Громкость музыки независимо от звуков игры (0-2000%).")
        @ConfigEditorSlider(minValue = 0f, maxValue = 2000f, minStep = 10f)
        var musicVolume = 50f

        @Expose
        @ConfigOption(name = "Время перехода", desc = "Длительность плавного перехода между треками (сек).")
        @ConfigEditorSlider(minValue = 1f, maxValue = 5f, minStep = 0.5f)
        var fadeDuration = 2f

        @Expose
        @ConfigOption(name = "Боевые локации", desc = "Адская крепость, Кладбище, Логово Арахны, Руины и др.")
        @Accordion
        var combat = LocationMusicEntryConfig()

        @Expose
        @ConfigOption(name = "Фермерские локации", desc = "Амбар, Грибная пустыня, Оазис, Ферма.")
        @Accordion
        var farming = LocationMusicEntryConfig()

        @Expose
        @ConfigOption(name = "Леснические локации", desc = "Дом викингов, Лес, Джунгли, Саванна и др.")
        @Accordion
        var foraging = LocationMusicEntryConfig()

        @Expose
        @ConfigOption(name = "Шахтёрские локации", desc = "Гномья кузница, Золотая шахта, Глубокие пещеры и др.")
        @Accordion
        var mining = LocationMusicEntryConfig()

        @Expose
        @ConfigOption(name = "Эндер локации", desc = "Драконье гнездо, Пустотный склеп, Энд, Убежище Зеалотов.")
        @Accordion
        var ender = LocationMusicEntryConfig()

        @Expose
        @ConfigOption(name = "Загадочные локации", desc = "Бездна, Гора, Туман.")
        @Accordion
        var mysterious = LocationMusicEntryConfig()

        @Expose
        @ConfigOption(name = "Хаб локации", desc = "Библиотека, Аукцион, Банк, Рынок и др.")
        @Accordion
        var hub = LocationMusicEntryConfig()

        @Expose
        @ConfigOption(name = "Острова", desc = "Остров и Личный остров.")
        @Accordion
        var island = LocationMusicEntryConfig()

        class LocationMusicEntryConfig {
            @Expose
            @ConfigOption(name = "Включить", desc = "Воспроизводить музыку в этой категории локаций.")
            @ConfigEditorBoolean
            var enabled = true
        }
    }
}
