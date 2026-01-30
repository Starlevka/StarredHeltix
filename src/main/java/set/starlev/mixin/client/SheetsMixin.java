package set.starlev.mixin.client;

import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.StarredHeltix;
import set.starlev.utils.detectors.DungeonDetector;

@Mixin(Sheets.class)
public class SheetsMixin {
    private static Material GREEN_CHEST_MATERIAL;

    @Inject(method = "chooseMaterial(Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState$ChestMaterialType;Lnet/minecraft/world/level/block/state/properties/ChestType;)Lnet/minecraft/client/resources/model/Material;", at = @At("HEAD"), cancellable = true)
    private static void onChestMaterial(ChestRenderState.ChestMaterialType materialType, ChestType type, CallbackInfoReturnable<Material> cir) {
        if (StarredHeltix.Companion.getFeature().getDungeons().getVisuals().getGreenChests() && DungeonDetector.INSTANCE.isInDungeon()) {
            // Для обычных и ловушечных сундуков используем зеленую текстуру
            if (materialType == ChestRenderState.ChestMaterialType.REGULAR || materialType == ChestRenderState.ChestMaterialType.TRAPPED) {
                if (GREEN_CHEST_MATERIAL == null) {
                    // Используем тот же атлас, что и для обычных сундуков
                    GREEN_CHEST_MATERIAL = new Material(Sheets.CHEST_SHEET, ResourceLocation.withDefaultNamespace("entity/chest/green_chest"));
                }
                cir.setReturnValue(GREEN_CHEST_MATERIAL);
            }
        }
    }
}
