package set.starlev.starredheltix.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.starredheltix.client.StarredHeltixClient;
import set.starlev.starredheltix.util.slotlocking.SlotLockManager;

@Mixin(HandledScreen.class)
public class SlotLockMixin {
    
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HandledScreen<?> handledScreen && client.player != null) {
            Slot slot = SlotLockManager.getHoveredSlot(handledScreen);
            boolean isLockingEnabled = StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled;
            
            // Debug information
            if (StarredHeltixClient.CONFIG.general.debugMode) {
                client.player.sendMessage(Text.of("§e[DEBUG] mouseClicked called. Button: " + button + 
                    ", Slot: " + (slot != null ? slot.id : "null") + 
                    ", Lock Mode: " + SlotLockManager.isLockModeEnabled() + 
                    ", Slot Locking Enabled: " + isLockingEnabled), true);
            }
            
            // If we're in lock mode and clicked on a slot with left mouse button, toggle lock state
            if (SlotLockManager.isLockModeEnabled() && slot != null && button == 0) {
                if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.of("§e[DEBUG] Attempting to toggle lock for slot: " + slot.id), true);
                }
                
                boolean wasLocked = SlotLockManager.isSlotLocked(slot.id);
                SlotLockManager.toggleSlotLock(slot.id);
                boolean isLockedNow = SlotLockManager.isSlotLocked(slot.id);
                
                if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.of("§e[DEBUG] Slot " + slot.id + " changed state from " + 
                        (wasLocked ? "locked" : "unlocked") + " to " + (isLockedNow ? "locked" : "unlocked")), true);
                }
                
                if (client.player != null) {
                    String message = isLockedNow ? 
                        "§aСлот заблокирован" : 
                        "§cСлот разблокирован";
                    client.player.sendMessage(Text.of(message), true);
                    
                    // Play sound effect
                    if (isLockedNow) {
                        // Play experience orb sound for locking
                        client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
                    } else {
                        // Play experience orb sound for unlocking (slightly different pitch)
                        client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.8f);
                    }
                }
                
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
            
            // Prevent interaction with locked slots (but allow interaction when in lock mode for toggling)
            if (slot != null && isLockingEnabled && SlotLockManager.isSlotLocked(slot.id) && !SlotLockManager.isLockModeEnabled()) {
                if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.of("§e[DEBUG] Blocking interaction with locked slot: " + slot.id), true);
                }
                
                cir.setReturnValue(true); // Mark the event as handled
                cir.cancel(); // Cancel the event to block interaction
                
                if (client.player != null) {
                    client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.5f, 0.5f);
                }
                return;
            }
        }
    }

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (slot != null && 
            StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled && 
            SlotLockManager.isSlotLocked(slot.id) && 
            !SlotLockManager.isLockModeEnabled()) {
            // Debug information
            if (StarredHeltixClient.CONFIG.general.debugMode) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.sendMessage(Text.of("§e[DEBUG] Blocking onMouseClick for locked slot: " + slot.id + 
                        ", slotId: " + slotId + ", button: " + button + ", actionType: " + actionType), true);
                }
            }
            
            // Consume the event to prevent interaction with locked slots
            ci.cancel();
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.5f, 0.5f);
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        // Slot lock mode is updated in the SlotLockManager's registered tick event
        // No need to call a separate update method here
    }
    
    @Inject(method = "isPointOverSlot", at = @At("HEAD"), cancellable = true)
    private void onIsPointOverSlot(Slot slot, double pointX, double pointY, CallbackInfoReturnable<Boolean> cir) {
        // Prevent interaction with locked slots (but allow when in lock mode)
        if (slot != null && 
            StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled && 
            SlotLockManager.isSlotLocked(slot.id) && 
            !SlotLockManager.isLockModeEnabled()) {
            // Debug information
            if (StarredHeltixClient.CONFIG.general.debugMode) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.sendMessage(Text.of("§e[DEBUG] isPointOverSlot returning false for locked slot: " + slot.id), true);
                }
            }
            
            cir.setReturnValue(false);
        }
    }
    
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HandledScreen<?> handledScreen && client.player != null) {
            // Debug information
            if (StarredHeltixClient.CONFIG.general.debugMode) {
                client.player.sendMessage(Text.of("§e[DEBUG] keyPressed called. keyCode: " + keyCode + ", scanCode: " + scanCode), true);
            }
            
            // Handle the L key for toggling slot lock mode
            if (keyCode == 76) { // L key code
                if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.of("§e[DEBUG] L key detected in inventory screen"), true);
                }
                
                // Toggle the lock mode directly
                if (StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled) {
                    // Access the private lockModeEnabled field using reflection
                    try {
                        // We need to directly toggle the lockModeEnabled field in SlotLockManager
                        // Since we can't easily access private static fields from a mixin, 
                        // we'll duplicate the logic here:
                        java.lang.reflect.Field lockModeField = SlotLockManager.class.getDeclaredField("lockModeEnabled");
                        lockModeField.setAccessible(true);
                        boolean currentLockMode = lockModeField.getBoolean(null);
                        boolean newLockMode = !currentLockMode;
                        lockModeField.setBoolean(null, newLockMode);
                        
                        if (newLockMode) {
                            client.player.sendMessage(Text.of("§aРежим блокировки слотов включен. Кликните по слоту ЛКМ для блокировки/разблокировки."), true);
                            // Play sound when toggling lock mode on
                            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.5f, 1.5f);
                        } else {
                            client.player.sendMessage(Text.of("§cРежим блокировки слотов выключен."), true);
                            // Play sound when toggling lock mode off
                            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 0.5f, 0.5f);
                        }
                    } catch (Exception e) {
                        // Fallback: Send a message to the player
                        client.player.sendMessage(Text.of("§cОшибка переключения режима блокировки слотов."), true);
                        client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.5f, 0.5f);
                    }
                } else {
                    client.player.sendMessage(Text.of("§cФункция блокировки слотов отключена в настройках."), true);
                    client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.5f, 0.5f);
                }
                
                // Consume the key press to prevent the inventory screen from handling it
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
            
            Slot slot = SlotLockManager.getHoveredSlot(handledScreen);
            
            // Check for key combinations that would drop items:
            // Q key or Ctrl+Q (drop item)
            if (slot != null && 
                StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled && 
                SlotLockManager.isSlotLocked(slot.id) && 
                keyCode == 81) { // Q key code
                // Debug information
                if (StarredHeltixClient.CONFIG.general.debugMode) {
                    client.player.sendMessage(Text.of("§e[DEBUG] Blocking Q key for locked slot: " + slot.id), true);
                }
                
                cir.setReturnValue(true); // Mark the event as handled
                cir.cancel(); // Cancel the event to block interaction
                
                if (client.player != null) {
                    client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.5f, 0.5f);
                }
                return;
            }
        }
    }
}