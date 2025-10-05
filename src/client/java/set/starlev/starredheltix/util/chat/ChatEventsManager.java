package set.starlev.starredheltix.util.chat;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import set.starlev.starredheltix.client.StarredHeltixClient;
import set.starlev.starredheltix.util.player.PlayerPingUtil;
import set.starlev.starredheltix.util.solver.fairysouls.TemporaryWaypointManager;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatEventsManager {
    private final MinecraftClient client;
    // Simplified patterns based on the working implementation
    private static final Pattern PARTY_MESSAGE_PATTERN = Pattern.compile("Пати");
    private static final Pattern PRIVATE_MESSAGE_PATTERN = Pattern.compile("\\[.*?->");
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("([^:]+):.*$");
    private static final Pattern COMMAND_PATTERN = Pattern.compile(":\\s*!(\\S+)");
    private static final Pattern PRIVATE_MESSAGE_PLAYER_PATTERN = Pattern.compile("\\[(.*?)\\s*->");
    private static final Pattern PRIVATE_COMMAND_PATTERN = Pattern.compile("]\\s*!(\\S+)");
    
    // Pattern to detect uptime messages to save them
    private static final Pattern UPTIME_PATTERN = Pattern.compile("^\\[.*?] Последняя перезагрузка.*");
    
    // Pattern to detect party messages that start with "Пати >"
    private static final Pattern PARTY_NOTIFICATION_PATTERN = Pattern.compile("^Пати >.*");

    public ChatEventsManager(MinecraftClient client) {
        this.client = client;
    }

    public void register() {
        // Register for party chat messages
        ClientReceiveMessageEvents.GAME.register(this::onChatMessage);
    }

    private void onChatMessage(Text message, boolean overlay) {
        String messageText = message.getString();
        
        // First, check if the mod is enabled
        if (!StarredHeltixClient.CONFIG.general.enabled) return;
        
        // Check if this is a party notification message (starts with "Пати >")
        if (PARTY_NOTIFICATION_PATTERN.matcher(messageText).find()) {
            // Play a sound effect for party notifications
            if (client.player != null) {
                client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
            }
        }
        
        // Check if this is an uptime message to save it
        Matcher uptimeMatcher = UPTIME_PATTERN.matcher(messageText);
        if (uptimeMatcher.find()) {
            // Save the uptime message
            StarredHeltixClient.CONFIG.lastUptimeMessage = messageText;
            StarredHeltixClient.CONFIG.save();
            return;
        }
        
        // Check if this is a party command message (sent via /pc)
        if (PARTY_MESSAGE_PATTERN.matcher(messageText).find()) {
            Matcher nameMatcher = PLAYER_NAME_PATTERN.matcher(messageText);
            String requestingPlayer = "";
            if (nameMatcher.find()) {
                String textBeforeColon = nameMatcher.group(1);
                requestingPlayer = this.extractPlayerName(textBeforeColon);
            }

            Matcher commandMatcher = COMMAND_PATTERN.matcher(messageText);
            String commandType = "";
            if (commandMatcher.find()) {
                commandType = commandMatcher.group(1) != null ? commandMatcher.group(1).trim() : "";
            }
            
            // Handle promote command which might not have args
            if (!"promote".equals(commandType) && !"pt".equals(commandType) && !"повысить".equals(commandType)) {
                Pattern argsPattern = Pattern.compile(":\\s*!\\S+\\s+(.+)$");
                Matcher argsMatcher = argsPattern.matcher(messageText);
                String commandArgs = "";
                if (argsMatcher.find()) {
                    commandArgs = argsMatcher.group(1).trim();
                }
                
                if (StarredHeltixClient.CONFIG.partyCommands.partyChatCommandsEnabled) {
                    this.handlePartyCommand(requestingPlayer, commandType, commandArgs);
                }
            } else if (StarredHeltixClient.CONFIG.partyCommands.partyChatCommandsEnabled) {
                // For promote command without explicit args, use the requesting player as target
                this.handlePartyCommand(requestingPlayer, commandType, requestingPlayer);
            }
            return;
        }
        
        // Check if this is a private message command
        if (PRIVATE_MESSAGE_PATTERN.matcher(messageText).find() && StarredHeltixClient.CONFIG.partyCommands.partyPrivateMessageCommandsEnabled) {
            Matcher playerMatcher = PRIVATE_MESSAGE_PLAYER_PATTERN.matcher(messageText);
            String requestingPlayer = "";
            if (playerMatcher.find()) {
                requestingPlayer = playerMatcher.group(1).trim();
            }

            Matcher commandMatcher = PRIVATE_COMMAND_PATTERN.matcher(messageText);
            String commandType = "";
            if (commandMatcher.find()) {
                commandType = commandMatcher.group(1) != null ? commandMatcher.group(1).trim() : "";
            }
            
            // Process the private message command
            handlePrivateMessageCommand(requestingPlayer, commandType, requestingPlayer);
            return;
        }
    }
    
    private String extractPlayerName(String fullTextBeforeColon) {
        int gtIndex = fullTextBeforeColon.indexOf('>');
        if (gtIndex < 0) {
            return "";
        } else {
            String afterGt = fullTextBeforeColon.substring(gtIndex + 1).trim();
            String[] parts = afterGt.split("\\s+");

            for (int i = parts.length - 1; i >= 0; --i) {
                if (!parts[i].isEmpty()) {
                    return parts[i];
                }
            }

            return "";
        }
    }
    
    private void handlePartyCommand(String requestingPlayer, String commandType, String commandArgs) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Validate that we have a client player
        if (client.player == null) return;
        
        switch (commandType.toLowerCase()) {
            case "promote":
            case "повысить":
            case "pt":
                if (StarredHeltixClient.CONFIG.partyCommands.partyPromoteEnabled) {
                    client.player.networkHandler.sendChatCommand("p promote " + commandArgs);
                } else if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.literal("§c[DEBUG] Promote command is disabled"), false);
                }
                break;
                
            case "kick":
            case "кик":
            case "k":
                if (StarredHeltixClient.CONFIG.partyCommands.partyKickEnabled) {
                    client.player.networkHandler.sendChatCommand("p kick " + commandArgs);
                } else if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.literal("§c[DEBUG] Kick command is disabled"), false);
                }
                break;
                
            case "invite":
            case "инвайт":
            case "inv":
                if (StarredHeltixClient.CONFIG.partyCommands.partyInviteEnabled) {
                    client.player.networkHandler.sendChatCommand("p invite " + commandArgs);
                } else if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.literal("§c[DEBUG] Invite command is disabled"), false);
                }
                break;
                
            case "ping":
            case "пинг":
                if (StarredHeltixClient.CONFIG.partyCommands.partyPingEnabled) {
                    handlePingCommand(requestingPlayer);
                } else if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.literal("§c[DEBUG] Ping command is disabled"), false);
                }
                break;
                
            case "uptime":
            case "аптайм":
                if (StarredHeltixClient.CONFIG.partyCommands.partyUptimeEnabled) {
                    handleUptimeCommand(requestingPlayer);
                } else if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.literal("§c[DEBUG] Uptime command is disabled"), false);
                }
                break;
                
            case "dt":
            case "дт":
                if (StarredHeltixClient.CONFIG.partyCommands.partyDtEnabled) {
                    handleDtCommand(requestingPlayer, commandArgs);
                } else if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.literal("§c[DEBUG] DT command is disabled"), false);
                }
                break;
                
            case "fps":
            case "фпс":
                if (StarredHeltixClient.CONFIG.partyCommands.partyFpsEnabled) {
                    handleFpsCommand(requestingPlayer);
                } else if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.literal("§c[DEBUG] FPS command is disabled"), false);
                }
                break;
                
            case "time":
            case "время":
                if (StarredHeltixClient.CONFIG.partyCommands.partyTimeEnabled) {
                    handleTimeCommand(requestingPlayer);
                } else if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.literal("§c[DEBUG] Time command is disabled"), false);
                }
                break;
                
            case "boykisser":
                if (StarredHeltixClient.CONFIG.partyCommands.partyBoykisserEnabled) {
                    handleBoykisserCommand();
                } else if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.literal("§c[DEBUG] Boykisser command is disabled"), false);
                }
                break;
                
            case "coords":
            case "координаты":
                if (StarredHeltixClient.CONFIG.partyCommands.partyCoordsEnabled) {
                    handleCoordsCommand(requestingPlayer);
                } else if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.literal("§c[DEBUG] Coords command is disabled"), false);
                }
                break;
                
            default:
                if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.literal("§c[DEBUG] Unknown command: " + commandType), false);
                }
                break;
        }
    }
    
    // Add the missing methods here
    private void handlePingCommand(String requestingPlayer) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Send the player's ping to party chat
        int ping = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid()).getLatency();
        client.player.networkHandler.sendChatCommand("pc ᯓ★ Мой пинг: " + ping + " мс");
    }
    
    private void handleUptimeCommand(String requestingPlayer) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Send the uptime information to party chat
        String uptimeMessage = StarredHeltixClient.CONFIG.lastUptimeMessage.isEmpty() ? 
            "Информация об аптайме еще не доступна" : 
            StarredHeltixClient.CONFIG.lastUptimeMessage;
        client.player.networkHandler.sendChatCommand("pc " + uptimeMessage);
    }
    
    private void handleDtCommand(String requestingPlayer) {
        handleDtCommand(requestingPlayer, "");
    }
    
    private void handleDtCommand(String requestingPlayer, String reason) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Send dungeon time information to party chat
        if (reason.isEmpty()) {
            client.player.networkHandler.sendChatCommand("pc ᯓ★ " + requestingPlayer + " нуждается в перерыве");
        } else {
            client.player.networkHandler.sendChatCommand("pc ᯓ★ " + requestingPlayer + " нуждается в перерыве по причине: \"" + reason + "\"");
        }
    }
    
    private void handleBoykisserCommand() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Define 16 different messages to send
        String[] messages = {
            "⠀⠀⣽⣿⣿⣿⣧⠀⠀⠀⠠⣤⣄⡀⠀⠀⠀⠀⣰⣿⣿⣿⣿⣿⡆⠀",
            "⠀⢀⣿⣿⣿⣿⣿⣷⡀⠀⠀⢿⣿⣿⣦⡀⠀⣰⣿⣿⣿⣿⣿⣿⡇⠀",
            "⠀⢸⣿⣿⣿⣿⣿⣿⡿⠄⣠⣤⣿⣿⣿⣿⣄⣿⣿⣿⣿⣿⣿⣿⡇⠀",
            "⠀⢸⣿⣿⣿⣿⣿⣿⣤⣬⣭⣬⣬⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠇⠀",
            "⠀⢸⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠇⠀",
            "⠀⠀⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡿⠿⠿⠿⠿⣿⣿⡿⠀⠀",
            "⠀⠀⠸⣿⣧⠀⣴⡆⠀⠀⢸⣿⣿⣿⣿⠀⠀⢸⣿⡌⣶⣿⠟⠁⠀⠀",
            "⠀⠀⠀⠹⡿⢸⣿⡇⠀⠀⢸⣿⣿⣿⣿⠀⠀⢈⣿⡇⢸⣯⣤⣤⠀⠀",
            "⠀⠙⣿⣿⣇⢸⣿⣇⠀⢀⣾⡿⢿⣿⣿⣀⣀⣼⣿⡇⣸⣿⡿⠁⠀⠀",
            "⠀⠀⢀⡟⡉⠞⢻⣿⣿⣿⣿⣶⣾⣿⣿⣿⣿⣿⠋⠘⣹⣿⡄⠀⠀⠀",
            "⠀⠀⣼⣿⣧⣶⣿⣿⣿⣟⠻⢋⣍⣉⣋⣼⣿⣿⣿⣶⢿⣿⣿⡄⠀⠀",
            "⠀⠀⠉⠉⠀⠙⠻⢿⣿⣿⣿⣿⣿⣿⣿⣿⡿⠿⠛⠁⠀⠉⠀⠀⠀",
            "⠀⠀⠀⠀⠀⠀⠀⠠⣬⣭⣽⣿⣿⣿⣿⣿⣷⡀⠀⠀⠀⠀⠀⠀⠀⠀",
            "⠀⠀⠀⠀⠀⠀⠀⠀⠙⣿⣿⣿⣿⣿⣿⣿⣿⣷⡀⠀⠀⠀⠀⠀⠀⠀",
            "⠀⠀⠀⠀⠀⠀⠀⠀⠼⢿⣿⣿⣿⣿⣿⣿⣿⣿⣧⠀⠀⠀⠀⠀⠀⠀",
            "⠀⠀⠀⠀⠀⠀⠀⠀⠀⣼⣿⣿⣿⣿⣿⣿⣿⣿⣿⡄⠀⠀⠀⠀⠀⠀"
        };
        
        // Use a single thread with scheduled execution to reduce lag
        java.util.concurrent.ScheduledExecutorService executor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        for (int i = 0; i < Math.min(16, messages.length); i++) {
            final int index = i;
            executor.schedule(() -> {
                if (client.player != null) {
                    client.player.networkHandler.sendChatCommand("pc " + messages[index]);
                }
            }, index * 149L, java.util.concurrent.TimeUnit.MILLISECONDS); // Reduced delay to 100ms for faster sending
        }
        
        // Shutdown the executor after all tasks are scheduled
        executor.schedule(executor::shutdown, 16 * 160L, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    private void handleFpsCommand(String requestingPlayer) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Send FPS information to party chat
        int fps = client.getCurrentFps();
        client.player.networkHandler.sendChatCommand("pc ᯓ★ Мой FPS: " + fps);
    }
    
    private void handleTimeCommand(String requestingPlayer) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        
        // Send world time information to party chat
        long time = client.world.getTimeOfDay();
        String formattedTime = formatWorldTime(time);
        client.player.networkHandler.sendChatCommand("pc ᯓ★ У меня время в IRL: " + formattedTime);
    }
    
    private void handleCoordsCommand(String requestingPlayer) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Send player coordinates to party chat
        int x = (int) Math.floor(client.player.getX());
        int y = (int) Math.floor(client.player.getY());
        int z = (int) Math.floor(client.player.getZ());
        client.player.networkHandler.sendChatCommand("pc ᯓ★ Мои координаты в игре: " + x + ", " + y + ", " + z);
        
        // Add temporary waypoint for 30 seconds
        TemporaryWaypointManager.addWaypoint(x, y, z, client.player.getName().getString());
    }
    
    /**
     * Handles private message commands sent to the player.
     * Currently supports: invite
     */
    private void handlePrivateMessageCommand(String requestingPlayer, String commandType, String commandArgs) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Validate that we have a client player
        if (client.player == null) return;
        
        switch (commandType.toLowerCase()) {
            case "invite":
            case "inv":
            case "инвайт":
                if (StarredHeltixClient.CONFIG.partyCommands.partyInviteEnabled) {
                    client.player.networkHandler.sendChatCommand("party invite " + requestingPlayer);
                } else if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.literal("§c[DEBUG] Invite command is disabled"), false);
                }
                break;
                
            default:
                if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.literal("§c[DEBUG] Unknown private message command: " + commandType), false);
                }
                break;
        }
    }
    
    private String formatWorldTime(long time) {
        // Convert world time to readable format based on player's OS timezone
        long hours = (time / 1000 + 6) % 24;
        long minutes = (time % 1000) * 60 / 1000;
        
        // Get current system time with timezone
        Instant now = Instant.now();
        ZoneId zoneId = ZoneId.systemDefault();
        
        // Format the time according to system timezone
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return now.atZone(zoneId).format(formatter);
    }
}