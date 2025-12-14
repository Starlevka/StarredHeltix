package set.starlev.config

import com.google.gson.GsonBuilder
import io.github.notenoughupdates.moulconfig.observer.PropertyTypeAdapterFactory
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import org.slf4j.LoggerFactory
import set.starlev.StarredHeltix
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.concurrent.fixedRateTimer

object ConfigManager {
    private val logger = LoggerFactory.getLogger("StarredHeltix")
    val gson = GsonBuilder().setPrettyPrinting()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeSpecialFloatingPointValues()
        .registerTypeAdapterFactory(PropertyTypeAdapterFactory())
        .enableComplexMapKeySerialization()
        .create()

    lateinit var features: Features

    private var configDirectory = File("config/starredheltix")
    private var configFile: File? = null
    lateinit var processor: MoulConfigProcessor<Features>

    fun firstLoad() {
        configDirectory.mkdirs()
        configFile = File(configDirectory, "config.json")
        logger.info("Загрузка конфига из {}", configFile)

        if (configFile!!.exists()) {
            try {
                val inputStreamReader = InputStreamReader(FileInputStream(configFile!!), StandardCharsets.UTF_8)
                val bufferedReader = BufferedReader(inputStreamReader)
                features = gson.fromJson(bufferedReader.readText(), Features::class.java)
                logger.info("Конфиг загружен")
            } catch (e: Exception) {
                logger.error("Ошибка при чтении конфига $configFile", e)
            }
        }

        if (!this::features.isInitialized) {
            logger.info("Создание нового конфига")
            features = Features()
            saveConfig("blank config")
        }

        logger.info("Инициализация MoulConfig")
        processor = MoulConfigProcessor(StarredHeltix.feature)
        BuiltinMoulConfigGuis.addProcessors(processor)
        val driver = ConfigProcessorDriver(processor)
        driver.warnForPrivateFields = false
        driver.processConfig(StarredHeltix.feature)

        fixedRateTimer(name = "starredheltix-config-auto-save", period = 60_000L, initialDelay = 60_000L) {
            try {
                saveConfig("auto-save-60s")
                logger.debug("Автосохранение конфига")
            } catch (e: Throwable) {
                logger.error("Ошибка автосохранения конфига!", e)
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
            logger.info("Конфиг сохранён: $reason")
        } catch (e: Exception) {
            logger.error("Не удалось сохранить конфиг в $file", e)
        }
    }
}
