package set.starlev.features.misc

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import set.starlev.StarredHeltix

object WelcomeMessage {
    private val mc = Minecraft.getInstance()
    private var hasShown = false

    fun init() {
        ClientPlayConnectionEvents.JOIN.register { handler, sender, client ->
            // Всегда проверяем обновления при входе (тихий режим)
            set.starlev.utils.ModUpdater.checkUpdate(quiet = true)

            if (!StarredHeltix.feature.misc.hasShownWelcome014) {
                sendWelcomeMessage()
                StarredHeltix.feature.misc.hasShownWelcome014 = true
                StarredHeltix.configManager.saveConfig("welcome-message-shown")
            }
        }
    }

    private fun sendWelcomeMessage() {
        val messages = listOf(
            Component.literal("§b§lДобро пожаловать в §6StarredHeltix! §6§l(версия 0.1.1)"),
            Component.literal("§d§lНовое в этой версии:"),
            Component.literal("§b• Музыкальная система §e- разная музыка в зависимости от локации"),
            Component.literal("§b• Система инвентаря §e- кастомные кнопки и оверлей экипировки"),
            Component.literal("§b• Фичи для 4 этажа"),
            Component.literal("§b• Решатель крестиков-ноликов §e- улучшенный солвер без ловушек"),
            Component.literal("§b• HUD скорости Ботинок Ранчера §e- отображение скорости для круп"),
            Component.literal("§b• HUD прогрессирующих зачарований"),
            Component.literal("§b• Кастомный scoreboard §e- настраиваемые элементы (Bank, CPS, BPS и др.)"),
            Component.literal("§b• Удалён ИИ §e- искусственный интеллект удалён из секретных функций"),
            Component.literal("§e§lОсновные команды:"),
            Component.literal("§b/sh §eили §b/starredheltix §e- открыть меню настроек"),
            Component.literal("§b/sh update §e- проверить обновления, §b/sh update install §e- установить"),
            Component.literal("§e§lБыстрые команды:"),
            Component.literal("§b/вход §e- быстрый вход с паролем"),
            Component.literal("§b/яготовлёвал §e- отправить сообщение готовности"),
            Component.literal("§e§lУправление биндами:"),
            Component.literal("§b/sh binds §e- открыть редактор биндов"),
            Component.literal("§e§lФильтры сообщений:"),
            Component.literal("§b/sh filter §e- открыть настройку фильтров"),
            Component.literal("§e§lРотация и HUD:"),
            Component.literal("§b/sh hud editor §e- открыть редактор HUD"),
            Component.literal("§e§lКнопки инвентаря:"),
            Component.literal("§b/sh buttons §e- открыть редактор кнопок инвентаря"),
            Component.literal("§e§lКонфигурация:"),
            Component.literal("§b/sh config password <пароль> §e- установить пароль для входа"),
            Component.literal("§b/sh reset config §e- сбросить всю конфигурацию"),
            Component.literal("§aНаслаждайтесь игрой на сервере §6Heltix Skyblock§a!")
        )
        messages.forEach { mc.player?.displayClientMessage(it, false) }
    }
}