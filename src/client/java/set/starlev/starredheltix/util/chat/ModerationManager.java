package set.starlev.starredheltix.util.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModerationManager {
    private static final Map<String, MuteInfo> mutedPlayers = new ConcurrentHashMap<>();
    
    // Moderators with immunity
    private static final String[] IMMUNE_MODERATORS = {"MegaChromeX", "ZurGames", "nik36c", "Starlev"};
    private static final String ADMIN = "Starlev";
    
    public static class MuteInfo {
        public final String mutedBy;
        public final long muteEndTime;
        public final String reason;
        
        public MuteInfo(String mutedBy, long muteEndTime, String reason) {
            this.mutedBy = mutedBy;
            this.muteEndTime = muteEndTime;
            this.reason = reason;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > muteEndTime;
        }
        
        public long getRemainingTime() {
            return Math.max(0, muteEndTime - System.currentTimeMillis());
        }
    }
    
    public static void mutePlayer(String playerName, String mutedBy, long durationMs, String reason) {
        // Check immunity - only admin can bypass immunity
        if (hasImmunity(playerName) && !isAdmin(mutedBy)) {
            return; // Silently ignore attempts to mute immune players
        }
        
        long muteEndTime = System.currentTimeMillis() + durationMs;
        mutedPlayers.put(playerName.toLowerCase(), new MuteInfo(mutedBy, muteEndTime, reason));
        
        // Notify the muted player
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getName().getString().equalsIgnoreCase(playerName)) {
            String durationText = formatDuration(durationMs);
            client.player.sendMessage(Text.literal("§c§l[МЬЮТ] §cВы получили мьют от §e" + mutedBy + " §cна §e" + durationText + " §cпо причине: §f" + reason), false);
            client.player.sendMessage(Text.literal("§7§oПредупреждение: это не оффициальный мьют сервера, а функция мода StarredHeltix"), false);
        }
    }
    
    public static void unmutePlayer(String playerName, String unmutedBy) {
        mutedPlayers.remove(playerName.toLowerCase());
        
        // Notify the unmuted player
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getName().getString().equalsIgnoreCase(playerName)) {
            client.player.sendMessage(Text.literal("§a§l[РАЗМЬЮТ] §aВаш мьют был снят модератором §e" + unmutedBy), false);
            client.player.sendMessage(Text.literal("§7§oПредупреждение: это не оффициальный размьют сервера, а функция мода StarredHeltix"), false);
        }
    }
    
    public static void kickPlayer(String playerName, String kickedBy, String reason) {
        // Check immunity - only admin can bypass immunity
        if (hasImmunity(playerName) && !isAdmin(kickedBy)) {
            return; // Silently ignore attempts to kick immune players
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getName().getString().equalsIgnoreCase(playerName)) {
            // Disconnect the player with custom message
            client.getNetworkHandler().getConnection().disconnect(
                Text.literal("§c§l[КИК]\n\n§cВы были кикнуты с сервера\n§cМодератор: §e" + kickedBy + "\n§cПричина: §f" + reason + "\n\n§7§oПредупреждение: это не оффициальный кик сервера,\nа функция мода StarredHeltix")
            );
        }
    }
    
    public static boolean isPlayerMuted(String playerName) {
        MuteInfo muteInfo = mutedPlayers.get(playerName.toLowerCase());
        if (muteInfo == null) return false;
        
        if (muteInfo.isExpired()) {
            mutedPlayers.remove(playerName.toLowerCase());
            return false;
        }
        
        return true;
    }
    
    public static MuteInfo getMuteInfo(String playerName) {
        return mutedPlayers.get(playerName.toLowerCase());
    }
    
    public static long parseDuration(String durationStr) {
        if (durationStr == null || durationStr.isEmpty()) return 0;
        
        durationStr = durationStr.toLowerCase().trim();
        
        try {
            if (durationStr.endsWith("s")) {
                return Long.parseLong(durationStr.substring(0, durationStr.length() - 1)) * 1000L;
            } else if (durationStr.endsWith("min")) {
                return Long.parseLong(durationStr.substring(0, durationStr.length() - 3)) * 60L * 1000L;
            } else if (durationStr.endsWith("h")) {
                return Long.parseLong(durationStr.substring(0, durationStr.length() - 1)) * 60L * 60L * 1000L;
            } else if (durationStr.endsWith("d")) {
                return Long.parseLong(durationStr.substring(0, durationStr.length() - 1)) * 24L * 60L * 60L * 1000L;
            }
        } catch (NumberFormatException e) {
            return 0;
        }
        
        return 0;
    }
    
    private static boolean isModerator(String playerName) {
        return "Starlev".equalsIgnoreCase(playerName) || "ZurGames".equalsIgnoreCase(playerName) || "MegaChromeX".equalsIgnoreCase(playerName);
    }
    
    private static boolean hasImmunity(String playerName) {
        for (String immune : IMMUNE_MODERATORS) {
            if (immune.equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean isAdmin(String playerName) {
        return ADMIN.equalsIgnoreCase(playerName);
    }
    
    private static String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + " дн.";
        } else if (hours > 0) {
            return hours + " ч.";
        } else if (minutes > 0) {
            return minutes + " мин.";
        } else {
            return seconds + " сек.";
        }
    }
}