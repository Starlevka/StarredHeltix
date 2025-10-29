package set.starlev.starredheltix.mixin.experiment;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import set.starlev.starredheltix.client.StarredHeltixClient;
// import set.starlev.starredheltix.util.solver.exptable.ExperimentTableMemoryManager;

@Mixin(HandledScreen.class)
public class ExperimentTableRenderMixin {
    // Keep track of the last known container title to detect when it changes
    private static String lastContainerTitle = "";
    // Keep track of the number of slots to detect when container size changes
    private static int lastSlotCount = 0;
    
    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void onDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        Screen currentScreen = ((Screen) (Object) this);
        if (!(currentScreen instanceof GenericContainerScreen)) {
            return;
        }
        
        GenericContainerScreen containerScreen = (GenericContainerScreen) currentScreen;
        String title = containerScreen.getTitle().getString();
        int slotCount = containerScreen.getScreenHandler().slots.size();
        
        // Check if container has changed significantly
        if (!title.equals(lastContainerTitle) || slotCount != lastSlotCount) {
            // Container has changed, clear memory
            // ExperimentTableMemoryManager.clearMemory();
            lastContainerTitle = title;
            lastSlotCount = slotCount;
            
            // Debug information
            if (StarredHeltixClient.CONFIG.general.debugMode && MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                    Text.literal("§e[DEBUG] Container changed during render, memory cleared"),
                    true);
            }
        }
        
        // Check if this is explicitly an experiment table or if the feature is enabled for all containers
        boolean isExperimentTable = title.equals("Стол экспериментов");
        boolean isExperimentFeatureEnabled = StarredHeltixClient.CONFIG.general.enabled;
        
        // Only proceed if it's an experiment table or if the feature is enabled for all containers
        if (!isExperimentTable && !isExperimentFeatureEnabled) {
            return;
        }

        // Only highlight slots that were clicked and are part of the solution
        // if (ExperimentTableMemoryManager.wasSlotClicked(slot.id) &&
        //     ExperimentTableMemoryManager.shouldHighlightSlot(slot.id)) {
        //     // Push matrix to render on top of everything
        //     context.getMatrices().push();
        //     context.getMatrices().translate(0, 0, 500); // Move to front layer

        //     // Use the slot number as display text
        //     String displayText = String.valueOf(slot.id);

        //     int x = slot.x;
        //     int y = slot.y;

        //     // Draw shadow (black text slightly offset)
        //     context.drawTextWithShadow(
        //         MinecraftClient.getInstance().textRenderer,
        //         Text.literal(displayText),
        //         x + 1,
        //         y + 1,
        //         0x000000); // Black shadow

        //     // Draw main text with bright color for solution slots
        //     context.drawTextWithShadow(
        //         MinecraftClient.getInstance().textRenderer,
        //         Text.literal("§a" + displayText), // Green color for solution slots
        //         x,
        //         y,
        //         0x55FFFF); // Cyan color as fallback

        //     // Pop matrix to restore previous state
        //     context.getMatrices().pop();
        // }

        // Also show clicked slots that are NOT part of solution with different color
        // else if (ExperimentTableMemoryManager.wasSlotClicked(slot.id)) {
        //     // Push matrix to render on top of everything
        //     context.getMatrices().push();
        //     context.getMatrices().translate(0, 0, 500); // Move to front layer

        //     // Use the slot number as display text
        //     String displayText = String.valueOf(slot.id);
            
        //     int x = slot.x;
        //     int y = slot.y;
            
        //     // Draw shadow (black text slightly offset)
        //     context.drawTextWithShadow(
        //         MinecraftClient.getInstance().textRenderer,
        //         Text.literal(displayText),
        //         x + 1,
        //         y + 1,
        //         0x000000); // Black shadow

        //     // Draw main text with different color for clicked but not solution slots
        //     context.drawTextWithShadow(
        //         MinecraftClient.getInstance().textRenderer,
        //         Text.literal("§7" + displayText), // Gray color for clicked but not solution
        //         x,
        //         y,
        //         0x55FFFF); // Cyan color as fallback
                
        //     // Pop matrix to restore previous state
        //     context.getMatrices().pop();
        // }
    }

    // Inject to detect slot clicks
    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"))
    private void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        Screen currentScreen = ((Screen) (Object) this);
        if (!(currentScreen instanceof GenericContainerScreen) || slot == null) {
            return;
        }

        GenericContainerScreen containerScreen = (GenericContainerScreen) currentScreen;
        String title = containerScreen.getTitle().getString();

        // Check if this is an experiment table
        boolean isExperimentTable = title.equals("Стол экспериментов");
        boolean isExperimentFeatureEnabled = StarredHeltixClient.CONFIG.general.enabled;

        // Only proceed if it's an experiment table or if the feature is enabled for all containers
        if (!isExperimentTable && !isExperimentFeatureEnabled) {
            return;
        }

        // Remember the clicked slot and its item
        // if (slot.hasStack()) {
        //     ExperimentTableMemoryManager.rememberClickedSlot(slot.id, slot.getStack());

        //     // Debug information
        //     if (StarredHeltixClient.CONFIG.general.debugMode && MinecraftClient.getInstance().player != null) {
        //         String itemName = slot.getStack().getName().getString();
        //         MinecraftClient.getInstance().player.sendMessage(
        //             Text.literal("§e[DEBUG] Clicked slot " + slot.id + ": " + itemName),
        //             true);
        //     }
        // }
    }
}