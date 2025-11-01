package set.starlev.starredheltix.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import set.starlev.starredheltix.commands.StarredHeltixCommands;
import set.starlev.starredheltix.commands.StarredHeltixPartyCommands;
import set.starlev.starredheltix.config.StarredHeltixConfig;
import set.starlev.starredheltix.util.chat.ChatEventsManager;
import set.starlev.starredheltix.util.qol.FishingNotifier;
import set.starlev.starredheltix.util.qol.FishingTimerRenderer;
import set.starlev.starredheltix.util.qol.InventoryFullNotifier;
import set.starlev.starredheltix.util.chat.AutoReadyNotifier;
import set.starlev.starredheltix.util.player.AutoSprint;
import set.starlev.starredheltix.util.qol.SlotLockManager;
import set.starlev.starredheltix.util.solver.bloodroom.BloodRoomTimer;
import set.starlev.starredheltix.util.updater.ModUpdater;

import set.starlev.starredheltix.util.qol.TreeCapCooldownVisualizer;
import set.starlev.starredheltix.util.qol.AbilityCooldownVisualizer;
import set.starlev.starredheltix.util.qol.VotingReminder;
import set.starlev.starredheltix.util.binds.CustomBindManager;
import set.starlev.starredheltix.util.ModVersionRegistry;
import set.starlev.starredheltix.util.ModNetworkManager;
import set.starlev.starredheltix.util.solver.dungeons.ThreeWeirdosSolver;
import set.starlev.starredheltix.sound.ModSounds;

public class StarredHeltixClient implements ClientModInitializer {
    public static StarredHeltixConfig CONFIG;
    


    // Counter for periodic checks
    private static int tickCounter = 0;
    private static int votingReminderDelay = 0;

    @Override
    public void onInitializeClient() {
        // Load configuration
        System.out.println("=== StarredHeltix Initialization ===");
        CONFIG = StarredHeltixConfig.load();



        // Register client events
        registerClientEvents();
        
        // Register network events
        registerNetworkEvents();
        
        // Initialize and register features
        initializeFeatures();
    }
    


    /**
     * Register client-side events
     */
    private void registerClientEvents() {
        // Register tick event for key handling
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient minecraftClient) {
        




        // Periodic check to ensure player list is updated
        tickCounter++;
        if (tickCounter >= 100) { // Check every 100 ticks (about 5 seconds)
            tickCounter = 0;
            if (minecraftClient.player != null) {
                minecraftClient.getNetworkHandler();
            } // This is just to ensure our system is working
        }

        // Show voting reminder after 5 minutes (6000 ticks) of being on server
        if (minecraftClient.player != null && minecraftClient.getNetworkHandler() != null) {
            votingReminderDelay++;
            if (votingReminderDelay >= 6000) { // 5 minutes
                votingReminderDelay = 0;
                VotingReminder.checkAndShowReminder();
            }
        }

    }

    /**
     * Register network events
     */
    private void registerNetworkEvents() {
        // Register connection events
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Reset voting reminder delay when joining server
            votingReminderDelay = 0;
        });

        // Register custom bind commands

    }
    
    /**
     * Initialize and register all features
     */
    private void initializeFeatures() {
        try {
            
            // Check for updates on startup
            ModUpdater.checkForUpdates();
            
            // Register commands
            StarredHeltixCommands.registerCommands();
            StarredHeltixPartyCommands.registerCommands();
            
            // Register Auto-sprint
            AutoSprint.register();
            
            // Register other features
            InventoryFullNotifier.register();
            
            // Register slot locking manager
            SlotLockManager.register();
            
            // Register auto ready notifier
            AutoReadyNotifier.register();

            // Register chat event manager
            ChatEventsManager chatEventsManager = new ChatEventsManager(MinecraftClient.getInstance());
            chatEventsManager.register();

            // Register blood room timer
            BloodRoomTimer.register();
            
            // Register woodworm cooldown visualizer
            TreeCapCooldownVisualizer.register();

            // Register ability cooldown visualizer
            AbilityCooldownVisualizer.register();

            // Register fishing notifier
            FishingNotifier.register();
            FishingTimerRenderer.register();
            
            // Initialize custom bind manager
            CustomBindManager.initialize();
            
            // Register mod version registry
            ModVersionRegistry.register();
            
            // Register mod network manager
            // Note: No explicit initialization needed as it's used statically
            
            // Register Three Weirdos solver
            ThreeWeirdosSolver.register();
            
            // Register custom sounds
            ModSounds.registerSounds();
            
            // Register welcome message
            set.starlev.starredheltix.util.qol.WelcomeMessage.register();
            
            // Register enderman highlighter
            set.starlev.starredheltix.util.render.EndermanHighlighter.register();
            
            // Register wolf highlighter
            set.starlev.starredheltix.util.render.WolfHighlighter.register();
            
            // Register waypoint manager
            set.starlev.starredheltix.util.waypoints.WaypointManager.register();

        } catch (Exception ignored) {
        }
    }

    /**
     * Reload the configuration
     */
    public static void reloadConfig() {
        try {
            CONFIG = StarredHeltixConfig.load();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(net.minecraft.text.Text.literal("§aКонфигурация успешно перезагружена"), false);
            }
        } catch (Exception e) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(net.minecraft.text.Text.literal("§cОшибка при перезагрузке конфигурации: " + e.getMessage()), false);
            }
        }
    }
}