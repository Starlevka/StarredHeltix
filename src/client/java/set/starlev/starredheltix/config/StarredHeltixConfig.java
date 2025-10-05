package set.starlev.starredheltix.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StarredHeltixConfig {
    // General settings
    public GeneralSettings general = new GeneralSettings();
    
    // Discord RPC settings
    public DiscordRpcSettings discordRpc = new DiscordRpcSettings();
    
    // Party commands settings
    public PartyCommandsSettings partyCommands = new PartyCommandsSettings();
    
    // Fairy souls settings
    public FairySoulsSettings fairySouls = new FairySoulsSettings();
    
    // Slot locking feature
    public SlotLockingSettings slotLocking = new SlotLockingSettings();
    
    // Message filters
    public MessageFilterSettings messageFilters = new MessageFilterSettings();
    
    // Blood room settings
    public BloodRoomSettings bloodRoom = new BloodRoomSettings();
    
    // Endermen highlight settings
    public EndermenHighlightSettings endermenHighlight = new EndermenHighlightSettings();
    
    // Wolf highlight settings
    public WolfHighlightSettings wolfHighlight = new WolfHighlightSettings();
    
    // Woodworm axe cooldown settings
    public WoodwormCooldownSettings woodwormCooldown = new WoodwormCooldownSettings();
    
    // Armor hiding settings
    public ArmorHidingSettings armorHiding = new ArmorHidingSettings();
    
    // Equipment display settings
    public EquipmentSettings equipment = new EquipmentSettings();
    
    // Character highlight settings
    public CharacterHighlightSettings characterHighlight = new CharacterHighlightSettings();
    
    // Auto-sprint settings
    public AutoSprintSettings autoSprint = new AutoSprintSettings();

    // Custom commands with keybindings
    public Map<String, String> customCommands = new HashMap<>();
    public Map<String, String> customCommandKeybindings = new HashMap<>();
    
    // Section positions in the configuration menu
    public int qolSectionX = -1;
    public int qolSectionY = -1;
    public int utilitiesSectionX = -1;
    public int utilitiesSectionY = -1;
    public int generalSectionX = -1;
    public int generalSectionY = -1;
    public int partyCommandsSectionX = -1;
    public int partyCommandsSectionY = -1;
    public int endermenHighlightSectionX = -1;
    public int endermenHighlightSectionY = -1;

    // For storing uptime information
    public String lastUptimeMessage = "";
    public long currentUptime = 0;

    // Login password for /вход command
    public String loginPassword = "";

    // General settings section
    public static class GeneralSettings {
        public boolean enabled = true;
        public boolean chattingEnabled = true;
        public boolean debugMode = false; // Is debug mode enabled
        public int debugUpdateInterval = 3000; // in milliseconds
        public boolean inventoryFullWarningEnabled = true; // Is inventory full warning enabled
    }
    
    // Discord RPC settings section
    public static class DiscordRpcSettings {
        public boolean rpcEnabled = true;
        public String rpcApplicationId = "1415296884267941969"; // replace with your App ID
        public String rpcDetails = "Играет на Heltix SkyBlock";
        public String rpcState = "Enjoying the game";
        public String rpcLargeImageKey = "starredheltix";
        public String rpcLargeImageText = "StarredHeltix Client";
        public String rpcSmallImageKey = "starpyps";
        public String rpcSmallImageText = "starlev.heltix.net";
        public int rpcUpdateMs = 5000; // Update interval in milliseconds
    }
    
    // Party commands settings section
    public static class PartyCommandsSettings {
        public boolean partyChatCommandsEnabled = true; // Are party chat commands enabled
        public boolean partyPromoteEnabled = true; // Is the !promote command enabled
        public boolean partyKickEnabled = true;   // Is the !kick command enabled
        public boolean partyInviteEnabled = true; // Is the !invite command enabled
        public boolean partyPingEnabled = true; // Is the !ping command enabled
        public boolean partyUptimeEnabled = true; // Is the !uptime command enabled
        public boolean partyDtEnabled = true; // Is the !dt command enabled
        public boolean partyFpsEnabled = true; // Is the !fps command enabled
        public boolean partyTimeEnabled = true; // Is the !time command enabled
        public boolean partyCoinEnabled = true; // Is the !coin command enabled
        public boolean partyDiceEnabled = true; // Is the !dice command enabled
        public boolean partyCoordsEnabled = true; // Is the !coords command enabled
        public boolean partyBoykisserEnabled = true; // Is the !boykisser command enabled
        public boolean partyPrivateMessageCommandsEnabled = true; // Are private message commands enabled
        public String customReadyPhrase = "Я готов к спуску в подземелья! :P"; // Custom phrase for the /яготовлёвал command
    }
    
    // Fairy souls settings section
    public static class FairySoulsSettings {
        public boolean fairySoulsEnabled = true; // Is the fairy souls feature enabled
        public boolean debugMode = false; // Is the fairy souls debug mode enabled
    }
    
    // Slot locking feature settings
    public static class SlotLockingSettings {
        public boolean slotLockingEnabled = true; // Is the slot locking feature enabled
        public Set<Integer> lockedSlots = new HashSet<>(); // List of locked slots
    }
    
    // Message filter settings
    public static class MessageFilterSettings {
        public Map<Integer, String> filters = new HashMap<>();
    }
    
    // Blood room settings
    public static class BloodRoomSettings {
        public boolean bloodRoomTimerEnabled = true; // Is the blood room timer enabled
    }
    
    /**
     * Settings for highlighting endermen in the End biome
     */
    public static class EndermenHighlightSettings {
        public boolean endermenHighlightEnabled = true; // Is endermen highlighting enabled
        public boolean endermenHighlightOnlyInEnd = false; // Highlight only in End dimension
        public int highlightColor = 0x00FF0A; // Default color is green
    }
    
    /**
     * Settings for highlighting wolves
     */
    public static class WolfHighlightSettings {
        public boolean wolfHighlightEnabled = true; // Is wolf highlighting enabled
        public int highlightColor = 0xFF9A00; // Default color is orange
    }
    
    /**
     * Settings for Woodworm axe cooldown visualization
     */
    public static class WoodwormCooldownSettings {
        public boolean enabled = true; // Is the woodworm cooldown visualization enabled
        public int baseCooldownMs = 2000; // Base cooldown in milliseconds (2 seconds)
        public int cooldownPercentage = 100; // Cooldown percentage modifier (1-50%)
    }
    
    /**
     * Settings for armor hiding
     */
    public static class ArmorHidingSettings {
        public boolean enabled = false; // Is armor hiding enabled
    }
    
    /**
     * Settings for equipment display
     */
    public static class EquipmentSettings {
        public boolean enabled = true; // Is equipment display enabled
    }
    
    /**
     * Settings for character highlight feature
     */
    public static class CharacterHighlightSettings {
        public boolean enabled = true; // Is character highlight feature enabled
        public int highlightColor = 0x00FF00; // Default color is green
        public int highlightDurationMs = 10000; // Highlight duration in milliseconds (10 seconds)
    }
    
    /**
     * Settings for auto-sprint feature
     */
    public static class AutoSprintSettings {
        public boolean enabled = true; // Is auto-sprint feature enabled
        public boolean onlyInSkyBlock = true; // Only auto-sprint in SkyBlock
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = new File("config/starredheltix");
    private static final File FILE = new File(CONFIG_DIR, "starredheltix.json");

    public StarredHeltixConfig() {
        customCommands = new HashMap<>();
        customCommandKeybindings = new HashMap<>();
        general = new GeneralSettings();
        discordRpc = new DiscordRpcSettings();
        partyCommands = new PartyCommandsSettings();
        fairySouls = new FairySoulsSettings();
        slotLocking = new SlotLockingSettings();
        messageFilters = new MessageFilterSettings();
        bloodRoom = new BloodRoomSettings();
        endermenHighlight = new EndermenHighlightSettings();
        wolfHighlight = new WolfHighlightSettings();
        woodwormCooldown = new WoodwormCooldownSettings();
        armorHiding = new ArmorHidingSettings();
        equipment = new EquipmentSettings();
        characterHighlight = new CharacterHighlightSettings();
        autoSprint = new AutoSprintSettings();
        messageFilters.filters = new HashMap<>();
    }

    public void save() {
        try {
            if (!CONFIG_DIR.exists()) {
                CONFIG_DIR.mkdirs();
            }
            
            FileWriter writer = new FileWriter(FILE);
            GSON.toJson(this, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static StarredHeltixConfig load() {
        try {
            if (!CONFIG_DIR.exists()) {
                CONFIG_DIR.mkdirs();
            }

            if (FILE.exists()) {
                FileReader reader = new FileReader(FILE);
                StarredHeltixConfig config = GSON.fromJson(reader, StarredHeltixConfig.class);
                reader.close();
                
                // Initialize nested objects if they're null (for configs created before this restructuring)
                if (config.general == null) {
                    config.general = new GeneralSettings();
                }
                
                if (config.discordRpc == null) {
                    config.discordRpc = new DiscordRpcSettings();
                }
                
                if (config.partyCommands == null) {
                    config.partyCommands = new PartyCommandsSettings();
                }
                
                if (config.fairySouls == null) {
                    config.fairySouls = new FairySoulsSettings();
                }
                
                if (config.slotLocking == null) {
                    config.slotLocking = new SlotLockingSettings();
                }
                
                if (config.messageFilters == null) {
                    config.messageFilters = new MessageFilterSettings();
                }
                
                if (config.bloodRoom == null) {
                    config.bloodRoom = new BloodRoomSettings();
                }
                
                if (config.endermenHighlight == null) {
                    config.endermenHighlight = new EndermenHighlightSettings();
                }
                
                if (config.wolfHighlight == null) {
                    config.wolfHighlight = new WolfHighlightSettings();
                }
                
                if (config.woodwormCooldown == null) {
                    config.woodwormCooldown = new WoodwormCooldownSettings();
                }
                
                if (config.armorHiding == null) {
                    config.armorHiding = new ArmorHidingSettings();
                }
                
                if (config.equipment == null) {
                    config.equipment = new EquipmentSettings();
                }
                
                if (config.characterHighlight == null) {
                    config.characterHighlight = new CharacterHighlightSettings();
                }
                
                if (config.autoSprint == null) {
                    config.autoSprint = new AutoSprintSettings();
                }
                
                // Initialize nested collections
                if (config.messageFilters.filters == null) {
                    config.messageFilters.filters = new HashMap<>();
                }
                
                if (config.slotLocking.lockedSlots == null) {
                    config.slotLocking.lockedSlots = new HashSet<>();
                }
                
                return config;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Return default config
        return new StarredHeltixConfig();
    }
}