package set.starlev.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.render.RenderEngine;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!((Object) this instanceof AbstractContainerScreen)) {
            RenderEngine.renderHud(graphics, delta);
        }
    }
}
