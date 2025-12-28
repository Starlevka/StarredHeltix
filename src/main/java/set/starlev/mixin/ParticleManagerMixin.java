package set.starlev.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.StarredHeltix;

import java.util.Random;

@Mixin(ParticleEngine.class)
public class ParticleManagerMixin {
    private static final Random starlev$random = new Random();

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void onCreateParticle(ParticleOptions options, double x, double y, double z, double dx, double dy, double dz, CallbackInfoReturnable<Particle> cir) {
        float reduction = StarredHeltix.Companion.getFeature().getOptimization().getVisualOptimizations().getParticleReduction();
        
        if (reduction <= 0) return;

        // Если ползунок на 100%, шанс удаления должен быть 0.5 (уменьшение в 2 раза)
        // Формула: (reduction / 100.0) * 0.5
        double skipChance = (reduction / 100.0) * 0.5;

        if (starlev$random.nextDouble() < skipChance) {
            cir.setReturnValue(null);
        }
    }
}
