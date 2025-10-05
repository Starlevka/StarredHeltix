package set.starlev.starredheltix.mixin;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.starredheltix.util.user.ModUserManager;

@Mixin(PlayerListHud.class)
public class PlayerNameIconMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void onGetPlayerName(PlayerListEntry playerEntry, CallbackInfoReturnable<Text> cir) {
        // For the tab list, we don't want to show the star icon
        // So we just return without modifying the player name
        // The original name will be displayed as is
        return;
    }
}