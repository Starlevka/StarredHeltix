package com.modfast.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.IdentityHashMap;
import java.util.Map;

@Mixin(BakedModelManager.class)
public class BakedModelManagerMixin {

    /**
     * Replaces the extremely slow `toStateMap` method which formats a logger string
     * for every missing blockstate variant across the entire registry.
     * We bypass the formatting and the sequential putIfAbsent overhead completely.
     */
    @Inject(method = "toStateMap", at = @At("HEAD"), cancellable = true)
    private static void fastToStateMap(Map<BlockState, BlockStateModel> blockStateModels, BlockStateModel missingModel, CallbackInfoReturnable<Map<BlockState, BlockStateModel>> cir) {
        Map<BlockState, BlockStateModel> mutableMap = new IdentityHashMap<>(blockStateModels);
        for (Block block : Registries.BLOCK) {
            for (BlockState state : block.getStateManager().getStates()) {
                if (!mutableMap.containsKey(state)) {
                    mutableMap.put(state, missingModel);
                }
            }
        }
        cir.setReturnValue(mutableMap);
    }
}
