package set.starlev.injections.mixins;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SpriteMapper;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.StarredHeltix;
import set.starlev.utils.detectors.DungeonDetector;

@Mixin(Sheets.class)
public class SheetsMixin {
    private static SpriteId GREEN_CHEST_SPRITE;

    @Inject(method = "chooseSprite", at = @At("HEAD"), cancellable = true)
    private static void onChestMaterial(ChestRenderState.ChestMaterialType materialType, ChestType type, CallbackInfoReturnable<SpriteId> cir) {
        // Оптимизация: сначала дешёвая проверка типа (без обращения к конфигу/детектору),
        // т.к. метод вызывается для каждого сундука каждый кадр.
        if (materialType != ChestRenderState.ChestMaterialType.REGULAR && materialType != ChestRenderState.ChestMaterialType.TRAPPED) {
            return;
        }
        try {
            if (StarredHeltix.Companion.getFeature().getDungeons().getVisuals().getGreenTextures().getGreenChests() && DungeonDetector.INSTANCE.isInDungeon()) {
                if (GREEN_CHEST_SPRITE == null) {
                    // Спрайт зелёного сундука из атласа сундуков
                    SpriteMapper mapper = Sheets.CHEST_MAPPER;
                    GREEN_CHEST_SPRITE = mapper.defaultNamespaceApply("green_chest");
                }
                cir.setReturnValue(GREEN_CHEST_SPRITE);
            }
        } catch (Throwable ignored) {
        }
    }
}
