package set.starlev.starredheltix.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.starredheltix.client.StarredHeltixClient;
import set.starlev.starredheltix.util.slotlocking.SlotLockManager;

@Mixin(ClientPlayerEntity.class)
public class PlayerDropItemMixin {
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void onDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        if (StarredHeltixClient.CONFIG.slotLocking.slotLockingEnabled && SlotLockManager.isHotbarSlotLocked()) {
            // Cancel the drop action if the selected hotbar slot is locked
            cir.cancel();
        }
    }
}