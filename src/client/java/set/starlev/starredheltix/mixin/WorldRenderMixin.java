package set.starlev.starredheltix.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.*;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.starredheltix.render.RenderContext;
import set.starlev.starredheltix.render.RenderEvents;

@Mixin(WorldRenderer.class)
public class WorldRenderMixin {
    @Shadow @Final private BufferBuilderStorage bufferBuilders;
    @Unique
    private RenderContext ctx;

    @Inject(method = "render", at = @At("HEAD"))
    private void preRender(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        MatrixStack matrices = new MatrixStack();
        ctx = new RenderContext(matrices, camera, tickCounter.getFixedDeltaTicks());
    }

    @Inject(method = "method_62214", at = @At("RETURN"))
    private void postRender(GpuBufferSlice gpuBufferSlice, WorldRenderState worldRenderState, Profiler profiler, Matrix4f matrix4f, Handle handle, Handle handle2, boolean bl, Frustum frustum, Handle handle3, Handle handle4, CallbackInfo ci) {
        if (ctx != null) {
            RenderEvents.fireWorldRender(ctx);
        }
    }
}