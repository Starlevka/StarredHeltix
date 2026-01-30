package set.starlev.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(LevelRenderer.class)
public class RenderOptimizationMixin {
    @Inject(method = "applyFrustum", at = @At("HEAD"))
    private void onApplyFrustum(Frustum frustum, CallbackInfo ci) {
        // Метод для оптимизации применения фрустума (отсечения невидимых объектов)
    }

    @Inject(method = "addWeatherPass", at = @At("HEAD"))
    private void optimizeWeather(FrameGraphBuilder frameGraphBuilder, Vec3 vec3, GpuBufferSlice gpuBufferSlice, CallbackInfo ci) {
        // Метод для оптимизации рендеринга погодных эффектов
    }
}
