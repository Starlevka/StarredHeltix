package set.starlev.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.core.registries.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.StarredHeltix;
import set.starlev.utils.detectors.BiomeIdentifier;

/**
 * Миксин для ClientLevel для предоставления ID биома и принудительных эффектов.
 */
@Mixin(net.minecraft.world.level.Level.class)
public abstract class ClientLevelMixin implements BiomeIdentifier {

    @Override
    public String starlev$getBiomeId(BlockPos pos) {
        net.minecraft.world.level.Level level = (net.minecraft.world.level.Level) (Object) this;
        Holder<Biome> biomeHolder = level.getBiomeManager().getBiome(pos);
        return biomeHolder.unwrapKey()
                .map(key -> key.location().toString())
                .orElse("unknown");
    }

    @Inject(method = "getRainLevel(F)F", at = @At("HEAD"), cancellable = true)
    private void onGetRainLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
        net.minecraft.world.level.Level level = (net.minecraft.world.level.Level) (Object) this;
        if (level.isClientSide && StarredHeltix.getFeature().getVisuals().getNewYear().getWinterAtmosphere()) {
            cir.setReturnValue(1.0f);
        }
    }

    @Inject(method = "isRaining()Z", at = @At("HEAD"), cancellable = true)
    private void onIsRaining(CallbackInfoReturnable<Boolean> cir) {
        net.minecraft.world.level.Level level = (net.minecraft.world.level.Level) (Object) this;
        if (level.isClientSide && StarredHeltix.getFeature().getVisuals().getNewYear().getWinterAtmosphere()) {
            cir.setReturnValue(true);
        }
    }
}
