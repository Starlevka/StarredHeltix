package set.starlev.starredheltix.util.qol;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import set.starlev.starredheltix.client.StarredHeltixClient;

import java.util.regex.Pattern;

public class AbilityCooldownVisualizer {
    private static final AbilityCooldownVisualizer INSTANCE = new AbilityCooldownVisualizer();

    // Patterns for ability detection
    private static final Pattern KIRKOBULUS_PATTERN = Pattern.compile(".*Вы использовали Киркобулус!.*");
    private static final Pattern MINING_SPEED_BOOST_PATTERN = Pattern.compile(".*Вы использовали Увеличение скорости копания!.*");

    // Cooldown tracking
    private long kirkobulusStartTime = 0;
    private long miningSpeedBoostStartTime = 0;
    private boolean kirkobulusActive = false;
    private boolean miningSpeedBoostActive = false;

    private AbilityCooldownVisualizer() {
        // Private constructor for singleton
    }

    public static AbilityCooldownVisualizer getInstance() {
        return INSTANCE;
    }

    public static void register() {
        // Register chat message event for ability detection
        ClientReceiveMessageEvents.GAME.register(INSTANCE::onChatMessage);

        // Register tick event for cooldown tracking
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            INSTANCE.onClientTick(client);
        });

        // Register HUD render callback for visual elements
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            INSTANCE.onHudRender(context, tickCounter);
        });
    }

    private void onChatMessage(Text message, boolean overlay) {
        if (!StarredHeltixClient.CONFIG.abilityCooldown.enabled) {
            return;
        }

        String messageText = message.getString();

        // Check for Kirkobulus ability
        if (StarredHeltixClient.CONFIG.abilityCooldown.kirkobulusEnabled &&
            KIRKOBULUS_PATTERN.matcher(messageText).find()) {

            if (!kirkobulusActive) {
                kirkobulusStartTime = System.currentTimeMillis();
                kirkobulusActive = true;

                if (StarredHeltixClient.CONFIG.general.debugMode) {
                    System.out.println("[AbilityCooldown] Kirkobulus cooldown started");
                }
            }
        }

        // Check for Mining Speed Boost ability
        if (StarredHeltixClient.CONFIG.abilityCooldown.miningSpeedBoostEnabled &&
            MINING_SPEED_BOOST_PATTERN.matcher(messageText).find()) {

            if (!miningSpeedBoostActive) {
                miningSpeedBoostStartTime = System.currentTimeMillis();
                miningSpeedBoostActive = true;

                if (StarredHeltixClient.CONFIG.general.debugMode) {
                    System.out.println("[AbilityCooldown] Mining Speed Boost cooldown started");
                }
            }
        }


    }

    private void onClientTick(MinecraftClient client) {
        if (!StarredHeltixClient.CONFIG.abilityCooldown.enabled) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        // Check Kirkobulus cooldown
        if (kirkobulusActive) {
            long cooldownDurationMs = (long) (StarredHeltixClient.CONFIG.abilityCooldown.kirkobulusCooldown * 1000);
            if (currentTime - kirkobulusStartTime >= cooldownDurationMs) {
                kirkobulusActive = false;
            }
        }

        // Check Mining Speed Boost cooldown
        if (miningSpeedBoostActive) {
            long cooldownDurationMs = (long) (StarredHeltixClient.CONFIG.abilityCooldown.miningSpeedBoostCooldown * 1000);
            if (currentTime - miningSpeedBoostStartTime >= cooldownDurationMs) {
                miningSpeedBoostActive = false;
            }
        }


    }

    private void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (!StarredHeltixClient.CONFIG.abilityCooldown.enabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.textRenderer == null) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        long currentTime = System.currentTimeMillis();

        // Render Kirkobulus cooldown on the left side
        if (kirkobulusActive && StarredHeltixClient.CONFIG.abilityCooldown.kirkobulusEnabled) {
            long cooldownDurationMs = (long) (StarredHeltixClient.CONFIG.abilityCooldown.kirkobulusCooldown * 1000);
            long timeRemaining = cooldownDurationMs - (currentTime - kirkobulusStartTime);

            if (timeRemaining >= 0) {
                double secondsRemaining = timeRemaining / 1000.0;
                String displayText = String.format("%.1f", secondsRemaining);

                int x = screenWidth / 2 - 100;
                int y = screenHeight / 2;
                
                // Scale up and make bold
                context.getMatrices().pushMatrix();
                context.getMatrices().scale(2.0f, 2.0f);
                
                context.drawTextWithShadow(client.textRenderer, "§l" + displayText, (int)(x / 2.0f), (int)(y / 2.0f), 0xFFFF0000);
                
                context.getMatrices().popMatrix();
            }
        }

        // Render Mining Speed Boost cooldown on the right side
        if (miningSpeedBoostActive && StarredHeltixClient.CONFIG.abilityCooldown.miningSpeedBoostEnabled) {
            long cooldownDurationMs = (long) (StarredHeltixClient.CONFIG.abilityCooldown.miningSpeedBoostCooldown * 1000);
            long timeRemaining = cooldownDurationMs - (currentTime - miningSpeedBoostStartTime);

            if (timeRemaining >= 0) {
                double secondsRemaining = timeRemaining / 1000.0;
                String displayText = String.format("%.1f", secondsRemaining);

                int x = screenWidth / 2 + 100;
                int y = screenHeight / 2;
                
                // Scale up and make bold
                context.getMatrices().pushMatrix();
                context.getMatrices().scale(2.0f, 2.0f);
                
                context.drawTextWithShadow(client.textRenderer, "§l" + displayText, (int)(x / 2.0f), (int)(y / 2.0f), 0xFF00FF00);
                
                context.getMatrices().popMatrix();
            }
        }
    }



    // Public methods for external access
    public boolean isKirkobulusActive() {
        return kirkobulusActive;
    }

    public boolean isMiningSpeedBoostActive() {
        return miningSpeedBoostActive;
    }

    public double getKirkobulusRemaining() {
        if (!kirkobulusActive) return 0.0;

        long currentTime = System.currentTimeMillis();
        long cooldownDurationMs = (long) (StarredHeltixClient.CONFIG.abilityCooldown.kirkobulusCooldown * 1000);
        long timeRemaining = cooldownDurationMs - (currentTime - kirkobulusStartTime);

        return Math.max(0.0, timeRemaining / 1000.0);
    }

    public double getMiningSpeedBoostRemaining() {
        if (!miningSpeedBoostActive) return 0.0;

        long currentTime = System.currentTimeMillis();
        long cooldownDurationMs = (long) (StarredHeltixClient.CONFIG.abilityCooldown.miningSpeedBoostCooldown * 1000);
        long timeRemaining = cooldownDurationMs - (currentTime - miningSpeedBoostStartTime);

        return Math.max(0.0, timeRemaining / 1000.0);
    }


}