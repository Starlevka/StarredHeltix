package set.starlev.injections.mixin.lazydfu;

import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import set.starlev.config.ConfigManager;

import java.util.IdentityHashMap;
import java.util.Map;

@Mixin(ModelManager.class)
public class BakedModelManagerMixin {

    /**
     * Заменяет крайне медленный метод toStateMap, который форматирует строку логгера
     * для каждого отсутствующего варианта blockstate по всему реестру.
     * Мы полностью обходим форматирование и накладные расходы sequential putIfAbsent.
     * 
     * Исходный код: https://github.com/GUN2RAS/FastBoot
     */
    @Inject(method = "createBlockStateToModelDispatch", at = @At("HEAD"), cancellable = true)
    private static void fastToStateMap(Map<BlockState, BlockStateModel> blockStateModels, BlockStateModel missingModel, CallbackInfoReturnable<Map<BlockState, BlockStateModel>> cir) {
        // Проверка конфига
        try {
            if (!ConfigManager.INSTANCE.getFeaturesSafe().getOptimization().getFastBoot().enabled) return;
            if (!ConfigManager.INSTANCE.getFeaturesSafe().getOptimization().getFastBoot().fastModelLoading) return;
        } catch (Throwable ignored) { return; }

        Map<BlockState, BlockStateModel> mutableMap = new IdentityHashMap<>(blockStateModels);
        for (Block block : BuiltInRegistries.BLOCK) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                if (!mutableMap.containsKey(state)) {
                    mutableMap.put(state, missingModel);
                }
            }
        }
        cir.setReturnValue(mutableMap);
    }
}
