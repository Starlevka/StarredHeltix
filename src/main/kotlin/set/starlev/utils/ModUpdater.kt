package set.starlev.utils

import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object ModUpdater {
    private val mc = Minecraft.getInstance()
    private const val GITHUB_API = "https://api.github.com/repos/Starlevka/StarredHeltix/releases/latest"
    private var downloadUrl: String? = null
    private var latestVersion: String? = null

    private val currentVersion: String
        get() = FabricLoader.getInstance().getModContainer("starredheltix")
            .map { it.metadata.version.friendlyString }
            .orElse("1.0.0")

    // daemon scheduler for delayed quiet message — не блокирует ForkJoinPool
    private val delayScheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "ModUpdater-delay").apply { isDaemon = true }
    }

    fun checkUpdate(quiet: Boolean = false) {
        CompletableFuture.runAsync {
            try {
                val response = HttpUtils.downloadString(GITHUB_API, "StarredHeltix-Updater") ?: throw IllegalStateException("no response")
                val json = JsonParser.parseString(response).asJsonObject

                val tag = json.get("tag_name")?.asString?.removePrefix("v")
                if (tag.isNullOrBlank()) throw IllegalStateException("tag_name missing")
                latestVersion = tag

                val ver = latestVersion ?: return@runAsync
                if (VersionUtils.isNewer(ver, currentVersion)) {
                    json.getAsJsonArray("assets")?.forEach { asset ->
                        val obj = asset.asJsonObject
                        if (obj.get("name")?.asString?.endsWith(".jar") == true) {
                            downloadUrl = obj.get("browser_download_url")?.asString
                        }
                    }

                    mc.execute {
                        if (!quiet) {
                            mc.player?.sendSystemMessage(
                                Component.literal("§6[StarredHeltix] §eНовая версия: $ver! §7/starredheltix update install")
                            )
                        } else {
                            // Если тихо (при входе), показываем только если есть обнова, с задержкой
                            delayScheduler.schedule({
                                mc.execute {
                                    mc.player?.sendSystemMessage(
                                        Component.literal("§6§l[StarredHeltix] §e§lДоступно обновление: §a§l$ver! §7(Текущая: $currentVersion)")
                                    )
                                    mc.player?.sendSystemMessage(
                                        Component.literal("§7Используйте §f/sh update install §7для автоматической установки.")
                                    )
                                }
                            }, 3, TimeUnit.SECONDS)
                        }
                    }
                } else if (!quiet) {
                    mc.execute {
                        mc.player?.sendSystemMessage(
                            Component.literal("§a[StarredHeltix] Последняя версия: $currentVersion")
                        )
                    }
                }
            } catch (e: Exception) {
                if (!quiet) {
                    mc.execute {
                        mc.player?.sendSystemMessage(
                            Component.literal("§c[StarredHeltix] Ошибка проверки: ${e.message}")
                        )
                    }
                }
            }
        }
    }

    fun installUpdate() {
        val url = downloadUrl
        if (url == null) {
            mc.player?.sendSystemMessage(
                Component.literal("§c[StarredHeltix] Сначала проверьте обновления!")
            )
            return
        }

        CompletableFuture.runAsync {
            try {
                mc.execute {
                    mc.player?.sendSystemMessage(
                        Component.literal("§e[StarredHeltix] Скачивание...")
                    )
                }

                val modsDir = Paths.get("mods")
                Files.createDirectories(modsDir)
                val ver = latestVersion ?: "latest"
                val fileName = "starredheltix-$ver.jar"
                val newMod = modsDir.resolve(fileName)

                java.net.URI(url).toURL().openStream().use { input ->
                    Files.copy(input, newMod, StandardCopyOption.REPLACE_EXISTING)
                }

                mc.execute {
                    mc.player?.sendSystemMessage(
                        Component.literal("§a[StarredHeltix] Мод установлен! §cУдалите старую версию из папки mods и перезапустите игру.")
                    )
                }
            } catch (e: Exception) {
                mc.execute {
                    mc.player?.sendSystemMessage(
                        Component.literal("§c[StarredHeltix] Ошибка: ${e.message}")
                    )
                }
            }
        }
    }
}
