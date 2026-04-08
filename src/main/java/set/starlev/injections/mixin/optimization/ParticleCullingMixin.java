package set.starlev.injections.mixin.optimization;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import set.starlev.utils.IWorldRenderer;
import set.starlev.StarredHeltix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.QuadParticleRenderState;

@Mixin(SingleQuadParticle.class)
public abstract class ParticleCullingMixin {

    // Кэшируем ссылку на IWorldRenderer — lookup только при смене уровня
    @org.spongepowered.asm.mixin.Unique
    private static IWorldRenderer starredheltix$cachedWorldRenderer;

    @org.spongepowered.asm.mixin.Unique
    private static LevelRenderer starredheltix$lastLevelRenderer;

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void onExtract(QuadParticleRenderState quadParticleRenderState, Camera camera, float f, CallbackInfo ci) {
        // Проверка конфига
        try {
            if (!StarredHeltix.Companion.getFeature().getOptimization().getRenderOptimizations().getParticleCulling()) return;
        } catch (Throwable ignored) { return; }

        // Быстрая проверка кэша без instanceof каждый вызов
        LevelRenderer currentRenderer = Minecraft.getInstance().levelRenderer;
        if (currentRenderer != starredheltix$lastLevelRenderer) {
            starredheltix$lastLevelRenderer = currentRenderer;
            starredheltix$cachedWorldRenderer = (currentRenderer instanceof IWorldRenderer) ? (IWorldRenderer) currentRenderer : null;
        }

        if (starredheltix$cachedWorldRenderer == null) return;

        Frustum frustum = starredheltix$cachedWorldRenderer.starredheltix$getFrustum();
        if (frustum == null) return;

        Particle particle = (Particle) (Object) this;
        if (!frustum.isVisible(particle.getBoundingBox())) {
            ci.cancel();
        }
    }
}
