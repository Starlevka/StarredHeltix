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

import com.mojang.blaze3d.resource.ResourceHandle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.util.profiling.ProfilerFiller;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow @Final private net.minecraft.client.renderer.RenderBuffers renderBuffers;

    @Inject(method = "method_62214", at = @At("RETURN"))
    private void onRenderWorld(GpuBufferSlice gpuBufferSlice, LevelRenderState worldRenderState, ProfilerFiller profiler, Matrix4f matrix4f, ResourceHandle handle, ResourceHandle handle2, boolean bl, Frustum frustum, ResourceHandle handle3, ResourceHandle handle4, CallbackInfo ci) {
        PoseStack poseStack = new PoseStack();
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        
        // В 1.21.10 при использовании нового PoseStack() в RETURN инъекции, 
        // он уже синхронизирован с матрицей вида, если мы не применяем лишних вращений.
        // Однако, для корректного World Rendering нам нужно учитывать позицию камеры.

        MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();

        // Создаем RenderContext
        RenderContext context = new RenderContext(
            poseStack, 
            camera, 
            mc.getDeltaTracker().getGameTimeDeltaTicks(), 
            bufferSource,
            worldRenderState.cameraRenderState
        );

        // Вызываем RenderEvents
        RenderEvents.fireWorldRender(context);

        // Также вызываем старый RenderEngine для совместимости
        // ВАЖНО: В 1.21.10 PoseStack в RETURN уже может иметь базовые трансформации.
        // Мы используем абсолютные координаты мира, поэтому транслейтим обратно на позицию камеры.
        poseStack.pushPose();
        poseStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
        set.starlev.render.RenderEngine.renderWorld(poseStack, bufferSource, mc.getDeltaTracker().getGameTimeDeltaTicks());
        poseStack.popPose();
        
        bufferSource.endBatch();
    }
}
