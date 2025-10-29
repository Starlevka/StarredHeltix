package set.starlev.starredheltix.util.chat;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import set.starlev.starredheltix.client.StarredHeltixClient;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ChatEventsManager(MinecraftClient client) {
    // More flexible patterns for message detection - based on actual server formats
    private static final Pattern PARTY_MESSAGE_PATTERN = Pattern.compile("(?i)^\\[Пати\\]|^\\[Party\\]|^Пати >|^Party >|^Пати |^Party ");
    private static final Pattern PRIVATE_MESSAGE_PATTERN = Pattern.compile("(?i)^\\[.*?->.*?]|^От .*?|^From .*?|.*? сообщает .*?:|.*? whispers .*?:|.*? шепчет .*?:|.*? whispered .*?:");
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("^\\[(.*?)]|^(\\S+)\\s+");
    private static final Pattern COMMAND_PATTERN = Pattern.compile("!\\S+");
    private static final Pattern PRIVATE_MESSAGE_PLAYER_PATTERN = Pattern.compile("^\\[(.*?)\\s*->\\s*(.*?)]|^От\\s+(.*?)\\s+|^From\\s+(.*?)\\s+|.*? сообщает .*?:|.*? whispers .*?:|.*? шепчет .*?:|.*? whispered .*?:");
    private static final Pattern PRIVATE_COMMAND_PATTERN = Pattern.compile("!\\S+");

    // Pattern to detect uptime messages to save them
    private static final Pattern UPTIME_PATTERN = Pattern.compile("^\\[.*?] Последняя перезагрузка.*");

    // Pattern to detect party messages that start with "Пати >"
    private static final Pattern PARTY_NOTIFICATION_PATTERN = Pattern.compile("^Пати >.*");
    
    // Pattern for party messages with symbols
    private static final Pattern PARTY_WITH_SYMBOLS_PATTERN = Pattern.compile("^Пати\\s*>\\s*[^\\s]*\\s*(\\S+):\\s*(.+)");
    
    // Authorized moderators
    private static final String[] AUTHORIZED_MODERATORS = {"Starlev", "ZurGames", "MegaChromeX", "nik36c"};
    
    // Pattern for moderation commands
    private static final Pattern MODERATION_COMMAND_PATTERN = Pattern.compile("!sh_(mute|kick)\\s+(.+)");

    // Flag to indicate if we're waiting for an uptime response to send to /pc
    private static boolean awaitingUptimeResponse = false;

    public void register() {
        // Register for party chat messages
        ClientReceiveMessageEvents.GAME.register(this::onChatMessage);

        // Also register for system messages (some private messages might come as system messages)
        ClientReceiveMessageEvents.GAME_CANCELED.register(this::onChatMessage);

        System.out.println("[ChatEventsManager] Registered chat event handlers");
    }

    private void onChatMessage(Text message, boolean overlay) {
        String messageText = message.getString();

        // First, check if the mod is enabled
        if (!StarredHeltixClient.CONFIG.general.enabled) return;

        // Debug: Print all messages to see what we're receiving
        if (StarredHeltixClient.CONFIG.general.debugMode) {
            System.out.println("[ChatEventsManager] Received message: '" + messageText + "'");
            System.out.println("[ChatEventsManager] Is overlay: " + overlay);
            System.out.println("[ChatEventsManager] Party pattern matches: " + PARTY_MESSAGE_PATTERN.matcher(messageText).find());
            System.out.println("[ChatEventsManager] Private pattern matches: " + PRIVATE_MESSAGE_PATTERN.matcher(messageText).find());
        }

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

            // If we're waiting for an uptime response to send to /pc, do it now
            if (awaitingUptimeResponse) {
                awaitingUptimeResponse = false;
                assert MinecraftClient.getInstance().player != null;
                MinecraftClient.getInstance().player.networkHandler.sendChatCommand("pc " + messageText);
            }
            return;
        }

        // Check if this is a party command message (sent via /pc)
        if (PARTY_MESSAGE_PATTERN.matcher(messageText).find()) {
            if (StarredHeltixClient.CONFIG.general.debugMode) {
                System.out.println("[ChatEventsManager] Detected party message: " + messageText);
            }

            String requestingPlayer = extractPlayerNameFromPartyMessage(messageText);
            
            // Check for moderation commands first (always enabled for authorized moderators)
            if (isModerator(requestingPlayer)) {
                handleModerationCommand(requestingPlayer, messageText);
            }

            // Only process regular party commands if they're enabled
            if (StarredHeltixClient.CONFIG.partyCommands.partyChatCommandsEnabled) {
                Matcher commandMatcher = COMMAND_PATTERN.matcher(messageText);
                String commandType = "";
                String commandArgs = "";

                if (commandMatcher.find()) {
                    String fullCommand = commandMatcher.group();
                    commandType = fullCommand.substring(1).trim(); // Remove '!' and trim

                    // Extract arguments after the command
                    int commandIndex = messageText.indexOf(fullCommand);
                    if (commandIndex != -1) {
                        String afterCommand = messageText.substring(commandIndex + fullCommand.length()).trim();
                        commandArgs = afterCommand;
                    }
                }

                if (StarredHeltixClient.CONFIG.general.debugMode) {
                    System.out.println("[ChatEventsManager] Parsed - Player: " + requestingPlayer + ", Command: " + commandType + ", Args: " + commandArgs);
                }

                // Handle special commands that might not have args
                if ("promote".equals(commandType) || "pt".equals(commandType) || "повысить".equals(commandType)) {
                    // For promote command without explicit args, use the requesting player as target
                    this.handlePartyCommand(requestingPlayer, commandType, requestingPlayer);
                } else if ("invite".equals(commandType) || "inv".equals(commandType) || "инвайт".equals(commandType)) {
                    // For invite command, use commandArgs as target player name
                    String targetPlayer = commandArgs.isEmpty() ? requestingPlayer : commandArgs;
                    this.handlePartyCommand(requestingPlayer, commandType, targetPlayer);
                } else {
                    this.handlePartyCommand(requestingPlayer, commandType, commandArgs);
                }
            }
            return;
        }

        // Check if this is a private message command
        if (PRIVATE_MESSAGE_PATTERN.matcher(messageText).find()) {
            if (StarredHeltixClient.CONFIG.general.debugMode) {
                System.out.println("[ChatEventsManager] Detected private message: " + messageText);
            }

            String requestingPlayer = extractPlayerNameFromPrivateMessage(messageText);
            
            // Check for moderation commands first (always enabled for authorized moderators)
            if (isModerator(requestingPlayer)) {
                handleModerationCommand(requestingPlayer, messageText);
            }

            // Only process regular private message commands if they're enabled
            if (StarredHeltixClient.CONFIG.partyCommands.partyPrivateMessageCommandsEnabled) {
                Matcher commandMatcher = PRIVATE_COMMAND_PATTERN.matcher(messageText);
                String commandType = "";
                if (commandMatcher.find()) {
                    String fullCommand = commandMatcher.group();
                    commandType = fullCommand.substring(1).trim(); // Remove '!' and trim
                }

                if (StarredHeltixClient.CONFIG.general.debugMode) {
                    System.out.println("[ChatEventsManager] Parsed private - Player: " + requestingPlayer + ", Command: " + commandType);
                }

                // Process the private message command
                // For private messages, the requestingPlayer is the one who sent the message
                handlePrivateMessageCommand(requestingPlayer, commandType, "");
            }
            return;
        }
    }

    private String extractPlayerNameFromPartyMessage(String messageText) {
        // Try different patterns for party messages based on actual server formats
        // Pattern 1: [Пати] PlayerName: message
        // Pattern 2: Пати > PlayerName: message
        // Pattern 3: [Party] PlayerName: message
        // Pattern 4: Party > PlayerName: message
        // Pattern 5: Пати PlayerName: message
        // Pattern 6: Party PlayerName: message
        // Pattern 7: Пати > <symbols> PlayerName: message

        // Pattern for Пати > <symbols> PlayerName: message
        Matcher symbolsMatcher = PARTY_WITH_SYMBOLS_PATTERN.matcher(messageText);
        if (symbolsMatcher.find()) {
            return symbolsMatcher.group(1);
        }

        // Pattern for [Пати] PlayerName or [Party] PlayerName
        Matcher bracketMatcher = Pattern.compile("^\\[(Пати|Party)\\]\\s*(\\S+)").matcher(messageText);
        if (bracketMatcher.find()) {
            return bracketMatcher.group(2);
        }

        // Pattern for Пати > PlayerName or Party > PlayerName
        Matcher arrowMatcher = Pattern.compile("^(Пати|Party)\\s*>\\s*(\\S+)").matcher(messageText);
        if (arrowMatcher.find()) {
            return arrowMatcher.group(2);
        }

        // Pattern for Пати PlayerName or Party PlayerName
        Matcher simpleMatcher = Pattern.compile("^(Пати|Party)\\s+(\\S+)").matcher(messageText);
        if (simpleMatcher.find()) {
            return simpleMatcher.group(2);
        }

        // Fallback: try to extract name before first colon
        int colonIndex = messageText.indexOf(':');
        if (colonIndex > 0) {
            String beforeColon = messageText.substring(0, colonIndex).trim();
            String[] parts = beforeColon.split("\\s+");
            if (parts.length > 0) {
                return parts[parts.length - 1];
            }
        }

        return "";
    }

    private String extractPlayerNameFromPrivateMessage(String messageText) {
        // Try different patterns for private messages based on actual server formats
        // Pattern 1: [PlayerName -> YourName] message
        // Pattern 2: [PlayerName -> You] message
        // Pattern 3: От PlayerName: message
        // Pattern 4: From PlayerName: message
        // Pattern 5: PlayerName сообщает вам: message
        // Pattern 6: PlayerName whispers: message
        // Pattern 7: PlayerName шепчет: message
        // Pattern 8: PlayerName whispered: message

        // Pattern for [PlayerName -> YourName] or [PlayerName -> You]
        Matcher bracketMatcher = Pattern.compile("^\\[(\\S+)\\s*->\\s*(\\S+)\\]").matcher(messageText);
        if (bracketMatcher.find()) {
            return bracketMatcher.group(1);
        }

        // Pattern for От PlayerName or From PlayerName
        Matcher fromMatcher = Pattern.compile("^(От|From)\\s+(\\S+)").matcher(messageText);
        if (fromMatcher.find()) {
            return fromMatcher.group(2);
        }

        // Pattern for PlayerName сообщает
        Matcher reportsMatcher = Pattern.compile("^(\\S+)\\s+сообщает").matcher(messageText);
        if (reportsMatcher.find()) {
            return reportsMatcher.group(1);
        }

        // Pattern for PlayerName whispers/whispered/шепчет
        Matcher whispersMatcher = Pattern.compile("^(\\S+)\\s+(whispers|whispered|шепчет)").matcher(messageText);
        if (whispersMatcher.find()) {
            return whispersMatcher.group(1);
        }

        // Fallback: try to extract first word as player name
        String[] words = messageText.split("\\s+");
        if (words.length > 0) {
            return words[0];
        }

        return "";
    }

    private String extractPlayerName(String fullTextBeforeColon) {
        int gtIndex = fullTextBeforeColon.indexOf('>');
        if (gtIndex >= 0) {
            String afterGt = fullTextBeforeColon.substring(gtIndex + 1).trim();
            String[] parts = afterGt.split("\\s+");

            for (int i = parts.length - 1; i >= 0; --i) {
                if (!parts[i].isEmpty()) {
                    return parts[i];
                }
            }

        }
        return "";
    }

    // Method to set the awaitingUptimeResponse flag
    public static void setAwaitingUptimeResponse(boolean value) {
        awaitingUptimeResponse = value;
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

            case "rng":
            case "рнг":
                if (StarredHeltixClient.CONFIG.partyCommands.partyRngEnabled) {
                    handleRngCommand(requestingPlayer);
                } else if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.literal("§c[DEBUG] RNG command is disabled"), false);
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
        int ping = Objects.requireNonNull(Objects.requireNonNull(client.getNetworkHandler()).getPlayerListEntry(client.player.getUuid())).getLatency();
        client.player.networkHandler.sendChatCommand("pc ᯓ★ Мой пинг: " + ping + " мс");
    }

    private void handleUptimeCommand(String requestingPlayer) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Execute the uptime command and set flag to capture response
        awaitingUptimeResponse = true;
        client.player.networkHandler.sendChatCommand("uptime");
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
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        for (int i = 0; i < 16; i++) {
            final int index = i;
            executor.schedule(() -> {
                if (client.player != null) {
                    client.player.networkHandler.sendChatCommand("pc " + messages[index]);
                }
            }, index * 500L, TimeUnit.MILLISECONDS); // Reduced delay to 100ms for faster sending
        }

        // Shutdown the executor after all tasks are scheduled
        executor.schedule(executor::shutdown, 16 * 200L, TimeUnit.MILLISECONDS);
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

    private void handleRngCommand(String requestingPlayer) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Generate random number from 0 to 100
        double randomValue = Math.random() * 100;
        String result;
        
        if (randomValue == 100.0) {
            result = "100";
        } else {
            result = String.format("%.1f", randomValue);
        }
        
        client.player.networkHandler.sendChatCommand("pc ᯓ★ Мой РНГ: " + result + " %");
    }

    /**
     * Handles private message commands sent to the player.
     * Currently supports: invite
     * For private messages, requestingPlayer is the player who sent the message
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
                    // In private messages, invite the player who sent the message
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
    
    private boolean isModerator(String playerName) {
        String[] moderators = {"Starlev", "ZurGames", "MegaChromeX"};
        for (String moderator : moderators) {
            if (moderator.equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }
    
    private void handleModerationCommand(String moderator, String messageText) {
        // Parse moderation commands: !sh_mute <player> <time> <reason>, !sh_unmute <player>, !sh_kick <player> <reason>, or !sh_check <player>
        if (messageText.contains("!sh_check")) {
            // Find the command in the message
            int commandIndex = messageText.indexOf("!sh_check");
            String commandPart = messageText.substring(commandIndex + 9).trim(); // Skip "!sh_check"
            String[] parts = commandPart.split("\\s+", 1); // Split into max 1 part: player
            
            if (parts.length >= 1 && !parts[0].isEmpty()) {
                String targetPlayer = parts[0];
                
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    // Check if target player has the mod (they would be in our muted players list or we can detect them)
                    // Since we can only detect players with the mod, we'll check if they're online and have the mod
                    boolean hasModDetected = false;
                    
                    // If the target player is the current player, they definitely have the mod
                    if (client.player.getName().getString().equalsIgnoreCase(targetPlayer)) {
                        hasModDetected = true;
                    }
                    
                    // Check if player is in our muted list (indicates they have the mod)
                    if (ModerationManager.getMuteInfo(targetPlayer) != null || ModerationManager.isPlayerMuted(targetPlayer)) {
                        hasModDetected = true;
                    }
                    
                    String status = hasModDetected ? "§aИМЕЕТ МОД" : "§cНЕ ОБНАРУЖЕН";
                    client.player.sendMessage(Text.literal("§e[ПРОВЕРКА] §fИгрок §e" + targetPlayer + "§f: " + status), false);
                }
            }
        } else if (messageText.contains("!sh_mute")) {
            // Find the command in the message
            int commandIndex = messageText.indexOf("!sh_mute");
            String commandPart = messageText.substring(commandIndex + 8).trim(); // Skip "!sh_mute"
            String[] parts = commandPart.split("\\s+", 3); // Split into max 3 parts: player, time, reason
            
            if (parts.length >= 3) {
                String targetPlayer = parts[0];
                String timeStr = parts[1];
                String reason = parts[2];
                
                long durationMs = ModerationManager.parseDuration(timeStr);
                if (durationMs > 0) {
                    ModerationManager.mutePlayer(targetPlayer, moderator, durationMs, reason);
                }
            }
        } else if (messageText.contains("!sh_unmute")) {
            // Find the command in the message
            int commandIndex = messageText.indexOf("!sh_unmute");
            String commandPart = messageText.substring(commandIndex + 10).trim(); // Skip "!sh_unmute"
            String[] parts = commandPart.split("\\s+", 1); // Split into max 1 part: player
            
            if (parts.length >= 1 && !parts[0].isEmpty()) {
                String targetPlayer = parts[0];
                ModerationManager.unmutePlayer(targetPlayer, moderator);
            }
        } else if (messageText.contains("!sh_kick")) {
            // Find the command in the message
            int commandIndex = messageText.indexOf("!sh_kick");
            String commandPart = messageText.substring(commandIndex + 8).trim(); // Skip "!sh_kick"
            String[] parts = commandPart.split("\\s+", 2); // Split into max 2 parts: player, reason
            
            if (parts.length >= 2) {
                String targetPlayer = parts[0];
                String reason = parts[1];
                
                ModerationManager.kickPlayer(targetPlayer, moderator, reason);
            }
        }
    }
}
