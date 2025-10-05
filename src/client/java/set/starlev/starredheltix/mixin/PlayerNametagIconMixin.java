package set.starlev.starredheltix.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.starredheltix.util.user.ModUserManager;

@Mixin(PlayerEntity.class)
public class PlayerNametagIconMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void onGetDisplayName(CallbackInfoReturnable<Text> cir) {
        // Get the original player name
        Text originalName = cir.getReturnValue();
        
        // Get the entity instance
        Entity entity = (Entity) (Object) this;
        
        // Check if this entity is a player
        if (entity instanceof AbstractClientPlayerEntity) {
            AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) entity;
            
            // In a client-only mod, we show stars for all players
            // Use ModUserManager to get the appropriate icon
            Text icon = ModUserManager.getInstance().getPlayerIcon(player.getUuid());
            if (!icon.getString().isEmpty()) {
                // Check if the name already has our icon to prevent duplication
                String nameString = originalName.getString();
                if (!nameString.contains("⭐")) {
                    // Create a new text with the mod icon prefix
                    Text modifiedName = Text.literal("").append(icon).append(" ").append(originalName);
                    
                    // Set the modified name as the return value
                    cir.setReturnValue(modifiedName);
                }
            }
        }
    }
}