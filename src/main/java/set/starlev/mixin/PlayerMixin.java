package set.starlev.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.secret.config.SecretMenuManager;
import set.starlev.secret.features.SecretFunFeatures;

@Mixin(Player.class)
public abstract class PlayerMixin {
    private final java.util.Map<Player, Component> starred_cache = new java.util.WeakHashMap<>();

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void onGetDisplayName(CallbackInfoReturnable<Component> cir) {
        if (!SecretMenuManager.INSTANCE.isConfigInitialized()) {
            return;
        }

        Component original = cir.getReturnValue();
        if (original == null) return;

        Player player = (Player) (Object) this;
        Component cached = starred_cache.get(player);
        if (cached != null && cached.getString().equals(original.getString())) {
            cir.setReturnValue(cached);
            return;
        }

        // В отображаемых именах игроков эффекты применяются всегда (force=true), если включены в конфиге
        Component modified = SecretFunFeatures.processComponent(original, true);
        if (modified != original) {
            starred_cache.put(player, modified);
            cir.setReturnValue(modified);
        }
    }
}
