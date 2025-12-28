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

            if (!StarredHeltix.feature.misc.hasShownWelcome) {
                sendWelcomeMessage()
                StarredHeltix.feature.misc.hasShownWelcome = true
                StarredHeltix.configManager.saveConfig("welcome-message-shown")
            }
        }
    }

    private fun sendWelcomeMessage() {
        val messages = listOf(
            Component.literal("§b§lДобро пожаловать в StarredHeltix! §f(версия 0.0.11)"),
            Component.literal("§d§lНовое в этой версии:"),
            Component.literal("§b• Авто-слеер §e- авто-звонок Маддоксу на ЛКМ в меню, в категории Слеерство"),
            Component.literal("§b• Зимняя атмосфера §e- снегопад, НПС §lМега-ящик§e???"),
            Component.literal("§b• Солверы для подземелий! (Крестики нолики и Крипер-лучи)"),
            Component.literal("§b• Изменения в Анимациях ударов"),
            Component.literal("§b• Уведомление о легендарных монстрах в Рыбалке"),
            Component.literal("§b• Death Detect §e- оповещение о смерти в подземельях"),
            Component.literal("§b• Фишки-команды для Фермерства"),
            Component.literal("§b• /sh mouselock - блокировка движения мыши"),
            Component.literal("§b• /sh rotation <yaw> <pitch> - подстановка позиции yaw и pitch"),
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