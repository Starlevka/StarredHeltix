package set.starlev.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.secret.config.SecretMenuManager;
import set.starlev.mixin.accessors.GuiFadeAccessor;

@Mixin(Gui.class)
public class TitleAnimationMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (SecretMenuManager.INSTANCE.isConfigInitialized() && 
            SecretMenuManager.INSTANCE.getSecretConfig().getInterfaceCategory().getInstantTitles()) {
            
            GuiFadeAccessor accessor = (GuiFadeAccessor) this;
            accessor.setTitleFadeInTime(0);
            accessor.setTitleFadeOutTime(0);
        }
    }
}
