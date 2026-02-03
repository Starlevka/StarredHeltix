package set.starlev.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.config.ConfigManager;
import set.starlev.features.overlays.NpcDialogueOverlay;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof PauseScreen) {
            // Check if NPC Dialogue Overlay is active and Close on ESC is enabled
            if (ConfigManager.INSTANCE.getFeatures().getSkyblock().getNpcDialogue().getCloseOnEsc()) {
                if (NpcDialogueOverlay.INSTANCE.isActive()) {
                    NpcDialogueOverlay.INSTANCE.close();
                    ci.cancel(); // Prevent PauseScreen from opening
                }
            }
        }
    }
}
