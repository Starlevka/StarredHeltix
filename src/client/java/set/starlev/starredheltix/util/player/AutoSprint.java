package set.starlev.starredheltix.util.player;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import set.starlev.starredheltix.client.StarredHeltixClient;

public class AutoSprint {
    public static void register() {
        // Register tick event for auto-sprint
        ClientTickEvents.START_CLIENT_TICK.register(AutoSprint::onClientTick);
    }
    
    private static void onClientTick(MinecraftClient client) {
        // Check if auto-sprint is enabled in config
        if (!StarredHeltixClient.CONFIG.autoSprint.enabled) {
            return;
        }
        
        // Check if we're in the right environment
        if (client.player == null) {
            return;
        }
        
        ClientPlayerEntity player = client.player;
        
        // Always enable sprinting when moving forward
        if (player.input.hasForwardMovement())
            player.setSprinting(true);
        }
}