package set.starlev.starredheltix.mixin;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.starredheltix.util.equipment.EquipmentManager;

@Mixin(HandledScreen.class)
public class InventoryClickMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // Check if this is an inventory screen
        if ((Object) this instanceof InventoryScreen) {
            // Only handle left mouse button clicks
            if (button == 0) {
                // Let the EquipmentManager handle the click
                if (EquipmentManager.handleInventoryClick(mouseX, mouseY)) {
                    // If EquipmentManager handled the click, cancel the original method
                    cir.cancel();
                }
            }
        }
    }
}