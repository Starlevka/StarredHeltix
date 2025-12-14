package set.starlev.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.render.RenderContext;
import set.starlev.render.RenderEvents;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow @Final private net.minecraft.client.renderer.RenderBuffers renderBuffers;

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void onRenderWorld(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, Matrix4f matrix4f, Matrix4f matrix4f2, Matrix4f matrix4f3, GpuBufferSlice gpuBufferSlice, Vector4f vector4f, boolean bl, CallbackInfo ci) {
        PoseStack poseStack = new PoseStack();
        
        // Apply camera rotation
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));

        MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();

        // Создаем RenderContext
        RenderContext context = new RenderContext(poseStack, camera, deltaTracker.getGameTimeDeltaTicks(), bufferSource);

        // Вызываем RenderEvents
        RenderEvents.fireWorldRender(context);

        // Также вызываем старый RenderEngine для совместимости
        poseStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
        set.starlev.render.RenderEngine.renderWorld(poseStack, bufferSource, deltaTracker.getGameTimeDeltaTicks());
        bufferSource.endBatch();
    }
}
