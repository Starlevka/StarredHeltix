package set.starlev.config

import com.google.gson.GsonBuilder
import io.github.notenoughupdates.moulconfig.observer.PropertyTypeAdapterFactory
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import set.starlev.StarredHeltix
import set.starlev.features.skyblock.scoreboard.ScoreboardConfigElement
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64
import kotlin.concurrent.fixedRateTimer

object ConfigManager {
    val gson = GsonBuilder().setPrettyPrinting()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeSpecialFloatingPointValues()
        .registerTypeAdapterFactory(PropertyTypeAdapterFactory())
        .enableComplexMapKeySerialization()
        .create()

    private const val ENC_PREFIX = "ENC:"
    private const val ENC_KEY = "StarredHeltixSalt2026_Starlevka"

    fun encodePassword(raw: String): String {
        if (raw.isEmpty()) return ""
        val bytes = raw.toByteArray(Charsets.UTF_8)
        val key = ENC_KEY.toByteArray(Charsets.UTF_8)
        for (i in bytes.indices) bytes[i] = (bytes[i].toInt() xor key[i % key.size].toInt()).toByte()
        return ENC_PREFIX + Base64.getEncoder().encodeToString(bytes)
    }

    fun decodePassword(enc: String): String {
        if (!enc.startsWith(ENC_PREFIX)) return enc
        return try {
            val b64 = enc.removePrefix(ENC_PREFIX)
            val bytes = Base64.getDecoder().decode(b64)
            val key = ENC_KEY.toByteArray(Charsets.UTF_8)
            for (i in bytes.indices) bytes[i] = (bytes[i].toInt() xor key[i % key.size].toInt()).toByte()
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) { "" }
    }

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
    private var autoSaveTimer: java.util.Timer? = null

    fun cancelAutoSave() {
        try { autoSaveTimer?.cancel() } catch (_: Exception) {}
        autoSaveTimer = null
    }

