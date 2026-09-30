package set.starlev.injections.mixins;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.StarredHeltix;

@Mixin(EffectsInInventory.class)
public class EffectsInInventoryMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V", at = @At("HEAD"), cancellable = true)
    private void onRenderEffects(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (StarredHeltix.Companion.getFeature().getOptimization().getVisualOptimizations().getHideStatusEffects()) {
            ci.cancel();
        }
    }
}
