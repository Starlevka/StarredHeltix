package set.starlev.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.events.GuiEvents;
import set.starlev.render.RenderEngine;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        GuiEvents.fireOpen((AbstractContainerScreen<?>) (Object) this);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        RenderEngine.renderHud(graphics, delta);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        GuiEvents.fireClose((AbstractContainerScreen<?>) (Object) this);
    }
}
