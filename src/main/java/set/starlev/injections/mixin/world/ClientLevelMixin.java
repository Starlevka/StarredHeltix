package set.starlev.injections.mixin.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.StarredHeltix;
import set.starlev.utils.detectors.BiomeIdentifier;

/**
    * Миксин для ClientLevel для предоставления ID биома и принудительных эффектов.
    */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin implements BiomeIdentifier {

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void onAddDestroyBlockEffect(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (StarredHeltix.Companion.getFeature().getOptimization().getVisualOptimizations().getDisableBlockBreakingParticles()) {
            ci.cancel();
        }
    }

    @Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
    private void onAddBreakingBlockEffect(BlockPos pos, Direction direction, CallbackInfo ci) {
        if (StarredHeltix.Companion.getFeature().getOptimization().getVisualOptimizations().getDisableBlockBreakingParticles()) {
            ci.cancel();
        }
    }
    
    @Override
    public String starlev$getBiomeId(BlockPos pos) {
        if (!((Object)this instanceof ClientLevel)) return "unknown";
        ClientLevel level = (ClientLevel) (Object) this;
        Holder<Biome> biomeHolder = level.getBiomeManager().getBiome(pos);
        return biomeHolder.unwrapKey()
                .map(key -> key.location().toString())
                .orElse("unknown");
    }
}
