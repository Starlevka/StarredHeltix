package set.starlev.starredheltix.util.qol;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import set.starlev.starredheltix.client.StarredHeltixClient;

public class WelcomeMessage {
    private static final String[] MODERATORS = {"Starlev", "ZurGames", "MegaChromeX", "nik36c"};
    
    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (StarredHeltixClient.CONFIG.general.firstTimeUser) {
                // Mark as not first time user
                StarredHeltixClient.CONFIG.general.firstTimeUser = false;
                StarredHeltixClient.CONFIG.save();
                
                // Show welcome message after 3 seconds on main thread
                client.execute(() -> {
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000);
                            client.execute(() -> showWelcomeMessage(client));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                });
            }
        });
    }
    
    private static void showWelcomeMessage(MinecraftClient client) {
        if (client.player == null) return;
        
        String playerName = client.player.getName().getString();
        boolean isModerator = false;
        
        for (String moderator : MODERATORS) {
            if (moderator.equalsIgnoreCase(playerName)) {
                isModerator = true;
                break;
            }
        }
        
        // Welcome message
        client.player.sendMessage(Text.literal("§6§l§k===§6§l Добро пожаловать в настройку мода StarredHeltix! §6§l§k==="), false);
        client.player.sendMessage(Text.literal("§aСпасибо за установку мода! Вот краткий гайд:"), false);
        client.player.sendMessage(Text.literal(""), false);
        
        // Basic commands
        client.player.sendMessage(Text.literal("§e§lОсновные команды:"), false);
        client.player.sendMessage(createClickableCommand("/starredheltix", "Главный раздел настройки мода через чат"), false);
        client.player.sendMessage(createClickableCommand("/starredheltix toggle", "Включить/выключить мод"), false);
        client.player.sendMessage(createClickableCommand("/вход", "Автоввод пароля /starredheltix config password <пароль> (ПАРОЛЬ НАХОДИТСЯ В КОНФИГЕ СБОРКИ (Правый Шифт))"), false);
        client.player.sendMessage(createClickableCommand("/яготовлёвал", "Отправить готовность к подземельям в пати чат"), false);
        client.player.sendMessage(Text.literal(""), false);
        
        // Features
        client.player.sendMessage(Text.literal("§b§lФункции:"), false);
        client.player.sendMessage(Text.literal("§7• Команды пати: !ping, !fps, !coords, !time, !uptime и т.д. в конфиге"), false);
        client.player.sendMessage(Text.literal("§7• Блокировка слотов (L для режима блокировки)"), false);
        client.player.sendMessage(Text.literal("§7• Уведомления рыбалки и инвентаря"), false);
        client.player.sendMessage(Text.literal("§7• Таймеры абилок из Сердца горы и Древоточеца"), false);
        client.player.sendMessage(Text.literal("§7• Решатель Трех незнакомцев"), false);
        client.player.sendMessage(Text.literal("§7• Авто-Спринт и фильтры сообщений"), false);
        client.player.sendMessage(Text.literal("§7• Копирование сообщений (Левый Шифт + ЛКМ)"), false);
        client.player.sendMessage(Text.literal(""), false);
        
        if (isModerator) {
            // Moderator commands
            client.player.sendMessage(Text.literal("§c§lМодераторские команды ЛЛЛ:"), false);
            client.player.sendMessage(Text.literal("§c!sh_mute <игрок> <время> <причина> §7- Забулить игрока"), false);
            client.player.sendMessage(Text.literal("§c!sh_unmute <игрок> §7- Не перестать булить игрока"), false);
            client.player.sendMessage(Text.literal("§c!sh_kick <игрок> <причина> §7- Выкинуть игрока вместе с его ПК"), false);
            client.player.sendMessage(createClickableCommand("/sh_check " + playerName, "Проверить игрока на наличие ВХ, ЧИТЫ, МАКРОСЫ, БАН!!!"), false);
            client.player.sendMessage(Text.literal("§7Команды работают в пати и ЛС"), false);
            client.player.sendMessage(Text.literal(""), false);
        }
        
        client.player.sendMessage(Text.literal("§6Удачной игры! §7Используйте §e/starredheltix §7для настроек"), false);
        client.player.sendMessage(Text.literal("§6§l==============================="), false);
    }
    
    private static Text createClickableCommand(String command, String description) {
        return Text.literal("§e" + command + " §7- " + description)
            .styled(style -> style
                .withClickEvent(new ClickEvent.SuggestCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("§aНажмите для вставки команды")))
                .withColor(Formatting.YELLOW));
    }
}