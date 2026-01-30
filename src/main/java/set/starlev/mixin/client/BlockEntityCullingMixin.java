package set.starlev.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.utils.IWorldRenderer;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityCullingMixin {
    @org.spongepowered.asm.mixin.Shadow
    public abstract <E extends net.minecraft.world.level.block.entity.BlockEntity, S extends BlockEntityRenderState> net.minecraft.client.renderer.blockentity.BlockEntityRenderer<E, S> getRenderer(S state);

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void onRender(BlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci) {
        // Пропускаем "глобальные" блочные сущности, которые должны быть видны издалека (например, маяки)
        net.minecraft.client.renderer.blockentity.BlockEntityRenderer<?, ?> renderer = getRenderer(state);
        if (renderer != null && renderer.shouldRenderOffScreen()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer instanceof IWorldRenderer worldRenderer) {
            Frustum frustum = worldRenderer.starredheltix$getFrustum();
            if (frustum != null && state.blockPos != null) {
                net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(state.blockPos);
                if (!frustum.isVisible(box)) {
                    ci.cancel();
                }
            }
        }
    }
}
