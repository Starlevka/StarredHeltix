package set.starlev.starredheltix.util.qol;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import set.starlev.starredheltix.client.StarredHeltixClient;

import java.util.HashSet;
import java.util.Set;

public class TreeCapCooldownVisualizer {
    private static final TreeCapCooldownVisualizer INSTANCE = new TreeCapCooldownVisualizer();

    // Wood types that the TreeCap axe can break
    private static final Set<String> WOOD_TYPES = new HashSet<>();

    static {
        // Add all wood-related block names
        WOOD_TYPES.add("oak_log");
        WOOD_TYPES.add("spruce_log");
        WOOD_TYPES.add("birch_log");
        WOOD_TYPES.add("jungle_log");
        WOOD_TYPES.add("acacia_log");
        WOOD_TYPES.add("dark_oak_log");
    }

    // Cooldown tracking
    private long lastBreakTime = 0;
    private boolean isOnCooldown = false;
    private static final double BASE_COOLDOWN_DURATION = 2.0; // 2 seconds in seconds

    private TreeCapCooldownVisualizer() {
        // Private constructor for singleton
    }

    public static TreeCapCooldownVisualizer getInstance() {
        return INSTANCE;
    }

    public static void register() {
        // Register tick event for cooldown tracking
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            TreeCapCooldownVisualizer.getInstance().onClientTick(client);
        });

        // Register HUD render callback for visual elements
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            TreeCapCooldownVisualizer.getInstance().onHudRender(context, tickCounter);
        });
    }

    private void onClientTick(MinecraftClient client) {
        if (!StarredHeltixClient.CONFIG.treecapCooldown.enabled) {
            return;
        }

        // Check if cooldown has expired
        if (isOnCooldown) {
            long currentTime = System.currentTimeMillis();
            long cooldownDuration = Math.round(calculateCooldownDuration() * 1000); // Convert to milliseconds

            // Check if the cooldown has passed
            if (currentTime - lastBreakTime >= cooldownDuration) {
                isOnCooldown = false;
            }
        }
    }

    private void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (!StarredHeltixClient.CONFIG.treecapCooldown.enabled || !isOnCooldown) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.textRenderer == null) {
            return;
        }

        // Calculate remaining cooldown time
        long currentTime = System.currentTimeMillis();
        long cooldownDurationMs = Math.round(calculateCooldownDuration() * 1000);
        long timeRemaining = cooldownDurationMs - (currentTime - lastBreakTime);

        if (timeRemaining >= 0) {
            double secondsRemaining = timeRemaining / 1000.0;
            String displayText = String.format("%.1f", secondsRemaining);
            
            renderCooldownText(context, displayText);
        }
    }
    
    private void renderCooldownText(DrawContext context, String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.textRenderer == null) {
            return;
        }
        
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Scale up the text more and make bold
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(3.0f, 3.0f);
        
        // Position above crosshair (adjusted for scale)
        int messageWidth = client.textRenderer.getWidth("§l" + message);
        int x = (int)((screenWidth / 3.0f - messageWidth) / 2);
        int y = (int)((screenHeight / 3.0f) / 2 - 20);
        
        // Draw text with red color, bold and shadow
        context.drawTextWithShadow(client.textRenderer, "§l" + message, x, y, 0xFFFF0000);
        
        context.getMatrices().popMatrix();
    }



    public void onBlockBreak(ClientPlayerEntity player, ItemStack stack, String blockName) {
        if (!StarredHeltixClient.CONFIG.treecapCooldown.enabled) {
            return;
        }

        // Check if this is a golden axe and block is wood
        if (stack.getItem() == Items.GOLDEN_AXE && isWoodBlock(blockName)) {
            // Start cooldown
            lastBreakTime = System.currentTimeMillis();
            isOnCooldown = true;

            if (StarredHeltixClient.CONFIG.general.debugMode) {
                System.out.println("[TreeCapCooldown] Cooldown started for block: " + blockName);
            }
        }
    }
    
    private boolean isWoodBlock(String blockName) {
        String lowerBlockName = blockName.toLowerCase();
        return lowerBlockName.contains("log") || lowerBlockName.contains("wood");
    }

    public boolean  isOnCooldown() {
        return isOnCooldown;
    }

    public long getRemainingCooldown() {
        if (!isOnCooldown) {
            return 0;
        }
        long cooldownDuration = Math.round(calculateCooldownDuration() * 1000); // Convert to milliseconds
        long currentTime = System.currentTimeMillis();
        return Math.max(0, cooldownDuration - (currentTime - lastBreakTime));
    }

    /**
     * Calculate the actual cooldown duration based on the configured percentage
     * Higher percentage = shorter cooldown
     * 0% = 2 seconds (base)
     * 50% = 1.5 seconds
     * 100% = 1 second (minimum)
     * Formula: cooldown = 2.0 - (percentage / 100.0) * 1.0
     * @return The actual cooldown duration in seconds
     */
    private double calculateCooldownDuration() {
        // Get the percentage from config (0-100)
        int percentage = Math.max(0, Math.min(100, StarredHeltixClient.CONFIG.treecapCooldown.cooldownPercentage));
        
        // Calculate cooldown using linear interpolation between 2.0s (0%) and 1.0s (100%)
        // Formula: cooldown = 2.0 - (percentage / 100.0) * 1.0
        // 0% = 2.0 - (0 / 100.0) * 1.0 = 2.0s
        // 50% = 2.0 - (50 / 100.0) * 1.0 = 1.5s
        // 100% = 2.0 - (100 / 100.0) * 1.0 = 1.0s
        return 2.0 - (percentage / 100.0) * 1.0;
    }
}
