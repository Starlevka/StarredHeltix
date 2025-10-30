package set.starlev.starredheltix.util.solver.dungeons;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import set.starlev.starredheltix.client.StarredHeltixClient;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThreeWeirdosSolver {
    private static final Pattern STRANGER_PATTERN = Pattern.compile("\\[Персонаж] (.+?):");
    private static final Map<String, String> strangerStatements = new HashMap<>();
    
    public static void register() {
        ClientReceiveMessageEvents.GAME.register(ThreeWeirdosSolver::onChatMessage);
    }
    
    private static void onChatMessage(Text message, boolean overlay) {
        if (!StarredHeltixClient.CONFIG.threeWeirdos.enabled) {
            return;
        }
        
        String messageText = message.getString();
        
        // Check if this is a stranger message
        Matcher matcher = STRANGER_PATTERN.matcher(messageText);
        if (matcher.find()) {
            String strangerName = matcher.group(1);
            String statement = messageText.substring(matcher.end()).trim();
            
            // Store the statement
            strangerStatements.put(strangerName, statement);
            
            // Check if we have exactly 3 statements to solve
            if (strangerStatements.size() == 3) {
                // Wait a bit to ensure all messages are processed, then solve
                new Thread(() -> {
                    try {
                        Thread.sleep(500); // Wait 0.5 seconds
                        MinecraftClient.getInstance().execute(() -> solvePuzzle());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
    }
    
    private static void solvePuzzle() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        String correctStranger = null;
        
        for (Map.Entry<String, String> entry : strangerStatements.entrySet()) {
            String name = entry.getKey();
            String statement = entry.getValue();
            
            // Проверяем все верные случаи
            if (statement.contains("В моем сундуке находится награда, и я говорю правду!") ||
                statement.startsWith("Они оба говорят правду. Также") ||
                statement.startsWith("По крайней мере один из них лжёт, и награды нет в сундуке") ||
                statement.equals("Награды нет ни в одном из сундуков.") ||
                statement.equals("Награда не в моём сундуке!")) {
                correctStranger = name;
                break;
            }
        }
        
        if (correctStranger != null) {
            client.player.sendMessage(Text.literal("§a§l[Три незнакомца] §aНаграда в сундуке: §e" + correctStranger), false);
        }
        
        // Clear statements for next puzzle
        strangerStatements.clear();
    }
}