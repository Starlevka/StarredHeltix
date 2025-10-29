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
    
    // Party commands settings
    public PartyCommandsSettings partyCommands = new PartyCommandsSettings();
    
    // Slot locking feature
    public SlotLockingSettings slotLocking = new SlotLockingSettings();
    
    // Message filters
    public MessageFilterSettings messageFilters = new MessageFilterSettings();
    
    // Blood room settings
    public BloodRoomSettings bloodRoom = new BloodRoomSettings();
    
    // Treecap axe cooldown settings
    public TreecapCooldownSettings treecapCooldown = new TreecapCooldownSettings();

    // Fishing notification settings
    public FishingNotificationSettings fishingNotification = new FishingNotificationSettings();
    

    // Auto-sprint settings
    public AutoSprintSettings autoSprint = new AutoSprintSettings();
    // Ability cooldown settings
    public AbilityCooldownSettings abilityCooldown = new AbilityCooldownSettings();

    // Three weirdos solver settings
    public threeWeirdosSettings threeWeirdos = new threeWeirdosSettings();
    
    // Custom binds settings
    public CustomBindsSettings customBinds = new CustomBindsSettings();
    
    /**
     * Settings for fishing notification feature
     */
    public static class FishingNotificationSettings {
        public boolean enabled = true; // Is fishing notification feature enabled
    }
    


    // For storing uptime information
    public String lastUptimeMessage = "";
    public long currentUptime = 0;

    // Login password for /вход command
    public String loginPassword = "";
    
    // Voting reminder data
    public String lastVotingDate = "";
    public boolean hasVotedToday = false;
    public boolean hasShownVotingReminderToday = false;

    // General settings section
    public static class GeneralSettings {
        public boolean enabled = true;
        public boolean chattingEnabled = true;
        public boolean debugMode = false; // Is debug mode enabled
        public boolean inventoryFullWarningEnabled = true; // Is inventory full warning enabled
        public boolean firstTimeUser = true; // Is this the first time using the mod
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
        public boolean partyCoordsEnabled = true; // Is the !coords command enabled
        public boolean partyBoykisserEnabled = false; // Is the !boykisser command enabled
        public boolean partyRngEnabled = true; // Is the !rng command enabled
        public boolean partyPrivateMessageCommandsEnabled = true; // Are private message commands enabled

        public String customReadyPhrase = "✮ Я готов к подземельям! /=> starreднелtix ✮"; // Custom phrase for the /яготовлёвал command
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
     * Settings for Treecap axe cooldown visualization
     */
    public static class TreecapCooldownSettings {
        public boolean enabled = true; // Is the woodworm cooldown visualization enabled
        public int cooldownPercentage = 100; // Cooldown percentage modifier (1-50%)
    }
    
    /**
     * Settings for three weirdos solver feature
     */
    public static class threeWeirdosSettings {
        public boolean enabled = true; // Is three weirdos solver feature enabled
    }
    
    /**
     * Settings for auto-sprint feature
     */
    public static class AutoSprintSettings {
        public boolean enabled = true; // Is auto-sprint feature enabled
    }

    /**
     * Settings for ability cooldown visualizer
     */
    public static class AbilityCooldownSettings {
        public boolean enabled = true; // Is ability cooldown visualizer enabled
        public boolean kirkobulusEnabled = true; // Is Kirkobulus ability detection enabled
        public boolean miningSpeedBoostEnabled = true; // Is Mining Speed Boost ability detection enabled
        public double kirkobulusCooldown = 60.0; // Kirkobulus cooldown in seconds
        public double miningSpeedBoostCooldown = 120.0; // Mining Speed Boost cooldown in seconds
    }
    
    /**
     * Settings for custom binds
     */
    public static class CustomBindsSettings {
        public Map<String, String> binds = new HashMap<>(); // name -> command
        public Map<String, Integer> keys = new HashMap<>(); // name -> keyCode
    }
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = new File("config/starredheltix");
    private static final File FILE = new File(CONFIG_DIR, "starredheltix.json");

    public StarredHeltixConfig() {
        general = new GeneralSettings();
        partyCommands = new PartyCommandsSettings();
        slotLocking = new SlotLockingSettings();
        messageFilters = new MessageFilterSettings();
        bloodRoom = new BloodRoomSettings();
        threeWeirdos = new threeWeirdosSettings();
        autoSprint = new AutoSprintSettings();
        abilityCooldown = new AbilityCooldownSettings();
        treecapCooldown = new TreecapCooldownSettings();
        customBinds = new CustomBindsSettings();
        fishingNotification = new FishingNotificationSettings();
        messageFilters.filters = new HashMap<>();
        customBinds.binds = new HashMap<>();
        customBinds.keys = new HashMap<>();
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
            System.out.println("Loading StarredHeltix config from: " + FILE.getAbsolutePath());
            if (!CONFIG_DIR.exists()) {
                CONFIG_DIR.mkdirs();
                System.out.println("Created config directory: " + CONFIG_DIR.getAbsolutePath());
            }

            if (FILE.exists()) {
                FileReader reader = new FileReader(FILE);
                StarredHeltixConfig config = GSON.fromJson(reader, StarredHeltixConfig.class);
                reader.close();
                System.out.println("Config loaded successfully");
                // Initialize nested objects if they're null (for configs created before this restructuring)
                if (config.general == null) {
                    config.general = new GeneralSettings();
                }

                
                if (config.partyCommands == null) {
                    config.partyCommands = new PartyCommandsSettings();
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
                
                if (config.treecapCooldown == null) {
                    config.treecapCooldown = new TreecapCooldownSettings();
                }
                
                if (config.autoSprint == null) {
                    config.autoSprint = new AutoSprintSettings();
                }
                
                if (config.threeWeirdos == null) {
                    config.threeWeirdos = new threeWeirdosSettings();
                }
                
                if (config.customBinds == null) {
                    config.customBinds = new CustomBindsSettings();
                }
                if (config.customBinds.binds == null) {
                    config.customBinds.binds = new HashMap<>();
                }
                if (config.customBinds.keys == null) {
                    config.customBinds.keys = new HashMap<>();
                }
                
                if (config.abilityCooldown == null) {
                    config.abilityCooldown = new AbilityCooldownSettings();
                }
                
                // Add fishing notification settings initialization
                if (config.fishingNotification == null) {
                    config.fishingNotification = new FishingNotificationSettings();
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