package set.starlev.injections.mixin.input;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.mining.AutoCommissions;

/**
    * Перехватывает ПКМ для авто-поручений:
    * блокирует реальный клик и переключает слот на Королевского голубя.
    */
@Mixin(MouseHandler.class)
public class MouseButtonMixin {

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onButton(long window, MouseButtonInfo mouseButtonInfo, int action, CallbackInfo ci) {
        // ПКМ нажат (button 1, action 1 = pressed)
        if (mouseButtonInfo.button() == 1 && action == 1) {
            if (AutoCommissions.INSTANCE.isWaitingForClick()) {
                AutoCommissions.INSTANCE.onRightClickWhileWaiting();
                ci.cancel();
            }
        }
    }
}