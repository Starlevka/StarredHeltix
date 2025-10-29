package set.starlev.starredheltix.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;

import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.starredheltix.util.qol.SlotLockManager;

@Mixin(HandledScreen.class)
public class SlotLockMixin {
    
    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
    private void onSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (slot == null || !SlotLockManager.isSlotLockingEnabled()) return;
        
        // Если режим блокировки включен и это левый клик
        if (SlotLockManager.isLockModeEnabled() && button == 0 && actionType == SlotActionType.PICKUP) {
            // Переключить блокировку слота
            SlotLockManager.toggleSlotLock(slot.id);
            ci.cancel();
            return;
        }
        
        // Блокировать любые действия с заблокированными слотами
        if (SlotLockManager.isSlotLocked(slot.id)) {
            ci.cancel();
        }
    }
}