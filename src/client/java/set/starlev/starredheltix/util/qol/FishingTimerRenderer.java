package set.starlev.starredheltix.util.qol;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.projectile.FishingBobberEntity;
import set.starlev.starredheltix.client.StarredHeltixClient;

import java.util.HashMap;
import java.util.Map;

public class FishingTimerRenderer {
    private static final Map<FishingBobberEntity, Long> fishingTimers = new HashMap<>();
    
    public static void register() {
        // Registration handled by mixin
    }
    
    public static void renderFishingTimers(float tickDelta) {
        if (!StarredHeltixClient.CONFIG.fishingNotification.enabled) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        
        // Clean up removed bobbers
        fishingTimers.entrySet().removeIf(entry -> entry.getKey().isRemoved());
        
        // Find and track fishing bobbers
        client.world.getEntitiesByClass(FishingBobberEntity.class, client.player.getBoundingBox().expand(50), bobber -> true)
            .forEach(bobber -> {
                if (bobber.getPlayerOwner() == client.player) {
                    fishingTimers.putIfAbsent(bobber, System.currentTimeMillis());
                    
                    // Check for bite
                    if (bobber.isSubmergedInWater() && bobber.getVelocity().lengthSquared() > 0.01) {
                        FishingNotifier.onFishingBite();
                    }
                }
            });
    }
    

}