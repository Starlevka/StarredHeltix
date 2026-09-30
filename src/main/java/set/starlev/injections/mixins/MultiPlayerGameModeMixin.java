package set.starlev.injections.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.features.foraging.TreeCapCooldown;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import set.starlev.utils.detectors.StatsDetectors;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
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

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void onDestroyBlockReturn(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            StatsDetectors.INSTANCE.registerBlockBreak();
            if (set.starlev.scripting.SHScriptEngine.INSTANCE.isLoaded()) {
                set.starlev.scripting.SHScriptEngine.INSTANCE.fireTrigger("blockBreak", pos.getX(), pos.getY(), pos.getZ());
            }
        }
    }

    private boolean isLog(String blockName) {
        // Проверяем стандартные логи Minecraft и похожие варианты
        return blockName.contains("log") || blockName.contains("wood");
    }
}