    fun firstLoad() {
        configDirectory.mkdirs()
        configFile = File(configDirectory, "config.json")

        if (configFile!!.exists()) {
            try {
                val inputStreamReader = InputStreamReader(FileInputStream(configFile!!), StandardCharsets.UTF_8)
                val bufferedReader = BufferedReader(inputStreamReader)
                features = gson.fromJson(bufferedReader.readText(), Features::class.java)
                // Миграция пароля: ENC:... -> plain в памяти, plain -> останется plain до первого save (зашифруется)
                val pwd = features.skyblock.autoLogin.password
                if (pwd.startsWith(ENC_PREFIX)) {
                    features.skyblock.autoLogin.password = decodePassword(pwd)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!this::features.isInitialized) {
            features = Features()
            saveConfig("blank config")
        }

        if (!features.skyblock.migratedInventoryHistoryToMisc) {
            val old = features.visuals.inventoryHistory
            val target = features.skyblock.inventoryHistory
            target.enabled = old.enabled
            target.duration = old.duration
            target.maxEntries = old.maxEntries
            target.showBackground = old.showBackground
            target.ignoreEquipped = old.ignoreEquipped
            features.skyblock.migratedInventoryHistoryToMisc = true
            saveConfig("migrate-inventory-history-to-skyblock")
        }

        // Миграция раскрытия «Общее» в корень Скайблока: переносим значения
        // из legacy-контейнера general.* в плоские поля (только если плоские ещё дефолтные,
        // чтобы не затереть уже настроенное; массивы/объекты копируем целиком).
        if (!features.skyblock.migratedGeneralFlatten) {
            val sb = features.skyblock
            val legacy = sb.general
            // autoSprint: переносим только включение (false->false ничего не меняет)
            if (legacy.autoSprint) sb.autoSprint = true
            // inventoryHistory / inventoryButtons: переносим если legacy отличен от дефолта.
            // Эвристика: legacy считаем «настроенным», если включён или плоский ещё дефолтный.
            if (legacy.inventoryHistory.enabled || !sb.inventoryHistory.enabled) {
                sb.inventoryHistory.enabled = legacy.inventoryHistory.enabled
                sb.inventoryHistory.duration = legacy.inventoryHistory.duration
                sb.inventoryHistory.maxEntries = legacy.inventoryHistory.maxEntries
                sb.inventoryHistory.showBackground = legacy.inventoryHistory.showBackground
                sb.inventoryHistory.ignoreEquipped = legacy.inventoryHistory.ignoreEquipped
            }
            if (legacy.inventoryButtons.enabled || !sb.inventoryButtons.enabled) {
                sb.inventoryButtons.enabled = legacy.inventoryButtons.enabled
                sb.inventoryButtons.onlyInventory = legacy.inventoryButtons.onlyInventory
                sb.inventoryButtons.tabBackgroundColorV2 = legacy.inventoryButtons.tabBackgroundColorV2
                sb.inventoryButtons.tabHoverBackgroundColorV2 = legacy.inventoryButtons.tabHoverBackgroundColorV2
                sb.inventoryButtons.tabBorderColorV2 = legacy.inventoryButtons.tabBorderColorV2
                sb.inventoryButtons.tabHoverBorderColorV2 = legacy.inventoryButtons.tabHoverBorderColorV2
            }
            sb.migratedGeneralFlatten = true
            saveConfig("migrate-general-flatten")
        }

        // Пост-чистка скорборда: после удаления элементов enum (например PURSE) Gson
        // превращает неизвестные имена в null — null-элементы крашат рендер скорборда
        // и редактор перетаскивания в конфиг-гуе.
        run {
            val entries = features.skyblock.scoreboard.scoreboardEntries
            val cleaned = entries.filterNotNull()
            if (cleaned.size != entries.size) {
                StarredHeltix.LOGGER.info("[Config] Санитайзер: убрано ${entries.size - cleaned.size} битых элементов порядка скорборда")
                entries.clear()
                entries.addAll(cleaned)
                saveConfig("sanitize-scoreboard-entries")
            }
        }

        // Миграция: строка «Текущая сила» (POWER) добавлена позже остальных —
        // у существующих конфигов её нет в сохранённом порядке, и элемент
        // вообще не появляется в списке. Вставляем после BPS один раз;
        // если пользователь потом удалит строку сам — повторно не добавляем.
        if (!features.skyblock.scoreboard.migratedPowerLine) {
            val entries = features.skyblock.scoreboard.scoreboardEntries
            if (entries.none { it == ScoreboardConfigElement.POWER }) {
                val bpsIdx = entries.indexOfFirst { it == ScoreboardConfigElement.BPS }
                if (bpsIdx >= 0) entries.add(bpsIdx + 1, ScoreboardConfigElement.POWER)
                else entries.add(ScoreboardConfigElement.POWER)
                StarredHeltix.LOGGER.info("[Config] Миграция: строка «Текущая сила» добавлена в порядок скорборда")
            }
            features.skyblock.scoreboard.migratedPowerLine = true
            saveConfig("migrate-power-scoreboard-line")
        }

        // Миграция старой системы анимации удара в редактор (как в Skysoft)
        if (!features.visuals.animations.swingAnimation.migratedToEditor) {
            features.visuals.animations.swingAnimation.applyLegacyMigration()
            saveConfig("migrate-swing-to-editor")
        }
        features.visuals.animations.swingAnimation.repairLoadedValues()

        processor = MoulConfigProcessor(StarredHeltix.feature)
        BuiltinMoulConfigGuis.addProcessors(processor)
        val driver = ConfigProcessorDriver(processor)
        driver.warnForPrivateFields = false
        driver.processConfig(StarredHeltix.feature)

        autoSaveTimer = fixedRateTimer(name = "starredheltix-config-auto-save", daemon = true, period = 600_000L, initialDelay = 600_000L) {
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
            // Шифруем пароль только на диск, в памяти остаётся plain для UI и /вход
            val feature = StarredHeltix.feature
            val plain = feature.skyblock.autoLogin.password
            val wasEncoded = plain.startsWith(ENC_PREFIX)
            if (plain.isNotEmpty() && !wasEncoded) {
                feature.skyblock.autoLogin.password = encodePassword(plain)
            }
            try {
                BufferedWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)).use { writer ->
                    writer.write(gson.toJson(feature))
                }
            } finally {
                // Возвращаем plain в память
                if (plain.isNotEmpty() && !wasEncoded) {
                    feature.skyblock.autoLogin.password = plain
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}