package set.starlev.starredheltix.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.starredheltix.util.qol.SlotLockManager;

@Mixin(HandledScreen.class)
public class InventoryRenderMixin {
    @Shadow protected int x;
    @Shadow protected int y;
    
    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void renderSlotLock(DrawContext context, Slot slot, CallbackInfo ci) {
        if (!SlotLockManager.isSlotLockingEnabled()) return;
        
        if (SlotLockManager.isSlotLocked(slot.id)) {
            context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x80FF0000);
        }
    }
    
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("TAIL"))
    private void renderLockMode(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (SlotLockManager.isLockModeEnabled()) {
            context.drawText(MinecraftClient.getInstance().textRenderer, "§eРежим блокировки слотов", 10, 10, 0xFFFFFF, true);
        }
    }
}