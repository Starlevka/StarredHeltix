package set.starlev.starredheltix.util.waypoints;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import set.starlev.starredheltix.client.StarredHeltixClient;
import set.starlev.starredheltix.render.RenderEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WaypointManager {
    private static final List<Waypoint> waypoints = new ArrayList<>();
    private static final Pattern COORDS_PATTERN = Pattern.compile(".*?(\\w+).*?x: (-?\\d+), y: (-?\\d+), z: (-?\\d+)");
    
    public static void register() {
        RenderEvents.register(ctx -> {
            if (!StarredHeltixClient.CONFIG.waypoints.enabled) return;
            
            waypoints.removeIf(Waypoint::isExpired);
            
            for (Waypoint waypoint : waypoints) {
                WaypointRenderer.renderWaypoint(ctx, waypoint);
            }
        });
        
        // Register chat message listener
        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!StarredHeltixClient.CONFIG.waypoints.enabled) return;
            handleChatMessage(message.getString());
        });
    }
    
    public static void handleChatMessage(String message) {
        Matcher matcher = COORDS_PATTERN.matcher(message);
        if (matcher.find()) {
            String playerName = matcher.group(1);
            int x = Integer.parseInt(matcher.group(2));
            int y = Integer.parseInt(matcher.group(3));
            int z = Integer.parseInt(matcher.group(4));
            
            Vec3d position = new Vec3d(x, y, z);
            Waypoint waypoint = new Waypoint(playerName, position, 30000); // 30 seconds
            waypoints.add(waypoint);
        }
    }
    
    public static void sendCoords() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            Vec3d pos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
            String message = String.format("/pc x: %d, y: %d, z: %d", (int)pos.x, (int)pos.y, (int)pos.z);
            client.player.networkHandler.sendChatMessage(message);
        }
    }
}