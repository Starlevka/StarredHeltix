package set.starlev.injections.mixins;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.utils.InputMath;

/**
 * Ввод чисел с суффиксами (1k / 2m / 3b / 10к) — порт из Skysoft
 * (`AbstractSignEditScreenMixin`). Считает сам {@link InputMath}, здесь только
 * подмена строки перед отправкой пакета с табличкой.
 *
 * 26.2: {@code ServerboundSignUpdatePacket} уходит из {@code removed()}, поэтому
 * считаем и там (ESC/потеря фокуса), и на Enter в {@code keyPressed}.
 */
@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin {

    @Shadow @Final private String[] messages;

    @Shadow public abstract void onClose();

    /** Enter в поле ввода: считаем выражение и закрываем экран (поведение Skysoft). */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void starlevSubmitInputMath(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!event.isConfirmation() || !starlevCompileInputMath()) return;
        onClose();
        cir.setReturnValue(true);
    }

    /** Подстраховка: экран закрыли без Enter — тоже успеваем посчитать. */
    @Inject(method = "removed", at = @At("HEAD"))
    private void starlevCompileInputMathBeforeSend(CallbackInfo ci) {
        starlevCompileInputMath();
    }

    @Unique
    private boolean starlevCompileInputMath() {
        String result = InputMath.INSTANCE.compile(this.messages);
        if (result == null) return false;
        this.messages[0] = result;
        return true;
    }
}
