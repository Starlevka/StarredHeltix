package set.starlev.starredheltix.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.starredheltix.client.StarredHeltixClient;
import set.starlev.starredheltix.util.solver.exptable.ExperimentTableMemoryManager;
import set.starlev.starredheltix.util.solver.exptable.ExperimentTableSolver;

@Mixin(HandledScreen.class)
public class ExperimentTableMixin {
    
    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"))
    private void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        // Check if this is an experiment table screen
        Screen currentScreen = ((Screen) (Object) this);
        if (!(currentScreen instanceof GenericContainerScreen)) {
            return;
        }
        
        GenericContainerScreen containerScreen = (GenericContainerScreen) currentScreen;
        String title = containerScreen.getTitle().getString();
        
        // Check if this is explicitly an experiment table or if the feature is enabled for all containers
        boolean isExperimentTable = title.equals("Стол экспериментов");
        boolean isExperimentFeatureEnabled = StarredHeltixClient.CONFIG.general.enabled;
        
        // Only proceed if it's an experiment table or if the feature is enabled for all containers
        if (!isExperimentTable && !isExperimentFeatureEnabled) {
            return;
        }
        
        // Track all slot changes, not just left clicks, to better handle custom items on servers
        if (slot != null) {
            // Get the item in the slot
            ItemStack itemStack = slot.getStack();
            
            // Remember this item regardless of the action type
            ExperimentTableMemoryManager.rememberItem(slot.id, itemStack);
            
            // Debug information
            if (StarredHeltixClient.CONFIG.general.debugMode) {
                MinecraftClient.getInstance().player.sendMessage(
                    Text.of("§e[DEBUG] Remembered item in slot " + slot.id + ": " + itemStack.getName().getString()), 
                    true);
            }
        }
    }
}