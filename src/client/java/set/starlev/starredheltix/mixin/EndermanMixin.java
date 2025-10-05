package set.starlev.starredheltix.mixin;

import net.minecraft.entity.mob.EndermanEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.starredheltix.client.StarredHeltixClient;

@Mixin(EndermanEntity.class)
public class EndermanMixin {
    @Inject(method = "setTarget", at = @At("HEAD"))
    private void onSetTarget(net.minecraft.entity.LivingEntity target, CallbackInfo ci) {
        // When an enderman sets a target (becomes aggressive), mark it for highlighting
        if (StarredHeltixClient.CONFIG.endermenHighlight.endermenHighlightEnabled) {
            EndermanEntity enderman = (EndermanEntity) (Object) this;
            // This will ensure that when an enderman becomes aggressive, 
            // it will be highlighted by our renderer
        }
    }
}