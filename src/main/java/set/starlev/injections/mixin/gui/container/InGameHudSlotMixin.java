package set.starlev.injections.mixin.gui.container;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.skyblock.SlotOverlayUtil;

/**
 * Рисует подсветку фона предметов в хотбаре.
 * Инжектимся в HEAD renderSlot у Gui (не AbstractContainerScreen),
 * чтобы перехватить отрисовку каждого предмета хотбара до его рендера.
 */
@Mixin(Gui.class)
public abstract class InGameHudSlotMixin {

    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void onRenderSlot(GuiGraphics context, int x, int y, DeltaTracker delta, Player player, ItemStack stack, int seed, CallbackInfo ci) {
        if (stack == null || stack.isEmpty()) return;

        SlotOverlayUtil.INSTANCE.drawOverlay(context, x, y, stack, true);
    }
}
