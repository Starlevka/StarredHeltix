package set.starlev.injections.mixin.lazydfu;

import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.config.ConfigManager;

/**
 * Максимизирует количество потоков для фонового исполнителя.
 * В современных версиях это уже неограниченно, но мы форсируем это,
 * чтобы убедиться, что другие моды не уменьшают это число.
 * Используем все ядра без вычитания 1 для максимальной скорости загрузки.
 *
 * Исходный код: https://github.com/GUN2RAS/FastBoot
 */
@Mixin(Util.class)
public abstract class UtilMixin {

    @Inject(method = "maxAllowedExecutorThreads", at = @At("HEAD"), cancellable = true)
    private static void maximizeThreads(CallbackInfoReturnable<Integer> cir) {
        if (ConfigManager.INSTANCE.getFeaturesSafe().getOptimization().getFastBoot().enabled && ConfigManager.INSTANCE.getFeaturesSafe().getOptimization().getFastBoot().maximizeThreads) {
            cir.setReturnValue(Math.max(Runtime.getRuntime().availableProcessors(), 4));
        }
    }
}
