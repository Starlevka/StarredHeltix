package set.starlev.injections.mixins;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.scripting.CameraController;
import set.starlev.scripting.SHScriptEngine;

/**
 * Пробрасывает нажатия клавиш в JS-модули.
 * Пока камера отцеплена (JS API `camera`), подавляет ввод движения игрока
 * (WASD/прыжок/крадясь/спринт), чтобы персонаж не уходил из-под камеры.
 * Опрос клавиш для управления камерой идёт через keybind.isDown (GLFW),
 * поэтому на управление камерой не влияет.
 * @author Starlev
 */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void onKeyPress(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (!SHScriptEngine.INSTANCE.isLoaded()) return;
        if (event == null) return;
        SHScriptEngine.INSTANCE.fireTrigger("key", event.key(), event.scancode(), action, event.modifiers());

        // Отцепленная камера: не отдаём клавиши движения игре (только без открытого экрана)
        if (CameraController.INSTANCE.getDetached()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.gui.screen() == null && isMovementKey(mc, event)) {
                ci.cancel();
            }
        }
    }

    private boolean isMovementKey(Minecraft mc, KeyEvent event) {
        var opts = mc.options;
        return opts.keyUp.matches(event)
                || opts.keyDown.matches(event)
                || opts.keyLeft.matches(event)
                || opts.keyRight.matches(event)
                || opts.keyJump.matches(event)
                || opts.keyShift.matches(event)
                || opts.keySprint.matches(event);
    }
}
