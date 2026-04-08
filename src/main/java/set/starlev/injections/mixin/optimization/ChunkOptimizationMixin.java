package set.starlev.injections.mixin.optimization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import set.starlev.StarredHeltix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionRenderDispatcher.class)
public class ChunkOptimizationMixin {

    @Inject(method = "runTask", at = @At("HEAD"))
    private void onRunTaskHead(CallbackInfo ci) {
        // Проверка конфига
        try {
            if (!StarredHeltix.Companion.getFeature().getOptimization().getRenderOptimizations().getChunkOptimization()) return;
        } catch (Throwable ignored) { return; }

        if (starredheltix$prioritySet.get()) return;
        starredheltix$prioritySet.set(true);

        try {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.isSameThread()) return;
        } catch (Throwable ignored) {
        }

        try {
            Thread thread = Thread.currentThread();
            int desired = Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1);
            if (thread.getPriority() > desired) {
                thread.setPriority(desired);
            }
        } catch (Throwable ignored) {
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private static final ThreadLocal<Boolean> starredheltix$prioritySet = ThreadLocal.withInitial(() -> false);
}
