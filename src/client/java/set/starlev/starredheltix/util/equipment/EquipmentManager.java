package set.starlev.starredheltix.util.equipment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Optional;

import set.starlev.starredheltix.client.StarredHeltixClient;

public class EquipmentManager {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File EQUIPMENT_FILE = new File("config/starredheltix/equipment.json");
    
    // Equipment slots order: Necklace, Cloak, Belt, Gloves
    public enum EquipmentSlot {
        NECKLACE(0),
        CLOAK(1),
        BELT(2),
        GLOVES(3);
        
        private final int index;
        
        EquipmentSlot(int index) {
            this.index = index;
        }
        
        public int getIndex() {
            return index;
        }
    }
    
    // Store equipment items with their NBT data
    private static final Map<EquipmentSlot, ItemStack> equipmentItems = new HashMap<>();
    
    // Flag to track if we're currently scanning for equipment
    private static boolean isScanning = false;
    
    // Counter to reduce highlighter frequency
    private static int highlighterTickCounter = 0;
    private static final int HIGHLIGHTER_TICK_DELAY = 200; // 10 times slower (200 ticks = 10 seconds)
    
    // Store slot positions for click detection
    private static int equipmentSlotX = 0;
    private static int[] equipmentSlotY = new int[4]; // For each equipment slot
    
    // Identifier for the equipment slot icon texture
    private static final Identifier SLOT_ICON = Identifier.of("starredheltix", "textures/gui/slot_icon.png");
    
    public static void register() {
        // Load equipment on startup
        loadEquipment();
        
        // Register tick event to check for equipment
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.player.currentScreenHandler != null) {
                // Check if we need to scan for equipment
                if (isScanning) {
                    scanForEquipment();
                    isScanning = false;
                }
            }
            
