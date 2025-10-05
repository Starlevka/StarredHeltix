package set.starlev.starredheltix.util.user;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ModUserManager {
    private static final ModUserManager INSTANCE = new ModUserManager();
    private final Set<UUID> modUsers = new HashSet<>();
    
    private ModUserManager() {
        // Private constructor for singleton
    }
    
    public static ModUserManager getInstance() {
        return INSTANCE;
    }
    
    public void addModUser(UUID playerUUID) {
        modUsers.add(playerUUID);
    }
    
    public void removeModUser(UUID playerUUID) {
        modUsers.remove(playerUUID);
    }
    
    public boolean isModUser(UUID playerUUID) {
        // In a client-only mod, we assume all players who are in the game are using the mod
        // This is a simplification for client-only mods where there's no server to relay this information
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getNetworkHandler() != null) {
            // Check if the player is in the current player list
            for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                if (entry.getProfile() != null && entry.getProfile().getId().equals(playerUUID)) {
                    return true;
                }
            }
        }
        
        // Also check if it's the current player
        if (client != null && client.player != null && client.player.getUuid().equals(playerUUID)) {
            return true;
        }
        
        // Check if we explicitly know this player uses the mod
        return modUsers.contains(playerUUID);
    }
    
    /**
     * For testing purposes, mark the current player as a mod user
     */
    public void markCurrentPlayerAsModUser() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            addModUser(client.player.getUuid());
        }
    }
    
    /**
     * Gets an icon for the player based on their status
     * @param playerUUID The player's UUID
     * @return A text component with the appropriate icon
     */
    public Text getPlayerIcon(UUID playerUUID) {
        // Show star only for players who are in the game (we assume they have the mod)
        if (isModUser(playerUUID)) {
            return Text.literal("⭐").formatted(Formatting.GOLD);
        }
        return Text.literal("");
    }
    
    /**
     * Modifies a player's name to include an icon if they're using the mod
     * @param playerName The original player name
     * @param playerUUID The player's UUID
     * @return The modified player name with icon if they're using the mod
     */
    public Text getModifiedPlayerName(String playerName, UUID playerUUID) {
        Text icon = getPlayerIcon(playerUUID);
        if (!icon.getString().isEmpty()) {
            return Text.literal("").append(icon).append(" ").append(playerName);
        }
        return Text.literal(playerName);
    }
    
    /**
     * Modifies a player's name to include an icon if they're using the mod
     * @param playerListEntry The player list entry
     * @return The modified player name with icon if they're using the mod
     */
    public Text getModifiedPlayerName(PlayerListEntry playerListEntry) {
        if (playerListEntry.getProfile() != null) {
            String playerName = playerListEntry.getProfile().getName();
            Text icon = getPlayerIcon(playerListEntry.getProfile().getId());
            if (!icon.getString().isEmpty()) {
                return Text.literal("").append(icon).append(" ").append(playerName);
            }
            return Text.literal(playerName);
        }
        
        // Last resort - return "Unknown" if we have no information
        return Text.literal("Unknown");
    }
    
    /**
     * Get the count of mod users
     * @return Number of known mod users
     */
    public int getModUserCount() {
        // In a client-only mod, we return the count of players in the game
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getNetworkHandler() != null) {
            return client.getNetworkHandler().getPlayerList().size() + (client.player != null ? 1 : 0);
        }
        return modUsers.size();
    }
    
    /**
     * Get the set of mod user UUIDs
     * @return Set of mod user UUIDs
     */
    public Set<UUID> getModUsers() {
        // In a client-only mod, we return all players in the game
        Set<UUID> players = new HashSet<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getNetworkHandler() != null) {
            for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                if (entry.getProfile() != null) {
                    players.add(entry.getProfile().getId());
                }
            }
        }
        if (client != null && client.player != null) {
            players.add(client.player.getUuid());
        }
        return players;
    }
}