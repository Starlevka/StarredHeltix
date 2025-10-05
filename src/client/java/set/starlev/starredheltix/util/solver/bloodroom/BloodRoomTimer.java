package set.starlev.starredheltix.util.solver.bloodroom;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import set.starlev.starredheltix.client.StarredHeltixClient;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Collection;

public class BloodRoomTimer {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static long bloodRoomEndTime = 0;
    private static long notificationEndTime = 0;
    private static boolean bloodRoomTimerActive = false;
    private static boolean notificationActive = false;
    
    // Регулярное выражение для поиска сообщения о победе над боссом
    private static final Pattern BOSS_MESSAGE_PATTERN = Pattern.compile(".*\\[БОСС] Наблюдатель: Поздравляю.*");
    
    // Регулярное выражение для определения этажа из скорборда
    private static final Pattern FLOOR_PATTERN = Pattern.compile(".*# (\\d+) этаж.*");
    
    public static void register() {
        // Регистрируем обработчик сообщений из чата
        ClientReceiveMessageEvents.GAME.register(BloodRoomTimer::onChatMessage);
        
        // Регистрируем отрисовку HUD
        HudRenderCallback.EVENT.register((context, tickCounter) -> onHudRender(context));
    }
    
    private static void onChatMessage(Text message, boolean overlay) {
        // Проверяем, включен ли таймер в конфигурации
        if (!StarredHeltixClient.CONFIG.bloodRoom.bloodRoomTimerEnabled) {
            return;
        }
        
        String messageString = message.getString();
        
        // Проверяем, является ли сообщение сообщением о победе над боссом
        Matcher matcher = BOSS_MESSAGE_PATTERN.matcher(messageString);
        if (matcher.matches()) {
            // Определяем текущий этаж
            int floor = getCurrentFloor();
            
            // Устанавливаем время в зависимости от этажа
            long delay;
            switch (floor) {
                case 1:
                    delay = 35000; // 35 секунд
                    break;
                case 2:
                    delay = 40000; // 40 секунд
                    break;
                case 3:
                    delay = 50000; // 50 секунд
                    break;
                default:
                    delay = 30000; // 30 секунд по умолчанию
                    break;
            }
            
            // Устанавливаем таймер
            bloodRoomEndTime = System.currentTimeMillis() + delay;
            bloodRoomTimerActive = true;
        }
    }
    
    private static int getCurrentFloor() {
        if (CLIENT.world == null || CLIENT.player == null) {
            return 0;
        }
        
        // Получаем скорборд
        Scoreboard scoreboard = CLIENT.player.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        
        if (objective != null) {
            Collection<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective);
            
            // Ищем строку с этажом
            for (ScoreboardEntry entry : entries) {
                String entryString = entry.name().getString();
                Matcher matcher = FLOOR_PATTERN.matcher(entryString);
                if (matcher.matches()) {
                    try {
                        return Integer.parseInt(matcher.group(1));
                    } catch (NumberFormatException e) {
                        // Если не удалось распарсить, продолжаем поиск
                    }
                }
            }
        }
        
        // Если не нашли этаж, возвращаем 0
        return 0;
    }
    
    private static void onHudRender(DrawContext drawContext) {
        // Проверяем, включен ли таймер в конфигурации
        if (!StarredHeltixClient.CONFIG.bloodRoom.bloodRoomTimerEnabled) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Обработка уведомления о завершении
        if (notificationActive) {
            if (currentTime >= notificationEndTime) {
                notificationActive = false;
            } else {
                renderNotification(drawContext, "Кровавая комната заполнена!");
                return;
            }
        }
        
        if (!bloodRoomTimerActive || CLIENT.world == null || CLIENT.player == null) {
            return;
        }
        
        if (currentTime >= bloodRoomEndTime) {
            // Время истекло - показываем уведомление на 2 секунды
            notificationActive = true;
            notificationEndTime = currentTime + 2000; // 2 секунды
            
            renderNotification(drawContext, "Кровавая комната заполнена!");
            
            // Проигрываем звук
            CLIENT.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));
            
            // Отключаем таймер
            bloodRoomTimerActive = false;
        } else {
            // Показываем оставшееся время с десятыми долями
            double timeLeft = (bloodRoomEndTime - currentTime) / 1000.0;
            renderNotification(drawContext, "Кровавая комната: " + String.format("%.1f", timeLeft) + "с");
        }
    }
    
    private static void renderNotification(DrawContext drawContext, String message) {
        if (CLIENT.textRenderer == null) {
            return;
        }
        
        int screenWidth = CLIENT.getWindow().getScaledWidth();
        int screenHeight = CLIENT.getWindow().getScaledHeight();
        
        // Вычисляем позицию для центрирования текста
        int messageWidth = CLIENT.textRenderer.getWidth(message);
        int x = (screenWidth - messageWidth) / 2;
        int y = screenHeight / 2 - 20; // Немного выше центра экрана
        
        // Рисуем текст красным цветом
        drawContext.drawTextWithShadow(CLIENT.textRenderer, message, x, y, 0xFFFF0000);
    }
}