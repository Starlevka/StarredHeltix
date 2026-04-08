package set.starlev.injections.mixin.gui.container;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.events.GuiEvents;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!((Object) this).getClass().getName().startsWith("set.starlev.")) {
            GuiEvents.fireForeground(graphics, mouseX, mouseY, (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) (Object) this);
        }
    }
}