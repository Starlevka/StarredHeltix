package set.starlev.starredheltix.util.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.WolfEntity;
import set.starlev.starredheltix.render.RenderEvents;

public class WolfHighlighter {
    public static void register() {
        RenderEvents.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null) return;
            
            for (Entity entity : client.world.getEntities()) {
                if (entity instanceof WolfEntity) {
                    double distance = entity.squaredDistanceTo(client.player);
                    if (distance > 96 * 96) continue;
                    
                    context.renderHitbox(entity.getBoundingBox(), 0.0f, 1.0f, 0.0f, 0.3f);
                }
            }
        });
    }
}