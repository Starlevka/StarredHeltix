package set.starlev.injections.mixins;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.config.ConfigManager;
import set.starlev.features.skyblock.NpcDialogueOverlay;

@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof PauseScreen) {
            // Проверяем: активен ли оверлей диалога с NPC и включена ли опция «Закрывать по ESC»
            if (ConfigManager.INSTANCE.getFeatures().getSkyblock().getNpcDialogue().getCloseOnEsc()) {
                if (NpcDialogueOverlay.INSTANCE.isActive()) {
                    NpcDialogueOverlay.INSTANCE.close();
                    ci.cancel(); // Не даём открыться экрану паузы
                }
            }
        }

        // ===== JS-события screenOpen / screenClose =====
        if (set.starlev.scripting.SHScriptEngine.INSTANCE.isLoaded()) {
            Screen current = ((Gui) (Object) this).screen();
            if (current != null && current != screen) {
                set.starlev.scripting.SHScriptEngine.INSTANCE.fireTrigger("screenClose", current.getClass().getSimpleName());
            }
            if (screen != null && screen != current) {
                set.starlev.scripting.SHScriptEngine.INSTANCE.fireTrigger("screenOpen", screen.getClass().getSimpleName());
            }
        }
    }
}
