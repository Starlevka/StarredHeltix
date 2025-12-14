package set.starlev.features.misc

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix

object WelcomeMessage {
    private val mc = Minecraft.getInstance()
    private var hasShown = false

    fun register() {
        ClientPlayConnectionEvents.JOIN.register { handler, sender, client ->
            if (StarredHeltix.feature.misc.welcomeMessage == "1" && !hasShown) {
                sendWelcomeMessage()
                StarredHeltix.feature.misc.welcomeMessage = "0"
                StarredHeltix.configManager.saveConfig("welcome-message-shown")
                hasShown = true
            }
        }
    }

    private fun sendWelcomeMessage() {
        val messages = listOf(
            Component.literal("§6§lДобро пожаловать в StarredHeltix! §7(версия 0.0.9)"),
            Component.literal("§e§lОсновные команды:"),
            Component.literal("§b/sh §eили §b/starredheltix §e- открыть меню настроек"),
            Component.literal("§b/sh update §e- проверить обновления, §b/sh update install §e- установить"),
            Component.literal("§e§lБыстрые команды:"),
            Component.literal("§b/вход §e- быстрый вход с паролем"),
            Component.literal("§b/яготовлёвал §e- отправить сообщение готовности"),
            Component.literal("§b/d §eили §b/в §e- быстрый /dh (голосование)"),
            Component.literal("§e§lУправление биндами:"),
            Component.literal("§b/sh binds create <имя> <команда> §e- создать бинд"),
            Component.literal("§b/sh binds delete <имя> §e- удалить бинд"),
            Component.literal("§b/sh binds setkey <имя> <клавиша> §e- назначить клавишу"),
            Component.literal("§b/sh binds list §e- список биндов"),
            Component.literal("§e§lФильтры сообщений:"),
            Component.literal("§b/sh filter add <сообщение> §e- добавить фильтр"),
            Component.literal("§b/sh filter remove <id> §e- удалить фильтр по ID"),
            Component.literal("§e§lРотация и HUD:"),
            Component.literal("§b/sh rotation §e- показать текущую ротацию"),
            Component.literal("§b/sh rotation <yaw> <pitch> §e- установить ротацию"),
            Component.literal("§b/sh hud editor §e- открыть редактор HUD"),
            Component.literal("§e§lКонфигурация:"),
            Component.literal("§b/sh config password <пароль> §e- установить пароль для входа"),
            Component.literal("§b/sh config readyphrase <фраза> §e- установить фразу готовности"),
            Component.literal("§b/sh reset voting §e- сбросить напоминание о голосовании"),
            Component.literal("§b/sh reset config §e- сбросить всю конфигурацию"),
            Component.literal("§aНаслаждайтесь игрой на сервере Heltix Skyblock!")
        )
        messages.forEach { mc.player?.displayClientMessage(it, false) }
    }
}