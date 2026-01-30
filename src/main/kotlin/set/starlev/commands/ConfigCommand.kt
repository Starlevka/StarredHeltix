package set.starlev.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix
import set.starlev.render.BindsGui
import set.starlev.render.FilterGui
import set.starlev.config.ConfigGuiManager
import set.starlev.config.Features
import set.starlev.features.chat.MessageFilterManager
import set.starlev.features.chat.CustomBindManager
import set.starlev.features.misc.MouseLock
import set.starlev.utils.ConfigUtils

object ConfigCommand {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            registerCommands(dispatcher)
        }
    }

    private fun registerCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        // Регистрировать обе команды (алиасы) с одинаковой функциональностью
        val commandBuilder = buildMainCommand()
        dispatcher.register(commandBuilder)
        
        // Также регистрировать как /starredheltix
        dispatcher.register(buildMainCommand("starredheltix"))

        // Простые команды
        registerSimpleCommands(dispatcher)
    }
    
    private fun buildMainCommand(name: String = "sh") = literal(name)
        .executes { ConfigGuiManager.openConfigGui(); 1 }
        .then(literal("reset")
            .then(literal("config").executes(::resetConfig))
        )
        .then(literal("filter")
            .executes { 
                StarredHeltix.screenToOpen = FilterGui(null)
                1
            }
            .then(literal("add")
                .then(argument("message", StringArgumentType.greedyString())
                    .executes(::addFilter)
                )
            )
            .then(literal("remove")
                .then(argument("id", IntegerArgumentType.integer(0, 999))
                    .suggests { _, builder ->
                        StarredHeltix.feature.chat.general.messageFilter.filters.forEachIndexed { index, filter ->
                            builder.suggest(index, Component.literal(filter))
                        }
                        builder.buildFuture()
                    }
                    .executes(::removeFilter)
                )
            )
            .then(literal("list")
                .executes(::listFilters)
            )
        )
    
        // Утилиты
        .then(literal("rotation")
            .executes { ctx ->
                val client = Minecraft.getInstance()
                if (client.player != null) {
                    val yaw = client.player!!.yRot
                    val pitch = client.player!!.xRot
                    ctx.source.sendFeedback(Component.literal("§6§l[Ротация игрока] §fТекущая: Yaw=§e${String.format("%.1f", yaw)}°§f, Pitch=§e${String.format("%.1f", pitch)}°"))
                    ctx.source.sendFeedback(Component.literal("§7Использование: /starredheltix rotation <yaw> <pitch>"))
                } else {
                    ctx.source.sendError(Component.literal("§cОшибка: игрок не найден"))
                    return@executes 0
                }
                1
            }
            .then(argument("yaw", DoubleArgumentType.doubleArg(-1080.0, 1080.0))
                .then(argument("pitch", DoubleArgumentType.doubleArg(-90.0, 90.0))
                    .executes(::setRotation)
                )
            )
        )
        // HUD редактор
        .then(literal("hud")
            .then(literal("editor").executes { ctx ->
                ConfigUtils.openHudEditor()
                ctx.source.sendFeedback(Component.literal("§aОкрыт редактор HUD элементов"))
                1
            })
            .then(literal("reset").executes { ctx ->
                set.starlev.hud.HudManager.resetAllPositions()
                ctx.source.sendFeedback(Component.literal("§aРасположение HUD элементов сброшено"))
                1
            })
        )
        // Мега-ящики дебаг
        .then(literal("megabox")
            .then(literal("spawn").executes { ctx ->
                set.starlev.features.visual.MegaChestNPCHandler.spawnDebugChest()
                1
            })
        )
        .then(literal("binds")
            .executes { 
                StarredHeltix.screenToOpen = BindsGui(null)
                1
            }
            .then(literal("create")
                .then(argument("name", StringArgumentType.word())
                    .then(argument("command", StringArgumentType.greedyString())
                        .executes { ctx ->
                            val name = StringArgumentType.getString(ctx, "name")
                            val command = StringArgumentType.getString(ctx, "command")
                            if (CustomBindManager.create(name, command)) 1 else 0
                        }
                    )
                )
            )
            .then(literal("delete")
                .then(argument("name", StringArgumentType.word())
                    .executes { ctx ->
                        val name = StringArgumentType.getString(ctx, "name")
                        if (CustomBindManager.delete(name)) 1 else 0
                    }
                )
            )
            .then(literal("setkey")
                .then(argument("name", StringArgumentType.word())
                    .then(argument("key", StringArgumentType.word())
                        .executes { ctx ->
                            val name = StringArgumentType.getString(ctx, "name")
                            val key = StringArgumentType.getString(ctx, "key")
                            if (CustomBindManager.setKey(name, key)) 1 else 0
                        }
                    )
                )
            )
            .then(literal("list").executes { ctx ->
                CustomBindManager.list()
                1
            })
        )
        // Конфигурация
        .then(literal("config")
            .then(literal("password")
                .then(argument("password", StringArgumentType.word())
                    .executes(::setPassword)
                )
            )
            .then(literal("readyphrase")
                .then(argument("phrase", StringArgumentType.greedyString())
                    .executes(::setReadyPhrase)
                )
            )
        )
        // Обновление
        .then(literal("update")
            .then(literal("check").executes { set.starlev.utils.ModUpdater.checkUpdate(); 1 })
            .then(literal("install").executes { set.starlev.utils.ModUpdater.installUpdate(); 1 })
        )
        // Секретный код
        .then(literal("code")
            .then(argument("code", StringArgumentType.word())
                .executes { ctx ->
                    val code = StringArgumentType.getString(ctx, "code")
                    val cleanCode = code.trim()
                    
                    if (cleanCode == set.starlev.secret.features.ai.AiConfig.AI_SECRET) {
                        if (!set.starlev.secret.config.SecretMenuManager.secretConfig.chatBot.isAiUnlocked) {
                            set.starlev.secret.config.SecretMenuManager.secretConfig.chatBot.isAiUnlocked = true
                            set.starlev.secret.config.SecretMenuManager.save()
                            ctx.source.sendFeedback(Component.literal("§d§l[Secret] §fСистема ИИ §aразблокирована§f!"))
                        } else {
                            ctx.source.sendFeedback(Component.literal("§d§l[Secret] §eСистема ИИ уже разблокирована!"))
                        }
                    } else {
                        ctx.source.sendError(Component.literal("§d§l[Secret] §cНеверный код активации!"))
                    }
                    1
                }
            )
        )
        .then(literal("coords").executes(::showCoords))
        .then(literal("mouselock")
            .executes { ctx ->
                MouseLock.toggle()
                1
            }
        )

    private fun registerSimpleCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        // /d и /в - алиасы для /dh (голосование или утилиты)
        dispatcher.register(
            literal("d")
                .executes { ctx ->
                    Minecraft.getInstance().player?.connection?.sendCommand("dh")
                    1
                }
        )
        dispatcher.register(
            literal("в")
                .executes { ctx ->
                    Minecraft.getInstance().player?.connection?.sendCommand("dh")
                    1
                }
        )

        // /вход - быстрый вход
        dispatcher.register(
            literal("вход")
                .executes { ctx ->
                    val password = StarredHeltix.feature.misc.autoLogin.password
                    if (password.isNotEmpty()) {
                        Minecraft.getInstance().player?.connection?.sendCommand("login $password")
                        ctx.source.sendFeedback(Component.literal("§aУспешный вход в систему"))
                    } else {
                        ctx.source.sendError(Component.literal("§cПароль не установлен. Используйте /starredheltix config password <пароль>"))
                    }
                    1
                }
        )

        // /яготовлёвал - фраза готовности
        dispatcher.register(
            literal("яготовлёвал")
                .executes { ctx ->
                    val config = StarredHeltix.feature.dungeons.autoReady
                    if (config.enabled) {
                        Minecraft.getInstance().player?.connection?.sendCommand("pc ${config.readyMessage}")
                        ctx.source.sendFeedback(Component.literal("§aСообщение о готовности отправлено"))
                    } else {
                        ctx.source.sendError(Component.literal("§cАвто-готовность отключена в настройках"))
                    }
                    1
                }
        )
    }

    private fun resetConfig(ctx: CommandContext<FabricClientCommandSource>): Int {
        StarredHeltix.configManager.features = Features()
        StarredHeltix.configManager.saveConfig("reset-config")
        ctx.source.sendFeedback(Component.literal("§aКонфиг сброшен. Перезапустите игру для полного применения."))
        return 1
    }

    private fun addFilter(ctx: CommandContext<FabricClientCommandSource>): Int {
        var message = StringArgumentType.getString(ctx, "message")
        
        // Убираем кавычки, если пользователь их ввел
        if (message.startsWith("\"") && message.endsWith("\"") && message.length > 1) {
            message = message.substring(1, message.length - 1)
        }
        
        MessageFilterManager.addFilter(message)
        val id = StarredHeltix.feature.chat.general.messageFilter.filters.indexOf(message)
        ctx.source.sendFeedback(Component.literal("§aФильтр добавлен: §e[$id] §f$message"))
        ctx.source.sendFeedback(Component.literal("§7(Будет скрывать сообщения, начинающиеся с этого или содержащие это в имени отправителя)"))
        return 1
    }

    private fun removeFilter(ctx: CommandContext<FabricClientCommandSource>): Int {
        val id = IntegerArgumentType.getInteger(ctx, "id")
        val filters = StarredHeltix.feature.chat.general.messageFilter.filters
        if (id >= 0 && id < filters.size) {
            val removed = filters.removeAt(id)
            StarredHeltix.configManager.saveConfig("filter-remove")
            ctx.source.sendFeedback(Component.literal("§aФильтр удалён: §e$removed"))
        } else {
            ctx.source.sendError(Component.literal("§cФильтр с ID $id не найден"))
        }
        return 1
    }

    private fun listFilters(ctx: CommandContext<FabricClientCommandSource>): Int {
        val filters = StarredHeltix.feature.chat.general.messageFilter.filters
        if (filters.isEmpty()) {
            ctx.source.sendFeedback(Component.literal("§cСписок фильтров пуст"))
            return 1
        }
        
        ctx.source.sendFeedback(Component.literal("§6§l[Список фильтров]"))
        filters.forEachIndexed { index, filter ->
            ctx.source.sendFeedback(Component.literal("§e$index. §f$filter"))
        }
        return 1
    }

    private fun setRotation(ctx: CommandContext<FabricClientCommandSource>): Int {
        val yaw = DoubleArgumentType.getDouble(ctx, "yaw")
        val pitch = DoubleArgumentType.getDouble(ctx, "pitch")
        val client = Minecraft.getInstance()

        if (client.player != null) {
            val normalizedYaw = normalizeYaw(yaw)
            client.player!!.yRot = normalizedYaw.toFloat()
            client.player!!.xRot = pitch.toFloat()

            val normalizationMessage = if (Math.abs(yaw - normalizedYaw) > 0.01) {
                " §7(округлено с ${String.format("%.1f", yaw)}°)"
            } else {
                ""
            }

            ctx.source.sendFeedback(Component.literal("§aРотация изменена: Yaw=§e${String.format("%.1f", normalizedYaw)}°§a, Pitch=§e${String.format("%.1f", pitch)}°§a$normalizationMessage"))
        } else {
            ctx.source.sendError(Component.literal("§cОшибка: игрок не найден"))
            return 0
        }
        return 1
    }

    private fun setPassword(ctx: CommandContext<FabricClientCommandSource>): Int {
        val password = StringArgumentType.getString(ctx, "password")
        StarredHeltix.feature.misc.autoLogin.password = password
        StarredHeltix.configManager.saveConfig("config-password")
        ctx.source.sendFeedback(Component.literal("§aПароль установлен"))
        return 1
    }

    private fun setReadyPhrase(ctx: CommandContext<FabricClientCommandSource>): Int {
        val phrase = StringArgumentType.getString(ctx, "phrase")
        StarredHeltix.feature.dungeons.autoReady.readyMessage = phrase
        StarredHeltix.configManager.saveConfig("config-ready-phrase")
        ctx.source.sendFeedback(Component.literal("§aФраза авто-готовности установлена: §e$phrase"))
        return 1
    }

    private fun normalizeYaw(yaw: Double): Double {
        var normalized = yaw % 360.0
        if (normalized > 180.0) normalized -= 360.0
        else if (normalized < -180.0) normalized += 360.0
        return normalized
    }

    private fun showCoords(ctx: CommandContext<FabricClientCommandSource>): Int {
        val client = Minecraft.getInstance()
        if (client.player != null) {
            val player = client.player!!
            val x = String.format("%.1f", player.x)
            val y = String.format("%.1f", player.y)
            val z = String.format("%.1f", player.z)

            val coordsMessage = " / / starredheltix x: $x y: $y z: $z / /"
            client.keyboardHandler.setClipboard(coordsMessage)

            ctx.source.sendFeedback(Component.literal("§aКоординаты скопированы в буфер обмена: §e$coordsMessage"))
        } else {
            ctx.source.sendError(Component.literal("§cОшибка: игрок не найден"))
            return 0
        }
        return 1
    }
}
