package set.starlev.starredheltix.util.solver.fairysouls;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TemporaryWaypointManager {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static final List<TemporaryWaypoint> waypoints = new ArrayList<>();
    
    static {
        // Register the renderer
        WorldRenderEvents.END.register(TemporaryWaypointManager::renderWaypoints);
    }
    
    public static void addWaypoint(int x, int y, int z, String playerName) {
        waypoints.add(new TemporaryWaypoint(new BlockPos(x, y, z), playerName, System.currentTimeMillis() + 30000)); // 30 seconds
    }
    
    public static String getFormattedTime() {
        long currentTime = System.currentTimeMillis();
        Iterator<TemporaryWaypoint> iterator = waypoints.iterator();
        while (iterator.hasNext()) {
            TemporaryWaypoint waypoint = iterator.next();
            if (waypoint.expirationTime > currentTime) {
                long secondsLeft = (waypoint.expirationTime - currentTime) / 1000;
                return String.valueOf(secondsLeft);
            }
        }
        return "0";
    }
    
    private static void renderWaypoints(WorldRenderContext context) {
        if (CLIENT.player == null || CLIENT.world == null) {
            return;
        }
        
        Camera camera = context.camera();
        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = camera.getPos();
        
        // Remove expired waypoints
        Iterator<TemporaryWaypoint> iterator = waypoints.iterator();
        while (iterator.hasNext()) {
            TemporaryWaypoint waypoint = iterator.next();
            if (waypoint.expirationTime <= System.currentTimeMillis()) {
                iterator.remove();
            } else {
                // Render waypoint
                double x = waypoint.pos.getX() - cameraPos.x;
                double y = waypoint.pos.getY() - cameraPos.y;
                double z = waypoint.pos.getZ() - cameraPos.z;
                renderWaypoint(context, matrices, x, y, z, waypoint);
            }
        }
    }
    
    private static void renderWaypoint(WorldRenderContext context, MatrixStack matrices, double x, double y, double z, TemporaryWaypoint waypoint) {
        matrices.push();
        matrices.translate(x + 0.5, y + 0.5, z + 0.5);

        // Draw player name text
        matrices.scale(0.025f, -0.025f, 0.025f); // Scale down the text and flip it upright
        matrices.multiply(context.camera().getRotation()); // Make text always face the camera (2D)
        
        // Calculate distance to player
        if (CLIENT.player != null) {
            BlockPos playerPos = CLIENT.player.getBlockPos();
            double dx = waypoint.pos.getX() - playerPos.getX();
            double dy = waypoint.pos.getY() - playerPos.getY();
            double dz = waypoint.pos.getZ() - playerPos.getZ();
            double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
            
            String text = waypoint.playerName + " (" + waypoint.pos.getX() + ", " + waypoint.pos.getY() + ", " + waypoint.pos.getZ() + ") [" + String.format("%.1f", distance) + " blocks]";
            int textColor = 0xFFFFFF; // White color
            CLIENT.textRenderer.draw(text, -CLIENT.textRenderer.getWidth(text) / 2, 0, textColor, false, 
                    matrices.peek().getPositionMatrix(), context.consumers(), net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
        }

        matrices.pop();
    }
    
    private static class TemporaryWaypoint {
        final BlockPos pos;
        final String playerName;
        final long expirationTime;
        
        TemporaryWaypoint(BlockPos pos, String playerName, long expirationTime) {
            this.pos = pos;
            this.playerName = playerName;
            this.expirationTime = expirationTime;
        }
    }
}