package set.starlev.starredheltix.util.slotlocking;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import set.starlev.starredheltix.client.StarredHeltixClient;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

public class SlotLockManager {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static Set<Integer> lockedSlots = new HashSet<>();
    private static KeyBinding lockKey;
    private static boolean lockModeEnabled = false;
    private static Field hoveredSlotField;
    private static String foundFieldName = null;
    
    // Identifier for the lock icon texture
    private static final Identifier LOCK_ICON = Identifier.of("starredheltix", "textures/gui/lock_icon.png");
    
    static {
        // Try to find the hoveredSlot field with multiple approaches
        findHoveredSlotField();
        
        // Debug information about field detection
        if (StarredHeltixClient.CONFIG.general.debugMode) {
            if (MinecraftClient.getInstance().player != null) {
                if (hoveredSlotField != null) {
                    MinecraftClient.getInstance().player.sendMessage(
                        Text.of("§e[DEBUG] Successfully found hoveredSlot field: " + foundFieldName), true);
                } else {
                    MinecraftClient.getInstance().player.sendMessage(
                        Text.of("§e[DEBUG] Failed to find hoveredSlot field"), true);
                    
                    // List all fields in HandledScreen for debugging
                    printAllFields();
                }
            }
        }
    }
    
    private static void findHoveredSlotField() {
        // Method 1: Try common field names
        String[] possibleNames = {
            "hoveredSlot", "field_2998", "field_21687", 
            "field_2997", "field_3001", "field_21688",
            "field_22782", "field_22783", "field_23169",
            "field_23890", "field_24232", "field_24676"
        };
        
        for (String name : possibleNames) {
            try {
                hoveredSlotField = HandledScreen.class.getDeclaredField(name);
                hoveredSlotField.setAccessible(true);
                foundFieldName = name;
                return;
            } catch (Exception e) {
                // Continue trying other names
            }
        }
        
        // Method 2: Try to find the field by type
        try {
            Field[] fields = HandledScreen.class.getDeclaredFields();
            for (Field field : fields) {
                if (field.getType() == Slot.class) {
                    field.setAccessible(true);
                    hoveredSlotField = field;
                    foundFieldName = field.getName();
                    return;
                }
            }
        } catch (Exception e) {
            // Silently fail
        }
        
        // If all methods fail, set to null
        hoveredSlotField = null;
        foundFieldName = null;
    }

