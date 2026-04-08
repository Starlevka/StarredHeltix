package set.starlev.injections.mixin.features.farming;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.features.farming.RancherSpeedHud;

@Mixin(ContainerEventHandler.class)
public interface RancherSpeedInputMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void starredheltix$mouseClicked(MouseButtonEvent event, boolean isDoubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (RancherSpeedHud.onMouseClicked(event)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void starredheltix$charTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (RancherSpeedHud.onCharTyped(event)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void starredheltix$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (RancherSpeedHud.onKeyPressed(event)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
