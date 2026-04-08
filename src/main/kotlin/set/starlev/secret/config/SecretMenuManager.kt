package set.starlev.secret.config

import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import set.starlev.StarredHeltix
import set.starlev.utils.ConfigUtils
import set.starlev.config.ConfigManager
import set.starlev.features.chat.mod.ModerationManager
import java.io.*
import java.nio.charset.StandardCharsets

object SecretMenuManager {
    private val secretFile = File("config/starredheltix/others/secret.json")
    lateinit var secretConfig: SecretConfig
    
    val isConfigInitialized: Boolean
        get() = this::secretConfig.isInitialized
    
    lateinit var processor: MoulConfigProcessor<SecretConfig>

    fun load(forceSave: Boolean = true) {
        if (secretFile.exists()) {
            try {
                val reader = BufferedReader(InputStreamReader(FileInputStream(secretFile), StandardCharsets.UTF_8))
                secretConfig = ConfigManager.gson.fromJson(reader.readText(), SecretConfig::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!this::secretConfig.isInitialized) {
            secretConfig = SecretConfig()
            if (forceSave) save()
        }

        processor = MoulConfigProcessor(secretConfig)
        BuiltinMoulConfigGuis.addProcessors(processor)
        ConfigProcessorDriver(processor).apply {
            warnForPrivateFields = false
            processConfig(secretConfig)
        }
    }

    fun save() {
        try {
            secretFile.parentFile.mkdirs()
            val writer = BufferedWriter(OutputStreamWriter(FileOutputStream(secretFile), StandardCharsets.UTF_8))
            writer.use { it.write(ConfigManager.gson.toJson(secretConfig)) }
            // Очищаем кэш эффектов текста, чтобы применить изменения
            set.starlev.utils.CacheManager.clearAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun open() {
        if (!this::secretConfig.isInitialized) load()

        // Обновляем статус перед открытием
        val mc = net.minecraft.client.Minecraft.getInstance()
        val player = mc.player?.name?.string ?: "Unknown"
        val isAdmin = ModerationManager.isAdmin(player)
        val isMod = ModerationManager.isModerator(player)

        val statusText = when {
            isAdmin -> "§4§lАдминистратор"
            isMod -> "§2§lМодератор"
            else -> "§7§lИгрок"
        }

        secretConfig.main.statusInfo = statusText
        secretConfig.isStaff = isAdmin || isMod

        // Фильтруем категории вручную
        val allCategories = processor.allCategories
        val visibleCategories = LinkedHashMap<String, io.github.notenoughupdates.moulconfig.processor.ProcessedCategory>()
        
        for ((id, category) in allCategories) {
            // Скрываем категорию модерации, если игрок не персонал
            if (id.contains("moderation") && !secretConfig.isStaff) continue
            visibleCategories[id] = category
        }

        val editor = io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor(visibleCategories, secretConfig)
        
        // Принудительно закрываем текущий экран и устанавливаем новый в следующем тике
        val screen = io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent(
            net.minecraft.network.chat.Component.empty(),
            io.github.notenoughupdates.moulconfig.gui.GuiContext(io.github.notenoughupdates.moulconfig.gui.GuiElementComponent(editor)),
            null
        )
        
        StarredHeltix.screenToOpen = screen
    }
}
