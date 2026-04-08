package set.starlev.injections.mixin.player.local;

import net.minecraft.world.entity.player.Input;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.chat.mod.MacroCheck;

@Mixin(LocalPlayer.class)
public abstract class MacroCheckMixin {
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onAiStep(CallbackInfo ci) {
        if (MacroCheck.INSTANCE.isBlocked()) {
            LocalPlayer player = (LocalPlayer) (Object) this;
            // Блокируем только ввод движения, не трогая гравитацию
            player.input.keyPresses = new Input(
                false, // forward
                false, // backward
                false, // left
                false, // right
                false, // jump
                false, // shift
                player.input.keyPresses.sprint() // sprint оставляем как есть
            );
        }
    }
}
