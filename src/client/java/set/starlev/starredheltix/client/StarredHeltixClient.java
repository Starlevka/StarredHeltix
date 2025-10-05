package set.starlev.starredheltix.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import set.starlev.starredheltix.commands.StarredHeltixCommands;
import set.starlev.starredheltix.commands.StarredHeltixPartyCommands;
import set.starlev.starredheltix.config.StarredHeltixConfig;
import set.starlev.starredheltix.network.ModIdentificationPacket;
import set.starlev.starredheltix.rpc.DiscordRpcManager;
import set.starlev.starredheltix.util.chat.ChatEventsManager;
import set.starlev.starredheltix.util.entity.CharacterHighlighter;
import set.starlev.starredheltix.util.entity.EndermenHighlighter;
import set.starlev.starredheltix.util.entity.WolfHighlighter;
import set.starlev.starredheltix.util.equipment.EquipmentManager;
import set.starlev.starredheltix.util.inventory.InventoryFullNotifier;
import set.starlev.starredheltix.util.player.AutoReadyNotifier;
import set.starlev.starredheltix.util.player.AutoSprint;
import set.starlev.starredheltix.util.slotlocking.SlotLockManager;
import set.starlev.starredheltix.util.solver.bloodroom.BloodRoomTimer;
import set.starlev.starredheltix.util.solver.exptable.ExperimentTableMemoryOverlay;
import set.starlev.starredheltix.util.solver.fairysouls.FairySoulsWaypointRenderer;
import set.starlev.starredheltix.util.solver.fairysouls.PlayerHeadWaypointRenderer;
import set.starlev.starredheltix.util.updater.ModUpdater;
import set.starlev.starredheltix.util.user.ModUserManager;
import set.starlev.starredheltix.util.woodworm.WoodwormCooldownVisualizer;

public class StarredHeltixClient implements ClientModInitializer {
    public static StarredHeltixConfig CONFIG;
    private static DiscordRpcManager discordRpcManager;
    
    // Debug keybinding
    private static KeyBinding debugKey;
    
    //Counter for periodic checks
    private static int tickCounter = 0;
    private static int discordRpcUpdateCounter = 0;

    @Override
    public void onInitializeClient() {
        // Load configuration
        CONFIG = StarredHeltixConfig.load();
        // Register keybindings
        registerKeybindings();
        // Register client events
        registerClientEvents();
        
        // Register network events
        registerNetworkEvents();
        
        // Initialize and register features
        initializeFeatures();
    }
    
    /**
     * Register keybindings
     */
    private void registerKeybindings() {
        // Register debug keybinding
        debugKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.starredheltix.debug",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F7,
            "category.starredheltix.main"
        ));
    }
    
    /**
     * Register client-side events
     */
    private void registerClientEvents() {
        // Register tick event for key handling
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        
        // Register shutdown event for Discord RPC
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (discordRpcManager != null) {
                discordRpcManager.shutdown();
            }
        }));
    }

    private void onClientTick(MinecraftClient minecraftClient) {
        
        // Handle debug key press
        while (debugKey.wasPressed()) {
            // Debug functionality
            System.out.println("StarredHeltix: Current mod users count: " + ModUserManager.getInstance().getModUserCount());
        }
        
        // Periodic check to ensure player list is updated
        tickCounter++;
        if (tickCounter >= 100) { // Check every 100 ticks (about 5 seconds)
            tickCounter = 0;
            if (minecraftClient.player != null) {
                minecraftClient.getNetworkHandler();
            } // This is just to ensure our system is working
        }
        
        // Update Discord RPC every 20 ticks (1 second)
        if (discordRpcManager != null) {
            discordRpcUpdateCounter++;
            if (discordRpcUpdateCounter >= 20) {
                discordRpcUpdateCounter = 0;
                discordRpcManager.tick();
            }
        }
    }

    /**
     * Register network events
     */
    private void registerNetworkEvents() {
        // Register connection events
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Connection event handling
            // Send mod identification packet to mark ourselves as a mod user
            ModIdentificationPacket.sendIdentificationPacket();
            
            // Also mark ourselves locally
            ModUserManager.getInstance().markCurrentPlayerAsModUser();
        });
    }
    
    /**
     * Initialize and register all features
     */
    private void initializeFeatures() {
        try {
            // Register packet types
            PayloadTypeRegistry.playC2S().register(ModIdentificationPacket.PACKET_TYPE, ModIdentificationPacket.CODEC);
            PayloadTypeRegistry.playS2C().register(ModIdentificationPacket.PACKET_TYPE, ModIdentificationPacket.CODEC);
            
            // Check for updates on startup
            ModUpdater.checkForUpdates();
            
            // Register commands
            StarredHeltixCommands.registerCommands();
            StarredHeltixPartyCommands.registerCommands();
            
            // Register Fairy Souls renderer
            FairySoulsWaypointRenderer.register();
            
            // Register Player Head waypoint renderer
            PlayerHeadWaypointRenderer.register();
            
            // Register Endermen highlighter
            EndermenHighlighter.register();
            
            // Register Wolf highlighter
            WolfHighlighter.register();
            
            // Register Character highlighter
            CharacterHighlighter.register();
            
            // Register Auto-sprint
            AutoSprint.register();
            
            // Register other features
            InventoryFullNotifier.register();
            
            // Register slot locking manager
            SlotLockManager.register();
            
            // Register auto ready notifier
            // Register auto ready notifier
            AutoReadyNotifier.register();
            
            // Register mod identification packet receiver
            ModIdentificationPacket.registerClientReceiver();
            
            // Register Discord RPC manager
            discordRpcManager = new DiscordRpcManager();
            try {
                discordRpcManager.init(CONFIG);
                System.out.println("Discord RPC initialized successfully");
            } catch (Exception e) {
                System.err.println("Failed to initialize Discord RPC: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Register chat event manager
            ChatEventsManager chatEventsManager = new ChatEventsManager(MinecraftClient.getInstance());
            chatEventsManager.register();
            
            // Register experiment table memory overlay
            ExperimentTableMemoryOverlay.register();
            
            // Register blood room timer
            BloodRoomTimer.register();
            
            // Register woodworm cooldown visualizer
            WoodwormCooldownVisualizer.register();
            
            // Register equipment manager
            EquipmentManager.register();
            
        } catch (Exception ignored) {
        }
    }

    /**
     * Reload the configuration
     */
    public static void reloadConfig() {
        try {
            StarredHeltixConfig.load();
            // Update Discord RPC with new config
            if (discordRpcManager != null) {
                discordRpcManager.update(CONFIG);
            }
        } catch (Exception ignored) {
        }
    }
    
    /**
     * Get the mod user count
     */
    public static int getModUserCount() {
        return ModUserManager.getInstance().getModUserCount();
    }
}