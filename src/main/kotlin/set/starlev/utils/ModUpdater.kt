package set.starlev.utils

import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.ModSounds
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture

object ModUpdater {
    private val mc = Minecraft.getInstance()
    private const val GITHUB_API = "https://api.github.com/repos/Starlevka/StarredHeltix/releases/latest"
    private var downloadUrl: String? = null
    private var latestVersion: String? = null
    
    private val currentVersion: String
        get() = FabricLoader.getInstance().getModContainer("starredheltix")
            .map { it.metadata.version.friendlyString }
            .orElse("0.0.8")

    fun checkUpdate(quiet: Boolean = false) {
        CompletableFuture.runAsync {
            try {
                val response = makeRequest(GITHUB_API)
                val json = JsonParser.parseString(response).asJsonObject
                
                latestVersion = json.get("tag_name").asString.removePrefix("v")
                
                if (isNewer(latestVersion!!, currentVersion)) {
                    json.getAsJsonArray("assets").forEach { asset ->
                        val obj = asset.asJsonObject
                        if (obj.get("name").asString.endsWith(".jar")) {
                            downloadUrl = obj.get("browser_download_url").asString
                        }
                    }
                    
                    mc.execute {
                        if (!quiet) {
                            mc.player?.displayClientMessage(
                                Component.literal("§6[StarredHeltix] §eНовая версия: $latestVersion! §7/starredheltix update install"),
                                false
                            )
                            mc.player?.playSound(ModSounds.UPDATE_AVAILABLE, 1.0f, 1.0f)
                        } else {
                            // Если тихо (при входе), показываем только если есть обнова
                            // Добавляем небольшую задержку, чтобы сообщение не потерялось в спаме при входе
                            CompletableFuture.runAsync {
                                try { Thread.sleep(3000) } catch (e: Exception) {}
                                mc.execute {
                                    mc.player?.displayClientMessage(
                                        Component.literal("§6§l[StarredHeltix] §e§lДоступно обновление: §a§l$latestVersion! §7(Текущая: $currentVersion)"),
                                        false
                                    )
                                    mc.player?.displayClientMessage(
                                        Component.literal("§7Используйте §f/sh update install §7для автоматической установки."),
                                        false
                                    )
                                    mc.player?.playSound(ModSounds.UPDATE_AVAILABLE, 1.0f, 1.0f)
                                }
                            }
                        }
                    }
                } else if (!quiet) {
                    mc.execute {
                        mc.player?.displayClientMessage(
                            Component.literal("§a[StarredHeltix] Последняя версия: $currentVersion"),
                            false
                        )
                    }
                }
            } catch (e: Exception) {
                if (!quiet) {
                    mc.execute {
                        mc.player?.displayClientMessage(
                            Component.literal("§c[StarredHeltix] Ошибка проверки: ${e.message}"),
                            false
                        )
                    }
                }
            }
        }
    }

    fun installUpdate() {
        if (downloadUrl == null) {
            mc.player?.displayClientMessage(
                Component.literal("§c[StarredHeltix] Сначала проверьте обновления!"),
                false
            )
            return
        }
        
        CompletableFuture.runAsync {
            try {
                mc.execute {
                    mc.player?.displayClientMessage(
                        Component.literal("§e[StarredHeltix] Скачивание..."),
                        false
                    )
                }
                
                val modsDir = Paths.get("mods")
                val fileName = "starredheltix-$latestVersion.jar"
                val newMod = modsDir.resolve(fileName)
                
                java.net.URI(downloadUrl!!).toURL().openStream().use { input ->
                    Files.copy(input, newMod)
                }
                
                mc.execute {
                    mc.player?.displayClientMessage(
                        Component.literal("§a[StarredHeltix] Мод установлен! §cУдалите старую версию из папки mods и перезапустите игру."),
                        false
                    )
                }
            } catch (e: Exception) {
                mc.execute {
                    mc.player?.displayClientMessage(
                        Component.literal("§c[StarredHeltix] Ошибка: ${e.message}"),
                        false
                    )
                }
            }
        }
    }

    private fun makeRequest(url: String): String {
        val conn = java.net.URI(url).toURL().openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "StarredHeltix-Updater")
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val latestParts = latest.replace(Regex("[^0-9.]"), "").split(".")
        val currentParts = current.replace(Regex("[^0-9.]"), "").split(".")
        
        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val l = latestParts.getOrNull(i)?.toIntOrNull() ?: 0
            val c = currentParts.getOrNull(i)?.toIntOrNull() ?: 0
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
