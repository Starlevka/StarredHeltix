package set.starlev.injections.mixin.optimization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.StarredHeltix;
import set.starlev.utils.IWorldRenderer;

@Mixin(EntityRenderDispatcher.class)
public class EntityCullingMixin {

    @org.spongepowered.asm.mixin.Unique
    private static IWorldRenderer starredheltix$cachedWorldRenderer;

    @org.spongepowered.asm.mixin.Unique
    private static LevelRenderer starredheltix$lastLevelRenderer;

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void onShouldRender(E entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!StarredHeltix.Companion.getFeature().getOptimization().getRenderOptimizations().getEntityCulling()) return;
        } catch (Throwable ignored) { return; }

        // Пропускаем игрока — он всегда должен рендериться от третьего лица
        if (entity == Minecraft.getInstance().player) return;

        // Быстрая проверка кэша без instanceof каждый вызов
        LevelRenderer currentRenderer = Minecraft.getInstance().levelRenderer;
        if (currentRenderer != starredheltix$lastLevelRenderer) {
            starredheltix$lastLevelRenderer = currentRenderer;
            starredheltix$cachedWorldRenderer = (currentRenderer instanceof IWorldRenderer) ? (IWorldRenderer) currentRenderer : null;
        }

        if (starredheltix$cachedWorldRenderer == null) return;

        Frustum cachedFrustum = starredheltix$cachedWorldRenderer.starredheltix$getFrustum();
        if (cachedFrustum == null) return;

        // Проверяем AABB сущности через frustum
        try {
            AABB box = entity.getBoundingBox();
            if (!cachedFrustum.isVisible(box)) {
                cir.setReturnValue(false);
            }
        } catch (Exception ignored) {}
    }
}