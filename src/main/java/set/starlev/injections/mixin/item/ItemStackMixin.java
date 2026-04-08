package set.starlev.injections.mixin.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.secret.config.SecretMenuManager;
import set.starlev.secret.features.SecretFunFeatures;

import java.util.List;
import java.util.ArrayList;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void onGetHoverName(CallbackInfoReturnable<Component> cir) {
        if (!SecretMenuManager.INSTANCE.isConfigInitialized()) return;
        
        Component original = cir.getReturnValue();
        if (original == null) return;
        
        // В предметах эффекты применяются всегда (force=true), если включены в конфиге
        Component modified = SecretFunFeatures.processComponent(original, true);
        if (modified != original) {
            cir.setReturnValue(modified);
        }
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void onGetTooltipLines(net.minecraft.world.item.Item.TooltipContext context, net.minecraft.world.entity.player.Player player, net.minecraft.world.item.TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        if (!SecretMenuManager.INSTANCE.isConfigInitialized()) return;
        
        List<Component> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;
        
        List<Component> modified = new ArrayList<>(original.size());
        boolean changed = false;
        
        for (Component line : original) {
            // В предметах эффекты применяются всегда (force=true), если включены в конфиге
            Component modLine = SecretFunFeatures.processComponent(line, true);
            if (modLine != line) {
                changed = true;
            }
            modified.add(modLine);
        }
        
        if (changed) {
            cir.setReturnValue(modified);
        }
    }
}
