package set.starlev.injections.mixin.optimization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.utils.IWorldRenderer;
import set.starlev.StarredHeltix;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityCullingMixin {
    @org.spongepowered.asm.mixin.Shadow
    public abstract <E extends net.minecraft.world.level.block.entity.BlockEntity, S extends BlockEntityRenderState> net.minecraft.client.renderer.blockentity.BlockEntityRenderer<E, S> getRenderer(S state);

    // Кэшируем ссылку на IWorldRenderer — обновляется только при смене уровня
    @org.spongepowered.asm.mixin.Unique
    private static IWorldRenderer starredheltix$cachedWorldRenderer;

    @org.spongepowered.asm.mixin.Unique
    private static LevelRenderer starredheltix$lastLevelRenderer;

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void onRender(BlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci) {
        // Проверка конфига
        try {
            if (!StarredHeltix.Companion.getFeature().getOptimization().getRenderOptimizations().getBlockEntityCulling()) return;
        } catch (Throwable ignored) { return; }

        // Пропускаем "глобальные" блочные сущности, которые должны быть видны издалека (например, маяки)
        net.minecraft.client.renderer.blockentity.BlockEntityRenderer<?, ?> renderer = getRenderer(state);
        if (renderer != null && renderer.shouldRenderOffScreen()) {
            return;
        }

        // Быстрая проверка кэша без instanceof каждый вызов
        LevelRenderer currentRenderer = Minecraft.getInstance().levelRenderer;
        if (currentRenderer != starredheltix$lastLevelRenderer) {
            starredheltix$lastLevelRenderer = currentRenderer;
            starredheltix$cachedWorldRenderer = (currentRenderer instanceof IWorldRenderer) ? (IWorldRenderer) currentRenderer : null;
        }

        if (starredheltix$cachedWorldRenderer == null) return;

        Frustum frustum = starredheltix$cachedWorldRenderer.starredheltix$getFrustum();
        if (frustum == null || state.blockPos == null) return;

        // AABB — immutable (поля final), создаём новый, но lookup frustum кэширован
        AABB box = new AABB(state.blockPos);
        if (!frustum.isVisible(box)) {
            ci.cancel();
        }
    }
}
