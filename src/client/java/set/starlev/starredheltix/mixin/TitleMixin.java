package set.starlev.starredheltix.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.starredheltix.util.qol.TitleBlocker;

@Mixin(InGameHud.class)
public class TitleMixin {
    @Inject(method = "setTitle", at = @At("HEAD"), cancellable = true)
    private void onSetTitle(Text title, CallbackInfo ci) {
        if (TitleBlocker.shouldBlockTitle()) {
            ci.cancel();
        }
    }
    
    @Inject(method = "setSubtitle", at = @At("HEAD"), cancellable = true)
    private void onSetSubtitle(Text subtitle, CallbackInfo ci) {
        if (TitleBlocker.shouldBlockTitle()) {
            ci.cancel();
        }
    }
}