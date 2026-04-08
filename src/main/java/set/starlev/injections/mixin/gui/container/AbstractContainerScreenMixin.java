package set.starlev.injections.mixin.gui.container;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.events.GuiEvents;
import set.starlev.render.RenderEngine;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    private boolean starredheltix$isStarlevScreen() {
        return ((Object) this).getClass().getName().startsWith("set.starlev.");
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        GuiEvents.fireOpen((AbstractContainerScreen<?>) (Object) this);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!starredheltix$isStarlevScreen()) {
            GuiEvents.fireRender(graphics);
            RenderEngine.renderHud(graphics, delta);
        }
        if (starredheltix$isStarlevScreen()) {
            RenderEngine.renderHud(graphics, delta);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!starredheltix$isStarlevScreen()) {
            GuiEvents.fireForeground(graphics, mouseX, mouseY, (AbstractContainerScreen<?>) (Object) this);
        }
        if (starredheltix$isStarlevScreen()) {
            GuiEvents.fireRender(graphics);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!starredheltix$isStarlevScreen()) {
            if (GuiEvents.fireClick(event.x(), event.y(), event.button(), (AbstractContainerScreen<?>) (Object) this)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        GuiEvents.fireClose((AbstractContainerScreen<?>) (Object) this);
    }
}
