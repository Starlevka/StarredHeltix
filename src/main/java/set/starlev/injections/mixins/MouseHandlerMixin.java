package set.starlev.injections.mixins;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.mining.AutoCommissions;
import set.starlev.features.farming.MouseLock;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {


    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onButton(long window, MouseButtonInfo mouseButtonInfo, int action, CallbackInfo ci) {
        // ПКМ нажат (button 1, action 1 = pressed)
        if (mouseButtonInfo.button() == 1 && action == 1) {
            if (AutoCommissions.INSTANCE.isWaitingForClick()) {
                AutoCommissions.INSTANCE.onRightClickWhileWaiting();
                ci.cancel();
            }
        }
        // Клик колёсиком (button 2) для ClickHighlight снимается здесь и поллится
        // через GLFW в ClickHighlight.init() — надёжнее, чем полагаться на этот
        // колбэк (Ixeris/Zoomify подменяют/dispatch'ят GLFW mouse-события).
    }



    @Inject(
        method = "onMove",
        at = @At("HEAD"),
        cancellable = true
    )
    private void blockMouseMovement(long window, double x, double y, CallbackInfo ci) {
        if (MouseLock.INSTANCE.active()) {
            // Отменяем событие движения мыши полностью
            ci.cancel();
        }
    }

}