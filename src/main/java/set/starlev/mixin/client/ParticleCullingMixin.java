package set.starlev.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import set.starlev.utils.IWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.phys.AABB;
import set.starlev.StarredHeltix;

import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.QuadParticleRenderState;

@Mixin(SingleQuadParticle.class)
public abstract class ParticleCullingMixin {

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void onExtract(QuadParticleRenderState quadParticleRenderState, Camera camera, float f, CallbackInfo ci) {
        if (StarredHeltix.getFeature().getOptimization().getEntityOptimization().getOptimizeParticles()) {
            Particle particle = (Particle) (Object) this;
            ParticleAccessor accessor = (ParticleAccessor) particle;
            
            // 1. Frustum Culling
            Frustum frustum = ((IWorldRenderer) Minecraft.getInstance().levelRenderer).starredheltix$getFrustum();
            if (frustum != null) {
                if (!frustum.isVisible(particle.getBoundingBox())) {
                    ci.cancel();
                    return;
                }
            }

            // 2. Distance Culling
            double maxDistance = StarredHeltix.getFeature().getOptimization().getEntityOptimization().getParticleDistance();
            
            double dx = accessor.starredheltix$getX() - camera.getPosition().x;
            double dy = accessor.starredheltix$getY() - camera.getPosition().y;
            double dz = accessor.starredheltix$getZ() - camera.getPosition().z;
            
            double distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq > maxDistance * maxDistance) {
                ci.cancel();
            }
        }
    }
}
