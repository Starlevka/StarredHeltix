package set.starlev.starredheltix.util.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import set.starlev.starredheltix.client.StarredHeltixClient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayerPingUtil {
    private static final Map<String, Integer> playerPings = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static MinecraftClient client;
    private static boolean scheduled = false;
    
    public static void initialize(MinecraftClient mc) {
        client = mc;
        
        // Schedule regular updates if not already scheduled
        if (!scheduled) {
            scheduled = true;
            scheduler.scheduleAtFixedRate(PlayerPingUtil::updateAllPlayersPing, 1, 1, TimeUnit.SECONDS);
        }
    }
    
    public static void updatePlayerPing(String playerName, int ping) {
        if (StarredHeltixClient.CONFIG != null && StarredHeltixClient.CONFIG.partyCommands.partyPingEnabled) {
            playerPings.put(playerName, ping);
        }
    }
    
    public static int getPlayerPing(String playerName) {
        return playerPings.getOrDefault(playerName, 0);
    }
    
    public static void clearPlayerPings() {
        playerPings.clear();
    }
    
    private static void updateAllPlayersPing() {
        if (client != null && client.getNetworkHandler() != null && 
            StarredHeltixClient.CONFIG != null && StarredHeltixClient.CONFIG.partyCommands.partyPingEnabled &&
            StarredHeltixClient.CONFIG.general.enabled) {
            
            client.getNetworkHandler().getPlayerList().forEach(entry -> {
                if (entry.getProfile() != null && entry.getProfile().getName() != null) {
                    updatePlayerPing(entry.getProfile().getName(), entry.getLatency());
                }
            });
        }
    }
}