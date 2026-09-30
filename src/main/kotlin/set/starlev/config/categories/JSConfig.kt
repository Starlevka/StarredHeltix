package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import net.minecraft.util.Util
import set.starlev.scripting.ModuleLoader
import set.starlev.scripting.ScriptConsole
import java.io.File

/**
 * Категория настроек для JavaScript-модулей в меню мода (/sh).
 */
class JSConfig {

    @Expose
    @ConfigOption(name = "Включить?", desc = "Разрешает загрузку и выполнение JavaScript модулей из config/starredheltix/modules")
    @ConfigEditorBoolean
    var enabled = true

    @Expose
    @ConfigOption(name = "Ошибки в чат", desc = "Отображать ошибки выполнения скриптов красным сообщением в игровой чат")
    @ConfigEditorBoolean
    var logErrorsToChat = true

    @Expose
    @ConfigOption(name = "Предупреждения в чат", desc = "Отображать предупреждения скриптов желтым сообщением в игровой чат")
    @ConfigEditorBoolean
    var logWarningsToChat = true

    @ConfigOption(name = "Папка модулей", desc = "Открывает папку config/starredheltix/modules в проводнике")
    @ConfigEditorButton(buttonText = "Открыть")
    val openFolderButton: Runnable = Runnable {
        try {
            val dir = ModuleLoader.modulesDir
            dir.mkdirs()
            Util.getPlatform().openFile(dir)
        } catch (e: Exception) {
            ScriptConsole.error("Config", "Не удалось открыть папку модулей: ${e.message}")
        }
    }

    @ConfigOption(name = "Перезагрузить?", desc = "Горячая перезагрузка всех JS-модулей без перезапуска Minecraft")
    @ConfigEditorButton(buttonText = "Да :L")
    val reloadModulesButton: Runnable = Runnable {
        ModuleLoader.loadAll()
    }

    @ConfigOption(name = "Менеджер модулей", desc = "Открыть GUI управления модулями: включение/выключение и настройки (config.json) каждого модуля")
    @ConfigEditorButton(buttonText = "Открыть")
    val modulesGuiButton: Runnable = Runnable {
        set.starlev.scripting.ModuleConfigGui.open()
    }
}