    public static void register() {
        // Load locked slots from config
        lockedSlots = StarredHeltixClient.CONFIG.slotLocking.lockedSlots;
        
        // Register keybinding for L key
        lockKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.starredheltix.lock_slot",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                "category.starredheltix.main"
        ));
        
        // Register tick event to handle key presses
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Handle lock mode toggle
            if (lockKey.wasPressed()) {
                if (StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled) {
                    // Toggle lock mode on/off
                    lockModeEnabled = !lockModeEnabled;
                    
                    if (client.player != null) {
                        if (lockModeEnabled) {
                            client.player.sendMessage(Text.of("§aРежим блокировки слотов включен. Кликните по слоту ЛКМ для блокировки/разблокировки."), true);
                            // Play sound when toggling lock mode on
                            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.5f, 1.5f);
                        } else {
                            client.player.sendMessage(Text.of("§cРежим блокировки слотов выключен."), true);
                            // Play sound when toggling lock mode off
                            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 0.5f, 0.5f);
                        }
                    }
                } else if (client.player != null) {
                    client.player.sendMessage(Text.of("§cФункция блокировки слотов отключена в настройках."), true);
                    // Play error sound when feature is disabled
                    client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.5f, 0.5f);
                }
            }
        });
        
        // Register HUD render callback for lock mode indicator
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            if (lockModeEnabled && StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled) {
                // The HUD rendering is handled by the InventoryRenderMixin
            }
        });
    }
    
    public static void toggleSlotLock(int slotIndex) {
        if (lockedSlots.contains(slotIndex)) {
            lockedSlots.remove(slotIndex);
            if (StarredHeltixClient.CONFIG.general.debugMode && MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(Text.of("§e[DEBUG] Unlocked slot: " + slotIndex), true);
            }
        } else {
            lockedSlots.add(slotIndex);
            if (StarredHeltixClient.CONFIG.general.debugMode && MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(Text.of("§e[DEBUG] Locked slot: " + slotIndex), true);
            }
        }
        saveLockedSlots();
    }
    
    public static boolean isSlotLocked(int slotIndex) {
        boolean locked = lockedSlots.contains(slotIndex);
        if (StarredHeltixClient.CONFIG.general.debugMode && locked && MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(Text.of("§e[DEBUG] Slot " + slotIndex + " is locked"), true);
        }
        return locked;
    }
    
    public static boolean isLockModeEnabled() {
        boolean enabled = lockModeEnabled && StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled;
        if (StarredHeltixClient.CONFIG.general.debugMode && MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(Text.of("§e[DEBUG] Lock mode enabled: " + enabled), true);
        }
        return enabled;
    }
    
    public static Set<Integer> getLockedSlots() {
        return new HashSet<>(lockedSlots); // Return a copy to prevent external modification
    }
    
    public static void clearAllLockedSlots() {
        lockedSlots.clear();
        saveLockedSlots();
        if (StarredHeltixClient.CONFIG.general.debugMode && MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(Text.of("§e[DEBUG] Cleared all locked slots"), true);
        }
    }
    
    public static void disableLockMode() {
        lockModeEnabled = false;
        if (StarredHeltixClient.CONFIG.general.debugMode && MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(Text.of("§e[DEBUG] Disabled lock mode"), true);
        }
    }
    
    private static void saveLockedSlots() {
        StarredHeltixClient.CONFIG.slotLocking.lockedSlots = lockedSlots;
        StarredHeltixClient.CONFIG.save();
    }
    
    private static void onHudRender(DrawContext context) {
        // Only render in inventory screens
        if (CLIENT.currentScreen == null || !isLockModeEnabled()) {
            return;
        }
        
        // We'll implement the rendering in a mixin for inventory screens
    }
    
    // Method to get hovered slot using reflection, safely
    public static Slot getHoveredSlot(HandledScreen<?> handledScreen) {
        try {
            // First try with reflection
            if (hoveredSlotField != null) {
                Slot slot = (Slot) hoveredSlotField.get(handledScreen);
                if (slot != null) {
                    if (StarredHeltixClient.CONFIG.general.debugMode && MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.sendMessage(
                            Text.of("§e[DEBUG] Hovered slot ID (reflection): " + slot.id), true);
                    }
                    return slot;
                }
            }
            
            // Fallback method - manually find the hovered slot
            Slot hoveredSlot = findHoveredSlotManually(handledScreen);
            if (hoveredSlot != null) {
                if (StarredHeltixClient.CONFIG.general.debugMode && MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.sendMessage(
                        Text.of("§e[DEBUG] Hovered slot ID (manual): " + hoveredSlot.id), true);
                }
                return hoveredSlot;
            }
            
            if (StarredHeltixClient.CONFIG.general.debugMode && MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                    Text.of("§e[DEBUG] No hovered slot found"), true);
            }
        } catch (Exception e) {
            if (StarredHeltixClient.CONFIG.general.debugMode && MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                    Text.of("§e[DEBUG] Error getting hovered slot: " + e.getMessage()), true);
            }
        }
        return null;
    }
    
    // Manual method to find the hovered slot by checking mouse position
    private static Slot findHoveredSlotManually(HandledScreen<?> screen) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null || client.mouse == null) {
                return null;
            }
            
            // Get mouse position
            double mouseX = client.mouse.getX() * (double)client.getWindow().getScaledWidth() / (double)client.getWindow().getWidth();
            double mouseY = client.mouse.getY() * (double)client.getWindow().getScaledHeight() / (double)client.getWindow().getHeight();
            
            // Get slots using reflection
            java.util.List<Slot> slots = getScreenSlots(screen);
            if (slots != null) {
                for (Slot slot : slots) {
                    if (isPointOverSlot(screen, slot, mouseX, mouseY)) {
                        return slot;
                    }
                }
            }
        } catch (Exception e) {
            if (StarredHeltixClient.CONFIG.general.debugMode && MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                    Text.of("§e[DEBUG] Error in findHoveredSlotManually: " + e.getMessage()), true);
            }
        }
        return null;
    }
    
    // Helper method to check if point is over slot
    private static boolean isPointOverSlot(HandledScreen<?> screen, Slot slot, double pointX, double pointY) {
        try {
            // Use reflection to call the private isPointOverSlot method
            java.lang.reflect.Method method = HandledScreen.class.getDeclaredMethod("isPointOverSlot", Slot.class, double.class, double.class);
            method.setAccessible(true);
            return (Boolean) method.invoke(screen, slot, pointX, pointY);
        } catch (Exception e) {
            // Fallback to manual calculation
            // Get the screen position using reflection
            try {
                Field xField = HandledScreen.class.getDeclaredField("x");
                Field yField = HandledScreen.class.getDeclaredField("y");
                xField.setAccessible(true);
                yField.setAccessible(true);
                
                int left = xField.getInt(screen);
                int top = yField.getInt(screen);
                
                pointX -= left;
                pointY -= top;
                return pointX >= slot.x - 1 && pointX <= slot.x + 16 + 1 && pointY >= slot.y - 1 && pointY <= slot.y + 16 + 1;
            } catch (Exception ex) {
                // Last resort - assume screen is at 0,0
                return pointX >= slot.x - 1 && pointX <= slot.x + 16 + 1 && pointY >= slot.y - 1 && pointY <= slot.y + 16 + 1;
            }
        }
    }
    
    // Helper method to get slots from the screen
    private static java.util.List<Slot> getScreenSlots(HandledScreen<?> screen) {
        try {
            // Try to get slots using reflection
            java.lang.reflect.Field slotsField = HandledScreen.class.getDeclaredField("slots");
            slotsField.setAccessible(true);
            return (java.util.List<Slot>) slotsField.get(screen);
        } catch (Exception e) {
            if (StarredHeltixClient.CONFIG.general.debugMode) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.sendMessage(Text.of("§e[DEBUG] Error getting slots field: " + e.getMessage()), true);
                }
            }
            
            // Fallback - try to get slots using the handler
            try {
                java.lang.reflect.Field handlerField = HandledScreen.class.getDeclaredField("handler");
                handlerField.setAccessible(true);
                Object handler = handlerField.get(screen);
                
                java.lang.reflect.Field slotsField = handler.getClass().getDeclaredField("slots");
                slotsField.setAccessible(true);
                return (java.util.List<Slot>) slotsField.get(handler);
            } catch (Exception e2) {
                if (StarredHeltixClient.CONFIG.general.debugMode) {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        client.player.sendMessage(Text.of("§e[DEBUG] Error getting handler slots: " + e2.getMessage()), true);
                    }
                }
            }
        }
        return null;
    }
    
    // Method to check if the currently selected hotbar slot is locked
    public static boolean isHotbarSlotLocked() {
        if (CLIENT.player == null || !StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled) {
            return false;
        }
        
        int selectedHotbarSlot = CLIENT.player.getInventory().selectedSlot;
        // Hotbar slots are numbered 36-44 in the player inventory container
        int hotbarSlotIndex = 36 + selectedHotbarSlot;
        return isSlotLocked(hotbarSlotIndex);
    }
    
    // Method to enable/disable slot locking feature
    public static void setSlotLockingEnabled(boolean enabled) {
        StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled = enabled;
        StarredHeltixClient.CONFIG.save();
    }
    
    // Debug method to print all fields in HandledScreen
    public static void printAllFields() {
        if (!StarredHeltixClient.CONFIG.general.debugMode || MinecraftClient.getInstance().player == null) {
            return;
        }
        
        try {
            Field[] fields = HandledScreen.class.getDeclaredFields();
            MinecraftClient.getInstance().player.sendMessage(
                Text.of("§e[DEBUG] All fields in HandledScreen:"), true);
            
            for (Field field : fields) {
                // Only show fields that might be related to slots
                if (field.getType().getSimpleName().contains("Slot") || 
                    field.getName().toLowerCase().contains("slot") ||
                    field.getName().toLowerCase().contains("hover")) {
                    MinecraftClient.getInstance().player.sendMessage(
                        Text.of("§e[DEBUG] Field: " + field.getName() + " Type: " + field.getType().getSimpleName()), true);
                }
            }
        } catch (Exception e) {
            MinecraftClient.getInstance().player.sendMessage(
                Text.of("§e[DEBUG] Error getting fields: " + e.getMessage()), true);
        }
    }
    
    // Method to check if the L key is pressed (for use in mixins)
    public static boolean isLockKeyPressed() {
        return lockKey.isPressed();
    }
}