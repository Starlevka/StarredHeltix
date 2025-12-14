package set.starlev.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.chat.mod.MacroCheck;

@Mixin(LocalPlayer.class)
public class MacroCheckMixin {
    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void onAiStep(CallbackInfo ci) {
        if (MacroCheck.INSTANCE.isBlocked()) {
            ci.cancel();
        }
    }
}
