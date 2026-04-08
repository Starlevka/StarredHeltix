package set.starlev.injections.mixin.render.level;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
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

@Mixin(LevelRenderer.class)
public class LevelRendererMixin implements set.starlev.utils.IWorldRenderer {
    @Shadow @Final private net.minecraft.client.renderer.RenderBuffers renderBuffers;

    @org.spongepowered.asm.mixin.Unique
    public net.minecraft.client.renderer.culling.Frustum starredheltix$capturedFrustum;

    @Override
    public net.minecraft.client.renderer.culling.Frustum starredheltix$getFrustum() {
        return starredheltix$capturedFrustum;
    }

    @Override
    public void starredheltix$setFrustum(Frustum frustum) {
        this.starredheltix$capturedFrustum = frustum;
    }

    @org.spongepowered.asm.mixin.Unique
    private final PoseStack starredheltix$reusablePoseStack = new PoseStack();

    @Inject(method = "method_62214", at = @At("HEAD"))
    private void onRenderWorldHead(GpuBufferSlice gpuBufferSlice, LevelRenderState worldRenderState, net.minecraft.util.profiling.ProfilerFiller profiler, Matrix4f matrix4f, ResourceHandle handle, ResourceHandle handle2, boolean bl, net.minecraft.client.renderer.culling.Frustum frustum, ResourceHandle handle3, ResourceHandle handle4, CallbackInfo ci) {
        this.starredheltix$capturedFrustum = frustum;
    }

    @Inject(method = "method_62214", at = @At("RETURN"))
    private void onRenderWorld(GpuBufferSlice gpuBufferSlice, LevelRenderState worldRenderState, net.minecraft.util.profiling.ProfilerFiller profiler, Matrix4f matrix4f, ResourceHandle handle, ResourceHandle handle2, boolean bl, net.minecraft.client.renderer.culling.Frustum frustum, ResourceHandle handle3, ResourceHandle handle4, CallbackInfo ci) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();

        // Переиспользуем PoseStack вместо создания нового каждый кадр
        PoseStack poseStack = this.starredheltix$reusablePoseStack;
        // Сбрасываем трансформации перед использованием
        poseStack.setIdentity();

        RenderContext context = new RenderContext(
            poseStack, 
            camera, 
            mc.getDeltaTracker().getGameTimeDeltaTicks(), 
            bufferSource,
            worldRenderState.cameraRenderState
        );

        RenderEvents.fireWorldRender(context);

        poseStack.pushPose();
        set.starlev.render.RenderEngine.renderWorld(poseStack, bufferSource, mc.getDeltaTracker().getGameTimeDeltaTicks());
        poseStack.popPose();

        // Флешим только необходимые буферы (lightning и debugQuads для ESP)
        // endBatch() без аргументов флешит все оставшиеся, поэтому отдельные вызовы избыточны
        bufferSource.endBatch(net.minecraft.client.renderer.RenderType.lightning());
        bufferSource.endBatch(net.minecraft.client.renderer.RenderType.debugQuads());
        bufferSource.endBatch();
    }
}
