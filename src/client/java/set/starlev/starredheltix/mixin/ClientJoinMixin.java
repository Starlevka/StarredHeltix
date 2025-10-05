package set.starlev.starredheltix.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.starredheltix.util.updater.ModUpdater;

@Mixin(MinecraftClient.class)
public class ClientJoinMixin {
    @Inject(method = "setScreen", at = @At("TAIL"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        // Check for updates when entering the title screen (every game start)
        if (screen instanceof TitleScreen) {
            ModUpdater.checkForUpdates();
        }
    }
}