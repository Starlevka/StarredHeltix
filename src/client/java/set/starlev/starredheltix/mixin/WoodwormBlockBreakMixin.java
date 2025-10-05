package set.starlev.starredheltix.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import set.starlev.starredheltix.util.woodworm.WoodwormCooldownVisualizer;

@Mixin(ClientPlayerInteractionManager.class)
public class WoodwormBlockBreakMixin {
    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void onBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // Get the player and item used
        ClientPlayerEntity player = net.minecraft.client.MinecraftClient.getInstance().player;
        if (player != null) {
            ItemStack heldItem = player.getMainHandStack();
            World world = player.getWorld();
            
            if (!world.isClient) {
                return; // Only run on client side
            }
            
            // Get the block name - using translation key instead of registry name
            String blockName = world.getBlockState(pos).getBlock().getTranslationKey();
            
            // Notify the cooldown visualizer
            WoodwormCooldownVisualizer.getInstance().onBlockBreak(player, heldItem, blockName);
        }
    }
}