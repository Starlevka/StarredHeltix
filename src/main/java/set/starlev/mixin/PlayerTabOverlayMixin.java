package set.starlev.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.secret.features.SecretFunFeatures;

import set.starlev.secret.config.SecretMenuManager;

import java.util.WeakHashMap;
import java.util.Map;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    private final Map<PlayerInfo, Component> starred_cache = new WeakHashMap<>();

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void onGetNameForDisplay(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
        if (!SecretMenuManager.INSTANCE.isConfigInitialized()) return;
        
        boolean starlevEnabled = SecretMenuManager.INSTANCE.getSecretConfig().getFunCategory().getStarlevNameEffect();
        boolean megaChromeEnabled = SecretMenuManager.INSTANCE.getSecretConfig().getFunCategory().getMegaChromeXEffect();
        
        if (!starlevEnabled && !megaChromeEnabled) {
            return;
        }

        Component original = cir.getReturnValue();
        if (original == null) return;

        // Используем кэш специфичный для PlayerInfo, чтобы избежать дёрганья
        Component cached = starred_cache.get(playerInfo);
        if (cached != null && cached.getString().equals(original.getString())) {
            cir.setReturnValue(cached);
            return;
        }

        Component modified = SecretFunFeatures.processComponent(original, true);
        if (modified != original) {
            starred_cache.put(playerInfo, modified);
            cir.setReturnValue(modified);
        }
    }
}
