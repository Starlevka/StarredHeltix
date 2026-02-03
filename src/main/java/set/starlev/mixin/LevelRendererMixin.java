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
public class LevelRendererMixin implements set.starlev.utils.IWorldRenderer {
    @Shadow @Final private net.minecraft.client.renderer.RenderBuffers renderBuffers;

    @org.spongepowered.asm.mixin.Unique
    public net.minecraft.client.renderer.culling.Frustum starredheltix$capturedFrustum;

    @Override
    public net.minecraft.client.renderer.culling.Frustum starredheltix$getFrustum() {
        return starredheltix$capturedFrustum;
    }

    @Inject(method = "method_62214", at = @At("HEAD"))
    private void onRenderWorldHead(GpuBufferSlice gpuBufferSlice, LevelRenderState worldRenderState, net.minecraft.util.profiling.ProfilerFiller profiler, Matrix4f matrix4f, ResourceHandle handle, ResourceHandle handle2, boolean bl, net.minecraft.client.renderer.culling.Frustum frustum, ResourceHandle handle3, ResourceHandle handle4, CallbackInfo ci) {
        this.starredheltix$capturedFrustum = frustum;
    }

    @Inject(method = "method_62214", at = @At("RETURN"))
    private void onRenderWorld(GpuBufferSlice gpuBufferSlice, LevelRenderState worldRenderState, net.minecraft.util.profiling.ProfilerFiller profiler, Matrix4f matrix4f, ResourceHandle handle, ResourceHandle handle2, boolean bl, net.minecraft.client.renderer.culling.Frustum frustum, ResourceHandle handle3, ResourceHandle handle4, CallbackInfo ci) {
        // Попытка подавления отрисовки через флаги в worldRenderState, если это доступно в 1.21.10
        if (set.starlev.StarredHeltix.Companion.getFeature().getOptimization().getVisualOptimizations().getDisableBlockBreakingParticles()) {
        }

        // В 1.21.10 мы можем попробовать использовать matrix4f для инициализации PoseStack
        PoseStack poseStack = new PoseStack();
        // poseStack.mulPose(matrix4f); // Это может быть опасно если matrix4f уже включает проекцию
        
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        
        MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();

        // Создаем RenderContext
        RenderContext context = new RenderContext(
            poseStack, 
            camera, 
            mc.getDeltaTracker().getGameTimeDeltaTicks(), 
            bufferSource,
            worldRenderState.cameraRenderState
        );

        RenderEvents.fireWorldRender(context);

        // Рисуем старый RenderEngine
        poseStack.pushPose();
        // Мы НЕ делаем транслейт на камеру здесь, так как RenderContext.renderText сам это делает
        set.starlev.render.RenderEngine.renderWorld(poseStack, bufferSource, mc.getDeltaTracker().getGameTimeDeltaTicks());
        poseStack.popPose();
        
        // Форсируем отрисовку всех буферов
        bufferSource.endBatch();
        
        // Для просвечивания сквозь блоки в 1.21.10 используем специфические типы
        bufferSource.endBatch(net.minecraft.client.renderer.RenderType.lightning());
        bufferSource.endBatch(net.minecraft.client.renderer.RenderType.debugQuads());
        // Добавим типы для текста. В 1.21.10 путь к текстуре может быть другим, 
        // используем стандартную текстуру шрифта если константа не найдена
        bufferSource.endBatch(net.minecraft.client.renderer.RenderType.textSeeThrough(net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/font/ascii.png")));
    }
}