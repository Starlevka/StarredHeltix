package set.starlev.starredheltix.util.player;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import set.starlev.starredheltix.client.StarredHeltixClient;

public class AutoSprint {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    public static void register() {
        // Register tick event for auto-sprint
        ClientTickEvents.START_CLIENT_TICK.register(AutoSprint::onClientTick);
    }
    
    private static void onClientTick(MinecraftClient client) {
        // Check if the feature is enabled
        if (!StarredHeltixClient.CONFIG.general.enabled || !StarredHeltixClient.CONFIG.autoSprint.enabled) {
            return;
        }
        
        // Check if we're in the right environment
        if (client.player == null) {
            return;
        }
        
        // Check if we should only sprint in SkyBlock
        if (StarredHeltixClient.CONFIG.autoSprint.onlyInSkyBlock) {
            // Only check world if we need to restrict to SkyBlock
            if (client.world == null || !isInSkyBlock(client.world)) {
                return;
            }
        }
        
        ClientPlayerEntity player = client.player;
        
        // Check if player can sprint
        if (!player.isSneaking() && 
            !player.isSpectator() && 
            !player.isRiding() && 
            player.getHungerManager().getFoodLevel() > 6) {
            
            // Check if player is moving forward
            if (player.input.movementForward > 0) {
                player.setSprinting(true);
            }
        }
    }
    
    private static boolean isInSkyBlock(ClientWorld world) {
        // Check if we're in SkyBlock by looking at the world name
        String worldName = world.getRegistryKey().getValue().getPath();
        return worldName.contains("skyblock") || worldName.contains("hub") || worldName.contains("private") || worldName.contains("farm") || worldName.contains("mining") || worldName.contains("combat");
    }
}