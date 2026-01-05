package set.starlev.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.StarredHeltix;
import set.starlev.features.visual.DisableGlowing;
import set.starlev.features.visual.SwingAnimation;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void onDeath(DamageSource source, CallbackInfo ci) {
        if (StarredHeltix.Companion.getFeature().getOptimization().getVisualOptimizations().getHideDeathAnimation()) {
            this.discard();
        }
        set.starlev.utils.detectors.EntityDeathDetector.INSTANCE.onEntityDeath(this);
    }

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (DisableGlowing.INSTANCE.shouldDisable()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
    private void onGetCurrentSwingDuration(CallbackInfoReturnable<Integer> cir) {
        if (SwingAnimation.INSTANCE.isEnabled()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && (Object)this == mc.player) {
                int speed = SwingAnimation.INSTANCE.getSwingSpeed();
                if (speed > 0) {
                    cir.setReturnValue(speed);
                }
            }
        }
    }
}
