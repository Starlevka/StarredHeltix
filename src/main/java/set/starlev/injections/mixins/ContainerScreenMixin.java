package set.starlev.injections.mixins;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerScreen.class)
public class ContainerScreenMixin {
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void hideLeapBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            if (set.starlev.features.combat.dungeons.LeapOverlay.INSTANCE.isLeapScreen((AbstractContainerScreen<?>) (Object) this)) {
                ci.cancel();
            }
        } catch (Exception e) {}
    }
}
