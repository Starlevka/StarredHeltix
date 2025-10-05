package set.starlev.starredheltix.util.woodworm;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import set.starlev.starredheltix.client.StarredHeltixClient;

import java.util.HashSet;
import java.util.Set;

public class WoodwormCooldownVisualizer {
    private static final WoodwormCooldownVisualizer INSTANCE = new WoodwormCooldownVisualizer();

    // Wood types that the Woodworm axe can break
    private static final Set<String> WOOD_TYPES = new HashSet<>();

    static {
        // Add all wood-related block names
        WOOD_TYPES.add("oak");
        WOOD_TYPES.add("spruce");
        WOOD_TYPES.add("birch");
        WOOD_TYPES.add("jungle");
        WOOD_TYPES.add("acacia");
        WOOD_TYPES.add("dark_oak");
        WOOD_TYPES.add("mangrove");
    }

    // Cooldown tracking
    private long lastBreakTime = 0;
    private boolean isOnCooldown = false;
    private static final int BASE_COOLDOWN_DURATION = 2000; // 2 seconds in milliseconds

    private WoodwormCooldownVisualizer() {
        // Private constructor for singleton
    }

    public static WoodwormCooldownVisualizer getInstance() {
        return INSTANCE;
    }

    public static void register() {
        // Register tick event for cooldown tracking
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            WoodwormCooldownVisualizer.getInstance().onClientTick(client);
        });

        // Register HUD render callback for visual elements
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            WoodwormCooldownVisualizer.getInstance().onHudRender(context, tickCounter);
        });
    }

    private void onClientTick(MinecraftClient client) {
        if (!StarredHeltixClient.CONFIG.woodwormCooldown.enabled) {
            return;
        }

        // Check if cooldown has expired
        if (isOnCooldown) {
            long currentTime = System.currentTimeMillis();
            long cooldownDuration = calculateCooldownDuration();

            // Check if the cooldown has passed
            if (currentTime - lastBreakTime >= cooldownDuration) {
                isOnCooldown = false;
            }
        }
    }

    private void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (!StarredHeltixClient.CONFIG.woodwormCooldown.enabled || !isOnCooldown) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        // Calculate remaining cooldown time
        long cooldownDuration = calculateCooldownDuration();
        long currentTime = System.currentTimeMillis();
        long timeRemaining = cooldownDuration - (currentTime - lastBreakTime);

        // Show the timer for the full cooldown duration
        if (timeRemaining >= 0) {
            // Calculate seconds with one decimal place
            double secondsRemaining = timeRemaining / 1000.0;
            String displayText = String.format("%.1f", secondsRemaining);

            // Position above the crosshair
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();
            int x = screenWidth / 2;
            int y = screenHeight / 2 - 30; // 30 pixels above crosshair

            // Draw the cooldown text with larger size
            context.getMatrices().push();
            float scale = 2.0f; // Scale text to 200%
            context.getMatrices().scale(scale, scale, scale);

            context.drawText(
                    client.textRenderer,
                    Text.literal(displayText).formatted(Formatting.RED, Formatting.BOLD),
                    (int) (x / scale) - client.textRenderer.getWidth(Text.literal(displayText).formatted(Formatting.RED, Formatting.BOLD)) / 2,
                    (int) (y / scale),
                    0xFFFFFF,
                    true
            );

            context.getMatrices().pop();
        }
    }

    public void onBlockBreak(ClientPlayerEntity player, ItemStack stack, String blockName) {
        if (!StarredHeltixClient.CONFIG.woodwormCooldown.enabled) {
            return;
        }

        // Check if this is a golden axe
        boolean isWood = false;
        if (stack.getItem() == Items.GOLDEN_AXE) {
            // Check if the broken block is wood-related
            String lowerBlockName = blockName.toLowerCase();

            // Debug: Print block name to console
            if (StarredHeltixClient.CONFIG.general.debugMode) {
                System.out.println("[WoodwormCooldown] Block broken: " + blockName);
            }

            // Improved wood detection logic
            isWood = WOOD_TYPES.stream().anyMatch(lowerBlockName::contains) &&
                    (lowerBlockName.contains("log") ||
                            lowerBlockName.contains("wood") ||
                            lowerBlockName.contains("planks") ||
                            lowerBlockName.contains("stairs") ||
                            lowerBlockName.contains("slab") ||
                            lowerBlockName.contains("fence") ||
                            lowerBlockName.contains("door") ||
                            lowerBlockName.contains("trapdoor"));

            // Debug: Print wood detection result
            if (StarredHeltixClient.CONFIG.general.debugMode) {
                System.out.println("[WoodwormCooldown] Is wood: " + isWood);
            }
        }

        if (isWood) {
            // Start cooldown
            lastBreakTime = System.currentTimeMillis();
            isOnCooldown = true;

            // Debug: Print cooldown activation
            if (StarredHeltixClient.CONFIG.general.debugMode) {
                System.out.println("[WoodwormCooldown] Cooldown started");
            }
        }
    }

    public boolean isOnCooldown() {
        return isOnCooldown;
    }

    public long getRemainingCooldown() {
        if (!isOnCooldown) {
            return 0;
        }
        long cooldownDuration = calculateCooldownDuration();
        long currentTime = System.currentTimeMillis();
        return Math.max(0, cooldownDuration - (currentTime - lastBreakTime));
    }

    /**
     * Calculate the actual cooldown duration based on the configured percentage
     * Higher percentage = shorter cooldown
     * 0% = 2 seconds (base)
     * 50% = 1.5 seconds
     * 100% = 1 second (minimum)
     * @return The actual cooldown duration in milliseconds
     */
    private long calculateCooldownDuration() {
        // Get the percentage from config (0-100)
        int percentage = Math.max(0, Math.min(100, StarredHeltixClient.CONFIG.woodwormCooldown.cooldownPercentage));
        
        // Calculate cooldown using linear interpolation between 2000ms (0%) and 1000ms (100%)
        // Formula: cooldown = 2000 - (percentage * 10)
        // 0% = 2000 - (0 * 10) = 2000ms
        // 50% = 2000 - (50 * 10) = 1500ms
        // 100% = 2000 - (100 * 10) = 1000ms
        return 2000 - (percentage * 10);
    }
}