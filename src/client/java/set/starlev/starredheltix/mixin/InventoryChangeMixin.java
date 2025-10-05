package set.starlev.starredheltix.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.starredheltix.client.StarredHeltixClient;
import set.starlev.starredheltix.util.inventory.InventoryFullNotifier;

@Mixin(MinecraftClient.class)
public class InventoryChangeMixin {
    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void onHandleInputEvents(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.currentScreenHandler != null) {
            if (StarredHeltixClient.CONFIG.general.inventoryFullWarningEnabled) {
                // The inventory full notification is handled by the registered tick event in InventoryFullNotifier
                // No need to call a method here as it's automatically handled
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onScreenTick(CallbackInfo ci) {
        // Trigger inventory full check when screen ticks (e.g. inventory screen)
        if (StarredHeltixClient.CONFIG.general.inventoryFullWarningEnabled) {
            // Re-register to trigger check
            // Note: This is not the correct approach. The inventory full check is handled by the registered tick event.
            // This method should be removed or refactored to not re-register the event.
        }
    }
}