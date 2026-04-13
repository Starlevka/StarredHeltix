package set.starlev.injections.mixin.lazydfu;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.config.ConfigManager;

/**
 * Агрессивная оптимизация DFU для игры только на серверах.
 * Полностью отключает создание DataFixer на клиенте.
 *
 * ПРЕДУПРЕЖДЕНИЕ: Эта оптимизация НЕСОВМЕСТИМА с одиночными мирами!
 * Используйте ТОЛЬКО если играете только на серверах.
 *
 * На серверах конвертация миров происходит на стороне сервера,
 * поэтому клиенту DataFixer не нужен.
 *
 * Исходный код: https://github.com/GUN2RAS/FastBoot
 */
@Mixin(targets = "net.minecraft.util.datafix.DataFixers")
public class DataFixersClientMixin {

    @Inject(method = "createFixerUpper", at = @At("HEAD"), cancellable = true)
    private static void skipDfuCreation(CallbackInfoReturnable<DataFixerBuilder.Result> cir) {
        if (ConfigManager.INSTANCE.getFeaturesSafe().getOptimization().getFastBoot().disableDfuOnClient) {
            // Возвращаем пустой Result с минимальной версией
            DataFixerBuilder builder = new DataFixerBuilder(0);
            cir.setReturnValue(builder.build());
        }
    }
}
