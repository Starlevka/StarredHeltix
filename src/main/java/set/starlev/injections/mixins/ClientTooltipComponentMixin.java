package set.starlev.injections.mixins;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.features.skyblock.StoragePreviewClientTooltip;
import set.starlev.features.skyblock.StoragePreviewTooltip;

/**
 * @author Starlev
 * @reason Маппинг StoragePreviewTooltip (общий маркер из ItemStackMixin) в клиентский
 *         рендер сетки хранилища. Без этого ClientTooltipComponent.create() кидает
 *         IllegalArgumentException("Unknown TooltipComponent") — порт TooltipComponentMixin
 *         из heltix-mod под архитектуру 26.2 (typeSwitch вместо instanceof-цепочки).
 */
@Mixin(ClientTooltipComponent.class)
public interface ClientTooltipComponentMixin {

    @Inject(
        method = "create(Lnet/minecraft/world/inventory/tooltip/TooltipComponent;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void starredheltix$storagePreview(TooltipComponent component, CallbackInfoReturnable<ClientTooltipComponent> cir) {
        if (component instanceof StoragePreviewTooltip preview) {
            cir.setReturnValue(new StoragePreviewClientTooltip(preview));
        }
    }
}
