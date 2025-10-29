package set.starlev.starredheltix.mixin.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.starredheltix.util.qol.TreeCapCooldownVisualizer;

@Mixin(ClientPlayerInteractionManager.class)
public class TreeCapBlockBreakMixin {
    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void onBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // Get the player and item used
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            ItemStack heldItem = player.getMainHandStack();
            World world = player.getEntityWorld();
            
            if (!world.isClient()) {
                return; // Only run on client side
            }
            
            // Get the block name - using translation key instead of registry name
            String blockName = world.getBlockState(pos).getBlock().getTranslationKey();
            
            // Notify the cooldown visualizer
            TreeCapCooldownVisualizer.getInstance().onBlockBreak(player, heldItem, blockName);
        }
    }
}
