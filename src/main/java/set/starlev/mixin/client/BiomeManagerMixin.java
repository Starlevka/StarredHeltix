package set.starlev.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.StarredHeltix;
import set.starlev.secret.features.SecretFunFeatures;

@Mixin(BiomeManager.class)
public class BiomeManagerMixin {
    @Inject(method = "getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;", at = @At("RETURN"), cancellable = true)
    private void onGetBiome(BlockPos pos, CallbackInfoReturnable<Holder<Biome>> cir) {
        if (SecretFunFeatures.INSTANCE.isSnowEverywhereEnabled()) {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null) {
                client.level.registryAccess().lookup(Registries.BIOME)
                        .flatMap(registry -> registry.get(Biomes.SNOWY_PLAINS))
                        .ifPresent(cir::setReturnValue);
            }
        }
    }
}
