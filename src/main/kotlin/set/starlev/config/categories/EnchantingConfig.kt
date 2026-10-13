package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class EnchantingConfig {
    @Expose
    @Category(name = "Решатели стола", desc = "Автоматические решения для мини-игр стола экспериментов")
    var tableSolvers = TableSolversConfig()

    class TableSolversConfig {
        @Expose
        @ConfigOption(name = "Суперпары", desc = "Автоматическое решение для мини-игры Суперпары. Подсвечивает совпадающие пары предметов")
        @ConfigEditorBoolean
        var superpairs = true

        @Expose
        @ConfigOption(name = "Показывать предметы в Суперпарах", desc = "Визуально заменяет стекло на ранее открытый предмет")
        @ConfigEditorBoolean
        var superpairsShowItems = true

        @Expose
        @ConfigOption(name = "Подсветка одинаковых пар", desc = "Подсвечивает найденные одинаковые пары зеленым (отдельно отключаемая подсветка Суперпар)")
        @ConfigEditorBoolean
        var superpairsShowCollected = true

        @Expose
        @ConfigOption(name = "Ритмотрон", desc = "Автоматическое решение для мини-игры Ритмотрон. Запоминает и показывает последовательность")
        @ConfigEditorBoolean
        var chronomatron = true

        @Expose
        @ConfigOption(name = "Секвенсор", desc = "Автоматическое решение для мини-игры Секвенсор. Подсвечивает следующий слот для нажатия")
        @ConfigEditorBoolean
        var ultrasequencer = true

        @Expose
        @ConfigOption(name = "Блокировка неправильных кликов", desc = "Блокирует нажатия на неправильные слоты в мини-играх")
        @ConfigEditorBoolean
        var blockIncorrectClicks = false
    }
}
