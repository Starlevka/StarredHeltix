package set.starlev.injections.mixin.gui.container;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.skyblock.SlotOverlayUtil;

/**
 * Рисует подсветку фона слотов в контейнерах (инвентарь, сундуки и т.д.).
 * Портировано из HELTIX HandledScreenMixin.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {

    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void onRenderSlot(GuiGraphics context, Slot slot, CallbackInfo ci) {
        ItemStack stack = slot.getItem();
        if (stack == null || stack.isEmpty()) return;

        SlotOverlayUtil.INSTANCE.drawOverlay(context, slot.x, slot.y, stack, false);
    }
}