            // Reduce highlighter frequency
            highlighterTickCounter++;
            if (highlighterTickCounter >= HIGHLIGHTER_TICK_DELAY) {
                highlighterTickCounter = 0;
            }
        });
        
        // Register screen open event to detect when player opens inventory
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof GenericContainerScreen) {
                // Check if this is an equipment interface
                GenericContainerScreen containerScreen = (GenericContainerScreen) screen;
                String title = containerScreen.getTitle().getString();
                
                // Check if the title contains keywords that indicate equipment interface
                if (title.contains("Железной киркой") && title.contains("Часами")) {
                    isScanning = true;
                }
            }
            
            // Register inventory screen rendering to display equipment
            if (screen instanceof InventoryScreen) {
                ScreenEvents.AFTER_INIT.register((client1, screen1, scaledWidth1, scaledHeight1) -> {
                    // Register after render event with proper parameters
                    ScreenEvents.afterRender(screen1).register(((screen2, context, mouseX, mouseY, delta) -> {
                        // Only render if the feature is enabled
                        if (StarredHeltixClient.CONFIG.equipment.enabled && screen2 instanceof InventoryScreen) {
                            renderEquipmentSlots((InventoryScreen) screen2, context);
                        }
                    }));
                });
            }
        });
    }
    
    /**
     * Scan for equipment items in the current screen
     */
    private static void scanForEquipment() {
        if (CLIENT.player == null || !(CLIENT.currentScreen instanceof GenericContainerScreen)) {
            return;
        }
        
        GenericContainerScreen screen = (GenericContainerScreen) CLIENT.currentScreen;
        DefaultedList<Slot> slots = screen.getScreenHandler().slots;
        
        // Debug: Print screen title
        if (StarredHeltixClient.CONFIG.general.debugMode) {
            String title = screen.getTitle().getString();
            CLIENT.player.sendMessage(Text.of("§e[DEBUG] Equipment scanning screen: " + title), false);
        }
        
        // Look for equipment items by checking their names
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (!slot.getStack().isEmpty()) {
                ItemStack stack = slot.getStack();
                String name = stack.getName().getString();
                
                // Check if item name contains equipment keywords (with uppercase as per server)
                if (name.contains("ОЖЕРЕЛЬЕ") || name.contains("NECKLACE")) {
                    equipmentItems.put(EquipmentSlot.NECKLACE, stack.copy());
                    if (StarredHeltixClient.CONFIG.general.debugMode) {
                        CLIENT.player.sendMessage(Text.of("§e[DEBUG] Found necklace: " + stack.getName().getString()), false);
                    }
                } else if (name.contains("ПЛАЩ") || name.contains("CLOAK")) {
                    equipmentItems.put(EquipmentSlot.CLOAK, stack.copy());
                    if (StarredHeltixClient.CONFIG.general.debugMode) {
                        CLIENT.player.sendMessage(Text.of("§e[DEBUG] Found cloak: " + stack.getName().getString()), false);
                    }
                } else if (name.contains("ПОЯС") || name.contains("BELT")) {
                    equipmentItems.put(EquipmentSlot.BELT, stack.copy());
                    if (StarredHeltixClient.CONFIG.general.debugMode) {
                        CLIENT.player.sendMessage(Text.of("§e[DEBUG] Found belt: " + stack.getName().getString()), false);
                    }
                } else if (name.contains("ПЕРЧАТКИ") || name.contains("GLOVES")) {
                    equipmentItems.put(EquipmentSlot.GLOVES, stack.copy());
                    if (StarredHeltixClient.CONFIG.general.debugMode) {
                        CLIENT.player.sendMessage(Text.of("§e[DEBUG] Found gloves: " + stack.getName().getString()), false);
                    }
                }
            }
        }
        
        // Save equipment to file
        saveEquipment();
        
        // Notify player
        if (CLIENT.player != null && !equipmentItems.isEmpty()) {
            CLIENT.player.sendMessage(Text.of("§aEquipment detected and saved!"), false);
        }
    }
    
    /**
     * Render equipment slots in the player inventory
     */
    private static void renderEquipmentSlots(InventoryScreen screen, DrawContext context) {
        // Reduce frequency of rendering
        if (highlighterTickCounter > 0) {
            return;
        }
        
        // Get screen dimensions using reflection like in SlotLockManager
        int x = 0;
        int y = 0;
        try {
            Field xField = HandledScreen.class.getDeclaredField("x");
            Field yField = HandledScreen.class.getDeclaredField("y");
            xField.setAccessible(true);
            yField.setAccessible(true);
            
            x = xField.getInt(screen);
            y = yField.getInt(screen);
        } catch (Exception e) {
            // Fallback to default values
            x = (context.getScaledWindowWidth() - 176) / 2;
            y = (context.getScaledWindowHeight() - 166) / 2;
        }
        
        // Calculate positions for equipment slots (to the left of armor slots)
        // Armor slots are at a fixed position relative to the inventory
        equipmentSlotX = x - 22; // Position to the left of armor slots
        int startY = y + 8; // Start Y position aligned with armor slots
        
        // Render each equipment slot (even if empty)
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            int slotY = startY + (slot.getIndex() * 18); // 18 pixels between slots
            equipmentSlotY[slot.getIndex()] = slotY; // Store Y position for click detection
            renderEquipmentSlot(slot, equipmentSlotX, slotY, context, screen);
        }
    }
    
    /**
     * Render a single equipment slot
     */
    private static void renderEquipmentSlot(EquipmentSlot slotType, int x, int y, 
                                          DrawContext context, 
                                          HandledScreen<?> screen) {
        // Get the item for this equipment slot
        ItemStack stack = equipmentItems.get(slotType);
        if (stack == null || stack.isEmpty()) {
            // Render empty slot with the slot icon texture
            context.drawTexture(RenderLayer::getGuiTextured, SLOT_ICON, x, y, 0, 0, 16, 16, 16, 16);
            return;
        }
        
        // Render the item
        context.drawItem(stack, x, y);
        
        // Render item count if > 1
        if (stack.getCount() > 1) {
            String count = String.valueOf(stack.getCount());
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, 
                    count, 
                    x + 17 - MinecraftClient.getInstance().textRenderer.getWidth(count), 
                    y + 9, 
                    0xFFFFFF);
        }
    }
    
    /**
     * Check if a mouse click is on an equipment slot
     * This method should be called from a mixin or another event handler
     */
    public static boolean handleInventoryClick(double mouseX, double mouseY) {
        // Only handle clicks when we're in an inventory screen
        if (!(CLIENT.currentScreen instanceof InventoryScreen)) {
            return false;
        }
        
        // Check if click is within any equipment slot
        for (int i = 0; i < EquipmentSlot.values().length; i++) {
            int x = equipmentSlotX;
            int y = equipmentSlotY[i];
            
            // Check if mouse is within the slot bounds (16x16)
            if (mouseX >= x && mouseX <= x + 16 && mouseY >= y && mouseY <= y + 16) {
                // Send /equipment command
                ClientPlayerEntity player = CLIENT.player;
                if (player != null) {
                    player.networkHandler.sendChatCommand("equipment");
                }
                return true; // We handled the click
            }
        }
        
        return false; // We didn't handle the click
    }
    
    /**
     * Save equipment to file
     */
    public static void saveEquipment() {
        try {
            // Create directories if they don't exist
            File dir = EQUIPMENT_FILE.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            
            // Convert equipment items to a serializable format
            Map<String, Map<String, Object>> serializedEquipment = new HashMap<>();
            for (Map.Entry<EquipmentSlot, ItemStack> entry : equipmentItems.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    // Store item NBT data for proper serialization
                    Map<String, Object> itemData = new HashMap<>();
                    itemData.put("count", entry.getValue().getCount());
                    
                    if (entry.getValue().hasNbt()) {
                        itemData.put("nbt", entry.getValue().getNbt().toString());
                    }
                    
                    itemData.put("id", net.minecraft.registry.Registries.ITEM.getId(entry.getValue().getItem()).toString());
                    serializedEquipment.put(entry.getKey().name(), itemData);
                }
            }
            
            // Write to file
            FileWriter writer = new FileWriter(EQUIPMENT_FILE);
            GSON.toJson(serializedEquipment, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Load equipment from file
     */
    public static void loadEquipment() {
        try {
            if (!EQUIPMENT_FILE.exists()) {
                return;
            }
            
            FileReader reader = new FileReader(EQUIPMENT_FILE);
            Type type = new TypeToken<Map<String, Map<String, Object>>>(){}.getType();
            Map<String, Map<String, Object>> serializedEquipment = GSON.fromJson(reader, type);
            reader.close();
            
            if (serializedEquipment != null) {
                for (Map.Entry<String, Map<String, Object>> entry : serializedEquipment.entrySet()) {
                    EquipmentSlot slot = EquipmentSlot.valueOf(entry.getKey());
                    Map<String, Object> itemData = entry.getValue();
                    
                    int count = ((Double) itemData.get("count")).intValue();
                    String id = (String) itemData.get("id");
                    String nbtString = (String) itemData.get("nbt");
                    
                    // Create ItemStack from data
                    net.minecraft.util.Identifier itemId = net.minecraft.util.Identifier.tryParse(id);
                    if (itemId != null) {
                        net.minecraft.item.Item item = net.minecraft.registry.Registries.ITEM.get(itemId);
                        if (item != null) {
                            ItemStack stack = new ItemStack(item, count);
                            if (nbtString != null) {
                                try {
                                    NbtCompound nbt = StringNbtReader.parse(nbtString);
                                    stack.setNbt(nbt);
                                } catch (Exception e) {
                                    // Failed to parse NBT, continue with basic item
                                }
                            }
                            equipmentItems.put(slot, stack);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Get equipment item for a specific slot
     */
    public static ItemStack getEquipment(EquipmentSlot slot) {
        return equipmentItems.get(slot);
    }
    
    /**
     * Set equipment item for a specific slot
     */
    public static void setEquipment(EquipmentSlot slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            equipmentItems.remove(slot);
        } else {
            equipmentItems.put(slot, stack.copy());
        }
        saveEquipment();
    }
    
    /**
     * Clear all equipment
     */
    public static void clearEquipment() {
        equipmentItems.clear();
        saveEquipment();
    }
}