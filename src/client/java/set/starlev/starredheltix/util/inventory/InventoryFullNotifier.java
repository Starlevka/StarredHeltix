package set.starlev.starredheltix.util.inventory;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import set.starlev.starredheltix.client.StarredHeltixClient;

public class InventoryFullNotifier {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static boolean wasInventoryFull = false;
    private static long notificationStartTime = 0;
    private static final long NOTIFICATION_DURATION = 3000; // 3 seconds
    private static long lastSoundTime = 0;
    private static final long SOUND_INTERVAL = 500; // Play sound every 500ms

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Check inventory full feature
            checkInventoryFull(client);
        });
    }
    
    private static void checkInventoryFull(MinecraftClient client) {
        // Check if the feature is enabled
        if (!StarredHeltixClient.CONFIG.general.inventoryFullWarningEnabled) {
            // If the notification is currently showing, hide it
            if (wasInventoryFull) {
                hideNotification();
                wasInventoryFull = false;
            }
            return;
        }
        
        if (client.player == null) return;
        
        boolean isInventoryFull = isInventoryFull(client.player);
        
        // If inventory just became full
        if (isInventoryFull && !wasInventoryFull) {
            showNotification();
            playSound();
            notificationStartTime = System.currentTimeMillis();
            lastSoundTime = notificationStartTime;
        }
        
        // Play sound periodically while notification is showing
        if (wasInventoryFull && isInventoryFull && 
            System.currentTimeMillis() - lastSoundTime >= SOUND_INTERVAL &&
            System.currentTimeMillis() - notificationStartTime < NOTIFICATION_DURATION) {
            playSound();
            lastSoundTime = System.currentTimeMillis();
        }
        
        // If we're showing notification and time has elapsed, hide it
        if (wasInventoryFull && (System.currentTimeMillis() - notificationStartTime >= NOTIFICATION_DURATION || !isInventoryFull)) {
            hideNotification();
            wasInventoryFull = false; // Make sure we reset the state
        }
        
        // Update state
        if (!wasInventoryFull && isInventoryFull) {
            wasInventoryFull = true;
        }
    }
    
    private static boolean isInventoryFull(ClientPlayerEntity player) {
        // Check if player inventory is full (only the main 36 slots, not armor or offhand)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                return false; // Found an empty slot
            }
        }
        return true; // All slots are occupied
    }
    
    private static void showNotification() {
        if (CLIENT.inGameHud != null) {
            CLIENT.inGameHud.setTitleTicks(0, 60, 0); // 3 seconds at 20 TPS
            CLIENT.inGameHud.setTitle(Text.of("§cИнвентарь заполнен!"));
        }
    }
    
    private static void hideNotification() {
        if (CLIENT.inGameHud != null) {
            CLIENT.inGameHud.setTitle(Text.of(""));
        }
    }
    
    private static void playSound() {
        if (CLIENT.player != null) {
            CLIENT.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.0f);
        }
    }
}