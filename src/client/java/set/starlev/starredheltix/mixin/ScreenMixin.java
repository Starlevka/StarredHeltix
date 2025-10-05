package set.starlev.starredheltix.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
    /**
     * Skip rendering the dark background for our custom screens
     */
    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), cancellable = true)
    private void onRenderBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Background rendering is now handled normally for all screens
        // Removed special handling for CustomConfigScreen since it's been deleted
    }
}