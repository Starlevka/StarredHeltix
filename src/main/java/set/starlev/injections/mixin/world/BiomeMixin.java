package set.starlev.injections.mixin.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.secret.features.SecretFunFeatures;

@Mixin(Biome.class)
public class BiomeMixin {
    @Inject(method = "getPrecipitationAt(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/biome/Biome$Precipitation;", at = @At("HEAD"), cancellable = true)
    private void onGetPrecipitationAt(BlockPos pos, int seaLevel, CallbackInfoReturnable<Biome.Precipitation> cir) {
        if (SecretFunFeatures.INSTANCE.isSnowEverywhereEnabled()) {
            cir.setReturnValue(Biome.Precipitation.SNOW);
        }
    }

    @Inject(method = "hasPrecipitation()Z", at = @At("HEAD"), cancellable = true)
    private void onHasPrecipitation(CallbackInfoReturnable<Boolean> cir) {
        if (SecretFunFeatures.INSTANCE.isSnowEverywhereEnabled()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "coldEnoughToSnow(Lnet/minecraft/core/BlockPos;I)Z", at = @At("HEAD"), cancellable = true)
    private void onColdEnoughToSnow(BlockPos pos, int seaLevel, CallbackInfoReturnable<Boolean> cir) {
        if (SecretFunFeatures.INSTANCE.isSnowEverywhereEnabled()) {
            cir.setReturnValue(true);
        }
    }
}
