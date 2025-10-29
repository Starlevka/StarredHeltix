package set.starlev.starredheltix.mixin.ui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.starredheltix.client.StarredHeltixClient;
// import set.starlev.starredheltix.util.solver.exptable.ExperimentTableMemoryManager;

import java.util.Map;

@Mixin(Screen.class)
public class ScreenCloseMixin {
    @Inject(method = "close", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {
        // When any screen is closed, check if it's an experiment table and clear memory
        Screen screen = (Screen) (Object) this;
        if (screen instanceof GenericContainerScreen) {
            GenericContainerScreen containerScreen = (GenericContainerScreen) screen;
            String title = containerScreen.getTitle().getString();
            
            // Check if this was an experiment table or if the feature is enabled for all containers
            boolean isExperimentTable = title.equals("Стол экспериментов");
            boolean isExperimentFeatureEnabled = StarredHeltixClient.CONFIG.general.enabled;
            
            // Always drop remembered items for experiment tables
            if (isExperimentTable) {
                dropRememberedItems(containerScreen);
                // ExperimentTableMemoryManager.clearMemory();
            }
            // For other containers, only clear memory if the feature is enabled
            else if (isExperimentFeatureEnabled) {
                // ExperimentTableMemoryManager.clearMemory();
            }
        }
    }
    
    /**
     * Drop all remembered items when closing the experiment table
     */
    private void dropRememberedItems(GenericContainerScreen containerScreen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Get the player's inventory
        PlayerEntity player = client.player;
        
        // Get remembered items
        // Map<Integer, ItemStack> rememberedItems = ExperimentTableMemoryManager.getAllRememberedItems();
        
        // Drop each remembered item
        // for (Map.Entry<Integer, ItemStack> entry : rememberedItems.entrySet()) {
        //     int slotId = entry.getKey();
        //     ItemStack itemStack = entry.getValue();
            
        //     if (!itemStack.isEmpty()) {
        //         // Drop the item in the world
        //         player.dropItem(itemStack, true, false);
                
        //         // Debug information
        //         if (StarredHeltixClient.CONFIG.general.debugMode) {
        //             player.sendMessage(
        //                 Text.literal("§aDebug: предмет выброшен из слота " + slotId + ": " + itemStack.getName().getString()).styled(style -> style.withColor(0x55FF55)),
        //                 true);
        //         }
        //     }
        // }
    }
}