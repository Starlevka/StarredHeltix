package set.starlev.mixin.client;

import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.StarredHeltix;

import net.minecraft.world.entity.LivingEntity;

@Mixin(LivingEntity.class)
public abstract class ArmorStandOptimizationMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        if ((Object)this instanceof ArmorStand armorStand) {
            if (StarredHeltix.getFeature().getOptimization().getEntityOptimization().getOptimizeHolograms()) {
                if (armorStand.isMarker()) {
                    ci.cancel();
                }
            }
        }
    }
}
