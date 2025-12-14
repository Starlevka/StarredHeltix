package set.starlev.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.render.RenderEngine;

@Mixin(Gui.class)
public class HudRenderMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderHud(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        RenderEngine.renderHud(graphics, delta.getGameTimeDeltaPartialTick(false));
    }
}
