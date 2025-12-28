package set.starlev.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import set.starlev.features.foraging.TreeCapCooldown;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;

@Mixin(MultiPlayerGameMode.class)
public class TreeCapBlockBreakMixin {
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level world = mc.level;
        
        if (player != null && world != null) {
            String blockName = world.getBlockState(pos).getBlock().getDescriptionId();
            
            // Проверяем что это логи (дерево)
            if (isLog(blockName)) {
                TreeCapCooldown.INSTANCE.onLogBreak(blockName);
            }
        }
    }

    private boolean isLog(String blockName) {
        // Проверяем стандартные логи Minecraft и похожие варианты
        return blockName.contains("log") || blockName.contains("wood");
    }
}
