package set.starlev.injections.mixin.lazydfu;

import com.mojang.datafixers.DSL.TypeReference;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.config.ConfigManager;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Безопасная оптимизация DFU для версии 1.21.10.
 * Пропускает предварительную оптимизацию (компиляцию схем) DataFixerUpper,
 * что экономит время при запуске игры.
 *
 * Эта оптимизация безопасна, потому что:
 * - Не отключает сами fixers (они всё равно работают при загрузке старых миров)
 * - Пропускает только предварительную оптимизацию схем
 * - Новые миры загружаются нормально без этой оптимизации
 *
 * Исходный код: https://github.com/GUN2RAS/FastBoot
 */
@Mixin(targets = "net.minecraft.util.datafix.DataFixers")
public class DataFixersMixin {

    @Inject(method = "optimize", at = @At("HEAD"), cancellable = true)
    private static void skipDfuOptimization(Set<TypeReference> types, CallbackInfoReturnable<CompletableFuture<?>> cir) {
        if (ConfigManager.INSTANCE.getFeaturesSafe().getOptimization().getFastBoot().skipDfuOptimization) {
            cir.setReturnValue(CompletableFuture.completedFuture(null));
        }
    }
}
