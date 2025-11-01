package set.starlev.starredheltix.util;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ModVersionRegistry {
    private static final String CURRENT_VERSION = getModVersion();
    private static final Map<String, String> playerVersions = new ConcurrentHashMap<>();
    
    public static void register() {
        // For now, just register connection events
        // In future versions, we can implement proper networking
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player != null) {
                // Register current player version
                playerVersions.put(client.player.getName().getString(), CURRENT_VERSION);
                // Register player with the network server
                ModNetworkManager.registerPlayer();
            }
        });
    }
    
    public static String getPlayerVersion(String playerName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getName().getString().equalsIgnoreCase(playerName)) {
            return CURRENT_VERSION;
        }
        return playerVersions.get(playerName);
    }
    
    public static void registerPlayerWithMod(String playerName) {
        if (!playerVersions.containsKey(playerName)) {
            playerVersions.put(playerName, CURRENT_VERSION);
            System.out.println("[ModVersionRegistry] Registered player with mod: " + playerName + " (v" + CURRENT_VERSION + ")");
        }
    }
    
    public static Map<String, String> getAllRegisteredPlayers() {
        return new java.util.HashMap<>(playerVersions);
    }
    
    public static boolean hasModVersion(String playerName, String minVersion) {
        String playerVersion = getPlayerVersion(playerName);
        return playerVersion != null && isVersionAtLeast(playerVersion, minVersion);
    }
    
    public static void addPlayer(String playerName, String version) {
        playerVersions.put(playerName, version);
    }
    
    private static boolean isVersionAtLeast(String version, String minVersion) {
        String[] versionParts = version.split("\\.");
        String[] minVersionParts = minVersion.split("\\.");
        
        for (int i = 0; i < Math.max(versionParts.length, minVersionParts.length); i++) {
            int versionPart = i < versionParts.length ? Integer.parseInt(versionParts[i]) : 0;
            int minVersionPart = i < minVersionParts.length ? Integer.parseInt(minVersionParts[i]) : 0;
            
            if (versionPart > minVersionPart) {
                return true;
            } else if (versionPart < minVersionPart) {
                return false;
            }
        }
        
        return true; // Equal versions
    }
    
    private static String getModVersion() {
        try {
            // Try to get version from fabric.mod.json
            java.io.InputStream stream = ModVersionRegistry.class.getResourceAsStream("/fabric.mod.json");
            if (stream != null) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(stream));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("\"version\"")) {
                        String version = line.split(":")[1].trim().replace("\"", "").replace(",", "");
                        reader.close();
                        return version;
                    }
                }
                reader.close();
            }
        } catch (Exception e) {
            // Fallback to version.txt
        }
        
        try {
            java.io.InputStream stream = ModVersionRegistry.class.getResourceAsStream("/version.txt");
            if (stream != null) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(stream));
                String version = reader.readLine();
                reader.close();
                return version != null ? version.trim() : "0.0.7";
            }
        } catch (Exception e) {
            // Final fallback
        }
        return "0.0.6";
    }
}