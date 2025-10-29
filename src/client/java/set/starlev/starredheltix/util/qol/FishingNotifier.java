package set.starlev.starredheltix.util.qol;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import set.starlev.starredheltix.client.StarredHeltixClient;

public class FishingNotifier {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static boolean notificationActive = false;
    private static long notificationEndTime = 0;
    private static final long NOTIFICATION_DURATION = 2000; // 2 seconds

    public static void register() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> onHudRender(context));
    }

    public static void onFishingBite() {
        if (!StarredHeltixClient.CONFIG.fishingNotification.enabled) {
            return;
        }

        notificationActive = true;
        notificationEndTime = System.currentTimeMillis() + NOTIFICATION_DURATION;
        

        
        // Play sound
        CLIENT.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5F));
    }

    private static void onHudRender(DrawContext drawContext) {
        if (!StarredHeltixClient.CONFIG.fishingNotification.enabled) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        if (notificationActive) {
            if (currentTime >= notificationEndTime) {
                notificationActive = false;
            } else {
                renderNotification(drawContext, "ТЯНИ!");
            }
        }
    }
    
    private static void renderNotification(DrawContext drawContext, String message) {
        if (CLIENT.textRenderer == null) {
            return;
        }
        
        int screenWidth = CLIENT.getWindow().getScaledWidth();
        int screenHeight = CLIENT.getWindow().getScaledHeight();
        
        // Scale up the text
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().scale(3.0f, 3.0f);
        
        int messageWidth = CLIENT.textRenderer.getWidth(message);
        int x = (int)((screenWidth / 3.0f - messageWidth) / 2);
        int y = (int)((screenHeight / 3.0f) / 2 - 10);
        
        drawContext.drawTextWithShadow(CLIENT.textRenderer, message, x, y, 0xFFFF0000);
        
        drawContext.getMatrices().popMatrix();
    }
}