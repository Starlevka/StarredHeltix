package set.starlev.mixin.client;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionRenderDispatcher.class)
public class ChunkOptimizationMixin {

    @Inject(method = "runTask", at = @At("HEAD"))
    private void onScheduleTasks(CallbackInfo ci) {
        // Логика планирования задач
    }

    @Inject(method = "runTask", at = @At("HEAD"))
    private void forceBackgroundPriority(CallbackInfo ci) {
        // Принудительный фоновый приоритет
    }
}
