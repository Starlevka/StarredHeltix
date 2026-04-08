package set.starlev.injections.mixin.input;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.features.misc.MouseLock;

/**
 * Блокирует движение мыши путем отмены события onMove.
 * Идеально! Как в SkyHanni, но намного проще.
 */
@Mixin(MouseHandler.class)
public class MouseInputMixin {

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
