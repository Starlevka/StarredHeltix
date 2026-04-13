package set.starlev.config

import com.google.gson.GsonBuilder
import io.github.notenoughupdates.moulconfig.observer.PropertyTypeAdapterFactory
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import set.starlev.StarredHeltix
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.concurrent.fixedRateTimer

object ConfigManager {
    val gson = GsonBuilder().setPrettyPrinting()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeSpecialFloatingPointValues()
        .registerTypeAdapterFactory(PropertyTypeAdapterFactory())
        .enableComplexMapKeySerialization()
        .create()

    lateinit var features: Features

    fun getFeaturesSafe(): Features {
        return if (this::features.isInitialized) {
            features
        } else {
            Features()
        }
    }

    private var configDirectory = File("config/starredheltix")
    private var configFile: File? = null
    lateinit var processor: MoulConfigProcessor<Features>

    fun firstLoad() {
        configDirectory.mkdirs()
        configFile = File(configDirectory, "config.json")

        if (configFile!!.exists()) {
            try {
                val inputStreamReader = InputStreamReader(FileInputStream(configFile!!), StandardCharsets.UTF_8)
                val bufferedReader = BufferedReader(inputStreamReader)
                features = gson.fromJson(bufferedReader.readText(), Features::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!this::features.isInitialized) {
            features = Features()
            saveConfig("blank config")
        }

        if (!features.misc.general.migratedInventoryHistoryToMisc) {
            val old = features.visuals.inventoryHistory
            val target = features.misc.general.inventoryHistory
            target.enabled = old.enabled
            target.duration = old.duration
            target.maxEntries = old.maxEntries
            target.showBackground = old.showBackground
            target.ignoreEquipped = old.ignoreEquipped
            features.misc.general.migratedInventoryHistoryToMisc = true
            saveConfig("migrate-inventory-history-to-misc")
        }

        processor = MoulConfigProcessor(StarredHeltix.feature)
        BuiltinMoulConfigGuis.addProcessors(processor)
        val driver = ConfigProcessorDriver(processor)
        driver.warnForPrivateFields = false
        driver.processConfig(StarredHeltix.feature)

        fixedRateTimer(name = "starredheltix-config-auto-save", period = 600_000L, initialDelay = 600_000L) {
            try {
                saveConfig("auto-save-600s")
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun saveConfig(reason: String) {
        val file = configFile ?: throw Error("Не удалось сохранить конфиг, configFile = null")
        try {
            file.parentFile.mkdirs()
            BufferedWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)).use { writer ->
                writer.write(gson.toJson(StarredHeltix.feature))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}