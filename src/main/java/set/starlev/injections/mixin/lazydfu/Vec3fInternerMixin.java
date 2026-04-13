package set.starlev.injections.mixin.lazydfu;

import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.config.ConfigManager;

/**
 * Пропускает Guava Strong Interner для Vec3f.
 * Мы жертвуем несколькими МБ RAM для огромного ускорения параллелизма
 * на многоядерных CPU. Interner - серьёзное узкое место из-за блокировок,
 * когда до 28 потоков одновременно запекают модели.
 *
 * Исходный код: https://github.com/GUN2RAS/FastBoot
 */
@Mixin(targets = "net.minecraft.client.render.model.ModelBaker$Vec3fInternerImpl")
public class Vec3fInternerMixin {

    @Inject(method = "intern", at = @At("HEAD"), cancellable = true)
    private void skipInternerContention(Vector3fc vec, CallbackInfoReturnable<Vector3fc> cir) {
        if (ConfigManager.INSTANCE.getFeaturesSafe().getOptimization().getFastBoot().enabled && ConfigManager.INSTANCE.getFeaturesSafe().getOptimization().getFastBoot().skipVec3fInterner) {
            cir.setReturnValue(vec);
        }
    }
}
